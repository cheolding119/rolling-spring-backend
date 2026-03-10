package com.rolling.api.domain.tournament.crawler;

import com.rolling.api.domain.tournament.model.TournamentModel;
import com.rolling.api.domain.tournament.service.KoreaJiuCrawlerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class KoreaJiuCrawler implements TournamentCrawler {

    private static final int REQUEST_TIMEOUT_MILLIS = 10_000;
    private static final String USER_AGENT = "Mozilla/5.0 (compatible; RollingCrawler/1.0)";
    private static final String DEFAULT_LIST_PAGE_URL =
            "https://www.koreajiu.com/%EB%B3%B5%EC%A0%9C-%EB%8C%80%ED%9A%8C%ED%95%98%EC%9D%B4%EB%9D%BC%EC%9D%B4%ED%8A%B8";
    private static final String DEFAULT_TITLE = "코리아 주짓수 챔피언십";
    private static final Set<String> TARGET_HOSTS = Set.of("koreajiu.com", "www.koreajiu.com");

    @Value("${tournament.crawler.koreajiu.list-page-urls:" + DEFAULT_LIST_PAGE_URL + "}")
    private List<String> listPageUrls;

    private final KoreaJiuCrawlerService detailCrawlerService;

    @Override
    public List<TournamentModel> crawlAll() {
        List<TournamentModel> tournaments = new ArrayList<>();
        List<String> targetUrls = (listPageUrls == null || listPageUrls.isEmpty())
                ? List.of(DEFAULT_LIST_PAGE_URL)
                : listPageUrls;

        for (String listPageUrl : targetUrls) {
            Document listDocument = fetchListDocument(listPageUrl);
            if (listDocument == null) {
                continue;
            }

            tournaments.addAll(crawlFromListDocument(listDocument, listPageUrl));
        }

        return tournaments;
    }

    List<TournamentModel> crawlFromListHtml(String html, String listPageUrl) {
        Objects.requireNonNull(html, "html must not be null");
        return crawlFromListDocument(Jsoup.parse(html, listPageUrl), listPageUrl);
    }

    private List<TournamentModel> crawlFromListDocument(Document listDocument, String listPageUrl) {
        List<ListItem> listItems = new ArrayList<>();
        for (Element applyAnchor : extractApplyAnchors(listDocument)) {
            String rawApplyLink = normalizeUrl(applyAnchor.attr("abs:href"));
            if (rawApplyLink == null) {
                continue;
            }

            Element cardRoot = findCardRoot(applyAnchor);
            String detailUrl = extractDetailUrl(cardRoot, rawApplyLink);

            listItems.add(new ListItem(
                    extractTitle(cardRoot),
                    null,
                    rawApplyLink,
                    detailUrl
            ));
        }

        Map<String, Integer> applyLinkCounts = countApplyLinks(listItems);
        List<TournamentModel> tournaments = new ArrayList<>();

        for (ListItem listItem : listItems) {
            String effectiveApplyLink = resolveApplyLink(listItem, applyLinkCounts);

            TournamentModel tournamentModel = new TournamentModel();
            tournamentModel.setTitle(listItem.title());
            tournamentModel.setPosterUrl(listItem.posterUrl());
            tournamentModel.setApplyLink(effectiveApplyLink);

            String detailUrl = listItem.detailUrl() != null ? listItem.detailUrl() : effectiveApplyLink;
            tournaments.add(detailCrawlerService.crawlDetailPage(detailUrl, tournamentModel));
        }

        return tournaments;
    }

    private Document fetchListDocument(String listPageUrl) {
        try {
            return Jsoup.connect(listPageUrl)
                    .userAgent(USER_AGENT)
                    .timeout(REQUEST_TIMEOUT_MILLIS)
                    .get();
        } catch (IOException | IllegalArgumentException e) {
            log.warn("KoreaJiu list crawl failed. listPageUrl={}", listPageUrl, e);
            return null;
        }
    }

    private List<Element> extractApplyAnchors(Document document) {
        Elements anchors = document.select("a[href]");
        if (anchors.isEmpty()) {
            return List.of();
        }

        return anchors.stream()
                .filter(anchor -> isTargetHost(anchor.attr("abs:href")))
                .filter(this::isApplyAnchor)
                .toList();
    }

    private boolean isApplyAnchor(Element anchor) {
        String normalizedText = normalize(anchor.text() + " " + anchor.attr("aria-label"));
        return normalizedText.contains("참가신청란")
                || normalizedText.contains("참가신청")
                || normalizedText.contains("참가 신청");
    }

    private Element findCardRoot(Element applyAnchor) {
        Element current = applyAnchor;
        while (current != null) {
            if ("section".equalsIgnoreCase(current.tagName()) && current.classNames().contains("wixui-column-strip")) {
                return current;
            }
            current = current.parent();
        }
        return applyAnchor.parent();
    }

    private String extractTitle(Element cardRoot) {
        if (cardRoot == null) {
            return DEFAULT_TITLE;
        }

        List<String> candidates = new ArrayList<>();
        for (Element richText : cardRoot.select(".wixui-rich-text")) {
            String text = normalize(richText.text());
            if (!text.isBlank()) {
                candidates.add(text);
            }
        }

        return selectBestTitle(candidates);
    }

    private String extractDetailUrl(Element cardRoot, String applyLink) {
        if (cardRoot == null) {
            return applyLink;
        }

        for (Element anchor : cardRoot.select("a[href]")) {
            String href = normalizeUrl(anchor.attr("abs:href"));
            if (href == null || href.equals(applyLink) || !isTargetHost(href)) {
                continue;
            }
            return href;
        }

        return applyLink;
    }

    private Map<String, Integer> countApplyLinks(List<ListItem> listItems) {
        Map<String, Integer> counts = new HashMap<>();
        for (ListItem listItem : listItems) {
            counts.merge(listItem.rawApplyLink(), 1, Integer::sum);
        }
        return counts;
    }

    private String resolveApplyLink(ListItem listItem, Map<String, Integer> applyLinkCounts) {
        String rawApplyLink = listItem.rawApplyLink();
        if (rawApplyLink == null) {
            return listItem.detailUrl();
        }

        if (applyLinkCounts.getOrDefault(rawApplyLink, 0) <= 1) {
            return rawApplyLink;
        }

        String detailUrl = listItem.detailUrl();
        if (detailUrl != null && !detailUrl.equals(rawApplyLink)) {
            log.info("KoreaJiu shared apply link detected. use detailUrl as unique applyLink. rawApplyLink={}, detailUrl={}",
                    rawApplyLink, detailUrl);
            return detailUrl;
        }

        log.warn("KoreaJiu duplicate apply link without distinct detailUrl. rawApplyLink={}", rawApplyLink);
        return rawApplyLink;
    }

    private String selectBestTitle(List<String> candidates) {
        if (candidates.isEmpty()) {
            return DEFAULT_TITLE;
        }

        for (String candidate : candidates) {
            if (isMeaningfulTitle(candidate) && !looksLikeScheduleLine(candidate)) {
                return candidate;
            }
        }

        for (String candidate : candidates) {
            if (isMeaningfulTitle(candidate)) {
                return candidate;
            }
        }

        return candidates.get(0);
    }

    private boolean isMeaningfulTitle(String value) {
        return value != null
                && !value.isBlank()
                && !normalize(value).contains("참가신청")
                && value.chars().anyMatch(ch -> ch >= 0xAC00 && ch <= 0xD7A3);
    }

    private boolean looksLikeScheduleLine(String value) {
        String normalized = normalize(value).toUpperCase(Locale.ROOT);
        return normalized.contains("SAT")
                || normalized.contains("SUN")
                || normalized.matches(".*\\d{2}\\.\\d{2}\\.\\d{2}.*")
                || normalized.matches(".*\\d{1,2}/\\d{1,2}/\\d{2,4}.*")
                || normalized.matches(".*\\d{1,2}월\\s*\\d{1,2}일.*");
    }

    private boolean isTargetHost(String url) {
        if (url == null || url.isBlank()) {
            return false;
        }

        try {
            String host = URI.create(url).getHost();
            return host != null && TARGET_HOSTS.contains(host.toLowerCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private String normalizeUrl(String url) {
        if (url == null) {
            return null;
        }

        String trimmed = url.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalize(String value) {
        return value
                .replace('\u00A0', ' ')
                .replaceAll("[\\u200B-\\u200D\\uFEFF]", "")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private record ListItem(
            String title,
            String posterUrl,
            String rawApplyLink,
            String detailUrl
    ) {
    }
}
