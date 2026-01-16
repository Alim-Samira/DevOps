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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/bets")
@Tag(name = "Betting System", description = "Système de paris pour watch parties")
public class BetController {

    private static final String ERROR_MISSING_DATA = "❌ Données manquantes";
    private static final String ERROR_ADMIN_REQUIRED = "❌ Admin requis";
    private static final int DEFAULT_VOTING_MINUTES = 10;
    private static final int DEFAULT_POINTS = 10;
    
    private static final String KEY_WATCH_PARTY = "watchParty";
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

    public BetController(BetService betService, UserService userService) {
        this.betService = betService;
        this.userService = userService;
    }

    // ==================== GET OPERATIONS ====================

    @GetMapping
    @Operation(summary = "Liste tous les paris actifs")
    public List<Bet> getAllActiveBets() {
        return betService.getAllActiveBets();
    }

    @GetMapping("/watchparty/{wpName}")
    @Operation(summary = "Récupère le pari actif d'une watch party")
    public Bet getActiveBet(@PathVariable String wpName) {
        return betService.getActiveBet(wpName);
    }

    @GetMapping("/users")
    @Operation(summary = "Liste tous les utilisateurs avec leurs points")
    public List<User> getUsersPoints() {
        return userService.getAllUsers();
    }

    // ==================== CREATE BETS ====================

    @PostMapping("/discrete")
    @Operation(summary = "Crée un pari à choix discrets",
               description = "Payload: { \"watchParty\": \"WP1\", \"admin\": \"alice\", \"question\": \"Qui gagne?\", \"choices\": [\"T1\", \"GenG\"], \"votingMinutes\": 10 }")
    public String createDiscreteChoiceBet(@RequestBody Map<String, Object> payload) {
        String wpName = (String) payload.get(KEY_WATCH_PARTY);
        String admin = (String) payload.get(KEY_ADMIN);
        String question = (String) payload.get(KEY_QUESTION);
        @SuppressWarnings("unchecked")
        List<String> choices = (List<String>) payload.get("choices");
        int votingMinutes = payload.get(KEY_VOTING_MINUTES) != null 
            ? (int) payload.get(KEY_VOTING_MINUTES) 
            : DEFAULT_VOTING_MINUTES;

        if (wpName == null || admin == null || question == null || choices == null) {
            return ERROR_MISSING_DATA;
        }

        String result = betService.createDiscreteChoiceBet(wpName, admin, question, choices, votingMinutes);
        // offersTicket will be processed within BetService via active bet
        if (Boolean.TRUE.equals(payload.get(KEY_OFFERS_TICKET))) {
            // Set flag on the newly created bet if present
            backend.models.Bet active = betService.getActiveBet(wpName);
            if (active != null) active.setOffersTicket(true);
        }
        return result;
    }

    @PostMapping("/numeric")
    @Operation(summary = "Crée un pari sur une valeur numérique",
               description = "Payload: { \"watchParty\": \"WP1\", \"admin\": \"alice\", \"question\": \"Nombre de kills?\", \"isInteger\": true, \"minValue\": 0, \"maxValue\": 100, \"votingMinutes\": 10 }")
    public String createNumericValueBet(@RequestBody Map<String, Object> payload) {
        String wpName = (String) payload.get(KEY_WATCH_PARTY);
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

        if (wpName == null || admin == null || question == null || isInteger == null) {
            return ERROR_MISSING_DATA;
        }

        String result = betService.createNumericValueBet(wpName, admin, question, isInteger, 
                                               minValue, maxValue, votingMinutes);
        if (Boolean.TRUE.equals(payload.get(KEY_OFFERS_TICKET))) {
            backend.models.Bet active = betService.getActiveBet(wpName);
            if (active != null) active.setOffersTicket(true);
        }
        return result;
    }

