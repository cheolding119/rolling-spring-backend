package com.rolling.api.domain.tournament.crawler;

import com.rolling.api.domain.tournament.entity.TournamentSource;
import com.rolling.api.domain.tournament.model.TournamentModel;
import com.rolling.api.domain.tournament.service.SpotliteCrawlerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class SpotliteCrawler implements TournamentCrawler {

    private static final int REQUEST_TIMEOUT_MILLIS = 10_000;
    private static final String USER_AGENT = "Mozilla/5.0 (compatible; RollingCrawler/1.0)";
    private static final String DEFAULT_LIST_PAGE_URL = "https://spotlite.co.kr/jiujitsu/";

    @Value("${tournament.crawler.spotlite.list-page-urls:" + DEFAULT_LIST_PAGE_URL + "}")
    private List<String> listPageUrls;

    private final SpotliteCrawlerService crawlerService;

    @Override
    public TournamentSource getSource() {
        return TournamentSource.SPOTLITE;
    }

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

            tournaments.addAll(crawlerService.parseListDocument(listDocument, listPageUrl));
        }

        return tournaments;
    }

    List<TournamentModel> crawlFromListHtml(String html, String listPageUrl) {
        Objects.requireNonNull(html, "html must not be null");
        return crawlerService.parseListDocument(Jsoup.parse(html, listPageUrl), listPageUrl);
    }

    private Document fetchListDocument(String listPageUrl) {
        try {
            return Jsoup.connect(listPageUrl)
                    .userAgent(USER_AGENT)
                    .timeout(REQUEST_TIMEOUT_MILLIS)
                    .get();
        } catch (IOException | IllegalArgumentException e) {
            log.warn("Spotlite list crawl failed. listPageUrl={}", listPageUrl, e);
            return null;
        }
    }
}
