package com.rolling.api.domain.tournament.service;

import com.rolling.api.domain.tournament.model.TournamentModel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class KoreaJiuCrawlerServiceTest {

    private final KoreaJiuCrawlerService crawlerService = new KoreaJiuCrawlerService();

    @Test
    @DisplayName("상세 페이지 줄 단위 텍스트에서 대회일정, 마감일, 장소를 과다 추출 없이 파싱한다")
    void parseDetailHtml_extractsTournamentFields() {
        String detailUrl = "https://www.koreajiu.com/about";
        TournamentModel seed = new TournamentModel();
        seed.setTitle("코리아 주짓수 챔피언십");
        seed.setPosterUrl("https://static.wixstatic.com/poster.png");
        seed.setApplyLink(detailUrl);

        String html = """
                <div class="wixui-rich-text">
                    <h1>2026 안산시장배 전국 주짓수 챔피언쉽</h1>
                </div>
                <div id="comp-ll0lmtbu" class="wixui-rich-text">
                    <p>대회일정</p>
                    <p>2026년 4월 4일 토요일</p>
                    <p>시작 : 2026년 3월 1일 일요일</p>
                    <p>마감 : 2025년 3월 29일 일요일 (※ 등록인원 이 마감되면 조기 마감 될수 있습니다)</p>
                    <p>시합장소 (LOCATION)</p>
                    <p>시합장소 :상록수 체육관 경기도 안산시 상록구 용신로 422 (본오동 1111-20)</p>
                    <p>참가비 : 50,000원</p>
                    <p>기타 안내 : 주차 가능</p>
                </div>
                """;

        TournamentModel tournamentModel = crawlerService.parseDetailHtml(html, detailUrl, seed);

        assertThat(tournamentModel.getTitle()).isEqualTo("2026 안산시장배 전국 주짓수 챔피언쉽");
        assertThat(tournamentModel.getPosterUrl()).isEqualTo("https://static.wixstatic.com/poster.png");
        assertThat(tournamentModel.getOrganizer()).isEqualTo("코리아 주짓수 챔피언십");
        assertThat(tournamentModel.getCompetitionDate()).isEqualTo("2026년 4월 4일");
        assertThat(tournamentModel.getRegistrationDeadline()).isEqualTo("2025년 3월 29일");
        assertThat(tournamentModel.getLocation()).isEqualTo("상록수 체육관 경기도 안산시 상록구 용신로 422 (본오동 1111-20)");
        assertThat(tournamentModel.getApplyLink()).isEqualTo(detailUrl);
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
        assertThat(tournamentModel.getApplyLink()).isEqualTo("https://www.koreajiu.com/about");
    }
}
