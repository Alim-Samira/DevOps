package backend.services;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import backend.models.MatchState;
import backend.models.WatchParty;

@Service
public class WatchPartyManager {
    private static final Logger log = LoggerFactory.getLogger(WatchPartyManager.class);

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
        log.info("WatchParty ajoutée : {}", wp.name());
    }

    public void planifyWatchParty(WatchParty wp) {
        if (wp.date().isAfter(LocalDateTime.now())) {
            watchPartiesPlanned.add(wp);
            wp.planify(); 
        } else {
            log.warn("Impossible de planifier une WatchParty passée : {}", wp.name());
        }
    }

    public List<WatchParty> watchPartiesPlanifiees() {
        return watchPartiesPlanned;
    }

    public void displayAllWatchParties() {
        if (watchParties.isEmpty()) {
            log.info("Aucune WatchParty enregistrée pour le moment.");
        } else {
            log.info("Liste des WatchParties :");
            for (WatchParty wp : watchParties) {
                log.info("- {}", wp.name());
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
            log.warn("This is not an auto watch party: {}", wp.name());
            return;
        }
        watchParties.add(wp);
        log.info("Auto watch party added: {}", wp.name());
    }
    
    /**
     * Remove a watch party by name
     */
    public boolean removeWatchParty(String name) {
        for (int i = 0; i < watchParties.size(); i++) {
            if (watchParties.get(i).name().equals(name)) {
                watchParties.remove(i);
                log.info("Watch party removed");
                return true;
            }
        }
        log.warn("Watch party not found");
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
    public String forceSchedulerUpdateReport() {
        return scheduler.forceUpdateReport();
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
            log.warn("Action réservée aux administrateurs.");
            return;
        }

        if (wp == null) {
            log.warn("WatchParty invalide.");
            return;
        }

        MatchState current = wp.matchState();

        if (current == MatchState.FINISHED) {
            log.warn("Impossible de changer l'état : le match est déjà terminé.");
            return;
        }

        wp.setMatchState(newState);
        log.info("État du match de la WatchParty '{}' changé à : {}", wp.name(), newState);
    }

    /**
     * Demande de lancement d'un mini-jeu côté WatchParty.
     * Ici on vérifie juste l'état du match et le rôle de l'utilisateur.
     * @param wp      la WatchParty ciblée
     * @param isAdmin true si l'utilisateur est admin
     */
    public void requestMiniGameLaunch(WatchParty wp, boolean isAdmin) {
        if (!isAdmin) {
            log.warn("Seuls les administrateurs peuvent lancer un mini-jeu.");
            return;
        }

        if (wp == null) {
            log.warn("WatchParty invalide.");
            return;
        }

        if (!wp.canLaunchMiniGame()) {
            log.warn("Impossible de lancer un mini-jeu : état actuel du match = {}", wp.matchState());
            return;
        }

        log.info("Demande de lancement de mini-jeu autorisée pour la WatchParty '{}'.", wp.name());
    }
}

