package backend.controllers;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import backend.models.Bet;
import backend.models.User;
import backend.services.BetService;
import backend.services.UserService;
import backend.services.WatchPartyManager;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api")
@Tag(name = "Betting System", description = "Système de paris pour watch parties")
public class BetController {

    private static final String ERROR_MISSING_DATA = "❌ Données manquantes";
    private static final String ERROR_ADMIN_REQUIRED = "❌ Admin requis";
    private static final String ERROR_NO_ACTIVE_BET = "❌ Aucun pari actif";
    private static final String ERROR_BET_NOT_PENDING = "❌ Le pari doit être en attente (PENDING)";
    private static final String ERROR_OFFERS_TICKET = "❌ Impossible d'utiliser un ticket sur un pari qui offre un ticket";
    private static final String ERROR_INVALID_TICKET_TYPE = "❌ Type de ticket invalide";
    private static final String ERROR_NO_TICKET = "❌ L'utilisateur ne possède pas ce ticket";
    private static final String ERROR_INCOMPATIBLE_TICKET = "❌ Ticket incompatible avec le type de pari";
    private static final String ERROR_MISSING_NEW_POINTS = "❌ Champ newPoints requis pour IN_OR_OUT";
    private static final String ERROR_MISSING_VALUE = "❌ newValue requis (String)";
    private static final String ERROR_INVALID_VALUE = "❌ newValue invalide";
    private static final String SUCCESS_PREFIX = "✅";
    private static final int DEFAULT_VOTING_MINUTES = 10;
    private static final int DEFAULT_POINTS = 10;
    
    private static final String KEY_ADMIN = "admin";
    private static final String KEY_QUESTION = "question";
    private static final String KEY_VOTING_MINUTES = "votingMinutes";
    private static final String KEY_OFFERS_TICKET = "offersTicket";
    private static final String KEY_TICKET_TYPE = "ticketType";
    private static final String KEY_NEW_VALUE = "newValue";
    private static final String KEY_NEW_POINTS = "newPoints";
    private static final String KEY_USER = "user";
    private static final String KEY_VALUE = "value";
    private static final String KEY_POINTS = "points";
    private static final String KEY_CORRECT_VALUE = "correctValue";

    private final BetService betService;
    private final UserService userService;
    private final WatchPartyManager watchPartyManager;

    public BetController(BetService betService, UserService userService, WatchPartyManager watchPartyManager) {
        this.betService = betService;
        this.userService = userService;
        this.watchPartyManager = watchPartyManager;
    }

    // ==================== GET OPERATIONS ====================

    @GetMapping("/bets/all")
    @Operation(summary = "Liste tous les paris actifs")
    public List<Bet> getAllActiveBets() {
        return betService.getAllActiveBets();
    }

    @GetMapping("/watchparties/{name}/bets")
    @Operation(summary = "Récupère le pari actif d'une watch party")
    public Bet getActiveBet(@PathVariable String name) {
        return betService.getActiveBet(name);
    }

    @GetMapping("/bets/users")
    @Operation(summary = "Liste tous les utilisateurs avec leurs points")
    public List<User> getUsersPoints() {
        return userService.getAllUsers();
    }

    // ==================== CREATE BETS ====================

    @PostMapping("/watchparties/{name}/bets/discrete")
    @Operation(summary = "Crée un pari à choix discrets",
               description = "Payload: { \"admin\": \"alice\", \"question\": \"Qui gagne?\", \"choices\": [\"T1\", \"GenG\"], \"votingMinutes\": 10 }")
    public String createDiscreteChoiceBet(@PathVariable String name, @RequestBody Map<String, Object> payload) {
        String admin = (String) payload.get(KEY_ADMIN);
        String question = (String) payload.get(KEY_QUESTION);
        @SuppressWarnings("unchecked")
        List<String> choices = (List<String>) payload.get("choices");
        int votingMinutes = payload.get(KEY_VOTING_MINUTES) != null 
            ? (int) payload.get(KEY_VOTING_MINUTES) 
            : DEFAULT_VOTING_MINUTES;

        if (admin == null || question == null || choices == null) {
            return ERROR_MISSING_DATA;
        }

        String result = betService.createDiscreteChoiceBet(name, admin, question, choices, votingMinutes);
        // offersTicket will be processed within BetService via active bet
        if (Boolean.TRUE.equals(payload.get(KEY_OFFERS_TICKET))) {
            // Set flag on the newly created bet if present
            backend.models.Bet active = betService.getActiveBet(name);
            if (active != null) active.setOffersTicket(true);
        }
        return result;
    }

