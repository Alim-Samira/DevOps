package backend.models;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    // Champs d'integration data Riot Games
    private String currentRiotGameId;     // null = pas de données live
    private LocalDateTime lastFrameProcessed;

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
        this.currentRiotGameId = null;
        this.lastFrameProcessed = null;
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
        this.currentRiotGameId = null;
        this.lastFrameProcessed = null;
    }

     public String name() {
        return name;
    }

    public String getName() {
        return name;
    }

    public LocalDateTime date() {
        return date;
    }

    public LocalDateTime getDate() {
        return date;
    }

    public String game() {
        return game;
    }

    public String getGame() {
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
        // Joining logic applies the same initialisation for manual and auto WPs
        if (isAutoWatchParty() && status != WatchPartyStatus.OPEN) {
            return false; // cannot join closed auto WP
        }

        if (!participants.contains(user)) {
            participants.add(user);
            // Initialize WP-specific points to 200 regardless of WP type
            user.setPointsForWatchParty(name, 200);
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
    
    /**
     * Helper: determines whether the given user should be considered an admin for this WatchParty.
     * Policy: prefer the WatchParty creator when present; otherwise fallback to global admin flag.
     */
    public boolean isAdmin(User user) {
        if (user == null) return false;
        if (this.creator != null) {
            return this.creator.getName().equalsIgnoreCase(user.getName());
        }
        return user.isAdmin();
    }
    
    // Getters
    public AutoConfig getAutoConfig() { return autoConfig; }
    public List<User> getParticipants() { return new ArrayList<>(participants); }
    public User getCreator() { return creator; }

    /**
     * Définit le créateur de la watchparty (utilisé pour les watchparties manuelles).
     * Ajoute également le créateur à la liste des participants et met à jour le chat.
     */
    public void setCreator(User creator) {
        this.creator = creator;
        if (creator != null && !this.participants.contains(creator)) {
            this.participants.add(creator);
            // Initialize creator's WP points to 200 (same rule as join)
            creator.setPointsForWatchParty(this.name, 200);
        }
        // Mettre à jour l'admin du chat pour refléter le créateur
        this.chat = new Chat(name + " Chat", creator != null ? creator : new User("system", true));
    }

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
        
        // Use WatchParty.isAdmin to decide whether the bet creator is allowed here
        if (!this.isAdmin(bet.getCreator())) {
            if (this.creator != null) {
                return "❌ Seul le créateur de la watchparty peut créer des paris";
            }
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

    public String getCurrentRiotGameId() { return currentRiotGameId; }
    public void setCurrentRiotGameId(String gameId) { this.currentRiotGameId = gameId; }
    public LocalDateTime getLastFrameProcessed() { return lastFrameProcessed; }
    public void setLastFrameProcessed(LocalDateTime time) { this.lastFrameProcessed = time; }
}
