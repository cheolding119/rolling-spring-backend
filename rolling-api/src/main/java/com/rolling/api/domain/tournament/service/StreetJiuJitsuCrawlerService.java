package com.rolling.api.domain.tournament.service;

import com.rolling.api.domain.tournament.model.TournamentModel;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class StreetJiuJitsuCrawlerService {

    private static final ZoneId SEOUL_ZONE = ZoneId.of("Asia/Seoul");
    private static final int REQUEST_TIMEOUT_MILLIS = 10_000;
    private static final String USER_AGENT = "Mozilla/5.0 (compatible; RollingCrawler/1.0)";

    private static final Pattern DATE_PATTERN =
            Pattern.compile("(\\d{4}\\s*년\\s*\\d{1,2}\\s*월\\s*\\d{1,2}\\s*일)");
    private static final Pattern MONTH_DAY_PATTERN =
            Pattern.compile("(\\d{1,2})\\s*월\\s*(\\d{1,2})\\s*일");
    private static final Pattern COMPETITION_DATE_SECTION_PATTERN =
            Pattern.compile("▶\\s*시합\\s*(?:일정|스케줄)\\s*:?\\s*(.+?)(?=▶|$)", Pattern.DOTALL);
    private static final Pattern LOCATION_PATTERN =
            Pattern.compile("▶\\s*시합\\s*장소\\s*:\\s*(.+?)(?=▶|$)", Pattern.DOTALL);
    private static final Pattern ORGANIZER_PATTERN =
            Pattern.compile("▶\\s*주최/주관\\s*:\\s*(.+?)(?=▶|$)", Pattern.DOTALL);
    private static final Pattern REGISTRATION_GENERAL_SECTION_PATTERN =
            Pattern.compile("▶\\s*시합\\s*접수[\\s\\S]*?일반\\s*접수\\s*:\\s*(.+?)(?=▶|$)", Pattern.DOTALL);
    private static final Pattern REGISTRATION_DEADLINE_PATTERN =
            Pattern.compile("접수\\s*마감\\s*[:：]\\s*(.+?)(?=\\s+-\\s+|▶|$)", Pattern.DOTALL);
    private static final List<Pattern> REGISTRATION_DEADLINE_PATTERNS = List.of(
            REGISTRATION_GENERAL_SECTION_PATTERN,
            REGISTRATION_DEADLINE_PATTERN
    );

    public TournamentModel crawlDetailPage(String detailUrl) {
        try {
            Document document = Jsoup.connect(detailUrl)
                    .userAgent(USER_AGENT)
                    .timeout(REQUEST_TIMEOUT_MILLIS)
                    .get();
            return parseDetailDocument(document, detailUrl);
        } catch (IOException | IllegalArgumentException e) {
            log.warn("Failed to crawl detail page. url={}", detailUrl, e);
            return null;
        }
    }

    TournamentModel parseDetailHtml(String html, String detailUrl) {
        Objects.requireNonNull(html, "html must not be null");
        return parseDetailDocument(Jsoup.parse(html, detailUrl), detailUrl);
    }

    private TournamentModel parseDetailDocument(Document document, String detailUrl) {
        String pageText = normalize(document.text());
        String infoText = extractInfoText(document);
        String parseSource = infoText != null ? infoText : pageText;

        String title = extractTitle(document);
        String posterUrl = extractPosterUrl(document);
        String competitionDateRaw = extractByPattern(parseSource, COMPETITION_DATE_SECTION_PATTERN, "competitionDate");
        String location = extractByPattern(parseSource, LOCATION_PATTERN, "location");
        String organizer = extractByPattern(parseSource, ORGANIZER_PATTERN, "organizer");
        String registrationDeadlineRaw = extractRegistrationDeadline(pageText);
        String competitionDate = extractDateOnly(competitionDateRaw, "competitionDate");

        TournamentModel tournamentModel = new TournamentModel();
        tournamentModel.setTitle(title);
        tournamentModel.setPosterUrl(posterUrl);
        tournamentModel.setCompetitionDate(competitionDate);
        tournamentModel.setLocation(location);
        tournamentModel.setOrganizer(organizer);
        tournamentModel.setRegistrationDeadline(extractDateOnly(registrationDeadlineRaw, "registrationDeadline", competitionDate));
        tournamentModel.setApplyLink(detailUrl);
        return tournamentModel;
    }

    private String extractTitle(Document document) {
        Element titleElement = document.select("h1:contains(스트릿 주짓수)").first();
        if (titleElement == null) {
            log.warn("title not found: h1 containing '스트릿 주짓수'");
            return null;
        }
        return normalize(titleElement.text());
    }

    private String extractPosterUrl(Document document) {
        Element titleElement = document.select("h1:contains(스트릿 주짓수)").first();
        Element section = titleElement != null ? titleElement.closest("section") : null;

        if (section != null) {
            String posterNearActionArea = extractPosterNearActionArea(section, titleElement);
            if (posterNearActionArea != null) {
                return posterNearActionArea;
            }
        }

        List<Element> candidates = document.select("img[src]").stream()
                .filter(this::isPosterScope)
                .filter(this::hasPosterSource)
                .sorted(Comparator.comparingInt(this::posterCandidateScore).reversed())
                .toList();

        if (candidates.isEmpty()) {
            log.warn("poster not found: no matching poster image candidate");
            return null;
        }

        return resolveImageUrl(candidates.get(0));
    }

    private String extractPosterNearActionArea(Element section, Element titleElement) {
        List<Element> candidates = new java.util.ArrayList<>();
        Elements orderedElements = section.select("img[src], div[data-semantic-classname=button], h1");

        for (Element element : orderedElements) {
            if (element == titleElement || isActionButton(element)) {
                return selectPosterCandidate(candidates);
            }

            if (isPosterScope(element) && hasPosterSource(element)) {
                candidates.add(element);
            }
        }

        return selectPosterCandidate(candidates);
    }

    private String selectPosterCandidate(List<Element> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }

        return candidates.stream()
                .filter(this::isPortraitImage)
                .reduce((first, second) -> second)
                .map(this::resolveImageUrl)
                .orElseGet(() -> resolveImageUrl(candidates.get(candidates.size() - 1)));
    }

    private boolean isActionButton(Element element) {
        return "div".equalsIgnoreCase(element.tagName())
                && "button".equalsIgnoreCase(element.attr("data-semantic-classname"));
    }

    private boolean isPosterScope(Element element) {
        for (Element parent = element.parent(); parent != null; parent = parent.parent()) {
            String id = parent.id();
            if ("SITE_HEADER".equals(id) || "SITE_FOOTER".equals(id)) {
                return false;
            }
        }
        return true;
    }

    private boolean hasPosterSource(Element element) {
        String imageUrl = resolveImageUrl(element);
        return imageUrl != null && imageUrl.contains("static.wixstatic.com/media/");
    }

    private int posterCandidateScore(Element element) {
        int score = 0;
        if ("high".equalsIgnoreCase(element.attr("fetchpriority"))) {
            score += 4;
        }
        if (isPortraitImage(element)) {
            score += 3;
        }
        if (element.classNames().contains("BI8PVQ") && element.classNames().contains("Tj01hh")) {
            score += 2;
        }
        if (isMainContentImage(element)) {
            score += 1;
        }
        return score;
    }

    private boolean isPortraitImage(Element element) {
        try {
            int width = Integer.parseInt(element.attr("width"));
            int height = Integer.parseInt(element.attr("height"));
            return height >= width;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean isMainContentImage(Element element) {
        for (Element parent = element.parent(); parent != null; parent = parent.parent()) {
            if ("PAGES_CONTAINER".equals(parent.id()) || "true".equals(parent.attr("data-main-content"))) {
                return true;
            }
        }
        return false;
    }

    private String resolveImageUrl(Element element) {
        String absoluteUrl = normalizeImageUrl(element.attr("abs:src"));
        if (absoluteUrl != null) {
            return absoluteUrl;
        }
        return normalizeImageUrl(element.attr("src"));
    }

    private String normalizeImageUrl(String imageUrl) {
        if (imageUrl == null) {
            return null;
        }

        String trimmed = imageUrl.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String extractInfoText(Document document) {
        Elements infoCandidates = document.getElementsContainingText("▶ 시합 일정");
        if (infoCandidates.isEmpty()) {
            infoCandidates = document.getElementsContainingText("▶ 시합 스케줄");
        }
        if (infoCandidates.isEmpty()) {
            log.warn("info block not found: element containing '▶ 시합 일정' or '▶ 시합 스케줄'");
            return null;
        }

        Element best = infoCandidates.stream()
                .filter(element -> element.text().contains("▶ 시합 장소") || element.text().contains("▶ 주최/주관"))
                .min(Comparator.comparingInt(element -> element.text().length()))
                .orElse(infoCandidates.first());
        return normalize(best.text());
    }

    private String extractRegistrationDeadline(String source) {
        if (source == null || source.isBlank()) {
            log.warn("registrationDeadline source text is empty");
            return null;
        }

        for (Pattern pattern : REGISTRATION_DEADLINE_PATTERNS) {
            Matcher matcher = pattern.matcher(source);
            if (matcher.find()) {
                return normalize(matcher.group(1));
            }
        }

        log.warn("Cannot parse field: registrationDeadline");
        return null;
    }

    private String extractByPattern(String source, Pattern pattern, String fieldName) {
        if (source == null || source.isBlank()) {
            log.warn("{} source text is empty", fieldName);
            return null;
        }
        Matcher matcher = pattern.matcher(source);
        if (!matcher.find()) {
            log.warn("Cannot parse field: {}", fieldName);
            return null;
        }
        return normalize(matcher.group(1));
    }

    private String extractDateOnly(String source, String fieldName) {
        if (source == null || source.isBlank()) {
            log.warn("Cannot extract date for {}: source is empty", fieldName);
            return null;
        }
        Matcher matcher = DATE_PATTERN.matcher(source);
        if (!matcher.find()) {
            log.warn("Cannot extract date for {}: {}", fieldName, source);
            return null;
        }
        return normalize(matcher.group(1));
    }

    private String extractDateOnly(String source, String fieldName, String referenceDate) {
        String extracted = extractDateOnly(source, fieldName);
        if (extracted != null) {
            return extracted;
        }
        return inferDateWithReferenceYear(source, fieldName, referenceDate);
    }

    private String inferDateWithReferenceYear(String source, String fieldName, String referenceDate) {
        if (source == null || source.isBlank()) {
            return null;
        }

        Matcher matcher = MONTH_DAY_PATTERN.matcher(source);
        if (!matcher.find()) {
            return null;
        }

        int month = Integer.parseInt(matcher.group(1));
        int day = Integer.parseInt(matcher.group(2));
        int year = extractReferenceYear(referenceDate);

        try {
            LocalDate inferredDate = LocalDate.of(year, month, day);
            LocalDate referenceLocalDate = toLocalDate(referenceDate);
            if (referenceLocalDate != null && inferredDate.isAfter(referenceLocalDate)) {
                inferredDate = inferredDate.minusYears(1);
            }
            return inferredDate.getYear() + "년 " + inferredDate.getMonthValue() + "월 " + inferredDate.getDayOfMonth() + "일";
        } catch (RuntimeException e) {
            log.warn("Cannot infer date for {}: {}", fieldName, source);
            return null;
        }
    }

    private int extractReferenceYear(String referenceDate) {
        Matcher matcher = DATE_PATTERN.matcher(referenceDate == null ? "" : referenceDate);
        if (matcher.find()) {
            String fullDate = normalize(matcher.group(1));
            return Integer.parseInt(fullDate.substring(0, 4));
        }
        return LocalDate.now(SEOUL_ZONE).getYear();
    }

    private LocalDate toLocalDate(String rawDate) {
        Matcher matcher = DATE_PATTERN.matcher(rawDate == null ? "" : rawDate);
        if (!matcher.find()) {
            return null;
        }

        String normalizedDate = normalize(matcher.group(1));
        String[] tokens = normalizedDate
                .replace("년", "")
                .replace("월", "")
                .replace("일", "")
                .trim()
                .split("\\s+");
        if (tokens.length != 3) {
            return null;
        }

        try {
            return LocalDate.of(
                    Integer.parseInt(tokens[0]),
                    Integer.parseInt(tokens[1]),
                    Integer.parseInt(tokens[2])
            );
        } catch (RuntimeException e) {
            return null;
        }
    }

    private String normalize(String value) {
        return value
                .replace('\u00A0', ' ')
                .replaceAll("[\\u200B-\\u200D\\uFEFF]", "")
                .replaceAll("\\s+", " ")
                .trim();
    }
}