    @PostMapping("/ranking")
    @Operation(summary = "Crée un pari de classement ordonné",
               description = "Payload: { \"watchParty\": \"WP1\", \"admin\": \"alice\", \"question\": \"Top 5 joueurs?\", \"items\": [\"Faker\", \"Chovy\", \"ShowMaker\", \"Doran\", \"Zeus\"], \"votingMinutes\": 10 }")
    public String createOrderedRankingBet(@RequestBody Map<String, Object> payload) {
        String wpName = (String) payload.get(KEY_WATCH_PARTY);
        String admin = (String) payload.get(KEY_ADMIN);
        String question = (String) payload.get(KEY_QUESTION);
        @SuppressWarnings("unchecked")
        List<String> items = (List<String>) payload.get("items");
        int votingMinutes = payload.get(KEY_VOTING_MINUTES) != null 
            ? (int) payload.get(KEY_VOTING_MINUTES) 
            : DEFAULT_VOTING_MINUTES;

        if (wpName == null || admin == null || question == null || items == null) {
            return ERROR_MISSING_DATA;
        }

        String result = betService.createOrderedRankingBet(wpName, admin, question, items, votingMinutes);
        if (Boolean.TRUE.equals(payload.get(KEY_OFFERS_TICKET))) {
            backend.models.Bet active = betService.getActiveBet(wpName);
            if (active != null) active.setOffersTicket(true);
        }
        return result;
    }

    // ==================== VOTING ====================

    @PostMapping("/{wpName}/vote")
    @Operation(summary = "Vote sur le pari actif d'une watch party",
               description = """
                   Payload varie selon le type:
                   - Discret: { "user": "bob", "value": "T1", "points": 50 }
                   - Numérique: { "user": "bob", "value": 35, "points": 50 }
                   - Classement: { "user": "bob", "value": ["Faker", "Chovy", ...], "points": 50 }""")
    public String vote(@PathVariable String wpName, @RequestBody Map<String, Object> payload) {
        String username = (String) payload.get(KEY_USER);
        Object value = payload.get(KEY_VALUE);
        int points = payload.get(KEY_POINTS) != null 
            ? (int) payload.get(KEY_POINTS) 
            : DEFAULT_POINTS;

        if (username == null || value == null) {
            return ERROR_MISSING_DATA;
        }

        return betService.vote(wpName, username, value, points);
    }

    // ==================== BET MANAGEMENT ====================

    @PostMapping("/{wpName}/end-voting")
    @Operation(summary = "Ferme la phase de vote (admin uniquement)")
    public String endVoting(@PathVariable String wpName, @RequestBody Map<String, String> payload) {
        String admin = payload.get(KEY_ADMIN);
        if (admin == null) {
            return ERROR_ADMIN_REQUIRED;
        }
        return betService.endVoting(wpName, admin);
    }

    @PostMapping("/{wpName}/resolve")
    @Operation(summary = "Résout le pari avec la valeur correcte (admin uniquement)",
               description = """
                   Payload varie selon le type:
                   - Discret: { "admin": "alice", "correctValue": "T1" }
                   - Numérique: { "admin": "alice", "correctValue": 35 }
                   - Classement: { "admin": "alice", "correctValue": ["Faker", "Chovy", ...] }""")
    public String resolveBet(@PathVariable String wpName, @RequestBody Map<String, Object> payload) {
        String admin = (String) payload.get(KEY_ADMIN);
        Object correctValue = payload.get(KEY_CORRECT_VALUE);

        if (admin == null || correctValue == null) {
            return ERROR_MISSING_DATA;
        }

        return betService.resolveBet(wpName, admin, correctValue);
    }

    @PostMapping("/{wpName}/cancel")
    @Operation(summary = "Annule le pari et rembourse les parieurs (admin uniquement)")
    public String cancelBet(@PathVariable String wpName, @RequestBody Map<String, String> payload) {
        String admin = payload.get(KEY_ADMIN);
        if (admin == null) {
            return ERROR_ADMIN_REQUIRED;
        }
        return betService.cancelBet(wpName, admin);
    }

    // ==================== TICKET USAGE ====================

