package com.rolling.api.domain.tournament.crawler;

import com.rolling.api.domain.tournament.entity.TournamentSource;
import com.rolling.api.domain.tournament.model.TournamentModel;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FlowCompCrawlerTest {

    private final FlowCompCrawler crawler = new FlowCompCrawler();

    @Test
    void parsesUpcomingTournamentWithoutImage() {
        String response = """
                {
                  "success": true,
                  "pagination": {"totalPage": 2},
                  "response": [
                    {
                      "seq": 76,
                      "csName": "PROVA20 화성",
                      "csDate": "2099-10-03 09:00",
                      "csAddress": "경기 화성시 향남로 470",
                      "csAddressDetail": "실내체육관",
                      "csRegisterEndDate": "2099-09-30 00:00",
                      "csImageUrl": "https://example.com/poster.jpg"
                    },
                    {
                      "seq": 104,
                      "csName": "장소 미정 대회",
                      "csDate": "2099-11-28 10:00",
                      "csAddress": null,
                      "csAddressDetail": null
                    }
                  ]
                }
                """;

        FlowCompCrawler.PageResult page = crawler.parsePage(response);

        assertThat(page.totalPages()).isEqualTo(2);
        assertThat(page.tournaments()).hasSize(1);
        TournamentModel tournament = page.tournaments().get(0);
        assertThat(tournament.getSource()).isEqualTo(TournamentSource.FLOWCOMP);
        assertThat(tournament.getTitle()).isEqualTo("PROVA20 화성");
        assertThat(tournament.getCompetitionDate()).isEqualTo("2099-10-03");
        assertThat(tournament.getRegistrationDeadline()).isEqualTo("2099-09-30");
        assertThat(tournament.getLocation()).isEqualTo("경기 화성시 향남로 470 실내체육관");
        assertThat(tournament.getApplyLink()).isEqualTo("https://www.flowcomp.co.kr/championship/76");
        assertThat(tournament.getOrganizer()).isNull();
        assertThat(tournament.getPosterUrl()).isNull();
    }

    @Test
    void rejectsInvalidResponse() {
        assertThatThrownBy(() -> crawler.parsePage("{\"success\":false,\"response\":[]}"))
                .isInstanceOf(IllegalStateException.class);
    }
}
