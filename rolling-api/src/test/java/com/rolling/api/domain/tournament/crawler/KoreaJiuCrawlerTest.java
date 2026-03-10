package com.rolling.api.domain.tournament.crawler;

import com.rolling.api.domain.tournament.model.TournamentModel;
import com.rolling.api.domain.tournament.service.KoreaJiuCrawlerService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class KoreaJiuCrawlerTest {

    @Test
    @DisplayName("목록 페이지에서 같은 참가 링크를 공유해도 카드별 상세 URL로 분리해서 크롤링한다")
    void crawlFromListHtml_extractsListAndDetailFields() {
        KoreaJiuCrawlerService crawlerService = mock(KoreaJiuCrawlerService.class);
        KoreaJiuCrawler crawler = new KoreaJiuCrawler(crawlerService);
        String listPageUrl = "https://www.koreajiu.com/%EB%B3%B5%EC%A0%9C-%EB%8C%80%ED%9A%8C%ED%95%98%EC%9D%B4%EB%9D%BC%EC%9D%B4%ED%8A%B8";
        String html = """
                <section class="comp-mlj4er432 wixui-column-strip">
                    <div class="comp-mlj4er45 wixui-column-strip__column">
                        <wow-image>
                            <img
                                src="https://static.wixstatic.com/media/poster-small.png"
                                srcset="https://static.wixstatic.com/media/poster-large.png 1x, https://static.wixstatic.com/media/poster-2x.png 2x"
                                alt="poster" />
                        </wow-image>
                    </div>
                    <div class="comp-mlj4er47 wixui-column-strip__column">
                        <div class="wixui-rich-text">
                            <h2>2026 안산시장배 전국 주짓수 챔피언쉽</h2>
                        </div>
                        <a href="https://www.koreajiu.com/tournaments/ansan-open">상세 보기</a>
                        <div data-semantic-classname="button">
                            <a data-testid="linkElement" href="https://www.koreajiu.com/about" aria-label="참가신청란 ">
                                <span data-testid="stylablebutton-label">참가신청란 </span>
                            </a>
                        </div>
                    </div>
                </section>
                <section class="comp-mlj4er432 wixui-column-strip">
                    <div class="comp-mlj4er45 wixui-column-strip__column">
                        <wow-image>
                            <img
                                src="https://static.wixstatic.com/media/poster-b-small.png"
                                srcset="https://static.wixstatic.com/media/poster-b-large.png 1x, https://static.wixstatic.com/media/poster-b-2x.png 2x"
                                alt="poster" />
                        </wow-image>
                    </div>
                    <div class="comp-mlj4er47 wixui-column-strip__column">
                        <div class="wixui-rich-text">
                            <h2>2026 인천시장배 전국 주짓수 챔피언쉽</h2>
                        </div>
                        <a href="https://www.koreajiu.com/tournaments/incheon-open">상세 보기</a>
                        <div data-semantic-classname="button">
                            <a data-testid="linkElement" href="https://www.koreajiu.com/about" aria-label="참가신청란 ">
                                <span data-testid="stylablebutton-label">참가신청란 </span>
                            </a>
                        </div>
                    </div>
                </section>
                """;

        when(crawlerService.crawlDetailPage(eq("https://www.koreajiu.com/tournaments/ansan-open"), any(TournamentModel.class)))
                .thenAnswer(invocation -> invocation.getArgument(1));
        when(crawlerService.crawlDetailPage(eq("https://www.koreajiu.com/tournaments/incheon-open"), any(TournamentModel.class)))
                .thenAnswer(invocation -> invocation.getArgument(1));

        List<TournamentModel> tournaments = crawler.crawlFromListHtml(html, listPageUrl);

        assertThat(tournaments).hasSize(2);

        TournamentModel firstTournament = tournaments.get(0);
        assertThat(firstTournament.getTitle()).isEqualTo("2026 안산시장배 전국 주짓수 챔피언쉽");
        assertThat(firstTournament.getPosterUrl()).isNull();
        assertThat(firstTournament.getApplyLink()).isEqualTo("https://www.koreajiu.com/tournaments/ansan-open");

        TournamentModel secondTournament = tournaments.get(1);
        assertThat(secondTournament.getTitle()).isEqualTo("2026 인천시장배 전국 주짓수 챔피언쉽");
        assertThat(secondTournament.getPosterUrl()).isNull();
        assertThat(secondTournament.getApplyLink()).isEqualTo("https://www.koreajiu.com/tournaments/incheon-open");
    }
}
