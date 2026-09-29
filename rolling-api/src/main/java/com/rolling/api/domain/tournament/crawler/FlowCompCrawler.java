package com.rolling.api.domain.tournament.crawler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rolling.api.domain.tournament.entity.TournamentSource;
import com.rolling.api.domain.tournament.model.TournamentModel;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
public class FlowCompCrawler implements TournamentCrawler {

    private static final String API_URL = "https://api.qqo.co.kr/api/championship/list";
    private static final String DETAIL_URL = "https://www.flowcomp.co.kr/championship/";
    private static final String USER_AGENT = "Mozilla/5.0 (compatible; RollingCrawler/1.0)";
    private static final int REQUEST_TIMEOUT_MILLIS = 10_000;
    private static final int PAGE_SIZE = 100;
    private static final int MAX_PAGES = 20;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    public TournamentSource getSource() {
        return TournamentSource.FLOWCOMP;
    }

    @Override
    public List<TournamentModel> crawlAll() {
        List<TournamentModel> tournaments = new ArrayList<>();
        Set<String> seenLinks = new HashSet<>();

        for (int page = 1; page <= MAX_PAGES; page++) {
            PageResult result = parsePage(fetchPage(page));
            for (TournamentModel tournament : result.tournaments()) {
                if (seenLinks.add(tournament.getApplyLink())) {
                    tournaments.add(tournament);
                }
            }
            if (page >= result.totalPages()) {
                return tournaments;
            }
        }

        log.warn("FlowComp crawl reached page limit. maxPages={}", MAX_PAGES);
        return tournaments;
    }

    private String fetchPage(int page) {
        String url = API_URL + "?page=" + page + "&size=" + PAGE_SIZE + "&sport=JIUJITSU";
        try {
            return Jsoup.connect(url)
                    .userAgent(USER_AGENT)
                    .timeout(REQUEST_TIMEOUT_MILLIS)
                    .ignoreContentType(true)
                    .execute()
                    .body();
        } catch (IOException e) {
            throw new IllegalStateException("FlowComp list request failed. page=" + page, e);
        }
    }

    PageResult parsePage(String json) {
        try {
            JsonNode root = OBJECT_MAPPER.readTree(json);
            JsonNode response = root.path("response");
            if (!root.path("success").asBoolean(false) || !response.isArray()) {
                throw new IllegalStateException("FlowComp list response is invalid");
            }

            List<TournamentModel> tournaments = new ArrayList<>();
            for (JsonNode row : response) {
                TournamentModel tournament = parseTournament(row);
                if (tournament != null) {
                    tournaments.add(tournament);
                }
            }

            int totalPages = root.path("pagination").path("totalPage").asInt(1);
            return new PageResult(tournaments, Math.max(1, totalPages));
        } catch (IOException e) {
            throw new IllegalStateException("FlowComp list JSON parsing failed", e);
        }
    }

    private TournamentModel parseTournament(JsonNode row) {
        int id = row.path("seq").asInt(-1);
        if (id <= 0) {
            return null;
        }

        String title = text(row, "csName");
        String competitionDate = date(row, "csDate");
        String address = text(row, "csAddress");
        String location = address == null ? null : join(address, text(row, "csAddressDetail"));
        if (title == null || competitionDate == null || location == null) {
            log.warn("Skip FlowComp tournament with missing fields. id={}", id);
            return null;
        }

        TournamentModel tournament = new TournamentModel();
        tournament.setSource(TournamentSource.FLOWCOMP);
        tournament.setTitle(title);
        tournament.setCompetitionDate(competitionDate);
        tournament.setRegistrationDeadline(date(row, "csRegisterEndDate"));
        tournament.setLocation(location);
        tournament.setApplyLink(DETAIL_URL + id);
        return tournament;
    }

    private String text(JsonNode row, String field) {
        JsonNode value = row.path(field);
        if (value.isMissingNode() || value.isNull()) {
            return null;
        }
        String normalized = value.asText().trim().replaceAll("\\s+", " ");
        return normalized.isEmpty() ? null : normalized;
    }

    private String date(JsonNode row, String field) {
        String value = text(row, field);
        if (value == null || value.length() < 10) {
            return null;
        }
        try {
            return LocalDate.parse(value.substring(0, 10)).toString();
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private String join(String first, String second) {
        if (first == null) {
            return second;
        }
        return second == null ? first : first + " " + second;
    }

    record PageResult(List<TournamentModel> tournaments, int totalPages) {
    }
}
