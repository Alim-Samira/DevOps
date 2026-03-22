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

    private static final String ERROR_ICON = "\u274c";
    private static final String SUCCESS_ICON = "\u2705";

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

    @Transient
    private String currentRiotGameId;

    @Transient
    private LocalDateTime lastFrameProcessed;

    public WatchParty() {
        this.userTickets = new HashMap<>();
        this.currentRiotGameId = null;
        this.lastFrameProcessed = null;
    }

    public WatchParty(String name, LocalDateTime date, String game) {
        this();
        this.name = name;
        this.date = date;
        this.game = game;
        this.planned = false;
        this.autoConfig = null;
        this.status = WatchPartyStatus.OPEN;
        this.participants = new ArrayList<>();
        this.creator = null;
        this.matchState = MatchState.PRE_MATCH;
        this.chat = new Chat(name + " Chat", new User("system", true));
    }

    private WatchParty(String name, User creator, AutoConfig autoConfig) {
        this();
        this.name = name;
        this.date = null;
        this.game = "League of Legends";
        this.planned = true;
        this.autoConfig = autoConfig;
        this.status = WatchPartyStatus.WAITING;
        this.participants = new ArrayList<>();
        this.creator = creator;
        this.matchState = MatchState.PRE_MATCH;
        this.chat = new Chat(name + " Chat", creator != null ? creator : new User("system", true));
    }

    public static WatchParty createAutoWatchParty(User creator, String target, AutoType type) {
        String name = type == AutoType.TEAM
            ? "Auto WP: Team " + target
            : "Auto WP: " + target;
        return new WatchParty(name, creator, new AutoConfig(type, target));
    }

    public String name() {
        return name;
    }

    public String getName() {
        return name;
    }

    public Long getId() {
        return id;
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

    public boolean isPlanned() {
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

    public boolean isAutoWatchParty() {
        return autoConfig != null;
    }

    public WatchPartyStatus getStatus() {
        return status;
    }

    public void updateStatus(Match upcomingMatch) {
        if (upcomingMatch == null) {
            if (status == WatchPartyStatus.OPEN) {
                status = WatchPartyStatus.CLOSED;
                kickAllParticipants();
                clearAllTickets();
            }
            clearLiveData();
            return;
        }

        if (upcomingMatch.isPast()) {
            if (status == WatchPartyStatus.OPEN) {
                status = WatchPartyStatus.CLOSED;
                kickAllParticipants();
            }
            if (autoConfig != null) {
                autoConfig.setCurrentMatch(null);
            }
            clearLiveData();
            return;
        }

        if (autoConfig != null) {
            autoConfig.setCurrentMatch(upcomingMatch);
        }
        this.date = upcomingMatch.getScheduledTime();

        if (upcomingMatch.isStartingSoon(30) && status != WatchPartyStatus.OPEN) {
            status = WatchPartyStatus.OPEN;
        }

        if (upcomingMatch.isFinished() && status == WatchPartyStatus.OPEN) {
            status = WatchPartyStatus.CLOSED;
            kickAllParticipants();
            clearAllTickets();
            clearLiveData();
        }
    }

    private void kickAllParticipants() {
        participants.clear();
        if (creator != null) {
            participants.add(creator);
        }
    }

    public boolean join(User user) {
        if (isAutoWatchParty() && status != WatchPartyStatus.OPEN) {
            return false;
        }

        if (!participants.contains(user)) {
            participants.add(user);
            user.setPointsForWatchParty(name, 200);
            return true;
        }
        return false;
    }

    public boolean leave(User user) {
        return participants.remove(user);
    }

    public boolean isCreator(User user) {
        return creator != null && creator.equals(user);
    }

    public boolean isAdmin(User user) {
        if (user == null) {
            return false;
        }
        if (creator != null) {
            return creator.getName().equalsIgnoreCase(user.getName());
        }
        return user.isAdmin();
    }

    public AutoConfig getAutoConfig() {
        return autoConfig;
    }

    public List<User> getParticipants() {
        return new ArrayList<>(participants);
    }

    public User getCreator() {
        return creator;
    }

    public void setCreator(User creator) {
        this.creator = creator;
        if (creator != null && !participants.contains(creator)) {
            participants.add(creator);
            creator.setPointsForWatchParty(this.name, 200);
        }
        this.chat = new Chat(name + " Chat", creator != null ? creator : new User("system", true));
    }

    public Chat getChat() {
        return chat;
    }

    public boolean canLaunchMiniGame() {
        return matchState == MatchState.PAUSED;
    }

    public String createBet(Bet bet) {
        if (activeBet != null && activeBet.getState() != Bet.State.RESOLVED
                && activeBet.getState() != Bet.State.CANCELED) {
            return ERROR_ICON + " Un pari est d\u00c3\u00a9j\u00c3\u00a0 actif pour cette watch party";
        }

        if (!this.isAdmin(bet.getCreator())) {
            if (this.creator != null) {
                return ERROR_ICON + " Seul le cr\u00c3\u00a9ateur de la watchparty peut cr\u00c3\u00a9er des paris";
            }
            return ERROR_ICON + " Seuls les admins peuvent cr\u00c3\u00a9er des paris";
        }

        activeBet = bet;
        return SUCCESS_ICON + " Pari cr\u00c3\u00a9\u00c3\u00a9: " + bet.getQuestion();
    }

    public Bet getActiveBet() {
        return activeBet;
    }

    public boolean hasActiveBet() {
        return activeBet != null && (activeBet.getState() == Bet.State.VOTING
                || activeBet.getState() == Bet.State.PENDING);
    }

    public String closeActiveBet() {
        if (activeBet == null) {
            return ERROR_ICON + " Aucun pari actif";
        }
        if (activeBet.getState() != Bet.State.VOTING) {
            return ERROR_ICON + " Le pari n'est pas en phase de vote";
        }
        return activeBet.endVoting();
    }

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

    private void clearLiveData() {
        currentRiotGameId = null;
        lastFrameProcessed = null;
    }

    public String getCurrentRiotGameId() {
        return currentRiotGameId;
    }

    public void setCurrentRiotGameId(String gameId) {
        this.currentRiotGameId = gameId;
    }

    public LocalDateTime getLastFrameProcessed() {
        return lastFrameProcessed;
    }

    public void setLastFrameProcessed(LocalDateTime time) {
        this.lastFrameProcessed = time;
    }
}
