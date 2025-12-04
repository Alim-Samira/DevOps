import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
// import java.util.stream.Collectors;

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

    public void planifyWatchParty(WatchParty wp) {
        if (wp.date().isAfter(LocalDateTime.now())) {
            watchPartiesPlanned.add(wp);
            wp.planify(); 
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
     * Force an immediate scheduler update and return a textual report of found matches.
     * @param daysAhead number of days ahead to search for upcoming matches
     * @return textual report (per watch party) listing matches or a "no match" message
     */
    public String forceSchedulerUpdateReport(int daysAhead) {
        return scheduler.forceUpdateReport(daysAhead);
    }
    
    /**
     * Check if scheduler is running
     */
    public boolean isSchedulerRunning() {
        return scheduler.isRunning();
    }

    //   GESTION DE L'ÉTAT DU MATCH
    /**
     * @param wp       la WatchParty ciblée
     * @param newState le nouvel état souhaité
     * @param isAdmin  true si l'utilisateur est admin
     */
    public void changeMatchState(WatchParty wp, MatchState newState, boolean isAdmin) {
        if (!isAdmin) {
            System.out.println(" Action réservée aux administrateurs.");
            return;
        }

        if (wp == null) {
            System.out.println(" WatchParty invalide.");
            return;
        }

        MatchState current = wp.matchState();

        if (current == MatchState.FINISHED) {
            System.out.println(" Impossible de changer l'état : le match est déjà terminé.");
            return;
        }

        wp.setMatchState(newState);
        System.out.println(" État du match de la WatchParty '" + wp.name()
                           + "' changé à : " + newState);
    }

    /**
     * Demande de lancement d'un mini-jeu côté WatchParty.
     * Ici on vérifie juste l'état du match et le rôle de l'utilisateur.
     * @param wp      la WatchParty ciblée
     * @param isAdmin true si l'utilisateur est admin
     */
    public void requestMiniGameLaunch(WatchParty wp, boolean isAdmin) {
        if (!isAdmin) {
            System.out.println(" Seuls les administrateurs peuvent lancer un mini-jeu.");
            return;
        }

        if (wp == null) {
            System.out.println(" WatchParty invalide.");
            return;
        }

        if (!wp.canLaunchMiniGame()) {
            System.out.println(" Impossible de lancer un mini-jeu : état actuel du match = "
                               + wp.matchState());
            return;
        }

        System.out.println(" Demande de lancement de mini-jeu autorisée pour la WatchParty '"
                           + wp.name() + "'.");
    }
}

