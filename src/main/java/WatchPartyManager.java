import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class WatchPartyManager {
    private List<WatchParty> watchParties;
    private List<WatchParty> watchPartiesPlanned;


    public WatchPartyManager() {
        this.watchParties = new ArrayList<>();
        this.watchPartiesPlanned = new ArrayList<>();

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