    @PostMapping("/watchparties/{name}/bets/numeric")
    @Operation(summary = "Crée un pari sur une valeur numérique",
               description = "Payload: { \"admin\": \"alice\", \"question\": \"Nombre de kills?\", \"isInteger\": true, \"minValue\": 0, \"maxValue\": 100, \"votingMinutes\": 10 }")
    public String createNumericValueBet(@PathVariable String name, @RequestBody Map<String, Object> payload) {
        String admin = (String) payload.get(KEY_ADMIN);
        String question = (String) payload.get(KEY_QUESTION);
        Boolean isInteger = (Boolean) payload.get("isInteger");
        Double minValue = payload.get("minValue") != null 
            ? ((Number) payload.get("minValue")).doubleValue() 
            : null;
        Double maxValue = payload.get("maxValue") != null 
            ? ((Number) payload.get("maxValue")).doubleValue() 
            : null;
        int votingMinutes = payload.get(KEY_VOTING_MINUTES) != null 
            ? (int) payload.get(KEY_VOTING_MINUTES) 
            : DEFAULT_VOTING_MINUTES;

        if (admin == null || question == null || isInteger == null) {
            return ERROR_MISSING_DATA;
        }

        String result = betService.createNumericValueBet(name, admin, question, isInteger, 
                                               minValue, maxValue, votingMinutes);
        if (Boolean.TRUE.equals(payload.get(KEY_OFFERS_TICKET))) {
            backend.models.Bet active = betService.getActiveBet(name);
            if (active != null) active.setOffersTicket(true);
        }
        return result;
    }

    @PostMapping("/watchparties/{name}/bets/ranking")
    @Operation(summary = "Crée un pari de classement ordonné",
               description = "Payload: { \"admin\": \"alice\", \"question\": \"Top 5 joueurs?\", \"items\": [\"Faker\", \"Chovy\", \"ShowMaker\", \"Doran\", \"Zeus\"], \"votingMinutes\": 10 }")
    public String createOrderedRankingBet(@PathVariable String name, @RequestBody Map<String, Object> payload) {
        String admin = (String) payload.get(KEY_ADMIN);
        String question = (String) payload.get(KEY_QUESTION);
        @SuppressWarnings("unchecked")
        List<String> items = (List<String>) payload.get("items");
        int votingMinutes = payload.get(KEY_VOTING_MINUTES) != null 
            ? (int) payload.get(KEY_VOTING_MINUTES) 
            : DEFAULT_VOTING_MINUTES;

        if (admin == null || question == null || items == null) {
            return ERROR_MISSING_DATA;
        }

        String result = betService.createOrderedRankingBet(name, admin, question, items, votingMinutes);
        if (Boolean.TRUE.equals(payload.get(KEY_OFFERS_TICKET))) {
            backend.models.Bet active = betService.getActiveBet(name);
            if (active != null) active.setOffersTicket(true);
        }
        return result;
    }

    // ==================== VOTING ====================

