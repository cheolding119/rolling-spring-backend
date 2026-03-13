package com.rolling.api.domain.tournament.service;

import com.rolling.api.domain.tournament.model.TournamentModel;
import com.rolling.api.domain.tournament.util.TournamentDateUtils;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class KoreaJiuCrawlerService {

    private static final int REQUEST_TIMEOUT_MILLIS = 10_000;
    private static final String USER_AGENT = "Mozilla/5.0 (compatible; RollingCrawler/1.0)";
    private static final String ORGANIZER = "코리아 주짓수 챔피언십";

    private static final Pattern FULL_DATE_PATTERN =
            Pattern.compile("(20\\d{2}\\s*년\\s*\\d{1,2}\\s*월\\s*\\d{1,2}\\s*일)");
    private static final Pattern MONTH_DAY_PATTERN =
            Pattern.compile("(\\d{1,2}\\s*월\\s*\\d{1,2}\\s*일)");

    public TournamentModel crawlDetailPage(String detailUrl, TournamentModel seedModel) {
        try {
            Document document = Jsoup.connect(detailUrl)
                    .userAgent(USER_AGENT)
                    .timeout(REQUEST_TIMEOUT_MILLIS)
                    .get();
            return parseDetailDocument(document, detailUrl, seedModel);
        } catch (IOException | IllegalArgumentException e) {
            log.warn("Failed to crawl KoreaJiu detail page. url={}", detailUrl, e);
            return seedModel;
        }
    }

    TournamentModel parseDetailHtml(String html, String detailUrl, TournamentModel seedModel) {
        Objects.requireNonNull(html, "html must not be null");
        return parseDetailDocument(Jsoup.parse(html, detailUrl), detailUrl, seedModel);
    }

    private TournamentModel parseDetailDocument(Document document, String detailUrl, TournamentModel seedModel) {
        TournamentModel tournamentModel = seedModel != null ? seedModel : new TournamentModel();
        tournamentModel.setOrganizer(ORGANIZER);
        if (tournamentModel.getApplyLink() == null || tournamentModel.getApplyLink().isBlank()) {
            tournamentModel.setApplyLink(detailUrl);
        }

        if (tournamentModel.getTitle() == null
                || tournamentModel.getTitle().isBlank()
                || ORGANIZER.equals(tournamentModel.getTitle())) {
            String extractedTitle = extractTitle(document);
            if (extractedTitle != null) {
                tournamentModel.setTitle(extractedTitle);
            }
        }

        String extractedPosterUrl = extractPosterUrl(document);
        if (extractedPosterUrl != null) {
            tournamentModel.setPosterUrl(extractedPosterUrl);
        }

        List<String> detailLines = extractDetailLines(document);
        if (detailLines.isEmpty()) {
            return tournamentModel;
        }

        String competitionDate = extractCompetitionDate(detailLines);
        tournamentModel.setCompetitionDate(competitionDate);
        tournamentModel.setRegistrationDeadline(extractRegistrationDeadline(detailLines, competitionDate));
        tournamentModel.setLocation(extractLocation(detailLines));
        return tournamentModel;
    }

    private String extractPosterUrl(Document document) {
        String posterNearApplyAction = extractPosterNearApplyAction(document);
        if (posterNearApplyAction != null) {
            return posterNearApplyAction;
        }

        List<Element> candidates = document.select("img[src]").stream()
                .filter(this::hasPosterSource)
                .filter(this::isPortraitImage)
                .sorted(Comparator.comparingInt(this::posterCandidateScore).reversed())
                .toList();

        if (candidates.isEmpty()) {
            log.warn("KoreaJiu poster not found");
            return null;
        }

        return resolveImageUrl(candidates.get(0));
    }

    private String extractPosterNearApplyAction(Document document) {
        for (Element applyAction : document.select("div[data-semantic-classname=button], a[href], button")) {
            if (!isApplyAction(applyAction)) {
                continue;
            }

            Element section = applyAction.closest("section");
            if (section == null) {
                continue;
            }

            List<Element> candidates = new ArrayList<>();
            Elements orderedElements = section.select("img[src], div[data-semantic-classname=button], a[href], button");

            for (Element element : orderedElements) {
                if (element == applyAction || isApplyAction(element)) {
                    String posterUrl = selectPosterCandidate(candidates);
                    if (posterUrl != null) {
                        return posterUrl;
                    }
                    break;
                }

                if ("img".equalsIgnoreCase(element.tagName()) && hasPosterSource(element)) {
                    candidates.add(element);
                }
            }
        }

        return null;
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

    private boolean isApplyAction(Element element) {
        if (element == null) {
            return false;
        }

        String normalizedText = normalize(element.text() + " " + element.attr("aria-label"));
        return normalizedText.contains("참가신청")
                || normalizedText.contains("참가 신청");
    }

    private boolean hasPosterSource(Element element) {
        String imageUrl = resolveImageUrl(element);
        return imageUrl != null && imageUrl.contains("static.wixstatic.com/media/");
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

    private int posterCandidateScore(Element element) {
        int score = 0;
        if ("high".equalsIgnoreCase(element.attr("fetchpriority"))) {
            score += 2;
        }
        if (isPortraitImage(element)) {
            score += 2;
        }
        if (element.classNames().contains("BI8PVQ") && element.classNames().contains("Tj01hh")) {
            score += 1;
        }
        return score;
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

    private String extractTitle(Document document) {
        for (Element heading : document.select("h1, h2, h3")) {
            String text = normalize(heading.text());
            if (isUsableTitle(text)) {
                return text;
            }
        }

        return null;
    }

    private List<String> extractDetailLines(Document document) {
        Element detailElement = findDetailContainer(document);
        if (detailElement == null) {
            log.warn("KoreaJiu detail text block not found");
            return List.of();
        }

        List<String> lines = new ArrayList<>();
        for (Element lineElement : detailElement.select("p, h1, h2, h3, h4, h5, h6, li")) {
            String line = normalize(lineElement.text());
            if (!line.isBlank()) {
                lines.add(line);
            }
        }

        if (!lines.isEmpty()) {
            return lines;
        }

        String fallback = normalize(detailElement.text());
        return fallback.isBlank() ? List.of() : List.of(fallback);
    }

    private Element findDetailContainer(Document document) {
        List<Element> candidates = document.select(".wixui-rich-text");
        for (Element candidate : candidates) {
            String text = normalize(candidate.text());
            if (text.contains("대회일정") || text.contains("시합장소") || text.contains("마감 :")) {
                return candidate;
            }
        }

        Element detailElement = document.getElementById("comp-ll0lmtbu");
        if (detailElement != null) {
            return detailElement;
        }

        return null;
    }

    private String extractCompetitionDate(List<String> detailLines) {
        int scheduleIndex = indexOfContaining(detailLines, "대회일정");
        if (scheduleIndex >= 0) {
            for (int index = scheduleIndex; index < detailLines.size(); index++) {
                String competitionDate = extractDate(detailLines.get(index), FULL_DATE_PATTERN);
                if (competitionDate != null) {
                    return competitionDate;
                }
            }
        }

        for (String detailLine : detailLines) {
            String competitionDate = extractDate(detailLine, FULL_DATE_PATTERN);
            if (competitionDate != null) {
                return competitionDate;
            }
        }

        log.warn("Cannot parse KoreaJiu field: competitionDate");
        return null;
    }

    private String extractRegistrationDeadline(List<String> detailLines, String competitionDate) {
        for (String detailLine : detailLines) {
            if (!detailLine.contains("마감")) {
                continue;
            }

            String registrationDeadline = extractDate(detailLine, FULL_DATE_PATTERN);
            if (registrationDeadline != null) {
                return registrationDeadline;
            }

            registrationDeadline = inferYear(extractDate(detailLine, MONTH_DAY_PATTERN), competitionDate);
            if (registrationDeadline != null) {
                return registrationDeadline;
            }
        }

        log.warn("Cannot parse KoreaJiu field: registrationDeadline");
        return null;
    }

    private String extractLocation(List<String> detailLines) {
        for (int index = 0; index < detailLines.size(); index++) {
            String detailLine = detailLines.get(index);
            if (!detailLine.contains("시합장소")) {
                continue;
            }

            String sameLineLocation = trimLocationLabel(detailLine);
            if (sameLineLocation != null) {
                return sameLineLocation;
            }

            for (int nextIndex = index + 1; nextIndex < detailLines.size(); nextIndex++) {
                String candidate = trimLocationLabel(detailLines.get(nextIndex));
                if (candidate != null) {
                    return candidate;
                }
            }
        }

        log.warn("Cannot parse KoreaJiu field: location");
        return null;
    }

    private String extractDate(String source, Pattern pattern) {
        if (source == null || source.isBlank()) {
            return null;
        }

        Matcher matcher = pattern.matcher(source);
        if (!matcher.find()) {
            return null;
        }

        return normalize(matcher.group(1));
    }

    private String inferYear(String monthDay, String competitionDate) {
        if (monthDay == null || competitionDate == null) {
            return null;
        }

        Matcher yearMatcher = FULL_DATE_PATTERN.matcher(competitionDate);
        if (!yearMatcher.find()) {
            return null;
        }

        String fullCompetitionDate = yearMatcher.group(1);
        LocalDate parsedCompetitionDate = TournamentDateUtils.parse(fullCompetitionDate);
        if (parsedCompetitionDate == null) {
            return null;
        }

        String inferredDate = parsedCompetitionDate.getYear() + "년 " + monthDay;
        LocalDate parsedDeadline = TournamentDateUtils.parse(inferredDate);
        if (parsedDeadline == null) {
            return null;
        }

        if (parsedDeadline.isAfter(parsedCompetitionDate)) {
            inferredDate = (parsedCompetitionDate.getYear() - 1) + "년 " + monthDay;
        }

        return normalize(inferredDate);
    }

    private String trimLocationLabel(String source) {
        String normalized = normalize(source);
        if (normalized.isBlank()) {
            return null;
        }

        String withoutLabel = normalized
                .replace("시합장소 (LOCATION)", "")
                .replace("시합장소:", "")
                .replace("시합장소 :", "")
                .replace("시합장소", "")
                .trim();

        if (withoutLabel.isBlank() || "LOCATION".equalsIgnoreCase(withoutLabel)) {
            return null;
        }

        return withoutLabel;
    }

    private int indexOfContaining(List<String> values, String keyword) {
        for (int index = 0; index < values.size(); index++) {
            if (values.get(index).contains(keyword)) {
                return index;
            }
        }
        return -1;
    }

    private boolean isUsableTitle(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }

        return !text.contains("대회일정")
                && !text.contains("시합장소")
                && !text.contains("참가신청")
                && !text.contains("마감")
                && !text.contains("시작")
                && text.chars().anyMatch(ch -> ch >= 0xAC00 && ch <= 0xD7A3);
    }

    private String normalize(String value) {
        return value
                .replace('\u00A0', ' ')
                .replaceAll("[\\u200B-\\u200D\\uFEFF]", "")
                .replaceAll("\\s+", " ")
                .trim();
    }
}
