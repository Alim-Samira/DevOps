package backend.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import backend.models.Bet;
import backend.models.DiscreteChoiceBet;
import backend.models.NumericValueBet;
import backend.models.OrderedRankingBet;
import backend.models.TicketType;
import backend.models.User;
import backend.models.WatchParty;
import backend.services.BetService;
import backend.services.BetSettlementService;
import backend.services.RankingService;
import backend.services.UserService;
import backend.services.WatchPartyManager;

class BetControllerTicketTest {

    private WatchPartyManager manager;
    private UserService userService;
    private RankingService rankingService;
    private BetService betService;
    private BetController controller;
    private User admin;
    private User bob;

    @BeforeEach
    void setUp() {
        manager = new WatchPartyManager();
        userService = new UserService();
        rankingService = new RankingService(userService, manager);
        betService = new BetService(manager, userService, rankingService, new BetSettlementService());
        controller = new BetController(betService, userService, manager);
        admin = userService.getUser("admin");
        bob = userService.getUser("bob");
    }

    @Test
    void useTicketShouldHandleSuccessfulTicketFlows() {
        WatchParty inOrOutWp = createWatchParty("Ticket InOrOut");
        Bet inOrOutBet = createPendingDiscreteBet(inOrOutWp, "A");
        inOrOutWp.grantTicket(bob, TicketType.IN_OR_OUT);

        String inOrOutMessage = controller.useTicket(inOrOutWp.getName(), Map.of(
                "user", "bob",
                "ticketType", "IN_OR_OUT",
                "newPoints", 0));

        assertTrue(inOrOutMessage.contains("Retrait"));
        assertEquals(200, bob.getPointsForWatchParty(inOrOutWp.getName()));
        assertFalse(inOrOutWp.hasTicket(bob, TicketType.IN_OR_OUT));
        assertTrue(inOrOutBet.getUserBets().isEmpty());

        WatchParty discreteWp = createWatchParty("Ticket Discrete");
        DiscreteChoiceBet discreteBet = createPendingDiscreteBet(discreteWp, "A");
        discreteWp.grantTicket(bob, TicketType.DISCRETE_CHOICE);

        String discreteMessage = controller.useTicket(discreteWp.getName(), Map.of(
                "user", "bob",
                "ticketType", "DISCRETE_CHOICE",
                "newValue", "B"));

        assertTrue(discreteMessage.contains("modifi"));
        assertEquals("B", discreteBet.getUserChoices().get(bob));
        assertFalse(discreteWp.hasTicket(bob, TicketType.DISCRETE_CHOICE));

        WatchParty numericWp = createWatchParty("Ticket Numeric");
        NumericValueBet numericBet = createPendingNumericBet(numericWp, 15);
        numericWp.grantTicket(bob, TicketType.NUMERIC_VALUE);

        String numericMessage = controller.useTicket(numericWp.getName(), Map.of(
                "user", "bob",
                "ticketType", "NUMERIC_VALUE",
                "newValue", "20"));

        assertTrue(numericMessage.contains("modifi"));
        assertEquals(20.0, numericBet.getUserValues().get(bob));
        assertFalse(numericWp.hasTicket(bob, TicketType.NUMERIC_VALUE));

        WatchParty rankingWp = createWatchParty("Ticket Ranking");
        OrderedRankingBet rankingBet = createPendingRankingBet(rankingWp);
        rankingWp.grantTicket(bob, TicketType.ORDERED_RANKING);

        String rankingMessage = controller.useTicket(rankingWp.getName(), Map.of(
                "user", "bob",
                "ticketType", "ORDERED_RANKING",
                "newValue", List.of("B", "A", "C")));

        assertTrue(rankingMessage.contains("modifi"));
        assertEquals(List.of("B", "A", "C"), rankingBet.getUserRankings().get(bob));
        assertFalse(rankingWp.hasTicket(bob, TicketType.ORDERED_RANKING));
    }

