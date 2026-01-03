package backend.models;
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
        this.planned = true;
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
            if (status == WatchPartyStatus.OPEN) {
                status = WatchPartyStatus.CLOSED;
                kickAllParticipants();
            }
            return;
        }
        
        // Safety check: reject past matches
        if (upcomingMatch.isPast()) {
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
        }
        
        // Check if match is finished
        if (upcomingMatch.isFinished() && status == WatchPartyStatus.OPEN) {
            status = WatchPartyStatus.CLOSED;
            kickAllParticipants();
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
            if (!participants.contains(user)) {
                participants.add(user);
                return true;
            }
            return false;
        }
        
        // Auto watch parties: only joinable when OPEN
        if (status != WatchPartyStatus.OPEN) {
            return false;
        }
        
        if (!participants.contains(user)) {
            participants.add(user);
            return true;
        }
        return false;
    }
    
    // Leave watch party
    public boolean leave(User user) {
        return participants.remove(user);
    }
    
    // Check if user is creator/admin
    public boolean isCreator(User user) {
        return creator != null && creator.equals(user);
    }
    
    // Update auto config target (admin only)
    public void updateAutoTarget(String newTarget) {
        if (autoConfig != null) {
            autoConfig = new AutoConfig(autoConfig.getType(), newTarget);
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
