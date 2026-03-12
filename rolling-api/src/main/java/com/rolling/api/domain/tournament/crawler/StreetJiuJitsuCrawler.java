package com.rolling.api.domain.tournament.crawler;

import com.rolling.api.domain.tournament.entity.TournamentSource;
import com.rolling.api.domain.tournament.model.TournamentModel;
import com.rolling.api.domain.tournament.service.StreetJiuJitsuCrawlerService;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StreetJiuJitsuCrawler implements TournamentCrawler {

    private static final int REQUEST_TIMEOUT_MILLIS = 10_000;
    private static final String USER_AGENT = "Mozilla/5.0 (compatible; RollingCrawler/1.0)";
    private static final Set<String> TARGET_HOSTS = Set.of("street-jiujitsu.com", "www.street-jiujitsu.com");

    @Value("${tournament.crawler.street-jiujitsu.list-page-urls:https://www.street-jiujitsu.com/}")
    private List<String> listPageUrls;

    private final StreetJiuJitsuCrawlerService detailCrawlerService;

    @Override
    public TournamentSource getSource() {
        return TournamentSource.STREET_JIU_JITSU;
    }

    @Override
    public List<TournamentModel> crawlAll() {
        List<TournamentModel> tournaments = new ArrayList<>();
        List<String> targetUrls = (listPageUrls == null || listPageUrls.isEmpty())
                ? List.of("https://www.street-jiujitsu.com/")
                : listPageUrls;

        for (String listPageUrl : targetUrls) {
            Document listDocument = fetchListDocument(listPageUrl);
            if (listDocument == null) {
                continue;
            }

            Set<String> detailUrls = extractDetailUrls(listDocument);
            for (String detailUrl : detailUrls) {
                try {
                    TournamentModel tournament = detailCrawlerService.crawlDetailPage(detailUrl);
                    if (tournament != null) {
                        tournaments.add(tournament);
                    }
                } catch (Exception e) {
                    log.warn("StreetJiuJitsu detail crawl failed. detailUrl={}", detailUrl, e);
                }
            }
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
            log.warn("StreetJiuJitsu list crawl failed. listPageUrl={}", listPageUrl, e);
            return null;
        }
    }

    private Set<String> extractDetailUrls(Document listDocument) {
        Elements links = listDocument.select("a[href]");
        if (links.isEmpty()) {
            return Set.of();
        }

        return links.stream()
                .map(this::toAbsoluteUrl)
                .map(this::removeFragment)
                .filter(url -> !url.isBlank())
                .filter(this::isHttpUrl)
                .filter(this::isTargetHost)
                .filter(url -> !isRootPage(url))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private String toAbsoluteUrl(Element element) {
        String absoluteUrl = element.attr("abs:href").trim();
        if (!absoluteUrl.isBlank()) {
            return absoluteUrl;
        }
        return element.attr("href").trim();
    }

    private String removeFragment(String url) {
        int hashIndex = url.indexOf('#');
        if (hashIndex < 0) {
            return url;
        }
        return url.substring(0, hashIndex).trim();
    }

    private boolean isHttpUrl(String url) {
        try {
            String scheme = URI.create(url).getScheme();
            return "http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private boolean isTargetHost(String url) {
        try {
            String host = URI.create(url).getHost();
            return host != null && TARGET_HOSTS.contains(host.toLowerCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private boolean isRootPage(String url) {
        try {
            String path = URI.create(url).getPath();
            return path == null || path.isBlank() || "/".equals(path);
        } catch (IllegalArgumentException e) {
            return true;
        }
    }
}