    @PostMapping("/{wpName}/use-ticket")
    @Operation(summary = "Utilise un ticket sur le pari actif (état PENDING)",
               description = "Payload: { \"user\": \"bob\", \"ticketType\": \"IN_OR_OUT\", \"newPoints\": 0 } ou selon le type de pari: newValue")
    public String useTicket(@PathVariable String wpName, @RequestBody Map<String, Object> payload) {
        String username = (String) payload.get(KEY_USER);
        String ticketTypeStr = (String) payload.get(KEY_TICKET_TYPE);
        Object newValue = payload.get(KEY_NEW_VALUE);
        Integer newPoints = payload.get(KEY_NEW_POINTS) instanceof Number 
            ? ((Number) payload.get(KEY_NEW_POINTS)).intValue() : null;

        if (username == null || ticketTypeStr == null) {
            return ERROR_MISSING_DATA;
        }

        backend.models.Bet bet = betService.getActiveBet(wpName);
        if (bet == null) return "❌ Aucun pari actif";
        if (bet.getState() != backend.models.Bet.State.PENDING) return "❌ Le pari doit être en attente (PENDING)";
        if (bet.isOffersTicket()) return "❌ Impossible d'utiliser un ticket sur un pari qui offre un ticket";

        backend.models.WatchParty wp = bet.getWatchParty();
        backend.models.User user = userService.getUser(username);

        backend.models.TicketType type;
        try {
            type = backend.models.TicketType.valueOf(ticketTypeStr);
        } catch (IllegalArgumentException e) {
            return "❌ Type de ticket invalide";
        }

        if (!wp.hasTicket(user, type)) {
            return "❌ L'utilisateur ne possède pas ce ticket";
        }

        // Appliquer selon type
        switch (type) {
            case IN_OR_OUT:
                if (newPoints == null) return "❌ Champ newPoints requis pour IN_OR_OUT";
                String res = bet.adjustBetPoints(user, newPoints);
                if (res.startsWith("✅")) wp.consumeTicket(user, type);
                return res;
            case DISCRETE_CHOICE:
                if (!(bet instanceof backend.models.DiscreteChoiceBet)) return "❌ Ticket incompatible avec le type de pari";
                if (!(newValue instanceof String)) return "❌ newValue requis (String)";
                backend.models.DiscreteChoiceBet dcb = (backend.models.DiscreteChoiceBet) bet;
                String choice = (String) newValue;
                String msgChoice = dcb.modifyChoice(user, choice);
                if (msgChoice.startsWith("✅")) wp.consumeTicket(user, type);
                return msgChoice;
            case NUMERIC_VALUE:
                if (!(bet instanceof backend.models.NumericValueBet)) return "❌ Ticket incompatible avec le type de pari";
                backend.models.NumericValueBet nvb = (backend.models.NumericValueBet) bet;
                Double val = null;
                if (newValue instanceof Number) val = ((Number) newValue).doubleValue();
                else if (newValue instanceof String) try { val = Double.parseDouble((String) newValue); } catch (Exception e) { val = null; }
                if (val == null) return "❌ newValue invalide";
                String msgVal = nvb.modifyValue(user, val);
                if (msgVal.startsWith("✅")) wp.consumeTicket(user, type);
                return msgVal;
            case ORDERED_RANKING:
                if (!(bet instanceof backend.models.OrderedRankingBet)) return "❌ Ticket incompatible avec le type de pari";
                if (!(newValue instanceof java.util.List)) return "❌ newValue requis (List<String>)";
                @SuppressWarnings("unchecked")
                java.util.List<String> ranking = (java.util.List<String>) newValue;
                backend.models.OrderedRankingBet orb = (backend.models.OrderedRankingBet) bet;
                String msgRank = orb.modifyRanking(user, ranking);
                if (msgRank.startsWith("✅")) wp.consumeTicket(user, type);
                return msgRank;
            default:
                return "❌ Type de ticket non supporté";
        }
    }
}