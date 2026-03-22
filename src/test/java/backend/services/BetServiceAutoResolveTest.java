package backend.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import backend.integration.lolesports.dto.Frame;
import backend.integration.lolesports.dto.GameEvent;
import backend.integration.lolesports.dto.ParticipantFrame;
import backend.integration.lolesports.dto.TeamFrame;
import backend.models.Bet;
import backend.models.TicketType;
import backend.models.User;
import backend.models.WatchParty;

class BetServiceAutoResolveTest {

    private WatchPartyManager manager;
    private UserService userService;
    private RankingService rankingService;
    private BetService betService;
    private WatchParty watchParty;
    private User admin;
    private User alice;
    private User bob;

    @BeforeEach
    void setUp() {
        manager = new WatchPartyManager();
        userService = new UserService();
        rankingService = new RankingService(userService, manager);
        betService = new BetService(manager, userService, rankingService, new BetSettlementService());

        admin = userService.getUser("admin");
        alice = userService.getUser("alice");
        bob = userService.getUser("bob");

        watchParty = new WatchParty("Live Resolve", LocalDateTime.now().plusDays(1), "League of Legends");
        watchParty.setPublic(true);
        watchParty.setCreator(admin);
        watchParty.join(alice);
        watchParty.join(bob);
        manager.addWatchParty(watchParty);
    }

    @Test
    void tryAutoResolveLiveBetShouldResolveAndGrantTickets() {
        betService.createDiscreteChoiceBet(
                watchParty.getName(),
                admin.getName(),
                "First Blood ?",
                List.of("Équipe bleue", "Équipe rouge"),
                10);

        Bet activeBet = watchParty.getActiveBet();
        activeBet.setOffersTicket(true);

        betService.vote(watchParty.getName(), alice.getName(), "Équipe rouge", 50);
        betService.vote(watchParty.getName(), bob.getName(), "Équipe bleue", 50);
        betService.endVoting(watchParty.getName(), admin.getName());

        Frame frame = new Frame(
                120_000L,
                new TeamFrame(13000, 2, List.of()),
                new TeamFrame(12200, 1, List.of()),
                List.of(new GameEvent("KILL", "Ahri", "Leona", null, "blue", 95_000L)));

        boolean resolved = betService.tryAutoResolveLiveBet(watchParty, frame);

        assertTrue(resolved);
        assertEquals(Bet.State.RESOLVED, activeBet.getState());
        assertEquals(150, alice.getPointsForWatchParty(watchParty.getName()));
        assertEquals(250, bob.getPointsForWatchParty(watchParty.getName()));
        assertTrue(watchParty.hasTicket(bob, TicketType.DISCRETE_CHOICE));
        assertEquals(1, rankingService.getGlobalPublicWins(true).get("bob"));
    }

    @Test
    void tryAutoResolveLiveBetShouldReturnFalseForUnsupportedBet() {
        betService.createDiscreteChoiceBet(
                watchParty.getName(),
                admin.getName(),
                "Question non supportée",
                List.of("Oui", "Non"),
                10);

        betService.endVoting(watchParty.getName(), admin.getName());
        Frame frame = new Frame(
                100_000L,
                new TeamFrame(10000, 0, List.of()),
                new TeamFrame(10000, 0, List.of()),
                List.of());

        assertFalse(betService.tryAutoResolveLiveBet(watchParty, frame));
        assertEquals(Bet.State.PENDING, watchParty.getActiveBet().getState());
    }

    @Test
    void resolveBetShouldHandleNumericBet() {
        betService.createNumericValueBet(
                watchParty.getName(),
                admin.getName(),
                "Combien de kills ?",
                true,
                0.0,
                100.0,
                10);

        betService.vote(watchParty.getName(), alice.getName(), 30, 50);
        betService.vote(watchParty.getName(), bob.getName(), 35, 50);
        betService.endVoting(watchParty.getName(), admin.getName());

        String result = betService.resolveBet(watchParty.getName(), admin.getName(), 35);

        assertTrue(result.contains("35"));
        assertEquals(Bet.State.RESOLVED, watchParty.getActiveBet().getState());
        assertEquals(150, alice.getPointsForWatchParty(watchParty.getName()));
        assertEquals(250, bob.getPointsForWatchParty(watchParty.getName()));
    }

    @Test
    void cancelBetShouldRefundRankingBet() {
        betService.createOrderedRankingBet(
                watchParty.getName(),
                admin.getName(),
                "Classement final",
                List.of("A", "B", "C"),
                10);

        betService.vote(watchParty.getName(), alice.getName(), List.of("A", "B", "C"), 40);
        String result = betService.cancelBet(watchParty.getName(), admin.getName());

        assertTrue(result.contains("rembours"));
        assertEquals(Bet.State.CANCELED, watchParty.getActiveBet().getState());
        assertEquals(200, alice.getPointsForWatchParty(watchParty.getName()));
    }

    @Test
    void tryAutoResolveLiveBetShouldRememberMilestoneReachedBeforeVotingClosed() {
        betService.createDiscreteChoiceBet(
                watchParty.getName(),
                admin.getName(),
                "Quel joueur atteindra le plus rapidement 10 kills ?",
                List.of("Faker", "Chovy"),
                10);

        Bet activeBet = watchParty.getActiveBet();
        betService.vote(watchParty.getName(), alice.getName(), "Chovy", 50);
        betService.vote(watchParty.getName(), bob.getName(), "Faker", 50);

        Frame preThreshold = new Frame(
                600_000L,
                new TeamFrame(32000, 7, List.of(
                        new ParticipantFrame("Ahri", "Faker", 9, 1, 4, 12000, 11))),
                new TeamFrame(30500, 6, List.of(
                        new ParticipantFrame("Orianna", "Chovy", 8, 1, 5, 11800, 11))),
                List.of());
        Frame thresholdReached = new Frame(
                660_000L,
                new TeamFrame(34000, 8, List.of(
                        new ParticipantFrame("Ahri", "Faker", 10, 1, 4, 12800, 12))),
                new TeamFrame(31800, 7, List.of(
                        new ParticipantFrame("Orianna", "Chovy", 8, 1, 6, 12200, 11))),
                List.of());

        assertFalse(betService.tryAutoResolveLiveBet(watchParty, null, preThreshold));
        assertFalse(betService.tryAutoResolveLiveBet(watchParty, preThreshold, thresholdReached));
        assertEquals(Bet.State.VOTING, activeBet.getState());

        betService.endVoting(watchParty.getName(), admin.getName());

        boolean resolved = betService.tryAutoResolveLiveBet(watchParty, thresholdReached, thresholdReached);

        assertTrue(resolved);
        assertEquals(Bet.State.RESOLVED, activeBet.getState());
        assertEquals(150, alice.getPointsForWatchParty(watchParty.getName()));
        assertEquals(250, bob.getPointsForWatchParty(watchParty.getName()));
    }
}
