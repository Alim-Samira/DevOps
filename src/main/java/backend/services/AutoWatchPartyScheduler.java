package backend.services;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import backend.models.AutoConfig;
import backend.models.Match;
import backend.models.WatchParty;

import java.util.List;

/**
 * Scheduler that automatically opens/closes watch parties based on match timing
 */
public class AutoWatchPartyScheduler {
    
    private WatchPartyManager manager;
    private LeaguepediaClient apiClient;
    private ScheduledExecutorService scheduler;
    private boolean running;
    
    public AutoWatchPartyScheduler(WatchPartyManager manager) {
        this.manager = manager;
        this.apiClient = new LeaguepediaClient();
        this.scheduler = Executors.newScheduledThreadPool(1);
        this.running = false;
    }
    
    /**
     * Start the scheduler - checks for updates every 5 minutes
     * (In production, you'd use 1 hour interval)
     */
    public void start() {
        if (running) {
            return;
        }
        
        running = true;
        
        // Run immediately, then every 5 minutes
        scheduler.scheduleAtFixedRate(
            this::updateAllAutoWatchParties,
            0,  // Initial delay
            5,  // Period (use 60 for production - 1 hour)
            TimeUnit.MINUTES
        );
    }
    
    /**
     * Stop the scheduler
     */
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
    
    /**
     * Check all auto watch parties and update their status
     */
    private void updateAllAutoWatchParties() {
        for (WatchParty wp : manager.getAllAutoWatchParties()) {
            try {
                updateWatchParty(wp);
            } catch (Exception e) {
                // Ignore errors for individual watch parties
            }
        }
    }
    
    /**
     * Update a single watch party based on its auto config
     */
    private void updateWatchParty(WatchParty wp) {
        AutoConfig config = wp.getAutoConfig();
        if (config == null) {
            return; // Not an auto watch party
        }
        
        config.updateLastChecked();
        
        // Find next match based on type
        Match nextMatch = null;
        if (config.isTeamBased()) {
            nextMatch = apiClient.getNextTeamMatch(config.getTarget());
        } else if (config.isTournamentBased()) {
            nextMatch = apiClient.getNextTournamentMatch(config.getTarget());
        }
        
        // Update match status if we have a current match
        if (config.getCurrentMatch() != null) {
            apiClient.updateMatchStatus(config.getCurrentMatch());
        }
        
        // Update watch party status
        wp.updateStatus(nextMatch);
    }
    
    /**
     * Force an immediate update (useful for testing)
     */
    public void forceUpdate() {
        updateAllAutoWatchParties();
    }

    /**
     * Force an immediate update and return a textual report of matches retrieved.
     */
    public String forceUpdateReport() {
        StringBuilder report = new StringBuilder();
        report.append("🔄 Forcing immediate update...\n");

        for (WatchParty wp : manager.getAllAutoWatchParties()) {
            processWatchPartyReport(wp, report);
        }

        report.append("✅ Auto watch party check complete\n");
        return report.toString();
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
        if (config.isTeamBased()) {
            return apiClient.fetchUpcomingMatchesForTeam(config.getTarget());
        }
        return apiClient.fetchUpcomingMatchesForTournament(config.getTarget());
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
    
    /**
     * Check if scheduler is running
     */
    public boolean isRunning() {
        return running;
    }
    
    /**
     * Get the API client (useful for debugging)
     */
    public LeaguepediaClient getApiClient() {
        return apiClient;
    }
}
