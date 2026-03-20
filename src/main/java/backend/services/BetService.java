package backend.services;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import backend.integration.lolesports.dto.Frame;
import backend.models.Bet;
import backend.models.DiscreteChoiceBet;
import backend.models.NumericValueBet;
import backend.models.OrderedRankingBet;
import backend.models.TicketType;
import backend.models.User;
import backend.models.WatchParty;

@Service
public class BetService {

    private static final String ADMIN_REQUIRED_ERROR = "❌ Seuls les admins peuvent créer des paris";
    private static final String WATCH_PARTY_NOT_FOUND = "❌ Watch party introuvable: ";
    private static final String ACTIVE_BET_EXISTS = "❌ Un pari est déjà actif pour cette watch party";
    private static final String NO_ACTIVE_BET = "❌ Aucun pari actif pour cette watch party";

    private final WatchPartyManager watchPartyManager;
    private final UserService userService;
    private final RankingService rankingService;
    private final BetSettlementService settlementService;

    public BetService(WatchPartyManager watchPartyManager,
                      UserService userService,
                      RankingService rankingService,
                      BetSettlementService settlementService) {
        this.watchPartyManager = watchPartyManager;
        this.userService = userService;
        this.rankingService = rankingService;
        this.settlementService = settlementService;
    }

    private String validateBetCreation(User admin, WatchParty wp, String watchPartyName) {
        if (wp == null) {
            return WATCH_PARTY_NOT_FOUND + watchPartyName;
        }
        if (!wp.isAdmin(admin)) {
            return ADMIN_REQUIRED_ERROR;
        }
        if (wp.hasActiveBet()) {
            return ACTIVE_BET_EXISTS;
        }
        return null;
    }

    public String createDiscreteChoiceBet(String watchPartyName,
                                          String adminName,
                                          String question,
                                          List<String> choices,
                                          int votingMinutes) {
        User admin = userService.getUser(adminName);
        WatchParty wp = watchPartyManager.getWatchPartyByName(watchPartyName);

        String validationError = validateBetCreation(admin, wp, watchPartyName);
        if (validationError != null) {
            return validationError;
        }

        LocalDateTime votingEndTime = LocalDateTime.now().plusMinutes(votingMinutes);
        DiscreteChoiceBet bet = new DiscreteChoiceBet(question, admin, wp, votingEndTime, choices);
        return wp.createBet(bet);
    }

    public String createNumericValueBet(String watchPartyName,
                                        String adminName,
                                        String question,
                                        boolean isInteger,
                                        Double minValue,
                                        Double maxValue,
                                        int votingMinutes) {
        User admin = userService.getUser(adminName);
        WatchParty wp = watchPartyManager.getWatchPartyByName(watchPartyName);

        String validationError = validateBetCreation(admin, wp, watchPartyName);
        if (validationError != null) {
            return validationError;
        }

        LocalDateTime votingEndTime = LocalDateTime.now().plusMinutes(votingMinutes);
        NumericValueBet bet = new NumericValueBet(question, admin, wp, votingEndTime, isInteger, minValue, maxValue);
        return wp.createBet(bet);
    }

    public String createOrderedRankingBet(String watchPartyName,
                                          String adminName,
                                          String question,
                                          List<String> items,
                                          int votingMinutes) {
        User admin = userService.getUser(adminName);
        WatchParty wp = watchPartyManager.getWatchPartyByName(watchPartyName);

        String validationError = validateBetCreation(admin, wp, watchPartyName);
        if (validationError != null) {
            return validationError;
        }

        LocalDateTime votingEndTime = LocalDateTime.now().plusMinutes(votingMinutes);
        OrderedRankingBet bet = new OrderedRankingBet(question, admin, wp, votingEndTime, items);
        return wp.createBet(bet);
    }

    public String vote(String watchPartyName, String username, Object votedValue, int points) {
        WatchParty wp = watchPartyManager.getWatchPartyByName(watchPartyName);
        if (wp == null) {
            return WATCH_PARTY_NOT_FOUND + watchPartyName;
        }
        if (!wp.hasActiveBet()) {
            return NO_ACTIVE_BET;
        }

        User user = userService.getUser(username);
        Bet bet = wp.getActiveBet();
        return bet.vote(user, votedValue, points);
    }

