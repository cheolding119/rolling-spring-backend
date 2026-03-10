package com.rolling.api.domain.tournament.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rolling.api.domain.tournament.model.TournamentModel;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class HeroesOfJiuJitsuCrawlerService {

    private static final int REQUEST_TIMEOUT_MILLIS = 10_000;
    private static final String USER_AGENT = "Mozilla/5.0 (compatible; RollingCrawler/1.0)";
    private static final ZoneId SEOUL_ZONE = ZoneId.of("Asia/Seoul");
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Pattern KOREAN_DATE_PATTERN =
            Pattern.compile("(20\\d{2}\\s*년\\s*\\d{1,2}\\s*월\\s*\\d{1,2}\\s*일)");
    private static final Pattern DOTTED_DATE_PATTERN =
            Pattern.compile("((\\d{2})\\.(\\d{2})\\.(\\d{2}))");
    private static final Pattern MAP_QUERY_PATTERN = Pattern.compile("[?&]q=([^&]+)");
    private static final Pattern JSON_PAYLOAD_LINE_PATTERN = Pattern.compile("^\\d+:(\\{.*)$");
    private static final Pattern NUMERIC_ID_PATTERN = Pattern.compile("(\\d+)");

    public TournamentModel parseListCardHtml(String html, String detailUrl) {
        Objects.requireNonNull(html, "html must not be null");
        Document document = Jsoup.parseBodyFragment(html, detailUrl);
        Element anchor = document.selectFirst("a[href]");
        if (anchor == null) {
            log.warn("HeroesOfJiuJitsu list card parsing skipped: anchor not found. detailUrl={}", detailUrl);
            return null;
        }

        TournamentModel tournamentModel = new TournamentModel();
        tournamentModel.setTitle(extractTitle(anchor));
        tournamentModel.setCompetitionDate(extractCompetitionDate(anchor));
        tournamentModel.setRegistrationDeadline(extractRegistrationDeadline(anchor));
        tournamentModel.setLocation(extractLocation(anchor));
        tournamentModel.setPosterUrl(extractPosterUrl(anchor));
        tournamentModel.setApplyLink(detailUrl);
        return tournamentModel;
    }

    public TournamentModel crawlDetailPage(String detailUrl, TournamentModel seedModel) {
        try {
            Document document = Jsoup.connect(detailUrl)
                    .userAgent(USER_AGENT)
                    .timeout(REQUEST_TIMEOUT_MILLIS)
                    .get();
            return parseDetailDocument(document, detailUrl, seedModel);
        } catch (IOException | IllegalArgumentException e) {
            log.warn("Failed to crawl HeroesOfJiuJitsu detail page. url={}", detailUrl, e);
            return seedModel;
        }
    }

    public List<TournamentModel> parseServerActionResponse(String responseBody, String listPageUrl) {
        Objects.requireNonNull(responseBody, "responseBody must not be null");

        String payload = extractJsonPayload(responseBody);
        if (payload == null) {
            log.warn("HeroesOfJiuJitsu server action payload not found");
            return List.of();
        }

        try {
            JsonNode root = OBJECT_MAPPER.readTree(payload);
            JsonNode listData = root.path("listData");
            if (!listData.isArray()) {
                log.warn("HeroesOfJiuJitsu listData is not an array");
                return List.of();
            }

            List<TournamentModel> tournaments = new ArrayList<>();
            for (JsonNode row : listData) {
                TournamentModel tournamentModel = toTournamentModel(row, listPageUrl);
                if (tournamentModel != null) {
                    tournaments.add(tournamentModel);
                }
            }
            return tournaments;
        } catch (Exception e) {
            log.warn("HeroesOfJiuJitsu server action parsing failed", e);
            return List.of();
        }
    }

    TournamentModel parseDetailHtml(String html, String detailUrl, TournamentModel seedModel) {
        Objects.requireNonNull(html, "html must not be null");
        return parseDetailDocument(Jsoup.parse(html, detailUrl), detailUrl, seedModel);
    }

    private TournamentModel parseDetailDocument(Document document, String detailUrl, TournamentModel seedModel) {
        TournamentModel tournamentModel = seedModel != null ? seedModel : new TournamentModel();
        if (tournamentModel.getApplyLink() == null || tournamentModel.getApplyLink().isBlank()) {
            tournamentModel.setApplyLink(detailUrl);
        }

        String title = extractTitle(document);
        if (title != null) {
            tournamentModel.setTitle(title);
        }

        String posterUrl = extractPosterUrl(document);
        if (posterUrl != null) {
            tournamentModel.setPosterUrl(posterUrl);
        }

        String organizer = extractOrganizer(document);
        if (organizer != null) {
            tournamentModel.setOrganizer(organizer);
        }

        String competitionDate = extractCompetitionDate(document);
        if (competitionDate != null) {
            tournamentModel.setCompetitionDate(competitionDate);
        }

        String registrationDeadline = extractRegistrationDeadline(document);
        if (registrationDeadline != null) {
            tournamentModel.setRegistrationDeadline(registrationDeadline);
        }

        String location = extractLocation(document);
        if (location != null) {
            tournamentModel.setLocation(location);
        }

        return tournamentModel;
    }

    private TournamentModel toTournamentModel(JsonNode row, String listPageUrl) {
        Long postId = extractPostId(text(row, "id"));
        if (postId == null || postId == 97L) {
            return null;
        }

        JsonNode meta = row.path("meta");
        if (meta.isMissingNode()) {
            return null;
        }

        if (!isVisible(row, meta) || !isActive(meta)) {
            return null;
        }

        TournamentModel tournamentModel = new TournamentModel();
        tournamentModel.setTitle(normalize(text(row, "title")));
        tournamentModel.setOrganizer(firstNonBlank(text(meta, "coHost"), text(meta, "organizer")));
        tournamentModel.setPosterUrl(normalizeUrl(text(meta, "thumbnail")));
        tournamentModel.setCompetitionDate(toSeoulIsoDate(firstNonBlank(text(meta, "eventDate"), text(meta, "startDate"))));
        tournamentModel.setRegistrationDeadline(toSeoulIsoDate(text(meta, "applicaitonEndDate")));
        tournamentModel.setLocation(normalize(text(meta, "venue")));
        tournamentModel.setApplyLink(buildDetailUrl(listPageUrl, postId));
        return tournamentModel;
    }

    private boolean isVisible(JsonNode row, JsonNode meta) {
        return !row.path("isPrivate").asBoolean(false)
                && row.path("isApproved").asBoolean(true)
                && meta.path("isPublished").asBoolean(true);
    }

    private boolean isActive(JsonNode meta) {
        Instant now = Instant.now();
        Instant start = parseInstant(text(meta, "applicaitonStartDate"));
        Instant end = parseInstant(text(meta, "applicaitonEndDate"));

        if (end != null && now.isAfter(end)) {
            return false;
        }

        if (start != null && now.isBefore(start)) {
            return false;
        }

        return true;
    }

    private String extractTitle(Element anchor) {
        Element titleElement = anchor.selectFirst("h3");
        if (titleElement != null) {
            return normalize(titleElement.text());
        }

        Element imageElement = anchor.selectFirst("img[alt]");
        return imageElement == null ? null : normalize(imageElement.attr("alt"));
    }

    private String extractTitle(Document document) {
        for (Element heading : document.select("h1, h2, h3")) {
            String text = normalize(heading.text());
            if (isUsableTitle(text)) {
                return text;
            }
        }

        return null;
    }

    private String extractCompetitionDate(Element anchor) {
        for (Element element : anchor.select("span.font-bold, div")) {
            String text = normalize(element.text());
            Matcher matcher = KOREAN_DATE_PATTERN.matcher(text);
            if (matcher.find()) {
                return normalize(matcher.group(1));
            }
        }
        return null;
    }

    private String extractCompetitionDate(Document document) {
        return extractDateByLabel(document, "대회 일자");
    }

    private String extractRegistrationDeadline(Element anchor) {
        String normalizedText = normalize(anchor.text());
        Matcher matcher = DOTTED_DATE_PATTERN.matcher(normalizedText);
        while (matcher.find()) {
            String matched = matcher.group(0);
            if (normalizedText.contains("신청 마감: " + matched)
                    || normalizedText.contains("마감: " + matched)) {
                return toKoreanDate(matched);
            }
        }
        return null;
    }

    private String extractRegistrationDeadline(Document document) {
        String labelScopedText = extractLabelScopedText(document, "신청 마감");
        if (labelScopedText == null) {
            return null;
        }

        Matcher koreanDateMatcher = KOREAN_DATE_PATTERN.matcher(labelScopedText);
        if (koreanDateMatcher.find()) {
            return normalize(koreanDateMatcher.group(1));
        }

        Matcher dottedDateMatcher = DOTTED_DATE_PATTERN.matcher(labelScopedText);
        if (dottedDateMatcher.find()) {
            return toKoreanDate(dottedDateMatcher.group(1));
        }

        return null;
    }

    private String extractLocation(Element anchor) {
        for (Element element : anchor.select("span.font-bold")) {
            String text = normalize(element.text());
            if (text.isBlank()) {
                continue;
            }
            if (KOREAN_DATE_PATTERN.matcher(text).find()) {
                continue;
            }
            if (text.contains("신청 마감")) {
                continue;
            }
            return text;
        }
        return null;
    }

    private String extractLocation(Document document) {
        Element mapFrame = document.selectFirst("iframe[src*='maps.google.com/maps']");
        if (mapFrame == null) {
            return null;
        }

        String src = normalizeUrl(mapFrame.attr("src"));
        if (src == null) {
            return null;
        }

        Matcher matcher = MAP_QUERY_PATTERN.matcher(src);
        if (!matcher.find()) {
            return null;
        }

        return normalize(URLDecoder.decode(matcher.group(1), StandardCharsets.UTF_8));
    }

    private String extractOrganizer(Document document) {
        for (Element element : document.select("span, div, p, strong")) {
            String label = normalize(element.text());
            if (!"공동주관".equals(label) && !"주최".equals(label) && !"주최/주관".equals(label)) {
                continue;
            }

            Element parent = element.parent();
            if (parent == null) {
                continue;
            }

            for (Element child : parent.children()) {
                String text = normalize(child.text());
                if (!text.isBlank() && !text.equals(label)) {
                    return text;
                }
            }
        }

        return null;
    }

    private String extractPosterUrl(Element root) {
        for (Element image : root.select("img[src]")) {
            String src = normalizeUrl(image.attr("abs:src"));
            if (src == null) {
                src = normalizeUrl(image.attr("src"));
            }

            if (src != null && !src.contains("/assets/logo/")) {
                return src;
            }
        }

        return null;
    }

    private String extractDateByLabel(Document document, String label) {
        String labelScopedText = extractLabelScopedText(document, label);
        if (labelScopedText == null) {
            return null;
        }

        Matcher matcher = KOREAN_DATE_PATTERN.matcher(labelScopedText);
        return matcher.find() ? normalize(matcher.group(1)) : null;
    }

    private String extractLabelScopedText(Document document, String label) {
        for (Element element : document.select("div, span, p, strong, h1, h2, h3")) {
            if (!label.equals(normalize(element.text()))) {
                continue;
            }

            List<Element> candidates = new ArrayList<>();
            if (element.parent() != null) {
                candidates.add(element.parent());
            }
            if (element.parent() != null && element.parent().parent() != null) {
                candidates.add(element.parent().parent());
            }

            for (Element candidate : candidates) {
                String text = normalize(candidate.text());
                if (!text.isBlank()) {
                    return text;
                }
            }
        }

        return null;
    }

    private String extractJsonPayload(String responseBody) {
        for (String line : responseBody.split("\\R")) {
            Matcher matcher = JSON_PAYLOAD_LINE_PATTERN.matcher(line);
            if (matcher.find() && matcher.group(1).contains("\"listData\"")) {
                return matcher.group(1);
            }
        }

        return null;
    }

    private Long extractPostId(String rawId) {
        if (rawId == null || rawId.isBlank()) {
            return null;
        }

        Matcher matcher = NUMERIC_ID_PATTERN.matcher(rawId);
        if (!matcher.find()) {
            return null;
        }

        try {
            return Long.parseLong(matcher.group(1));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String buildDetailUrl(String listPageUrl, Long postId) {
        if (listPageUrl == null || postId == null) {
            return null;
        }

        String baseUrl = listPageUrl.endsWith("/") ? listPageUrl : listPageUrl + "/";
        return normalizeUrl(URI.create(baseUrl).resolve(String.valueOf(postId)).toString());
    }

    private Instant parseInstant(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }

        try {
            return Instant.parse(raw);
        } catch (RuntimeException e) {
            return null;
        }
    }

    private String toSeoulIsoDate(String raw) {
        Instant parsed = parseInstant(raw);
        if (parsed == null) {
            return null;
        }

        LocalDate localDate = parsed.atZone(SEOUL_ZONE).toLocalDate();
        return localDate.toString();
    }

    private String text(JsonNode node, String fieldName) {
        if (node == null || fieldName == null) {
            return null;
        }

        JsonNode value = node.path(fieldName);
        return value.isMissingNode() || value.isNull() ? null : value.asText();
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            String normalized = normalize(value);
            if (!normalized.isBlank()) {
                return normalized;
            }
        }

        return null;
    }

    private String toKoreanDate(String dottedDate) {
        String[] tokens = dottedDate.split("\\.");
        if (tokens.length != 3) {
            return null;
        }

        try {
            int year = 2000 + Integer.parseInt(tokens[0]);
            int month = Integer.parseInt(tokens[1]);
            int day = Integer.parseInt(tokens[2]);
            return year + "년 " + month + "월 " + day + "일";
        } catch (RuntimeException e) {
            log.warn("HeroesOfJiuJitsu dotted date parse failed. dottedDate={}", dottedDate);
            return null;
        }
    }

    private boolean isUsableTitle(String text) {
        return text != null
                && !text.isBlank()
                && !text.contains("신청 마감")
                && !text.contains("대회 일자")
                && !text.contains("주요 일정")
                && !text.contains("경기 룰")
                && text.chars().anyMatch(ch -> ch >= 0xAC00 && ch <= 0xD7A3);
    }

    private String normalizeUrl(String value) {
        String normalized = normalize(value);
        return normalized.isBlank() ? null : normalized;
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }

        return value
                .replace('\u00A0', ' ')
                .replaceAll("[\\u200B-\\u200D\\uFEFF]", "")
                .replaceAll("\\s+", " ")
                .trim();
    }
}
