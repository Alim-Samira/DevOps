import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

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
            System.out.println("[!] Scheduler already running");
            return;
        }
        
        running = true;
        System.out.println("🚀 Auto Watch Party Scheduler started");
        System.out.println("   Checking for updates every 5 minutes...");
        
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
            System.out.println("🛑 Auto Watch Party Scheduler stopped");
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * Check all auto watch parties and update their status
     */
    private void updateAllAutoWatchParties() {
        System.out.println("\n🔄 Checking auto watch parties...");
        
        for (WatchParty wp : manager.getAllAutoWatchParties()) {
            try {
                updateWatchParty(wp);
            } catch (Exception e) {
                System.err.println("[X] Error updating watch party '" + wp.name() + "': " + e.getMessage());
            }
        }
        
        System.out.println("✅ Auto watch party check complete\n");
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
        System.out.println("🔄 Forcing immediate update...");
        updateAllAutoWatchParties();
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
