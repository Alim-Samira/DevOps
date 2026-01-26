package backend.models;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.EnumSet;
import java.util.HashMap;

@Entity
@Table(name = "watch_parties")
public class WatchParty {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private LocalDateTime date;
    private String game;
    private boolean planned;

    @Column(name = "is_public")
    private boolean isPublic = true;

    // Save state (OPEN/CLOSED) in base
    @Enumerated(EnumType.STRING)
    private WatchPartyStatus status;

    //Links chat to watchparty in DB 
    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "chat_id")
    private Chat chat;

    // "IN-MEMORY" (managed by the server & not Db) ---
    @Transient
    private AutoConfig autoConfig; 
    
    @Transient
    private List<User> participants; 
    
    @Transient
    private User creator;
    
    @Transient
    private MatchState matchState;
    
    @Transient
    private Bet activeBet;

    @Transient
    private Map<User, EnumSet<TicketType>> userTickets;

    // --- CONSTRUCTEURS ---

    public WatchParty() {} // needed for DB

    // Manual constructor
    public WatchParty(String name, LocalDateTime date, String game) {
        this.name = name;
        this.date = date;
        this.game = game;
        this.planned = false;
        this.status = WatchPartyStatus.OPEN;
        this.participants = new ArrayList<>();
        this.creator = null;
        this.matchState = MatchState.PRE_MATCH;
        // Création automatique du Chat lié
        this.chat = new Chat(name + " Chat", new User("System", true));
        this.userTickets = new HashMap<>();
    }

    // Auto constructor (Private)
    private WatchParty(String name, User creator, AutoConfig autoConfig) {
        this.name = name;
        this.date = LocalDateTime.now();
        this.game = "League of Legends";
        this.planned = true;
        this.autoConfig = autoConfig;
        this.status = WatchPartyStatus.WAITING;
        this.participants = new ArrayList<>();
        this.creator = creator;
        this.matchState = MatchState.PRE_MATCH;
        this.chat = new Chat(name + " Chat", creator);
        this.userTickets = new HashMap<>();
    }

    // Factory Method (used for controllers)
    public static WatchParty createAutoWatchParty(User creator, String target, AutoType type) {
        String name = type == AutoType.TEAM ? "Auto WP: Team " + target : "Auto WP: " + target;
        return new WatchParty(name, creator, new AutoConfig(type, target));
    }


    public Long getId() { return id; }
    public String getName() { return name; }
    public Chat getChat() { return chat; }
    
    public boolean isPublic() { return isPublic; }
    public void setPublic(boolean isPublic) { this.isPublic = isPublic; }

    public boolean join(User user) {
        if (participants == null) participants = new ArrayList<>();
        if (!participants.contains(user)) {
            participants.add(user);
            // Logique d'attribution des points (Public vs Privé)
            if (isPublic) {
                user.addPublicPoints(500);
            } else {
                user.addPointsForWatchParty(name, 500);
            }
            return true;
        }
        return false;
    }
    
    public boolean leave(User user) { 
        if(participants != null) return participants.remove(user);
        return false;
    }

    // simple Getters 
    public String name() { return name; }
    public LocalDateTime date() { return date; }
    public String game() { return game; }
    public boolean isPlanned(){ return planned; }
    public MatchState matchState() { return matchState; }
    public void setMatchState(MatchState matchState) { this.matchState = matchState; }
    public void planify() { this.planned = true; }
    public boolean isAutoWatchParty() { return autoConfig != null; }
    public WatchPartyStatus getStatus() { return status; }
    public boolean isCreator(User user) { return creator != null && creator.equals(user); }
    public AutoConfig getAutoConfig() { return autoConfig; }
    public List<User> getParticipants() { return participants != null ? new ArrayList<>(participants) : new ArrayList<>(); }
    public User getCreator() { return creator; }
    
    public boolean canLaunchMiniGame() { return matchState == MatchState.PAUSED; }

    // Tickets (In-Memory)
    public void grantTicket(User user, TicketType type) {
        if (userTickets == null) userTickets = new HashMap<>();
        userTickets.computeIfAbsent(user, u -> EnumSet.noneOf(TicketType.class)).add(type);
    }
    public boolean hasTicket(User user, TicketType type) {
        return userTickets != null && userTickets.containsKey(user) && userTickets.get(user).contains(type);
    }
    public boolean consumeTicket(User user, TicketType type) {
         if (hasTicket(user, type)) {
             userTickets.get(user).remove(type);
             return true;
         }
         return false;
    }

    // BEts (In-Memory)
    public String createBet(Bet bet) {
        this.activeBet = bet;
        return "✅ Pari créé";
    }
    public Bet getActiveBet() { return activeBet; }
    public boolean hasActiveBet() { return activeBet != null; }
    public String closeActiveBet() { return activeBet != null ? activeBet.endVoting() : "Aucun pari"; }
    
    public void updateStatus(Match upcomingMatch) {
        // Avoid errors if match is null
        if (upcomingMatch != null && upcomingMatch.isStartingSoon(30)) status = WatchPartyStatus.OPEN;
    }
}