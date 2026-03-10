package com.rolling.api.domain.tournament.service;

import com.rolling.api.domain.tournament.model.TournamentModel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class
HeroesOfJiuJitsuCrawlerServiceTest {

    private final HeroesOfJiuJitsuCrawlerService crawlerService = new HeroesOfJiuJitsuCrawlerService();

    @Test
    @DisplayName("목록 카드에서 대회 핵심 필드를 파싱한다")
    void parseListCardHtml_extractsTournamentFields() {
        String detailUrl = "https://www.heroesofsports.kr/events/application/240";
        String posterUrl = "https://sportseventsolution.s3.ap-northeast-2.amazonaws.com/attachment/1772135305441-KakaoTalk_20260122_191340231_02.jpg";
        String html = """
                <a href="/events/application/240"><div class="group flex flex-col items-center sm:flex-row sm:items-start gap-5 sm:gap-8 lg:gap-12"><div class="relative w-full max-w-[240px] sm:w-72 sm:max-w-none lg:w-[340px] flex-shrink-0 rounded-2xl overflow-hidden shadow-xl ring-1 ring-gray-200 group-hover:shadow-2xl group-hover:ring-heroRed/20 transition-all duration-500"><img class="w-full aspect-[3/4] object-cover group-hover:scale-[1.03] transition-transform duration-700" src="%s" alt="2026 히어로즈 오브 주짓수 리그 시즌16(대구)"><div class="absolute bottom-0 left-0 right-0 h-1 bg-gradient-to-r from-heroRed to-transparent scale-x-0 group-hover:scale-x-100 transition-transform duration-500 origin-left"></div></div><div class="text-center sm:text-left flex-1 sm:py-2 lg:py-4"><h3 class="text-base sm:text-2xl lg:text-3xl font-black text-gray-900 leading-snug mb-3 sm:mb-5 group-hover:text-heroRed transition-colors">2026 히어로즈 오브 주짓수 리그 시즌16(대구)</h3><div class="space-y-1.5 sm:space-y-2.5 mb-4 sm:mb-6 inline-flex flex-col items-center sm:items-start"><div class="flex items-center gap-2 sm:gap-2.5 text-sm sm:text-base lg:text-lg text-gray-700"><span class="font-bold">2026년 4월 5일</span></div><div class="flex items-center gap-2 sm:gap-2.5 text-sm sm:text-base lg:text-lg text-gray-700"><span class="font-bold">대구 시민체육관</span></div><div class="flex items-center gap-2 sm:gap-2.5 text-sm sm:text-base lg:text-lg text-gray-700"><span class="font-bold">신청 마감: 26.03.27</span></div></div></div></div></a>
                """.replace("%s", posterUrl);

        TournamentModel tournamentModel = crawlerService.parseListCardHtml(html, detailUrl);

        assertThat(tournamentModel.getTitle()).isEqualTo("2026 히어로즈 오브 주짓수 리그 시즌16(대구)");
        assertThat(tournamentModel.getCompetitionDate()).isEqualTo("2026년 4월 5일");
        assertThat(tournamentModel.getRegistrationDeadline()).isEqualTo("2026년 3월 27일");
        assertThat(tournamentModel.getLocation()).isEqualTo("대구 시민체육관");
        assertThat(tournamentModel.getPosterUrl()).isEqualTo(posterUrl);
        assertThat(tournamentModel.getOrganizer()).isNull();
        assertThat(tournamentModel.getApplyLink()).isEqualTo(detailUrl);
    }

    @Test
    @DisplayName("카드 구조가 부족해도 applyLink는 유지한다")
    void parseListCardHtml_whenFieldsMissing_keepsApplyLink() {
        String detailUrl = "https://www.heroesofsports.kr/events/application/240";

        TournamentModel tournamentModel = crawlerService.parseListCardHtml("<a href=\"/events/application/240\">내용 없음</a>", detailUrl);

        assertThat(tournamentModel.getTitle()).isNull();
        assertThat(tournamentModel.getCompetitionDate()).isNull();
        assertThat(tournamentModel.getRegistrationDeadline()).isNull();
        assertThat(tournamentModel.getLocation()).isNull();
        assertThat(tournamentModel.getApplyLink()).isEqualTo(detailUrl);
    }

    @Test
    @DisplayName("상세 페이지에서 organizer, posterUrl과 누락 필드를 보강한다")
    void parseDetailHtml_enrichesTournamentFields() {
        String detailUrl = "https://www.heroesofsports.kr/events/application/240";
        String posterUrl = "https://sportseventsolution.s3.ap-northeast-2.amazonaws.com/attachment/1772135305441-KakaoTalk_20260122_191340231_02.jpg";
        String html = """
                <html>
                <body>
                <div class="relative w-full min-h-[480px] overflow-hidden bg-black">
                    <img src="__POSTER_URL__" alt="" class="absolute inset-0 w-full h-full object-cover">
                    <div>
                        <h1>2026 히어로즈 오브 주짓수 리그 시즌16(대구)</h1>
                        <div>
                            <div>대회 일자</div>
                            <div>2026년 4월 5일</div>
                        </div>
                    </div>
                </div>
                <div class="divide-y divide-gray-100">
                    <div class="px-5 py-3 flex items-center justify-between">
                        <div>
                            <span>신청 마감</span>
                            <span>~ 26.03.27 23:59</span>
                        </div>
                    </div>
                </div>
                <div>
                    <span class="text-sm text-gray-900 font-bold">대구 시민체육관</span>
                    <iframe src="https://maps.google.com/maps?q=%EB%8C%80%EA%B5%AC%20%EC%8B%9C%EB%AF%BC%EC%B2%B4%EC%9C%A1%EA%B4%80&output=embed&hl=ko"></iframe>
                </div>
                <div class="px-5 py-3 flex items-center gap-2.5">
                    <span class="text-gray-400 text-xs font-bold w-14 shrink-0">공동주관</span>
                    <span class="text-sm text-gray-800 font-medium">히어로즈 오브 스포츠, 굿네이버스</span>
                </div>
                </body>
                </html>
                """.replace("__POSTER_URL__", posterUrl);

        TournamentModel seed = new TournamentModel();
        seed.setApplyLink(detailUrl);

        TournamentModel tournamentModel = crawlerService.parseDetailHtml(html, detailUrl, seed);

        assertThat(tournamentModel.getTitle()).isEqualTo("2026 히어로즈 오브 주짓수 리그 시즌16(대구)");
        assertThat(tournamentModel.getCompetitionDate()).isEqualTo("2026년 4월 5일");
        assertThat(tournamentModel.getRegistrationDeadline()).isEqualTo("2026년 3월 27일");
        assertThat(tournamentModel.getLocation()).isEqualTo("대구 시민체육관");
        assertThat(tournamentModel.getPosterUrl()).isEqualTo(posterUrl);
        assertThat(tournamentModel.getOrganizer()).isEqualTo("히어로즈 오브 스포츠, 굿네이버스");
        assertThat(tournamentModel.getApplyLink()).isEqualTo(detailUrl);
    }

    @Test
    @DisplayName("server action 응답에서 현재 신청 가능한 대회만 추출한다")
    void parseServerActionResponse_extractsActiveTournaments() {
        String listPageUrl = "https://www.heroesofsports.kr/events/application";
        String responseBody = """
                0:["$@1",["ref",null]]
                1:{"listData":[{"id":"$n240","title":"2026 히어로즈 오브 주짓수 리그 시즌16(대구)","meta":{"coHost":"히어로즈 오브 스포츠, 굿네이버스","venue":"대구 시민체육관","thumbnail":"https://sportseventsolution.s3.ap-northeast-2.amazonaws.com/attachment/poster.jpg","eventDate":"2099-04-04T15:00:00.000Z","applicaitonStartDate":"2000-03-01T15:00:00.000Z","applicaitonEndDate":"2099-03-27T14:59:59.999Z","isPublished":true},"isApproved":true,"isPrivate":false},{"id":"$n241","title":"오픈 예정 대회","meta":{"coHost":"히어로즈 오브 스포츠","venue":"서울 체육관","thumbnail":"https://example.com/upcoming.jpg","eventDate":"2999-04-04T15:00:00.000Z","applicaitonStartDate":"2999-03-01T15:00:00.000Z","applicaitonEndDate":"2999-03-27T14:59:59.999Z","isPublished":true},"isApproved":true,"isPrivate":false}],"totalCount":2}
                """;

        TournamentModel tournamentModel = crawlerService.parseServerActionResponse(responseBody, listPageUrl).get(0);

        assertThat(crawlerService.parseServerActionResponse(responseBody, listPageUrl)).hasSize(1);
        assertThat(tournamentModel.getTitle()).isEqualTo("2026 히어로즈 오브 주짓수 리그 시즌16(대구)");
        assertThat(tournamentModel.getCompetitionDate()).isEqualTo("2099-04-05");
        assertThat(tournamentModel.getRegistrationDeadline()).isEqualTo("2099-03-27");
        assertThat(tournamentModel.getLocation()).isEqualTo("대구 시민체육관");
        assertThat(tournamentModel.getPosterUrl()).isEqualTo("https://sportseventsolution.s3.ap-northeast-2.amazonaws.com/attachment/poster.jpg");
        assertThat(tournamentModel.getOrganizer()).isEqualTo("히어로즈 오브 스포츠, 굿네이버스");
        assertThat(tournamentModel.getApplyLink()).isEqualTo("https://www.heroesofsports.kr/events/application/240");
    }
}
