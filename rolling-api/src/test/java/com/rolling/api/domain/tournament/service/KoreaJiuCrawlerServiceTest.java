package com.rolling.api.domain.tournament.service;

import com.rolling.api.domain.tournament.model.TournamentModel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class KoreaJiuCrawlerServiceTest {

    private final KoreaJiuCrawlerService crawlerService = new KoreaJiuCrawlerService();

    @Test
    @DisplayName("상세 페이지 상단 참가신청 버튼 앞 포스터와 대회 정보를 함께 파싱한다")
    void parseDetailHtml_extractsPosterAndTournamentFields() {
        String detailUrl = "https://www.koreajiu.com/about";
        TournamentModel seed = new TournamentModel();
        seed.setTitle("코리아 주짓수 챔피언십");
        seed.setPosterUrl("https://static.wixstatic.com/old-poster.png");
        seed.setApplyLink(detailUrl);

        String html = """
                <section id="comp-lb3mhepn" class="wixui-section">
                    <div id="comp-ljgiiglc" class="MazNVa wixui-image">
                        <div data-testid="linkElement" class="j7pOnl">
                            <img fetchpriority="high"
                                 sizes="743px"
                                 src="https://static.wixstatic.com/media/907d60_6fb8ae692c65476f936d01e645a735a8~mv2.png/v1/fill/w_733,h_1100,al_c,q_90,enc_avif,quality_auto/poster.png"
                                 alt="poster"
                                 style="object-fit:cover"
                                 class="BI8PVQ Tj01hh"
                                 width="743"
                                 height="1115">
                        </div>
                    </div>
                    <div id="comp-mmbso583" data-semantic-classname="button">
                        <a href="https://ko.spotlite.co.kr/jiujitsu/367/" aria-label="참가신청 ">참가신청 </a>
                    </div>
                </section>
                <section id="comp-me9qfoo9" class="wixui-section">
                    <div class="wixui-rich-text">
                        <h1>2026 안산시장배 전국 주짓수 챔피언쉽</h1>
                    </div>
                </section>
                <div id="comp-ll0lmtbu" class="wixui-rich-text">
                    <p>날짜 (SCHEDULE)</p>
                    <p>대회일정</p>
                    <p>2026년 4월 4일 토요일</p>
                    <p>등록날짜</p>
                    <p>시작 : 2026년 3월 1일 일요일</p>
                    <p>마감 : 2025년 3월 29일 일요일 (※ 등록인원 이 마감되면 조기 마감 될수 있습니다)</p>
                    <p>시합장소 (LOCATION)</p>
                    <p>시합장소 :상록수 체육관 경기도 안산시 상록구 용신로 422 (본오동 1111-20)</p>
                </div>
                """;

        TournamentModel tournamentModel = crawlerService.parseDetailHtml(html, detailUrl, seed);

        assertThat(tournamentModel.getTitle()).isEqualTo("2026 안산시장배 전국 주짓수 챔피언쉽");
        assertThat(tournamentModel.getPosterUrl()).isEqualTo("https://static.wixstatic.com/media/907d60_6fb8ae692c65476f936d01e645a735a8~mv2.png/v1/fill/w_733,h_1100,al_c,q_90,enc_avif,quality_auto/poster.png");
        assertThat(tournamentModel.getOrganizer()).isEqualTo("코리아 주짓수 챔피언십");
        assertThat(tournamentModel.getCompetitionDate()).isEqualTo("2026년 4월 4일");
        assertThat(tournamentModel.getRegistrationDeadline()).isEqualTo("2025년 3월 29일");
        assertThat(tournamentModel.getLocation()).isEqualTo("상록수 체육관 경기도 안산시 상록구 용신로 422 (본오동 1111-20)");
        assertThat(tournamentModel.getApplyLink()).isEqualTo(detailUrl);
    }

    @Test
    @DisplayName("상세 페이지에서는 참가신청 버튼 섹션의 마지막 세로 포스터를 우선 선택한다")
    void parseDetailHtml_prefersPosterClosestToApplyAction() {
        String detailUrl = "https://www.koreajiu.com/about";
        TournamentModel seed = new TournamentModel();
        seed.setTitle("코리아 주짓수 챔피언십");
        seed.setApplyLink(detailUrl);

        String html = """
                <section class="wixui-section">
                    <div class="wixui-image">
                        <img fetchpriority="high"
                             src="https://static.wixstatic.com/media/907d60_old-top-banner.png"
                             class="BI8PVQ Tj01hh"
                             width="700"
                             height="420"
                             alt="banner">
                    </div>
                    <div class="wixui-image">
                        <img fetchpriority="high"
                             src="https://static.wixstatic.com/media/907d60_current-poster.png"
                             class="BI8PVQ Tj01hh"
                             width="743"
                             height="1115"
                             alt="current poster">
                    </div>
                    <div data-semantic-classname="button">
                        <a href="https://ko.spotlite.co.kr/jiujitsu/367/" aria-label="참가신청 ">참가신청</a>
                    </div>
                </section>
                <section class="wixui-section">
                    <div class="wixui-rich-text">
                        <h1>2026 안산시장배 전국 주짓수 챔피언쉽</h1>
                    </div>
                </section>
                <section class="wixui-section">
                    <div class="wixui-image">
                        <img loading="lazy"
                             src="https://static.wixstatic.com/media/907d60_later-image.jpg"
                             width="979"
                             height="582"
                             alt="later image">
                    </div>
                </section>
                <div id="comp-ll0lmtbu" class="wixui-rich-text">
                    <p>대회일정</p>
                    <p>2026년 4월 4일 토요일</p>
                    <p>마감 : 3월 29일 일요일</p>
                    <p>시합장소</p>
                    <p>안산 상록수 체육관</p>
                </div>
                """;

        TournamentModel tournamentModel = crawlerService.parseDetailHtml(html, detailUrl, seed);

        assertThat(tournamentModel.getPosterUrl()).isEqualTo("https://static.wixstatic.com/media/907d60_current-poster.png");
        assertThat(tournamentModel.getRegistrationDeadline()).isEqualTo("2026년 3월 29일");
    }

    @Test
    @DisplayName("마감일에 연도가 없으면 대회일정의 연도를 기준으로 보정한다")
    void parseDetailHtml_infersDeadlineYearFromCompetitionDate() {
        String detailUrl = "https://www.koreajiu.com/about";
        TournamentModel seed = new TournamentModel();
        seed.setTitle("코리아 주짓수 챔피언십");
        seed.setApplyLink(detailUrl);

        String html = """
                <div id="comp-ll0lmtbu" class="wixui-rich-text">
                    <p>대회일정</p>
                    <p>2026년 4월 4일 토요일</p>
                    <p>마감 : 3월 29일 일요일</p>
                    <p>시합장소</p>
                    <p>안산 상록수 체육관</p>
                </div>
                """;

        TournamentModel tournamentModel = crawlerService.parseDetailHtml(html, detailUrl, seed);

        assertThat(tournamentModel.getCompetitionDate()).isEqualTo("2026년 4월 4일");
        assertThat(tournamentModel.getRegistrationDeadline()).isEqualTo("2026년 3월 29일");
        assertThat(tournamentModel.getLocation()).isEqualTo("안산 상록수 체육관");
    }

    @Test
    @DisplayName("상세 키워드가 없으면 기존 모델 값만 유지한다")
    void parseDetailHtml_whenKeywordsMissing_keepsSeedValues() {
        String detailUrl = "https://www.koreajiu.com/about";
        TournamentModel seed = new TournamentModel();
        seed.setTitle("코리아 주짓수 챔피언십");
        seed.setApplyLink("https://www.koreajiu.com/about");

        TournamentModel tournamentModel = crawlerService.parseDetailHtml("<div>내용 없음</div>", detailUrl, seed);

        assertThat(tournamentModel.getTitle()).isEqualTo("코리아 주짓수 챔피언십");
        assertThat(tournamentModel.getOrganizer()).isEqualTo("코리아 주짓수 챔피언십");
        assertThat(tournamentModel.getCompetitionDate()).isNull();
        assertThat(tournamentModel.getRegistrationDeadline()).isNull();
        assertThat(tournamentModel.getLocation()).isNull();
        assertThat(tournamentModel.getPosterUrl()).isNull();
        assertThat(tournamentModel.getApplyLink()).isEqualTo("https://www.koreajiu.com/about");
    }
}
