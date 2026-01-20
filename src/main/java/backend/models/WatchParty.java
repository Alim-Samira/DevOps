package backend.models;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.EnumSet;
import java.util.HashMap;

public class WatchParty {

    private String name;
    private LocalDateTime date;
    private String game;
    private boolean planned;
    private boolean isPublic = true; // distinction public/private
    
    // Auto watch party fields
    private AutoConfig autoConfig; // null for manual watch parties
    private WatchPartyStatus status;
    private List<User> participants;
    private User creator;
    // Chat unique pour la watchparty
    private Chat chat;
    // Tickets détenus par les utilisateurs pour cette watchparty
    private Map<User, EnumSet<TicketType>> userTickets;
    
    // Match state management (admin features)
    private MatchState matchState;
    
    // Betting system - one active bet per watch party
    private Bet activeBet;

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
        this.chat = new Chat(name + " Chat", new User("system", true));
        this.userTickets = new HashMap<>();
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
        this.chat = new Chat(name + " Chat", creator != null ? creator : new User("system", true));
        this.userTickets = new HashMap<>();
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

    public boolean isPublic() {
        return isPublic;
    }

    public void setPublic(boolean isPublic) {
        this.isPublic = isPublic;
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
                clearAllTickets();
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
            clearAllTickets();
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
                // Bonus initial de 500 points dans le contexte de la watchparty
                if (isPublic) {
                    user.addPublicPoints(500);
                } else {
                    user.addPointsForWatchParty(name, 500);
                }
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
            // Bonus initial de 500 points
            if (isPublic) {
                user.addPublicPoints(500);
            } else {
                user.addPointsForWatchParty(name, 500);
            }
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
    public Chat getChat() { return chat; }

    // Admin feature: whether a mini-game can be launched
    public boolean canLaunchMiniGame() {
        // Only when the match is paused
        return matchState == MatchState.PAUSED;
    }
    
    // ==================== BETTING SYSTEM ====================
    
    /**
     * Crée et associe un pari à cette watch party
     * @return message de succès ou d'erreur
     */
    public String createBet(Bet bet) {
        if (activeBet != null && activeBet.getState() != Bet.State.RESOLVED 
            && activeBet.getState() != Bet.State.CANCELED) {
            return "❌ Un pari est déjà actif pour cette watch party";
        }
        
        if (!bet.getCreator().isAdmin()) {
            return "❌ Seuls les admins peuvent créer des paris";
        }
        
        activeBet = bet;
        return "✅ Pari créé: " + bet.getQuestion();
    }
    
    /**
     * Obtient le pari actif
     */
    public Bet getActiveBet() {
        return activeBet;
    }
    
    /**
     * Vérifie si un pari est actif
     */
    public boolean hasActiveBet() {
        return activeBet != null && (activeBet.getState() == Bet.State.VOTING 
                                   || activeBet.getState() == Bet.State.PENDING);
    }
    
    /**
     * Ferme le pari actif
     */
    public String closeActiveBet() {
        if (activeBet == null) {
            return "❌ Aucun pari actif";
        }
        if (activeBet.getState() != Bet.State.VOTING) {
            return "❌ Le pari n'est pas en phase de vote";
        }
        return activeBet.endVoting();
    }

    // ==================== TICKETS ====================
    public void grantTicket(User user, TicketType type) {
        EnumSet<TicketType> set = userTickets.computeIfAbsent(user, u -> EnumSet.noneOf(TicketType.class));
        set.add(type);
    }

    public boolean hasTicket(User user, TicketType type) {
        EnumSet<TicketType> set = userTickets.get(user);
        return set != null && set.contains(type);
    }

    public boolean consumeTicket(User user, TicketType type) {
        EnumSet<TicketType> set = userTickets.get(user);
        if (set != null && set.contains(type)) {
            set.remove(type);
            if (set.isEmpty()) {
                userTickets.remove(user);
            }
            return true;
        }
        return false;
    }

    private void clearAllTickets() {
        if (userTickets != null) {
            userTickets.clear();
        }
    }
}
