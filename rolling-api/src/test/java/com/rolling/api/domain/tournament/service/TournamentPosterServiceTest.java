package com.rolling.api.domain.tournament.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class TournamentPosterServiceTest {

    @Mock
    private S3Presigner s3Presigner;

    private TournamentPosterService tournamentPosterService;

    @BeforeEach
    void setUp() {
        tournamentPosterService = new TournamentPosterService(
                s3Presigner,
                Clock.fixed(Instant.parse("2026-04-11T00:00:00Z"), ZoneId.of("Asia/Seoul"))
        );
        ReflectionTestUtils.setField(tournamentPosterService, "bucket", "rolling-jiujitsu-bucket");
        ReflectionTestUtils.setField(tournamentPosterService, "region", "ap-northeast-2");
    }

    @Test
    @DisplayName("공개 base URL이 있으면 그 도메인으로 포스터 URL을 조립한다")
    void buildPublicUrl_usesConfiguredPublicBaseUrl() {
        ReflectionTestUtils.setField(tournamentPosterService, "publicBaseUrl", "https://cdn.rolling.com/");

        assertThat(tournamentPosterService.buildPublicUrl("tournaments/posters/poster.jpg"))
                .isEqualTo("https://cdn.rolling.com/tournaments/posters/poster.jpg");
    }

    @Test
    @DisplayName("공개 base URL이 없으면 S3 버킷 직링크를 사용한다")
    void buildPublicUrl_fallsBackToS3BucketUrl() {
        ReflectionTestUtils.setField(tournamentPosterService, "publicBaseUrl", "");

        assertThat(tournamentPosterService.buildPublicUrl("tournaments/posters/poster.jpg"))
                .isEqualTo("https://rolling-jiujitsu-bucket.s3.ap-northeast-2.amazonaws.com/tournaments/posters/poster.jpg");
    }
}
