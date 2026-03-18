package com.rolling.api.domain.tournament.controller;

import com.rolling.api.domain.tournament.dto.TournamentCrawlResult;
import com.rolling.api.domain.tournament.entity.TournamentSource;
import com.rolling.api.domain.tournament.service.TournamentManagerService;
import com.rolling.api.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Tournament", description = "대회 크롤링/관리 API")
@RestController
@RequestMapping("/api/v1/tournaments")
@RequiredArgsConstructor
public class TournamentCrawlerController {

    private final TournamentManagerService tournamentManagerService;

    @Operation(summary = "대회 크롤링 수동 실행", description = "source를 지정하면 해당 출처 크롤러만, 생략하면 전체 크롤러를 실행하고 결과를 DB에 upsert 저장합니다.")
    @Parameter(name = "X-Crawler-Admin-Key", in = ParameterIn.HEADER, required = true, description = "운영 API 인증 키")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "실행 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "지원하지 않는 크롤러 또는 잘못된 source"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @PostMapping("/crawl")
    public ResponseEntity<ApiResponse<TournamentCrawlResult>> crawlAndSave(
            @Parameter(description = "실행할 출처 (STREET_JIU_JITSU, KOREA_JIU, HEROES_OF_JIU_JITSU)")
            @RequestParam(required = false) TournamentSource source) {
        TournamentCrawlResult response = source == null
                ? tournamentManagerService.crawlAndSaveAll()
                : tournamentManagerService.crawlAndSaveBySource(source);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