    @PostMapping("/watchparties/{name}/bets/vote")
    @Operation(summary = "Vote sur le pari actif d'une watch party",
               description = """
                   Payload varie selon le type:
                   - Discret: { "user": "bob", "value": "T1", "points": 50 }
                   - Numérique: { "user": "bob", "value": 35, "points": 50 }
                   - Classement: { "user": "bob", "value": ["Faker", "Chovy", ...], "points": 50 }""")
    public String vote(@PathVariable String name, @RequestBody Map<String, Object> payload) {
        String username = (String) payload.get(KEY_USER);
        Object value = payload.get(KEY_VALUE);
        int points = payload.get(KEY_POINTS) != null 
            ? (int) payload.get(KEY_POINTS) 
            : DEFAULT_POINTS;

        if (username == null || value == null) {
            return ERROR_MISSING_DATA;
        }

        return betService.vote(name, username, value, points);
    }

    // ==================== BET MANAGEMENT ====================

    @PostMapping("/watchparties/{name}/bets/end-voting")
    @Operation(summary = "Ferme la phase de vote (admin uniquement)")
    public String endVoting(@PathVariable String name, @RequestBody Map<String, String> payload) {
        String admin = payload.get(KEY_ADMIN);
        if (admin == null) {
            return ERROR_ADMIN_REQUIRED;
        }
        return betService.endVoting(name, admin);
    }

    @PostMapping("/watchparties/{name}/bets/resolve")
    @Operation(summary = "Résout le pari avec la valeur correcte (admin uniquement)",
               description = """
                   Payload varie selon le type:
                   - Discret: { "admin": "alice", "correctValue": "T1" }
                   - Numérique: { "admin": "alice", "correctValue": 35 }
                   - Classement: { "admin": "alice", "correctValue": ["Faker", "Chovy", ...] }""")
    public String resolveBet(@PathVariable String name, @RequestBody Map<String, Object> payload) {
        String admin = (String) payload.get(KEY_ADMIN);
        Object correctValue = payload.get(KEY_CORRECT_VALUE);

        if (admin == null || correctValue == null) {
            return ERROR_MISSING_DATA;
        }

        return betService.resolveBet(name, admin, correctValue);
    }

    @PostMapping("/watchparties/{name}/bets/cancel")
    @Operation(summary = "Annule le pari et rembourse les parieurs (admin uniquement)")
    public String cancelBet(@PathVariable String name, @RequestBody Map<String, String> payload) {
        String admin = payload.get(KEY_ADMIN);
        if (admin == null) {
            return ERROR_ADMIN_REQUIRED;
        }
        return betService.cancelBet(name, admin);
    }

    // ==================== TICKET USAGE ====================

    @PostMapping("/watchparties/{name}/bets/use-ticket")
    @Operation(summary = "Utilise un ticket sur le pari actif (état PENDING)",
               description = "Payload: { \"user\": \"bob\", \"ticketType\": \"IN_OR_OUT\", \"newPoints\": 0 } ou selon le type de pari: newValue")
    public String useTicket(@PathVariable String name, @RequestBody Map<String, Object> payload) {
        String username = (String) payload.get(KEY_USER);
        String ticketTypeStr = (String) payload.get(KEY_TICKET_TYPE);
        Object newValue = payload.get(KEY_NEW_VALUE);
        Integer newPoints = parseNewPoints(payload);

        if (username == null || ticketTypeStr == null) {
            return ERROR_MISSING_DATA;
        }

        backend.models.Bet bet = betService.getActiveBet(name);
        String betValidationError = validateBet(bet);
        if (betValidationError != null) {
            return betValidationError;
        }

        backend.models.WatchParty wp = watchPartyManager.getWatchPartyByName(bet.getWatchPartyName());
        backend.models.User user = userService.getUser(username);

        backend.models.TicketType type;
        try {
            type = backend.models.TicketType.valueOf(ticketTypeStr);
        } catch (IllegalArgumentException e) {
            return ERROR_INVALID_TICKET_TYPE;
        }

        if (!wp.hasTicket(user, type)) {
            return ERROR_NO_TICKET;
        }

        return handleTicketUsage(bet, user, type, wp, newValue, newPoints);
    }

    private Integer parseNewPoints(Map<String, Object> payload) {
        Object obj = payload.get(KEY_NEW_POINTS);
        if (obj instanceof Number num) {
            return num.intValue();
        }
        return null;
    }

