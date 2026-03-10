package com.rolling.api.domain.tournament.crawler;

import com.rolling.api.domain.tournament.model.TournamentModel;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Objects;

@Component
public class TournamentApplyLinkCrawler {

    public void extractApplyLink(String html, TournamentModel tournamentModel) {
        Objects.requireNonNull(html, "html must not be null");
        Objects.requireNonNull(tournamentModel, "tournamentModel must not be null");

        Document document = Jsoup.parse(html);
        Element applyAnchor = document.selectFirst("a[data-testid=linkElement][href]");
        if (applyAnchor == null) {
            applyAnchor = document.selectFirst("a[href]");
        }
        if (applyAnchor == null) {
            throw new IllegalArgumentException("No anchor tag with href found in tournament HTML.");
        }

        String href = applyAnchor.attr("href").trim();
        if (href.isBlank()) {
            throw new IllegalArgumentException("Extracted href is blank.");
        }

        tournamentModel.setApplyLink(href);
    }

    public TournamentModel crawlFromListHtml(String html) {
        TournamentModel tournamentModel = new TournamentModel();
        extractApplyLink(html, tournamentModel);
        return tournamentModel;
    }

    public TournamentModel crawlFromListPage(String listPageUrl) throws IOException {
        Document listDocument = Jsoup.connect(listPageUrl).get();
        return crawlFromListHtml(listDocument.html());
    }

    public Document crawlDetailPage(TournamentModel tournamentModel) throws IOException {
        if (tournamentModel == null || tournamentModel.getApplyLink() == null || tournamentModel.getApplyLink().isBlank()) {
            throw new IllegalArgumentException("applyLink is empty.");
        }
        return Jsoup.connect(tournamentModel.getApplyLink()).get();
    }
}
