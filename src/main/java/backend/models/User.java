package backend.models;
import java.util.HashMap;
import java.util.Map;

public class User {
    private String name;
    private boolean isAdmin;
    private boolean isModerator; 

    // Nouveau système de points
    // Points publics globaux (utilisés/obtenus dans les watchparties publiques)
    private int publicPoints;
    // Points par watchparty (privées ou publiques spécifiques)
    // Clé: nom de la watchparty
    private Map<String, Integer> pointsByWatchParty;

    // Master Constructor (Sets everything)
    public User(String name, boolean isAdmin, boolean isModerator) {
        this.name = name;
        this.isAdmin = isAdmin;
        this.isModerator = isModerator;
        this.publicPoints = 200; // Default starting public points
        this.pointsByWatchParty = new HashMap<>();
        this.publicWins = 0;
        this.winsByWatchParty = new HashMap<>();
    }

    // Automatically sets isModerator to false.
    public User(String name, boolean isAdmin) {
        this(name, isAdmin, false); // Chains to the Master Constructor
    }

    // Creates an Anonymous user with no permissions. 
    public User() {
        this("Anonymous", false, false); 
    }

    public String getName() {
        return name;
    }

    public boolean isAdmin() {
        return isAdmin;
    }

    public boolean isModerator() {
        return isModerator;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setAdmin(boolean isAdmin) {
        this.isAdmin = isAdmin;
    }
    
    public void setModerator(boolean isModerator) {
        this.isModerator = isModerator;
    }

    // Public points system
    public int getPublicPoints() {
        return publicPoints;
    }

    public void addPublicPoints(int delta) {
        this.publicPoints = Math.max(0, this.publicPoints + delta);
    }

    public int getPointsForWatchParty(String watchPartyName) {
        return pointsByWatchParty.getOrDefault(watchPartyName, 0);
    }

    public void setPointsForWatchParty(String watchPartyName, int points) {
        pointsByWatchParty.put(watchPartyName, Math.max(0, points));
    }

    public void addPointsForWatchParty(String watchPartyName, int delta) {
        int current = pointsByWatchParty.getOrDefault(watchPartyName, 0);
        pointsByWatchParty.put(watchPartyName, Math.max(0, current + delta));
    }

    public Map<String, Integer> getPointsByWatchParty() {
        return new HashMap<>(pointsByWatchParty);
    }

    // Wins system
    private int publicWins;
    private Map<String, Integer> winsByWatchParty;

    public int getPublicWins() {
        return publicWins;
    }

    public void addPublicWin() {
        this.publicWins = Math.max(0, this.publicWins + 1);
    }

    public int getWinsForWatchParty(String watchPartyName) {
        return winsByWatchParty.getOrDefault(watchPartyName, 0);
    }

    public void addWinForWatchParty(String watchPartyName) {
        int current = winsByWatchParty.getOrDefault(watchPartyName, 0);
        winsByWatchParty.put(watchPartyName, Math.max(0, current + 1));
    }

    public Map<String, Integer> getWinsByWatchParty() {
        return new HashMap<>(winsByWatchParty);
    }
}
// 