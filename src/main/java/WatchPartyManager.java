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
}