    private String validateBet(backend.models.Bet bet) {
        if (bet == null) {
            return ERROR_NO_ACTIVE_BET;
        }
        if (bet.getState() != backend.models.Bet.State.PENDING) {
            return ERROR_BET_NOT_PENDING;
        }
        if (bet.isOffersTicket()) {
            return ERROR_OFFERS_TICKET;
        }
        return null;
    }

    private String handleTicketUsage(backend.models.Bet bet, backend.models.User user, 
                                     backend.models.TicketType type, backend.models.WatchParty wp,
                                     Object newValue, Integer newPoints) {
        return switch (type) {
            case IN_OR_OUT -> handleInOrOutTicket(bet, user, wp, newPoints);
            case DISCRETE_CHOICE -> handleDiscreteChoiceTicket(bet, user, wp, newValue);
            case NUMERIC_VALUE -> handleNumericValueTicket(bet, user, wp, newValue);
            case ORDERED_RANKING -> handleOrderedRankingTicket(bet, user, wp, newValue);
        };
    }

    private String handleInOrOutTicket(backend.models.Bet bet, backend.models.User user, 
                                       backend.models.WatchParty wp, Integer newPoints) {
        if (newPoints == null) {
            return ERROR_MISSING_NEW_POINTS;
        }
        String res = bet.adjustBetPoints(user, newPoints);
        if (res.startsWith(SUCCESS_PREFIX)) {
            wp.consumeTicket(user, backend.models.TicketType.IN_OR_OUT);
        }
        return res;
    }

    private String handleDiscreteChoiceTicket(backend.models.Bet bet, backend.models.User user,
                                              backend.models.WatchParty wp, Object newValue) {
        if (!(bet instanceof backend.models.DiscreteChoiceBet dcb)) {
            return ERROR_INCOMPATIBLE_TICKET;
        }
        if (!(newValue instanceof String choice)) {
            return ERROR_MISSING_VALUE;
        }
        String msgChoice = dcb.modifyChoice(user, choice);
        if (msgChoice.startsWith(SUCCESS_PREFIX)) {
            wp.consumeTicket(user, backend.models.TicketType.DISCRETE_CHOICE);
        }
        return msgChoice;
    }

    private String handleNumericValueTicket(backend.models.Bet bet, backend.models.User user,
                                            backend.models.WatchParty wp, Object newValue) {
        if (!(bet instanceof backend.models.NumericValueBet nvb)) {
            return ERROR_INCOMPATIBLE_TICKET;
        }
        Double val = parseDoubleValue(newValue);
        if (val == null) {
            return ERROR_INVALID_VALUE;
        }
        String msgVal = nvb.modifyValue(user, val);
        if (msgVal.startsWith(SUCCESS_PREFIX)) {
            wp.consumeTicket(user, backend.models.TicketType.NUMERIC_VALUE);
        }
        return msgVal;
    }

    private String handleOrderedRankingTicket(backend.models.Bet bet, backend.models.User user,
                                              backend.models.WatchParty wp, Object newValue) {
        if (!(bet instanceof backend.models.OrderedRankingBet orb)) {
            return ERROR_INCOMPATIBLE_TICKET;
        }
        if (!(newValue instanceof java.util.List<?> ranking)) {
            return "❌ newValue requis (List<String>)";
        }
        @SuppressWarnings("unchecked")
        java.util.List<String> typedRanking = (java.util.List<String>) ranking;
        String msgRank = orb.modifyRanking(user, typedRanking);
        if (msgRank.startsWith(SUCCESS_PREFIX)) {
            wp.consumeTicket(user, backend.models.TicketType.ORDERED_RANKING);
        }
        return msgRank;
    }

    private Double parseDoubleValue(Object newValue) {
        if (newValue instanceof Number num) {
            return num.doubleValue();
        }
        if (newValue instanceof String str) {
            try {
                return Double.parseDouble(str);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
}