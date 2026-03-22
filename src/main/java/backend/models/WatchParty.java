package backend.models;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

@Entity
@Table(name = "watch_parties")
public class WatchParty {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private LocalDateTime date;
    private String game;

    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean planned;

    @Column(name = "is_public", nullable = false, columnDefinition = "boolean default true")
    private boolean isPublic = true;

    @Enumerated(EnumType.STRING)
    private WatchPartyStatus status;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "chat_id")
    private Chat chat;

    @Embedded
    private AutoConfig autoConfig;

    @ManyToMany
    @JoinTable(
        name = "wp_participants",
        joinColumns = @JoinColumn(name = "wp_id"),
        inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private List<User> participants = new ArrayList<>();

    @ManyToOne
    @JoinColumn(name = "creator_id")
    private User creator;

    @Enumerated(EnumType.STRING)
    private MatchState matchState;

    @Transient
    private Bet activeBet;

    @Transient
    private Map<User, EnumSet<TicketType>> userTickets;

    public WatchParty() {}

    public WatchParty(String name, LocalDateTime date, String game) {
        this.name = name;
        this.date = date;
        this.game = game;
        this.planned = false;
        this.status = WatchPartyStatus.OPEN;
        this.participants = new ArrayList<>();
        this.creator = null;
        this.matchState = MatchState.PRE_MATCH;
        this.chat = new Chat(name + " Chat", new User("System", true));
        this.userTickets = new HashMap<>();
    }

    private WatchParty(String name, User creator, AutoConfig autoConfig) {
        this.name = name;
        this.date = LocalDateTime.now();
        this.game = "League of Legends";
        this.planned = true;
        this.autoConfig = autoConfig;
        this.status = WatchPartyStatus.WAITING;
        this.participants = new ArrayList<>();
        if (creator != null) {
            this.participants.add(creator);  // Ajouter le créateur automatiquement
        }
        this.creator = creator;
        this.matchState = MatchState.PRE_MATCH;
        this.chat = new Chat(name + " Chat", creator);
        this.userTickets = new HashMap<>();
    }

    public static WatchParty createAutoWatchParty(User creator, String target, AutoType type) {
        String name = type == AutoType.TEAM ? "Auto WP: Team " + target : "Auto WP: " + target;
        return new WatchParty(name, creator, new AutoConfig(type, target));
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public Chat getChat() { return chat; }
    public boolean isPublic() { return isPublic; }
    public void setPublic(boolean isPublic) { this.isPublic = isPublic; }
    public void setCreator(User creator) {
        this.creator = creator;
        // Automatiquement ajouter le créateur aux participants
        if (creator != null && (participants == null || !participants.contains(creator))) {
            if (participants == null) participants = new ArrayList<>();
            participants.add(creator);
        }
    }

    public boolean join(User user) {
        if (isAutoWatchParty() && this.status == WatchPartyStatus.WAITING) {
            return false;
        }
        if (participants == null) participants = new ArrayList<>();
        if (!participants.contains(user)) {
            participants.add(user);
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
        if (participants != null) return participants.remove(user);
        return false;
    }

    public String name() { return name; }
    public LocalDateTime date() { return date; }
    public String game() { return game; }
    public boolean isPlanned() { return planned; }
    public MatchState matchState() { return matchState; }
    public void setMatchState(MatchState matchState) { this.matchState = matchState; }
    public void planify() { this.planned = true; }
    public boolean isAutoWatchParty() { return autoConfig != null; }
    public WatchPartyStatus getStatus() { return status; }
    public boolean isCreator(User user) { return creator != null && creator.equals(user); }

public boolean isAdmin(User user) {
    if (user == null) return false;
    if (this.creator != null) {
        return this.creator.getName().equalsIgnoreCase(user.getName());
    }
    return user.isAdmin();
}

    public AutoConfig getAutoConfig() { return autoConfig; }
    public List<User> getParticipants() { return participants != null ? new ArrayList<>(participants) : new ArrayList<>(); }
    public User getCreator() { return creator; }
    public boolean canLaunchMiniGame() { return matchState == MatchState.PAUSED; }

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

    public String createBet(Bet bet) {
        if (hasActiveBet()) {
            return "Error: A bet is already active";
        }
        this.activeBet = bet;
        return "Pari cree";
    }

    public Bet getActiveBet() { return activeBet; }

    public boolean hasActiveBet() {
        if (activeBet == null) return false;
        return !activeBet.isResolved();
    }

    public String closeActiveBet() {
        if (activeBet == null) return "Aucun pari";
        String result = activeBet.endVoting();
        this.activeBet = null;
        return result;
    }

    public void updateStatus(Match upcomingMatch) {
        if (upcomingMatch != null && upcomingMatch.isStartingSoon(30)) {
            this.status = WatchPartyStatus.OPEN;
        } else if (upcomingMatch != null && upcomingMatch.isFinished()) {
            this.status = WatchPartyStatus.CLOSED;
        }
    }
}