    @Test
    void useTicketShouldRejectInvalidStatesAndPayloads() {
        WatchParty noBetWp = createWatchParty("No Bet");
        assertTrue(controller.useTicket(noBetWp.getName(), Map.of(
                "user", "bob",
                "ticketType", "IN_OR_OUT")).contains("Aucun pari actif"));

        WatchParty votingWp = createWatchParty("Voting Bet");
        createVotingDiscreteBet(votingWp);
        votingWp.grantTicket(bob, TicketType.IN_OR_OUT);
        assertTrue(controller.useTicket(votingWp.getName(), Map.of(
                "user", "bob",
                "ticketType", "IN_OR_OUT",
                "newPoints", 10)).contains("PENDING"));

        WatchParty ticketlessWp = createWatchParty("Ticketless Bet");
        createPendingDiscreteBet(ticketlessWp, "A");
        assertTrue(controller.useTicket(ticketlessWp.getName(), Map.of(
                "user", "bob",
                "ticketType", "IN_OR_OUT",
                "newPoints", 10)).contains("poss"));

        assertTrue(controller.useTicket(ticketlessWp.getName(), Map.of(
                "user", "bob",
                "ticketType", "UNKNOWN",
                "newPoints", 10)).contains("invalide"));

        WatchParty offerTicketWp = createWatchParty("Offer Ticket");
        Bet offerTicketBet = createPendingDiscreteBet(offerTicketWp, "A");
        offerTicketBet.setOffersTicket(true);
        offerTicketWp.grantTicket(bob, TicketType.IN_OR_OUT);
        assertTrue(controller.useTicket(offerTicketWp.getName(), Map.of(
                "user", "bob",
                "ticketType", "IN_OR_OUT",
                "newPoints", 10)).contains("Impossible"));

        WatchParty incompatibleWp = createWatchParty("Incompatible");
        createPendingDiscreteBet(incompatibleWp, "A");
        incompatibleWp.grantTicket(bob, TicketType.NUMERIC_VALUE);
        assertTrue(controller.useTicket(incompatibleWp.getName(), Map.of(
                "user", "bob",
                "ticketType", "NUMERIC_VALUE",
                "newValue", 25)).contains("incompatible"));

        WatchParty missingPointsWp = createWatchParty("Missing Points");
        createPendingDiscreteBet(missingPointsWp, "A");
        missingPointsWp.grantTicket(bob, TicketType.IN_OR_OUT);
        assertTrue(controller.useTicket(missingPointsWp.getName(), Map.of(
                "user", "bob",
                "ticketType", "IN_OR_OUT")).contains("newPoints"));

        WatchParty invalidNumericWp = createWatchParty("Invalid Numeric");
        createPendingNumericBet(invalidNumericWp, 12);
        invalidNumericWp.grantTicket(bob, TicketType.NUMERIC_VALUE);
        assertTrue(controller.useTicket(invalidNumericWp.getName(), Map.of(
                "user", "bob",
                "ticketType", "NUMERIC_VALUE",
                "newValue", "not-a-number")).contains("invalide"));

        WatchParty invalidRankingWp = createWatchParty("Invalid Ranking");
        createPendingRankingBet(invalidRankingWp);
        invalidRankingWp.grantTicket(bob, TicketType.ORDERED_RANKING);
        assertTrue(controller.useTicket(invalidRankingWp.getName(), Map.of(
                "user", "bob",
                "ticketType", "ORDERED_RANKING",
                "newValue", "A > B > C")).contains("List<String>"));
    }

    private WatchParty createWatchParty(String name) {
        WatchParty wp = new WatchParty(name, LocalDateTime.now().plusDays(1), "LoL");
        wp.setPublic(true);
        wp.setCreator(admin);
        wp.join(bob);
        manager.addWatchParty(wp);
        return wp;
    }

    private DiscreteChoiceBet createVotingDiscreteBet(WatchParty wp) {
        betService.createDiscreteChoiceBet(wp.getName(), admin.getName(), "Who wins?", List.of("A", "B"), 10);
        betService.vote(wp.getName(), bob.getName(), "A", 40);
        return (DiscreteChoiceBet) wp.getActiveBet();
    }

    private DiscreteChoiceBet createPendingDiscreteBet(WatchParty wp, String choice) {
        DiscreteChoiceBet bet = createVotingDiscreteBet(wp);
        betService.endVoting(wp.getName(), admin.getName());
        if (!"A".equals(choice)) {
            bet.modifyChoice(bob, choice);
        }
        return bet;
    }

    private NumericValueBet createPendingNumericBet(WatchParty wp, int value) {
        betService.createNumericValueBet(wp.getName(), admin.getName(), "How many kills?", true, 0.0, 50.0, 10);
        betService.vote(wp.getName(), bob.getName(), value, 40);
        betService.endVoting(wp.getName(), admin.getName());
        return (NumericValueBet) wp.getActiveBet();
    }

    private OrderedRankingBet createPendingRankingBet(WatchParty wp) {
        betService.createOrderedRankingBet(wp.getName(), admin.getName(), "Top 3?", List.of("A", "B", "C"), 10);
        betService.vote(wp.getName(), bob.getName(), List.of("A", "B", "C"), 40);
        betService.endVoting(wp.getName(), admin.getName());
        return (OrderedRankingBet) wp.getActiveBet();
    }
}
