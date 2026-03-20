package backend.services;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import jakarta.annotation.PreDestroy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import backend.integration.lolesports.LiveMatchMonitorService;
import backend.integration.lolesports.LolEsportsClient;
import backend.models.AutoConfig;
import backend.models.Bet;
import backend.models.Match;
import backend.models.WatchParty;

/**
 * Scheduler that automatically opens/closes watch parties based on match timing.
 */
@Service
public class AutoWatchPartyScheduler {

    private final WatchPartyManager manager;
    private final LeaguepediaClient apiClient;
    private final ScheduledExecutorService scheduler;
    private final LolEsportsClient lolClient;
    private final LiveMatchMonitorService liveMonitor;

    private boolean running;

    public AutoWatchPartyScheduler(WatchPartyManager manager) {
        this(manager, new LeaguepediaClient(), null, null);
    }

    @Autowired
    public AutoWatchPartyScheduler(WatchPartyManager manager,
                                   LolEsportsClient lolClient,
                                   LiveMatchMonitorService liveMonitor) {
        this(manager, new LeaguepediaClient(), lolClient, liveMonitor);
        manager.setScheduler(this);
    }

    AutoWatchPartyScheduler(WatchPartyManager manager,
                            LeaguepediaClient apiClient,
                            LolEsportsClient lolClient,
                            LiveMatchMonitorService liveMonitor) {
        this.manager = manager;
        this.apiClient = apiClient;
        this.lolClient = lolClient;
        this.liveMonitor = liveMonitor;
        this.scheduler = Executors.newScheduledThreadPool(1);
        this.running = false;
    }

    /**
     * Start the scheduler. Checks for updates every 5 minutes.
     */
    public void start() {
        if (running) {
            return;
        }

        running = true;
        scheduler.scheduleAtFixedRate(this::updateAllAutoWatchParties, 0, 5, TimeUnit.MINUTES);
    }

    public void stop() {
        if (!running) {
            return;
        }

        running = false;
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    @PreDestroy
    void shutdownOnContextClose() {
        stop();
    }

    private void updateAllAutoWatchParties() {
        for (WatchParty wp : manager.getAllAutoWatchParties()) {
            try {
                updateWatchParty(wp);
            } catch (Exception ignored) {
                // Best effort scheduler: a single failing watch party must not block the others.
            }
        }

        checkAndAutoCloseBets();
    }

    private void updateWatchParty(WatchParty wp) {
        AutoConfig config = wp.getAutoConfig();
        if (config == null) {
            return;
        }

        config.updateLastChecked();

        Match nextMatch = null;
        try {
            if (config.isTeamBased()) {
                nextMatch = apiClient.getNextTeamMatch(config.getTarget());
            } else if (config.isTournamentBased()) {
                nextMatch = apiClient.getNextTournamentMatch(config.getTarget());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }

        if (config.getCurrentMatch() != null) {
            apiClient.updateMatchStatus(config.getCurrentMatch());
        }

        wp.updateStatus(nextMatch);
        maybeStartLiveMonitoring(wp, nextMatch);
    }

    private void maybeStartLiveMonitoring(WatchParty wp, Match nextMatch) {
        if (nextMatch == null
                || !nextMatch.isInProgress()
                || wp.getCurrentRiotGameId() != null
                || lolClient == null
                || liveMonitor == null) {
            return;
        }

        if (nextMatch.getRiotEventId() == null || nextMatch.getRiotEventId().isBlank()) {
            lolClient.findLiveEventId(nextMatch).ifPresent(nextMatch::setRiotEventId);
        }
        if (nextMatch.getRiotEventId() == null || nextMatch.getRiotEventId().isBlank()) {
            return;
        }

        String gameId = lolClient.getFirstGameId(nextMatch.getRiotEventId()).orElse(null);
        if (gameId != null) {
            liveMonitor.startMonitoring(wp, gameId);
        }
    }

    public void forceUpdate() {
        updateAllAutoWatchParties();
    }

    public String forceUpdateReport() {
        StringBuilder report = new StringBuilder();
        report.append("🔄 Forcing immediate update...\n");

        for (WatchParty wp : manager.getAllAutoWatchParties()) {
            processWatchPartyReport(wp, report);
        }

        report.append("✅ Auto watch party check complete\n");
        return report.toString();
    }

    private void checkAndAutoCloseBets() {
        LocalDateTime now = LocalDateTime.now();
        for (WatchParty wp : manager.getAllWatchParties()) {
            Bet activeBet = wp.getActiveBet();
            if (activeBet != null
                    && activeBet.getState() == Bet.State.VOTING
                    && now.isAfter(activeBet.getVotingEndTime())) {
                activeBet.endVoting();
            }
        }
    }

    private void processWatchPartyReport(WatchParty wp, StringBuilder report) {
        try {
            AutoConfig config = wp.getAutoConfig();
            if (config == null) {
                report.append("[SKIP] ").append(wp.name()).append(" : no auto-config\n");
                return;
            }

            List<Match> matches = fetchMatches(config);
            appendMatchReport(report, wp, config, matches);
            updateWatchPartyStatus(wp, config, matches);
        } catch (Exception e) {
            report.append("[ERROR] ").append(wp.name())
                  .append(" : ").append(e.getMessage()).append("\n");
        }
    }

    private List<Match> fetchMatches(AutoConfig config) {
        try {
            if (config.isTeamBased()) {
                return apiClient.fetchUpcomingMatchesForTeam(config.getTarget());
            }
            return apiClient.fetchUpcomingMatchesForTournament(config.getTarget());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Collections.emptyList();
        }
    }

    private void appendMatchReport(StringBuilder report, WatchParty wp, AutoConfig config, List<Match> matches) {
        if (matches == null || matches.isEmpty()) {
            report.append("[NO MATCH] ").append(wp.name())
                  .append(" (target='").append(config.getTarget())
                  .append("') : Aucun match trouvé\n");
        } else {
            report.append("[MATCHES] ").append(wp.name())
                  .append(" (target='").append(config.getTarget())
                  .append("') :\n");
            appendMatchDetails(report, matches);
        }
    }

    private void appendMatchDetails(StringBuilder report, List<Match> matches) {
        for (Match m : matches) {
            report.append("  - ").append(m.getTeam1())
                  .append(" vs ").append(m.getTeam2())
                  .append(" @ ").append(m.getScheduledTime())
                  .append(" (").append(m.getTournament()).append(")\n");
        }
    }

    private void updateWatchPartyStatus(WatchParty wp, AutoConfig config, List<Match> matches) {
        Match nextMatch = (matches == null || matches.isEmpty()) ? null : matches.get(0);
        if (config.getCurrentMatch() != null) {
            apiClient.updateMatchStatus(config.getCurrentMatch());
        }
        wp.updateStatus(nextMatch);
    }

    public boolean isRunning() {
        return running;
    }

    public LeaguepediaClient getApiClient() {
        return apiClient;
    }
}