    public String endVoting(String watchPartyName, String adminName) {
        WatchParty wp = watchPartyManager.getWatchPartyByName(watchPartyName);
        if (wp == null) {
            return WATCH_PARTY_NOT_FOUND + watchPartyName;
        }

        User admin = userService.getUser(adminName);
        if (!wp.isAdmin(admin)) {
            return "❌ Seuls le créateur de la watchparty ou les admins globaux peuvent fermer le vote";
        }

        return wp.closeActiveBet();
    }

    public String resolveBet(String watchPartyName, String adminName, Object correctValue) {
        WatchParty wp = watchPartyManager.getWatchPartyByName(watchPartyName);
        if (wp == null) {
            return WATCH_PARTY_NOT_FOUND + watchPartyName;
        }

        User admin = userService.getUser(adminName);
        if (!wp.isAdmin(admin)) {
            return "❌ Seuls le créateur de la watchparty ou les admins globaux peuvent résoudre un pari";
        }
        if (!wp.hasActiveBet()) {
            return NO_ACTIVE_BET;
        }

        return resolveActiveBet(wp, wp.getActiveBet(), correctValue);
    }

    public String cancelBet(String watchPartyName, String adminName) {
        WatchParty wp = watchPartyManager.getWatchPartyByName(watchPartyName);
        if (wp == null) {
            return WATCH_PARTY_NOT_FOUND + watchPartyName;
        }

        User admin = userService.getUser(adminName);
        if (!wp.isAdmin(admin)) {
            return "❌ Seuls le créateur de la watchparty ou les admins globaux peuvent annuler un pari";
        }
        if (!wp.hasActiveBet()) {
            return NO_ACTIVE_BET;
        }

        Bet bet = wp.getActiveBet();
        String result = bet.cancel();
        if (bet.getState() == Bet.State.CANCELED) {
            refreshRankingCache(wp);
        }
        return result;
    }

    public List<Bet> getAllActiveBets() {
        List<Bet> activeBets = new ArrayList<>();
        for (WatchParty wp : watchPartyManager.getAllWatchParties()) {
            if (wp.hasActiveBet()) {
                activeBets.add(wp.getActiveBet());
            }
        }
        return activeBets;
    }

    public Bet getActiveBet(String watchPartyName) {
        WatchParty wp = watchPartyManager.getWatchPartyByName(watchPartyName);
        if (wp == null) {
            return null;
        }
        return wp.getActiveBet();
    }

    public boolean tryAutoResolveLiveBet(WatchParty wp, Frame frame) {
        if (wp == null || frame == null) {
            return false;
        }

        Bet activeBet = wp.getActiveBet();
        if (activeBet == null) {
            return false;
        }

        if (activeBet.getState() == Bet.State.VOTING
                && LocalDateTime.now().isAfter(activeBet.getVotingEndTime())) {
            activeBet.endVoting();
        }

        if (activeBet.getState() != Bet.State.PENDING) {
            return false;
        }

        Optional<Object> correctValue = settlementService.findCorrectValue(activeBet, frame, wp);
        if (correctValue.isEmpty()) {
            return false;
        }

        resolveActiveBet(wp, activeBet, correctValue.get());
        return activeBet.getState() == Bet.State.RESOLVED;
    }

    private String resolveActiveBet(WatchParty wp, Bet bet, Object correctValue) {
        String result = bet.resolve(correctValue);
        if (bet.getState() == Bet.State.RESOLVED) {
        refreshRankingCache(wp);
            distributeTicketsIfNeeded(wp, bet);
        }
        return result;
    }

    private void refreshRankingCache(WatchParty wp) {
        if (wp.isPublic()) {
            rankingService.refreshAll();
        } else {
            rankingService.refreshWatchParty(wp.getName());
        }
    }

    private void distributeTicketsIfNeeded(WatchParty wp, Bet bet) {
        if (!bet.isOffersTicket()) {
            return;
        }

        TicketType ticketType = getTicketTypeForBet(bet);
        for (User winner : bet.getLastWinners()) {
            wp.grantTicket(winner, ticketType);
            if (Math.random() < 0.10) { // NOSONAR S2245: Random is acceptable for game mechanics.
                wp.grantTicket(winner, TicketType.IN_OR_OUT);
            }
        }
    }

    private TicketType getTicketTypeForBet(Bet bet) {
        if (bet instanceof DiscreteChoiceBet) {
            return TicketType.DISCRETE_CHOICE;
        }
        if (bet instanceof NumericValueBet) {
            return TicketType.NUMERIC_VALUE;
        }
        if (bet instanceof OrderedRankingBet) {
            return TicketType.ORDERED_RANKING;
        }
        return TicketType.DISCRETE_CHOICE;
    }
}
