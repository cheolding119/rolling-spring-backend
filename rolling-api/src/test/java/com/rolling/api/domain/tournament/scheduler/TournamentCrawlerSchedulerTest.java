package com.rolling.api.domain.tournament.scheduler;

import com.rolling.api.domain.tournament.dto.TournamentCrawlResult;
import com.rolling.api.domain.tournament.service.TournamentManagerService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TournamentCrawlerSchedulerTest {

    @Mock
    private TournamentManagerService tournamentManagerService;

    @Test
    @DisplayName("스케줄러는 대회 크롤링 저장 서비스를 호출한다")
    void scheduledCrawlAndSave_callsManagerService() {
        TournamentCrawlerScheduler scheduler = new TournamentCrawlerScheduler(tournamentManagerService);
        when(tournamentManagerService.crawlAndSaveAll()).thenReturn(
                TournamentCrawlResult.builder()
                        .crawledCount(5)
                        .createdCount(2)
                        .updatedCount(2)
                        .skippedCount(1)
                        .build()
        );

        scheduler.scheduledCrawlAndSave();

        verify(tournamentManagerService).crawlAndSaveAll();
    }

    @Test
    @DisplayName("스케줄러 실행 중 예외가 발생해도 메서드 예외를 전파하지 않는다")
    void scheduledCrawlAndSave_whenServiceFails_doesNotThrow() {
        TournamentCrawlerScheduler scheduler = new TournamentCrawlerScheduler(tournamentManagerService);
        when(tournamentManagerService.crawlAndSaveAll()).thenThrow(new RuntimeException("crawl failed"));

        assertThatCode(scheduler::scheduledCrawlAndSave).doesNotThrowAnyException();
    }
}
