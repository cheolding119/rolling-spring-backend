package com.rolling.api.domain.tournament.crawler;

import com.rolling.api.domain.tournament.model.TournamentModel;
import com.rolling.api.domain.tournament.service.HeroesOfJiuJitsuCrawlerService;
import org.jsoup.Connection;
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
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class HeroesOfJiuJitsuCrawler implements TournamentCrawler {

    private static final int REQUEST_TIMEOUT_MILLIS = 10_000;
    private static final String USER_AGENT = "Mozilla/5.0 (compatible; RollingCrawler/1.0)";
    private static final String DEFAULT_LIST_PAGE_URL = "https://www.heroesofsports.kr/events/application";
    private static final String SERVER_ACTION_REQUEST_BODY = "[\"application\",1,100]";
    private static final Set<String> TARGET_HOSTS = Set.of("heroesofsports.kr", "www.heroesofsports.kr");
    private static final Pattern DETAIL_PATH_PATTERN = Pattern.compile("/events/application/\\d+$");
    private static final Pattern PAGE_BUNDLE_PATH_PATTERN = Pattern.compile("/_next/static/chunks/app/\\(service\\)/\\(default\\)/events/application/page-[^\"']+\\.js");
    private static final Pattern ACTION_REFERENCE_PATTERN =
            Pattern.compile("([A-Za-z_$][\\w$]*)=\\(0,[A-Za-z_$][\\w$]*\\.\\$\\)\\(\"([a-f0-9]{40})\"\\)");
    private static final Pattern LIST_ACTION_CALL_PATTERN =
            Pattern.compile("([A-Za-z_$][\\w$]*)\\(\"application\",1,100\\)");

    @Value("${tournament.crawler.heroes.list-page-urls:" + DEFAULT_LIST_PAGE_URL + "}")
    private List<String> listPageUrls;

    private final HeroesOfJiuJitsuCrawlerService crawlerService;

    @Override
    public List<TournamentModel> crawlAll() {
        List<TournamentModel> tournaments = new ArrayList<>();
        List<String> targetUrls = (listPageUrls == null || listPageUrls.isEmpty())
                ? List.of(DEFAULT_LIST_PAGE_URL)
                : listPageUrls;

        for (String listPageUrl : targetUrls) {
            String pageHtml = fetchPageHtml(listPageUrl);
            if (pageHtml == null) {
                continue;
            }

            List<TournamentModel> crawled = crawlFromServerAction(pageHtml, listPageUrl);
            if (crawled.isEmpty()) {
                crawled = crawlFromListHtml(pageHtml, listPageUrl);
            }
            tournaments.addAll(crawled);
        }

        return tournaments;
    }

    List<TournamentModel> crawlFromListHtml(String html, String listPageUrl) {
        Objects.requireNonNull(html, "html must not be null");
        return crawlFromListDocument(Jsoup.parse(html, listPageUrl), listPageUrl);
    }

    private List<TournamentModel> crawlFromListDocument(Document listDocument, String listPageUrl) {
        Map<String, Element> uniqueAnchors = extractDetailAnchors(listDocument);
        List<TournamentModel> tournaments = new ArrayList<>();

        for (Map.Entry<String, Element> entry : uniqueAnchors.entrySet()) {
            TournamentModel tournamentModel = crawlerService.parseListCardHtml(entry.getValue().outerHtml(), entry.getKey());
            if (tournamentModel != null) {
                tournaments.add(crawlerService.crawlDetailPage(entry.getKey(), tournamentModel));
            }
        }

        if (tournaments.isEmpty()) {
            log.warn("HeroesOfJiuJitsu list crawl found no tournaments. listPageUrl={}", listPageUrl);
        }

        return tournaments;
    }

    private List<TournamentModel> crawlFromServerAction(String pageHtml, String listPageUrl) {
        String bundleUrl = extractApplicationPageBundleUrl(pageHtml, listPageUrl);
        if (bundleUrl == null) {
            return List.of();
        }

        String bundleScript = fetchRawText(bundleUrl);
        if (bundleScript == null) {
            return List.of();
        }

        String actionId = extractListActionId(bundleScript);
        if (actionId == null) {
            log.warn("HeroesOfJiuJitsu list action id not found. listPageUrl={}", listPageUrl);
            return List.of();
        }

        String responseBody = executeListServerAction(listPageUrl, actionId);
        if (responseBody == null) {
            return List.of();
        }

        return crawlerService.parseServerActionResponse(responseBody, listPageUrl);
    }

    private String fetchPageHtml(String listPageUrl) {
        return fetchRawText(listPageUrl);
    }

    private String fetchRawText(String url) {
        try {
            Connection.Response response = Jsoup.connect(url)
                    .userAgent(USER_AGENT)
                    .timeout(REQUEST_TIMEOUT_MILLIS)
                    .ignoreContentType(true)
                    .execute();
            return decodeUtf8Body(response);
        } catch (IOException | IllegalArgumentException e) {
            log.warn("HeroesOfJiuJitsu request failed. url={}", url, e);
            return null;
        }
    }

    private String executeListServerAction(String listPageUrl, String actionId) {
        try {
            Connection.Response response = Jsoup.connect(listPageUrl)
                    .userAgent(USER_AGENT)
                    .timeout(REQUEST_TIMEOUT_MILLIS)
                    .ignoreContentType(true)
                    .header("Next-Action", actionId)
                    .header("Content-Type", "text/plain;charset=UTF-8")
                    .method(Connection.Method.POST)
                    .requestBody(SERVER_ACTION_REQUEST_BODY)
                    .execute();
            return decodeUtf8Body(response);
        } catch (IOException | IllegalArgumentException e) {
            log.warn("HeroesOfJiuJitsu server action failed. listPageUrl={}, actionId={}", listPageUrl, actionId, e);
            return null;
        }
    }

    private String decodeUtf8Body(Connection.Response response) {
        if (response == null) {
            return null;
        }

        return new String(response.bodyAsBytes(), StandardCharsets.UTF_8);
    }

    private Map<String, Element> extractDetailAnchors(Document document) {
        Elements anchors = document.select("a[href]");
        if (anchors.isEmpty()) {
            return Map.of();
        }

        Map<String, Element> uniqueAnchors = new LinkedHashMap<>();
        for (Element anchor : anchors) {
            String url = normalizeUrl(anchor.attr("abs:href"));
            if (!isTargetHost(url) || !isDetailUrl(url) || !isOpenTournamentAnchor(anchor)) {
                continue;
            }

            Element existing = uniqueAnchors.get(url);
            if (existing == null || scoreAnchor(anchor) > scoreAnchor(existing)) {
                uniqueAnchors.put(url, anchor);
            }
        }
        return uniqueAnchors;
    }

    private int scoreAnchor(Element anchor) {
        int score = 0;
        if (anchor.selectFirst("h3") != null) {
            score += 3;
        }
        if (anchor.selectFirst("span.font-bold") != null) {
            score += 2;
        }
        if (anchor.selectFirst("img[alt]") != null) {
            score += 1;
        }
        String normalizedText = normalize(anchor.text());
        if (normalizedText.contains("대회 신청하기")) {
            score += 2;
        }
        if (normalizedText.contains("신청하기")) {
            score += 1;
        }
        return score;
    }

    String extractApplicationPageBundleUrl(String pageHtml, String listPageUrl) {
        Objects.requireNonNull(pageHtml, "pageHtml must not be null");

        Matcher matcher = PAGE_BUNDLE_PATH_PATTERN.matcher(pageHtml);
        if (!matcher.find()) {
            return null;
        }

        return normalizeUrl(URI.create(listPageUrl).resolve(matcher.group()).toString());
    }

    String extractListActionId(String bundleScript) {
        Objects.requireNonNull(bundleScript, "bundleScript must not be null");

        Matcher actionCallMatcher = LIST_ACTION_CALL_PATTERN.matcher(bundleScript);
        if (!actionCallMatcher.find()) {
            return null;
        }

        String actionVariable = actionCallMatcher.group(1);
        Matcher actionReferenceMatcher = ACTION_REFERENCE_PATTERN.matcher(bundleScript);
        while (actionReferenceMatcher.find()) {
            if (actionVariable.equals(actionReferenceMatcher.group(1))) {
                return actionReferenceMatcher.group(2);
            }
        }

        return null;
    }

    private boolean isOpenTournamentAnchor(Element anchor) {
        String normalizedText = normalize(anchor.text());
        return normalizedText.contains("신청하기") || normalizedText.contains("대회 신청하기");
    }

    private boolean isDetailUrl(String url) {
        if (url == null || url.isBlank()) {
            return false;
        }

        try {
            String path = URI.create(url).getPath();
            return path != null && DETAIL_PATH_PATTERN.matcher(path).find();
        } catch (IllegalArgumentException e) {
            return false;
        }
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
