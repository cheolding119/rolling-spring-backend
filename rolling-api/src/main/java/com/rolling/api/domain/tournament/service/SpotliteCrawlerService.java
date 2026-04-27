package com.rolling.api.domain.tournament.service;

import com.rolling.api.domain.tournament.model.TournamentModel;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Slf4j
@Service
public class SpotliteCrawlerService {

    private static final Set<String> ACTIVE_STATUSES = Set.of("registration_open");
    private static final Set<String> TARGET_HOSTS = Set.of("spotlite.co.kr", "www.spotlite.co.kr");

    public List<TournamentModel> parseListDocument(Document document, String listPageUrl) {
        if (document == null) {
            return List.of();
        }

        List<TournamentModel> tournaments = new ArrayList<>();
        for (Element card : document.select("a.competition-card[href]")) {
            TournamentModel tournament = parseCompetitionCard(card);
            if (tournament != null) {
                tournaments.add(tournament);
            }
        }

        if (tournaments.isEmpty()) {
            log.warn("Spotlite list crawl found no active tournaments. listPageUrl={}", listPageUrl);
        }
        return tournaments;
    }

    private TournamentModel parseCompetitionCard(Element card) {
        if (!isActiveRegistration(card)) {
            return null;
        }

        String applyLink = normalizeUrl(card.attr("abs:href"));
        if (!isTargetHost(applyLink)) {
            log.warn("Skip Spotlite tournament: invalid applyLink={}", applyLink);
            return null;
        }

        TournamentModel model = new TournamentModel();
        model.setTitle(text(card, ".card-title"));
        model.setOrganizer(stripPrefix(text(card, ".meta-organizer"), "주최:"));
        model.setPosterUrl(null);
        model.setCompetitionDate(normalize(card.attr("data-end")));
        model.setRegistrationDeadline(normalize(card.attr("data-reg-end")));
        model.setLocation(text(card, ".card-info-item span"));
        model.setApplyLink(applyLink);
        return model;
    }

    private boolean isActiveRegistration(Element card) {
        String status = normalize(card.attr("data-status"));
        return status != null && ACTIVE_STATUSES.contains(status);
    }

    private String text(Element root, String selector) {
        Element element = root.selectFirst(selector);
        return element == null ? null : normalize(element.text());
    }

    private String stripPrefix(String value, String prefix) {
        if (value == null) {
            return null;
        }

        String normalizedPrefix = normalize(prefix);
        if (normalizedPrefix != null && value.startsWith(normalizedPrefix)) {
            return normalize(value.substring(normalizedPrefix.length()));
        }
        return value;
    }

    private boolean isTargetHost(String url) {
        if (url == null || url.isBlank()) {
            return false;
        }

        try {
            String host = URI.create(url).getHost();
            return host != null && TARGET_HOSTS.contains(host.toLowerCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private String normalizeUrl(String url) {
        String normalized = normalize(url);
        if (normalized == null) {
            return null;
        }

        int hashIndex = normalized.indexOf('#');
        return hashIndex < 0 ? normalized : normalized.substring(0, hashIndex).trim();
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value
                .replace('\u00A0', ' ')
                .replaceAll("[\\u200B-\\u200D\\uFEFF]", "")
                .replaceAll("\\s+", " ")
                .trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
