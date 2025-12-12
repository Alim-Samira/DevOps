import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class WatchParty {

    private String name;
    private LocalDateTime date;
    private String game;
    private boolean planned;
    
    // Auto watch party fields
    private AutoConfig autoConfig; // null for manual watch parties
    private WatchPartyStatus status;
    private List<User> participants;
    private User creator;
    
    // Match state management (admin features)
    private MatchState matchState;

    // Constructor for manual watch parties (existing)
    public WatchParty(String name, LocalDateTime date, String game) {
        this.name = name;
        this.date = date;
        this.game = game;
        this.planned = false;
        this.autoConfig = null;
        this.status = WatchPartyStatus.OPEN;
        this.participants = new ArrayList<>();
        this.creator = null;
        this.matchState = MatchState.PRE_MATCH; // initial state
    }
    
    // Constructor for auto watch parties
    private WatchParty(String name, User creator, AutoConfig autoConfig) {
        this.name = name;
        this.date = null; // Will be set by current match
        this.game = "League of Legends";
        this.planned = true;
        this.autoConfig = autoConfig;
        this.status = WatchPartyStatus.WAITING;
        this.participants = new ArrayList<>();
        this.creator = creator;
        this.matchState = MatchState.PRE_MATCH; // initial state
    }

     public String name() {
        return name;
    }

    public LocalDateTime date() {
        return date;
    }

    public String game() {
        return game;
    }

    public boolean isPlanned(){
        return planned;
    }

    public MatchState matchState() {
        return matchState;
    }

    public void setMatchState(MatchState matchState) {
        this.matchState = matchState;
    }

    public void planify() {
        this.planned= true;
        System.out.println(" WatchParty planifiée : " + name +
                           " | Jeu : " + game +
                           " | Date : " + date);    
    }

    
    public void displayInfos() {
        System.out.println("Nom : " + name);
        System.out.println("date : " + date);
        System.out.println("jeu : " + game);
        System.out.println("Planifiee : " + (planned ? "oui" : "non"));
        
        // Show match state
        System.out.println("État du match : " + matchState);

        if (isAutoWatchParty()) {
            System.out.println("Type : Auto Watch Party (" + autoConfig.getType() + ")");
            System.out.println("Cible : " + autoConfig.getTarget());
            System.out.println("Status : " + status);
            if (autoConfig.getCurrentMatch() != null) {
                System.out.println("Match actuel : " + autoConfig.getCurrentMatch());
            }
        }
        
        System.out.println("Participants : " + participants.size());
    }
    
    // Factory method to create auto watch party
    public static WatchParty createAutoWatchParty(User creator, String target, AutoType type) {
        String name = type == AutoType.TEAM 
            ? "Auto WP: Team " + target
            : "Auto WP: " + target;
        return new WatchParty(name, creator, new AutoConfig(type, target));
    }
    
    // Check if this is an auto watch party
    public boolean isAutoWatchParty() {
        return autoConfig != null;
    }
    
    // Get status
    public WatchPartyStatus getStatus() {
        return status;
    }
    
    // Update status based on match timing
    public void updateStatus(Match upcomingMatch) {
        if (upcomingMatch == null) {
            // No upcoming match
            if (status == WatchPartyStatus.OPEN) {
                status = WatchPartyStatus.CLOSED;
                kickAllParticipants();
                System.out.println("[X] Watch party '" + name + "' closed - no upcoming match");
            }
            return;
        }
        
        // Safety check: reject past matches
        if (upcomingMatch.isPast()) {
            System.out.println("[!] Warning: Attempted to set past match for watch party '" + name + "' - ignoring");
            if (status == WatchPartyStatus.OPEN) {
                status = WatchPartyStatus.CLOSED;
                kickAllParticipants();
            }
            autoConfig.setCurrentMatch(null);
            return;
        }
        
        autoConfig.setCurrentMatch(upcomingMatch);
        this.date = upcomingMatch.getScheduledTime();
        
        // Check if match is starting soon (30 minutes before)
        if (upcomingMatch.isStartingSoon(30) && status != WatchPartyStatus.OPEN) {
            status = WatchPartyStatus.OPEN;
            System.out.println("[+] Watch party '" + name + "' is now OPEN!");
            System.out.println("   Match: " + upcomingMatch);
            System.out.println("   Stream: " + upcomingMatch.getStreamUrl());
        }
        
        // Check if match is finished
        if (upcomingMatch.isFinished() && status == WatchPartyStatus.OPEN) {
            status = WatchPartyStatus.CLOSED;
            kickAllParticipants();
            System.out.println("[X] Watch party '" + name + "' closed - match finished");
        }
    }
    
    // Kick all participants except creator
    private void kickAllParticipants() {
        participants.clear();
        if (creator != null) {
            participants.add(creator);
        }
    }
    
    // Join watch party
    public boolean join(User user) {
        if (!isAutoWatchParty()) {
            // Manual watch parties are always joinable
            if (!participants.contains(user)) {
                participants.add(user);
                System.out.println(user.getName() + " joined watch party: " + name);
                return true;
            }
            return false;
        }
        
        // Auto watch parties: only joinable when OPEN
        if (status != WatchPartyStatus.OPEN) {
            System.out.println("[X] Cannot join - watch party is " + status);
            return false;
        }
        
        if (!participants.contains(user)) {
            participants.add(user);
            System.out.println("✅ " + user.getName() + " joined watch party: " + name);
            return true;
        }
        return false;
    }
    
    // Leave watch party
    public boolean leave(User user) {
        if (participants.remove(user)) {
            System.out.println(user.getName() + " left watch party: " + name);
            return true;
        }
        return false;
    }
    
    // Check if user is creator/admin
    public boolean isCreator(User user) {
        return creator != null && creator.equals(user);
    }
    
    // Update auto config target (admin only)
    public void updateAutoTarget(String newTarget) {
        if (autoConfig != null) {
            autoConfig = new AutoConfig(autoConfig.getType(), newTarget);
            System.out.println("Updated auto watch party target to: " + newTarget);
        }
    }
    
    // Getters
    public AutoConfig getAutoConfig() { return autoConfig; }
    public List<User> getParticipants() { return new ArrayList<>(participants); }
    public User getCreator() { return creator; }

    // Admin feature: whether a mini-game can be launched
    public boolean canLaunchMiniGame() {
        // Only when the match is paused
        return matchState == MatchState.PAUSED;
    }
}
