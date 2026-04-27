package com.rolling.api.domain.tournament.crawler;

import com.rolling.api.domain.tournament.model.TournamentModel;
import com.rolling.api.domain.tournament.service.SpotliteCrawlerService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SpotliteCrawlerTest {

    private final SpotliteCrawler crawler = new SpotliteCrawler(new SpotliteCrawlerService());

    @Test
    @DisplayName("스포트라이트 목록에서 접수중 대회의 최소 사실 정보만 파싱한다")
    void crawlFromListHtml_extractsRegistrationOpenTournamentFields() {
        String html = """
                <div class="competition-grid">
                    <a href="/jiujitsu/415" class="competition-card"
                        data-status="registration_open"
                        data-reg-end="2026-05-04"
                        data-end="2026-05-10">
                        <div class="card-image-wrapper">
                            <img src="https://imagedelivery.net/example/poster/public" alt="서천 주짓수 페스티벌 with COS">
                        </div>
                        <div class="card-content">
                            <h3 class="card-title">서천 주짓수 페스티벌 with COS</h3>
                            <div class="card-meta">
                                <span class="meta-organizer">주최: 서천군주짓수회</span>
                                <span class="meta-date">날짜: 2026-05-09(토) ~ 05-10(일)</span>
                                <span class="meta-reg">접수기간: 05-04 10:00</span>
                            </div>
                            <div class="card-info">
                                <div class="card-info-item">
                                    <span>충남 서천 한산모시체육관</span>
                                </div>
                            </div>
                        </div>
                    </a>
                    <a href="/jiujitsu/999" class="competition-card"
                        data-status="registration_closed"
                        data-reg-end="2026-04-01"
                        data-end="2026-04-10">
                        <h3 class="card-title">마감된 대회</h3>
                    </a>
                </div>
                """;

        List<TournamentModel> tournaments = crawler.crawlFromListHtml(html, "https://spotlite.co.kr/jiujitsu/");

        assertThat(tournaments).hasSize(1);
        TournamentModel tournament = tournaments.get(0);
        assertThat(tournament.getTitle()).isEqualTo("서천 주짓수 페스티벌 with COS");
        assertThat(tournament.getOrganizer()).isEqualTo("서천군주짓수회");
        assertThat(tournament.getPosterUrl()).isNull();
        assertThat(tournament.getCompetitionDate()).isEqualTo("2026-05-10");
        assertThat(tournament.getRegistrationDeadline()).isEqualTo("2026-05-04");
        assertThat(tournament.getLocation()).isEqualTo("충남 서천 한산모시체육관");
        assertThat(tournament.getApplyLink()).isEqualTo("https://spotlite.co.kr/jiujitsu/415");
    }

    @Test
    @DisplayName("접수중이 아닌 카드는 저장 후보에서 제외한다")
    void crawlFromListHtml_skipsNonOpenStatuses() {
        String html = """
                <a href="/jiujitsu/420" class="competition-card"
                    data-status="before_registration"
                    data-reg-end="2026-05-02"
                    data-end="2026-05-09">
                    <h3 class="card-title">접수 전 대회</h3>
                </a>
                """;

        List<TournamentModel> tournaments = crawler.crawlFromListHtml(html, "https://spotlite.co.kr/jiujitsu/");

        assertThat(tournaments).isEmpty();
    }
}
