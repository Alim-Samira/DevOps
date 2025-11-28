import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class WatchPartyManager {
    private List<WatchParty> watchParties;
    private List<WatchParty> watchPartiesPlanned;
    private AutoWatchPartyScheduler scheduler;


    public WatchPartyManager() {
        this.watchParties = new ArrayList<>();
        this.watchPartiesPlanned = new ArrayList<>();
        this.scheduler = new AutoWatchPartyScheduler(this);
    }

    public void addWatchParty(WatchParty wp) {
        watchParties.add(wp);
        System.out.println(" WatchParty ajoutée : " + wp.name());
    }

    public void toPlanWatchParty(WatchParty wp) {
        if (wp.date().isAfter(LocalDateTime.now())) {
            watchPartiesPlanned.add(wp);
            wp.toPlan(); 
        } else {
            System.out.println(" Impossible de planifier une WatchParty passée : " + wp.name());
        }
    }

    public List<WatchParty> watchPartiesPlanifiees() {
        return watchPartiesPlanned;
    }

    public void displayAllWatchParties() {
        if (watchParties.isEmpty()) {
            System.out.println("Aucune WatchParty enregistrée pour le moment.");
        } else {
            System.out.println("\n Liste des WatchParties :");
            for (WatchParty wp : watchParties) {
                wp.displayInfos();
                System.out.println("-------------------------");
            }
        }
    }
    
    // === Auto Watch Party Methods ===
    
    /**
     * Get all auto watch parties
     */
    public List<WatchParty> getAllAutoWatchParties() {
        List<WatchParty> autoWPs = new ArrayList<>();
        for (WatchParty wp : watchParties) {
            if (wp.isAutoWatchParty()) {
                autoWPs.add(wp);
            }
        }
        return autoWPs;
    }
    
    /**
     * Add an auto watch party
     */
    public void addAutoWatchParty(WatchParty wp) {
        if (!wp.isAutoWatchParty()) {
            System.out.println("[!] This is not an auto watch party!");
            return;
        }
        watchParties.add(wp);
        System.out.println("✅ Auto watch party added: " + wp.name());
    }
    
    /**
     * Remove a watch party by name
     */
    public boolean removeWatchParty(String name) {
        for (int i = 0; i < watchParties.size(); i++) {
            if (watchParties.get(i).name().equals(name)) {
                watchParties.remove(i);
                System.out.println("[-] Removed watch party: " + name);
                return true;
            }
        }
        System.out.println("[X] Watch party not found: " + name);
        return false;
    }
    
    /**
     * Get a watch party by name
     */
    public WatchParty getWatchPartyByName(String name) {
        for (WatchParty wp : watchParties) {
            if (wp.name().equals(name)) {
                return wp;
            }
        }
        return null;
    }
    
    /**
     * Get all watch parties
     */
    public List<WatchParty> getAllWatchParties() {
        return new ArrayList<>(watchParties);
    }
    
    /**
     * Start the auto watch party scheduler
     */
    public void startScheduler() {
        scheduler.start();
    }
    
    /**
     * Stop the auto watch party scheduler
     */
    public void stopScheduler() {
        scheduler.stop();
    }
    
    /**
     * Force an immediate scheduler update (for testing)
     */
    public void forceSchedulerUpdate() {
        scheduler.forceUpdate();
    }
    
    /**
     * Check if scheduler is running
     */
    public boolean isSchedulerRunning() {
        return scheduler.isRunning();
    }
}

