package com.rolling.api.domain.tournament.crawler;

import com.rolling.api.domain.tournament.model.TournamentModel;
import com.rolling.api.domain.tournament.service.HeroesOfJiuJitsuCrawlerService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

class HeroesOfJiuJitsuCrawlerTest {

    @Test
    @DisplayName("중복된 상세 링크가 있어도 정보가 더 많은 카드 하나만 추출한다")
    void crawlFromListHtml_deduplicatesByDetailUrl() {
        HeroesOfJiuJitsuCrawlerService crawlerService = spy(new HeroesOfJiuJitsuCrawlerService());
        HeroesOfJiuJitsuCrawler crawler = new HeroesOfJiuJitsuCrawler(crawlerService);
        String listPageUrl = "https://www.heroesofsports.kr/events/application";
        String html = """
                <a href="/events/application/240">
                    <div class="featured-card">
                        <h3>상단 추천</h3>
                    </div>
                </a>
                <a href="/events/application/240"><div class="group flex flex-col items-center sm:flex-row sm:items-start gap-5 sm:gap-8 lg:gap-12"><div class="relative w-full max-w-[240px] sm:w-72 sm:max-w-none lg:w-[340px] flex-shrink-0 rounded-2xl overflow-hidden shadow-xl ring-1 ring-gray-200 group-hover:shadow-2xl group-hover:ring-heroRed/20 transition-all duration-500"><img class="w-full aspect-[3/4] object-cover group-hover:scale-[1.03] transition-transform duration-700" src="https://sportseventsolution.s3.ap-northeast-2.amazonaws.com/attachment/1772135305441-KakaoTalk_20260122_191340231_02.jpg" alt="2026 히어로즈 오브 주짓수 리그 시즌16(대구)"><div class="absolute bottom-0 left-0 right-0 h-1 bg-gradient-to-r from-heroRed to-transparent scale-x-0 group-hover:scale-x-100 transition-transform duration-500 origin-left"></div></div><div class="text-center sm:text-left flex-1 sm:py-2 lg:py-4"><h3 class="text-base sm:text-2xl lg:text-3xl font-black text-gray-900 leading-snug mb-3 sm:mb-5 group-hover:text-heroRed transition-colors">2026 히어로즈 오브 주짓수 리그 시즌16(대구)</h3><div class="space-y-1.5 sm:space-y-2.5 mb-4 sm:mb-6 inline-flex flex-col items-center sm:items-start"><div class="flex items-center gap-2 sm:gap-2.5 text-sm sm:text-base lg:text-lg text-gray-700"><span class="font-bold">2026년 4월 5일</span></div><div class="flex items-center gap-2 sm:gap-2.5 text-sm sm:text-base lg:text-lg text-gray-700"><span class="font-bold">대구 시민체육관</span></div><div class="flex items-center gap-2 sm:gap-2.5 text-sm sm:text-base lg:text-lg text-gray-700"><span class="font-bold">신청 마감: 26.03.27</span></div></div><span>신청하기</span></div></div></a>
                """;
        doAnswer(invocation -> invocation.getArgument(1)).when(crawlerService)
                .crawlDetailPage(eq("https://www.heroesofsports.kr/events/application/240"), any(TournamentModel.class));

        List<TournamentModel> tournaments = crawler.crawlFromListHtml(html, listPageUrl);

        assertThat(tournaments).hasSize(1);
        TournamentModel tournament = tournaments.get(0);
        assertThat(tournament.getTitle()).isEqualTo("2026 히어로즈 오브 주짓수 리그 시즌16(대구)");
        assertThat(tournament.getCompetitionDate()).isEqualTo("2026년 4월 5일");
        assertThat(tournament.getRegistrationDeadline()).isEqualTo("2026년 3월 27일");
        assertThat(tournament.getLocation()).isEqualTo("대구 시민체육관");
        assertThat(tournament.getApplyLink()).isEqualTo("https://www.heroesofsports.kr/events/application/240");
        assertThat(tournament.getPosterUrl())
                .isEqualTo("https://sportseventsolution.s3.ap-northeast-2.amazonaws.com/attachment/1772135305441-KakaoTalk_20260122_191340231_02.jpg");
        verify(crawlerService, times(1))
                .crawlDetailPage(eq("https://www.heroesofsports.kr/events/application/240"), any(TournamentModel.class));
    }

    @Test
    @DisplayName("신청하기 버튼이 없는 상세 링크는 신청 가능 대회로 보지 않는다")
    void crawlFromListHtml_skipsNonOpenTournamentAnchors() {
        HeroesOfJiuJitsuCrawlerService crawlerService = spy(new HeroesOfJiuJitsuCrawlerService());
        HeroesOfJiuJitsuCrawler crawler = new HeroesOfJiuJitsuCrawler(crawlerService);
        String listPageUrl = "https://www.heroesofsports.kr/events/application";
        String html = """
                <a href="/events/application/239">
                    <div>
                        <h3>2025 히어로즈 오브 주짓수 리그 시즌15(마산)</h3>
                        <span>대회 결과 보기</span>
                    </div>
                </a>
                <a href="/events/application/240">
                    <div>
                        <h3>2026 히어로즈 오브 주짓수 리그 시즌16(대구)</h3>
                        <span class="font-bold">2026년 4월 5일</span>
                        <span class="font-bold">대구 시민체육관</span>
                        <span class="font-bold">신청 마감: 26.03.27</span>
                        <span>신청하기</span>
                    </div>
                </a>
                """;
        doAnswer(invocation -> invocation.getArgument(1)).when(crawlerService)
                .crawlDetailPage(eq("https://www.heroesofsports.kr/events/application/240"), any(TournamentModel.class));

        List<TournamentModel> tournaments = crawler.crawlFromListHtml(html, listPageUrl);

        assertThat(tournaments).hasSize(1);
        assertThat(tournaments.get(0).getApplyLink()).isEqualTo("https://www.heroesofsports.kr/events/application/240");
    }

    @Test
    @DisplayName("application 페이지 번들 경로를 추출한다")
    void extractApplicationPageBundleUrl_extractsBundlePath() {
        HeroesOfJiuJitsuCrawler crawler = new HeroesOfJiuJitsuCrawler(new HeroesOfJiuJitsuCrawlerService());
        String listPageUrl = "https://www.heroesofsports.kr/events/application";
        String pageHtml = """
                <html>
                <head>
                    <script src="/_next/static/chunks/app/(service)/(default)/events/application/page-0bd8d15f142985cc.js"></script>
                </head>
                </html>
                """;

        String bundleUrl = crawler.extractApplicationPageBundleUrl(pageHtml, listPageUrl);

        assertThat(bundleUrl)
                .isEqualTo("https://www.heroesofsports.kr/_next/static/chunks/app/(service)/(default)/events/application/page-0bd8d15f142985cc.js");
    }

    @Test
    @DisplayName("번들 스크립트에서 목록 조회 server action id를 추출한다")
    void extractListActionId_extractsActionId() {
        HeroesOfJiuJitsuCrawler crawler = new HeroesOfJiuJitsuCrawler(new HeroesOfJiuJitsuCrawlerService());
        String bundleScript = """
                var m=s(31162),h=(0,m.$)("8208a698e76f13d37f03139731f23471dcb3c5e2"),u=(0,m.$)("33a327354f77d43fbe7650efe1a478c73a836709");
                function E(){h("application",1,100).then(e=>e);u().then(e=>e)}
                """;

        String actionId = crawler.extractListActionId(bundleScript);

        assertThat(actionId).isEqualTo("8208a698e76f13d37f03139731f23471dcb3c5e2");
    }
}
