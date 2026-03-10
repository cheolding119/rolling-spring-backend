package com.rolling.api.domain.tournament.controller;

import com.rolling.api.domain.tournament.dto.TournamentCrawlResult;
import com.rolling.api.domain.tournament.service.TournamentManagerService;
import com.rolling.api.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Tournament", description = "대회 크롤링/관리 API")
@RestController
@RequestMapping("/api/v1/tournaments")
@RequiredArgsConstructor
public class TournamentCrawlerController {

    private final TournamentManagerService tournamentManagerService;

    @Operation(summary = "대회 크롤링 수동 실행", description = "등록된 크롤러를 실행하고 결과를 DB에 upsert 저장합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "실행 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @PostMapping("/crawl")
    public ResponseEntity<ApiResponse<TournamentCrawlResult>> crawlAndSave() {
        TournamentCrawlResult response = tournamentManagerService.crawlAndSaveAll();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "스트릿 주짓수 크롤링 수동 실행", description = "스트릿 주짓수 크롤러만 실행하고 결과를 DB에 upsert 저장합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "실행 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "지원하지 않는 크롤러"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @PostMapping("/crawl/street")
    public ResponseEntity<ApiResponse<TournamentCrawlResult>> crawlStreet() {
        TournamentCrawlResult response = tournamentManagerService.crawlAndSaveBySource("street");
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "코주챔 크롤링 수동 실행", description = "코리아 주짓수 챔피언십 크롤러만 실행하고 결과를 DB에 upsert 저장합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "실행 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "지원하지 않는 크롤러"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @PostMapping("/crawl/koreajiu")
    public ResponseEntity<ApiResponse<TournamentCrawlResult>> crawlKoreaJiu() {
        TournamentCrawlResult response = tournamentManagerService.crawlAndSaveBySource("koreajiu");
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "히어로즈 오브 주짓수 크롤링 수동 실행", description = "히어로즈 오브 주짓수 크롤러만 실행하고 결과를 DB에 upsert 저장합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "실행 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "지원하지 않는 크롤러"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @PostMapping("/crawl/heroes")
    public ResponseEntity<ApiResponse<TournamentCrawlResult>> crawlHeroes() {
        TournamentCrawlResult response = tournamentManagerService.crawlAndSaveBySource("heroes");
        return ResponseEntity.ok(ApiResponse.success(response));
    }

}
