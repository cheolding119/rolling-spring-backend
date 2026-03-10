package com.rolling.api.domain.tournament.crawler;

import com.rolling.api.domain.tournament.model.TournamentModel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TournamentApplyLinkCrawlerTest {

    private final TournamentApplyLinkCrawler crawler = new TournamentApplyLinkCrawler();

    @Test
    @DisplayName("HTML에서 상세 페이지 URL을 추출해 applyLink에 저장한다")
    void extractApplyLink_setsApplyLinkOnTournamentModel() {
        String html = """
                <div id="comp-mkaoal7x1" data-semantic-classname="button">
                  <a data-testid="linkElement"
                     href="https://www.street-jiujitsu.com/\uBCF5\uC81C-\uC2A4\uD2B8\uB9BF153-\uC131\uB0A8-\uC624\uD508"
                     target="_self">
                    <span>\uCC38\uAC00\uC2E0\uCCAD</span>
                  </a>
                </div>
                """;

        TournamentModel tournamentModel = new TournamentModel();

        crawler.extractApplyLink(html, tournamentModel);

        assertThat(tournamentModel.getApplyLink())
                .isEqualTo("https://www.street-jiujitsu.com/\uBCF5\uC81C-\uC2A4\uD2B8\uB9BF153-\uC131\uB0A8-\uC624\uD508");
    }
}
