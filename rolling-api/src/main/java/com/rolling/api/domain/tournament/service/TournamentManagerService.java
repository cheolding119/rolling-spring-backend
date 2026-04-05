package com.rolling.api.domain.tournament.service;

import com.rolling.api.domain.tournament.crawler.TournamentCrawler;
import com.rolling.api.domain.tournament.dto.TournamentCrawlResult;
import com.rolling.api.domain.tournament.entity.Tournament;
import com.rolling.api.domain.tournament.entity.TournamentSource;
import com.rolling.api.domain.tournament.model.TournamentModel;
import com.rolling.api.domain.tournament.repository.TournamentRepository;
import com.rolling.api.domain.tournament.util.TournamentDateUtils;
import com.rolling.api.global.exception.BusinessException;
import com.rolling.api.infra.s3.S3Uploader;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TournamentManagerService {

    private static final ZoneId SEOUL_ZONE = ZoneId.of("Asia/Seoul");

    private final List<TournamentCrawler> crawlers;
    private final TournamentRepository tournamentRepository;
    private final S3Uploader s3Uploader;
    private MeterRegistry meterRegistry;

    @Autowired(required = false)
    void setMeterRegistry(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Transactional(readOnly = true)
    public List<TournamentModel> crawlAll() {
        List<TournamentModel> merged = new ArrayList<>();

        for (TournamentCrawler crawler : crawlers) {
            long startedAt = System.nanoTime();
            String source = sourceTag(crawler.getSource());
            try {
                List<TournamentModel> crawled = applySource(crawler.crawlAll(), crawler.getSource());
                recordCrawlDuration(source, startedAt);
                if (!crawled.isEmpty()) {
                    merged.addAll(crawled);
                }
            } catch (Exception e) {
                recordCrawlDuration(source, startedAt);
                incrementTournamentItems(source, "failed", 1);
                log.error("Tournament crawler failed: {}", crawler.getClass().getSimpleName(), e);
            }
        }

        return merged;
    }

    @Transactional
    public TournamentCrawlResult crawlAndSaveAll() {
        int deletedCount = deleteExpiredTournamentsByRegistrationDeadline();
        List<TournamentModel> crawled = crawlAll();
        return saveCrawledTournaments(crawled, deletedCount);
    }

    @Transactional
    public TournamentCrawlResult crawlAndSaveBySource(TournamentSource source) {
        if (source == null) {
            throw BusinessException.badRequest("대회 출처는 필수입니다");
        }

        TournamentCrawler crawler = findCrawlerBySource(source);
        int deletedCount = deleteExpiredTournamentsByRegistrationDeadline();
        List<TournamentModel> crawled = crawlSingle(crawler);
        return saveCrawledTournaments(crawled, deletedCount);
    }

    private TournamentCrawlResult saveCrawledTournaments(List<TournamentModel> crawled, int deletedCount) {
        List<TournamentModel> safeCrawled = crawled == null ? List.of() : crawled;

        int createdCount = 0;
        int updatedCount = 0;
        int skippedCount = 0;

        for (TournamentModel crawledModel : safeCrawled) {
            enrichPosterUrl(crawledModel);
            Optional<NormalizedTournament> normalizedOptional = normalizeAndValidate(crawledModel);
            if (normalizedOptional.isEmpty()) {
                skippedCount++;
                incrementTournamentItems(sourceTag(crawledModel == null ? null : crawledModel.getSource()), "skipped", 1);
                continue;
            }

            UpsertResult result = upsert(normalizedOptional.get());
            if (result == UpsertResult.CREATED) {
                createdCount++;
                incrementTournamentItems(sourceTag(normalizedOptional.get().source()), "created", 1);
            } else {
                updatedCount++;
                incrementTournamentItems(sourceTag(normalizedOptional.get().source()), "updated", 1);
            }
        }

        incrementDeletedTournaments(deletedCount);

        TournamentCrawlResult crawlResult = TournamentCrawlResult.builder()
                .crawledCount(safeCrawled.size())
                .createdCount(createdCount)
                .updatedCount(updatedCount)
                .skippedCount(skippedCount)
                .build();

        log.info("Tournament crawl/save finished: deleted={}, crawled={}, created={}, updated={}, skipped={}",
                deletedCount,
                crawlResult.getCrawledCount(),
                crawlResult.getCreatedCount(),
                crawlResult.getUpdatedCount(),
                crawlResult.getSkippedCount());

        return crawlResult;
    }

    private List<TournamentModel> crawlSingle(TournamentCrawler crawler) {
        long startedAt = System.nanoTime();
        String source = sourceTag(crawler.getSource());
        try {
            List<TournamentModel> crawled = applySource(crawler.crawlAll(), crawler.getSource());
            recordCrawlDuration(source, startedAt);
            return crawled;
        } catch (Exception e) {
            recordCrawlDuration(source, startedAt);
            incrementTournamentItems(source, "failed", 1);
            log.error("Tournament crawler failed: {}", crawler.getClass().getSimpleName(), e);
            return List.of();
        }
    }

    private TournamentCrawler findCrawlerBySource(TournamentSource source) {
        return crawlers.stream()
                .filter(crawler -> crawler.getSource() == source)
                .findFirst()
                .orElseThrow(() -> BusinessException.badRequest("지원하지 않는 대회 크롤러입니다: " + source));
    }

    private List<TournamentModel> applySource(List<TournamentModel> crawled, TournamentSource source) {
        List<TournamentModel> safeCrawled = crawled == null ? List.of() : crawled;
        if (source == null) {
            return safeCrawled;
        }

        for (TournamentModel model : safeCrawled) {
            if (model != null) {
                model.setSource(source);
            }
        }
        return safeCrawled;
    }

    private int deleteExpiredTournamentsByRegistrationDeadline() {
        LocalDate today = LocalDate.now(SEOUL_ZONE);
        List<Long> deleteIds = tournamentRepository.findAll().stream()
                .filter(tournament -> {
                    LocalDate registrationDeadline = TournamentDateUtils.parse(tournament.getRegistrationDeadline());
                    return registrationDeadline != null && registrationDeadline.isBefore(today);
                })
                .map(Tournament::getId)
                .filter(id -> id != null)
                .toList();

        if (deleteIds.isEmpty()) {
            return 0;
        }

        tournamentRepository.deleteAllByIdInBatch(deleteIds);
        log.info("Deleted expired tournaments by registration deadline. today={}, deletedCount={}", today, deleteIds.size());
        return deleteIds.size();
    }

    private void enrichPosterUrl(TournamentModel crawledModel) {
        if (crawledModel == null || crawledModel.getPosterUrl() == null || crawledModel.getPosterUrl().isBlank()) {
            return;
        }

        crawledModel.setPosterUrl(s3Uploader.uploadImageFromUrl(crawledModel.getPosterUrl()));
    }

    private Optional<NormalizedTournament> normalizeAndValidate(TournamentModel crawledModel) {
        if (crawledModel == null) {
            log.warn("Skip tournament save: crawled model is null");
            return Optional.empty();
        }

        NormalizedTournament normalizedTournament = new NormalizedTournament(
                crawledModel.getSource(),
                normalize(crawledModel.getTitle()),
                normalize(crawledModel.getOrganizer()),
                normalize(crawledModel.getPosterUrl()),
                normalizeDate(crawledModel.getCompetitionDate()),
                normalizeDate(crawledModel.getRegistrationDeadline()),
                normalize(crawledModel.getLocation()),
                normalize(crawledModel.getApplyLink())
        );

        if (normalizedTournament.source() == null
                || normalizedTournament.applyLink() == null
                || normalizedTournament.title() == null
                || normalizedTournament.competitionDate() == null
                || normalizedTournament.location() == null) {
            log.warn("Skip tournament save due to missing required fields. source={}, title={}, competitionDate={}, location={}, applyLink={}",
                    normalizedTournament.source(),
                    normalizedTournament.title(),
                    normalizedTournament.competitionDate(),
                    normalizedTournament.location(),
                    normalizedTournament.applyLink());
            return Optional.empty();
        }

        if (isOverMaxLength(normalizedTournament.title(), 255)
                || isOverMaxLength(normalizedTournament.organizer(), 255)
                || isOverMaxLength(normalizedTournament.posterUrl(), 1000)
                || isOverMaxLength(normalizedTournament.competitionDate(), 255)
                || isOverMaxLength(normalizedTournament.registrationDeadline(), 255)
                || isOverMaxLength(normalizedTournament.location(), 255)
                || isOverMaxLength(normalizedTournament.applyLink(), 512)) {
            log.warn("Skip tournament save due to field length overflow. titleLength={}, organizerLength={}, posterUrlLength={}, competitionDateLength={}, registrationDeadlineLength={}, locationLength={}, applyLinkLength={}",
                    lengthOf(normalizedTournament.title()),
                    lengthOf(normalizedTournament.organizer()),
                    lengthOf(normalizedTournament.posterUrl()),
                    lengthOf(normalizedTournament.competitionDate()),
                    lengthOf(normalizedTournament.registrationDeadline()),
                    lengthOf(normalizedTournament.location()),
                    lengthOf(normalizedTournament.applyLink()));
            return Optional.empty();
        }

        LocalDate competitionDate = TournamentDateUtils.parse(normalizedTournament.competitionDate());
        LocalDate today = LocalDate.now(SEOUL_ZONE);
        if (competitionDate != null && competitionDate.isBefore(today)) {
            log.info("Skip tournament save due to past competition date. competitionDate={}, today={}, applyLink={}",
                    competitionDate,
                    today,
                    normalizedTournament.applyLink());
            return Optional.empty();
        }

        return Optional.of(normalizedTournament);
    }

    private UpsertResult upsert(NormalizedTournament normalizedTournament) {
        Optional<Tournament> existingByApplyLink = tournamentRepository.findByApplyLink(normalizedTournament.applyLink());

        Tournament tournament = existingByApplyLink
                .orElseGet(() -> tournamentRepository
                        .findByTitleAndCompetitionDate(normalizedTournament.title(), normalizedTournament.competitionDate())
                        .orElseGet(() -> Tournament.builder()
                                .hostUserId(null)
                                .source(normalizedTournament.source())
                                .title(normalizedTournament.title())
                                .organizer(normalizedTournament.organizer())
                                .posterUrl(normalizedTournament.posterUrl())
                                .competitionDate(normalizedTournament.competitionDate())
                                .registrationDeadline(normalizedTournament.registrationDeadline())
                                .location(normalizedTournament.location())
                                .applyLink(normalizedTournament.applyLink())
                                .build()));

        boolean isNew = tournament.getId() == null;
        tournament.assignSourceIfAbsent(normalizedTournament.source());
        tournament.updateFromCrawler(
                normalizedTournament.title(),
                normalizedTournament.organizer(),
                normalizedTournament.posterUrl(),
                normalizedTournament.competitionDate(),
                normalizedTournament.registrationDeadline(),
                normalizedTournament.location(),
                normalizedTournament.applyLink()
        );
        tournamentRepository.save(tournament);

        return isNew ? UpsertResult.CREATED : UpsertResult.UPDATED;
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalizeDate(String value) {
        String normalized = normalize(value);
        if (normalized == null) {
            return null;
        }
        return TournamentDateUtils.normalizeToIso(normalized);
    }

    private boolean isOverMaxLength(String value, int maxLength) {
        return value != null && value.length() > maxLength;
    }

    private int lengthOf(String value) {
        return value == null ? 0 : value.length();
    }

    private enum UpsertResult {
        CREATED,
        UPDATED
    }

    private record NormalizedTournament(
            TournamentSource source,
            String title,
            String organizer,
            String posterUrl,
            String competitionDate,
            String registrationDeadline,
            String location,
            String applyLink
    ) {
    }

    private void incrementTournamentItems(String source, String result, double amount) {
        if (meterRegistry == null || amount <= 0) {
            return;
        }
        meterRegistry.counter("rolling_tournament_crawl_items_total", "source", source, "result", result)
                .increment(amount);
    }

    private void incrementDeletedTournaments(int deletedCount) {
        if (meterRegistry == null || deletedCount <= 0) {
            return;
        }
        meterRegistry.counter("rolling_tournament_crawl_deleted_total")
                .increment(deletedCount);
    }

    private void recordCrawlDuration(String source, long startedAtNanos) {
        if (meterRegistry == null) {
            return;
        }
        meterRegistry.timer("rolling_tournament_crawl_duration_seconds", "source", source)
                .record(Duration.ofNanos(Math.max(0L, System.nanoTime() - startedAtNanos)));
    }

    private String sourceTag(TournamentSource source) {
        return source == null ? "unknown" : source.name().toLowerCase();
    }
}
