package com.rolling.api.domain.tournament.service;

import com.rolling.api.domain.tournament.model.TournamentModel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StreetJiuJitsuCrawlerServiceTest {

    private final StreetJiuJitsuCrawlerService crawlerService = new StreetJiuJitsuCrawlerService();

    @Test
    @DisplayName("상세 페이지 HTML에서 키워드 기반으로 대회 핵심 필드를 파싱한다")
    void parseDetailHtml_extractsTournamentFields() {
        String detailUrl = "https://www.street-jiujitsu.com/tournament-detail";
        String html = """
                <div id="comp-random-title-id">
                    <h1>스트릿 주짓수 154 전주 오픈</h1>
                </div>
                <div id="comp-random-info-id">
                    ▶ 시합 일정 : 2026년 3월 7일 (토) 오전 11:00
                    ▶ 시합 장소 : 전주대학교 체육관
                    ▶ 주최/주관 : 스트릿 주짓수
                    ▶ 시합 접수
                    일반접수 : 2026년 2월 27일 (금) 까지
                </div>
                """;

        TournamentModel tournamentModel = crawlerService.parseDetailHtml(html, detailUrl);

        assertThat(tournamentModel.getTitle()).isEqualTo("스트릿 주짓수 154 전주 오픈");
        assertThat(tournamentModel.getCompetitionDate()).isEqualTo("2026년 3월 7일");
        assertThat(tournamentModel.getLocation()).isEqualTo("전주대학교 체육관");
        assertThat(tournamentModel.getOrganizer()).isEqualTo("스트릿 주짓수");
        assertThat(tournamentModel.getRegistrationDeadline()).isEqualTo("2026년 2월 27일");
        assertThat(tournamentModel.getApplyLink()).isEqualTo(detailUrl);
    }

    @Test
    @DisplayName("신청 페이지의 접수 마감 문구에서도 접수 마감일을 추출한다")
    void parseDetailHtml_extractsRegistrationDeadlineFromScheduleText() {
        String detailUrl = "https://www.street-jiujitsu.com/tournament-apply";
        String html = """
                <div>
                    <h1>스트릿 주짓수 155 부산 오픈</h1>
                </div>
                <div>
                    ▶ 시합 일정 : 2026년 3월 8일 (일) 오전 11:00
                    ▶ 시합 장소 : 부산 사직실내체육관
                    ▶ 주최/주관 : 스트릿 주짓수
                </div>
                <span style="color:#FFFFFF;" class="wixui-rich-text__text">
                    ▶ 시합 스케줄
                    - 접수 마감 : 2026년 2월 27일 (금)
                    - 선수명단 확인, 단독출전 수정 및 환불 기간 : 2월 28일 ~ 2월 29일
                    - 대진표 및 타임테이블 확인 : 3월 4일 ~ 3월 5일
                </span>
                """;

        TournamentModel tournamentModel = crawlerService.parseDetailHtml(html, detailUrl);

        assertThat(tournamentModel.getTitle()).isEqualTo("스트릿 주짓수 155 부산 오픈");
        assertThat(tournamentModel.getCompetitionDate()).isEqualTo("2026년 3월 8일");
        assertThat(tournamentModel.getRegistrationDeadline()).isEqualTo("2026년 2월 27일");
        assertThat(tournamentModel.getApplyLink()).isEqualTo(detailUrl);
    }

    @Test
    @DisplayName("접수 마감일에 연도가 없으면 대회일 기준 연도를 보정한다")
    void parseDetailHtml_infersRegistrationDeadlineYearFromCompetitionDate() {
        String detailUrl = "https://www.street-jiujitsu.com/tournament-detail";
        String html = """
                <div>
                    <h1>스트릿 주짓수 156 대구 오픈</h1>
                </div>
                <div>
                    ▶ 시합 일정 : 2026년 3월 15일 (일) 오전 11:00
                    ▶ 시합 장소 : 대구체육관
                    ▶ 주최/주관 : 스트릿 주짓수
                    ▶ 시합 접수
                    일반접수 : 3월 6일 (금) 까지 신청 및 입금
                </div>
                """;

        TournamentModel tournamentModel = crawlerService.parseDetailHtml(html, detailUrl);

        assertThat(tournamentModel.getCompetitionDate()).isEqualTo("2026년 3월 15일");
        assertThat(tournamentModel.getRegistrationDeadline()).isEqualTo("2026년 3월 6일");
    }

    @Test
    @DisplayName("일부 키워드를 찾지 못해도 예외 없이 null로 반환한다")
    void parseDetailHtml_whenKeywordsMissing_returnsNullFields() {
        String detailUrl = "https://www.street-jiujitsu.com/tournament-detail";
        String html = """
                <div><h1>다른 타이틀</h1></div>
                <div>내용 없음</div>
                """;

        TournamentModel tournamentModel = crawlerService.parseDetailHtml(html, detailUrl);

        assertThat(tournamentModel.getTitle()).isNull();
        assertThat(tournamentModel.getCompetitionDate()).isNull();
        assertThat(tournamentModel.getLocation()).isNull();
        assertThat(tournamentModel.getOrganizer()).isNull();
        assertThat(tournamentModel.getRegistrationDeadline()).isNull();
        assertThat(tournamentModel.getApplyLink()).isEqualTo(detailUrl);
    }
}
