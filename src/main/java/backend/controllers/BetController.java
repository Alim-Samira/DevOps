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
        String wpName = (String) payload.get("watchParty");
        String admin = (String) payload.get("admin");
        String question = (String) payload.get("question");
        @SuppressWarnings("unchecked")
        List<String> choices = (List<String>) payload.get("choices");
        int votingMinutes = payload.get("votingMinutes") != null 
            ? (int) payload.get("votingMinutes") 
            : 10;

        if (wpName == null || admin == null || question == null || choices == null) {
            return "❌ Données manquantes";
        }

        return betService.createDiscreteChoiceBet(wpName, admin, question, choices, votingMinutes);
    }

    @PostMapping("/numeric")
    @Operation(summary = "Crée un pari sur une valeur numérique",
               description = "Payload: { \"watchParty\": \"WP1\", \"admin\": \"alice\", \"question\": \"Nombre de kills?\", \"isInteger\": true, \"minValue\": 0, \"maxValue\": 100, \"votingMinutes\": 10 }")
    public String createNumericValueBet(@RequestBody Map<String, Object> payload) {
        String wpName = (String) payload.get("watchParty");
        String admin = (String) payload.get("admin");
        String question = (String) payload.get("question");
        Boolean isInteger = (Boolean) payload.get("isInteger");
        Double minValue = payload.get("minValue") != null 
            ? ((Number) payload.get("minValue")).doubleValue() 
            : null;
        Double maxValue = payload.get("maxValue") != null 
            ? ((Number) payload.get("maxValue")).doubleValue() 
            : null;
        int votingMinutes = payload.get("votingMinutes") != null 
            ? (int) payload.get("votingMinutes") 
            : 10;

        if (wpName == null || admin == null || question == null || isInteger == null) {
            return "❌ Données manquantes";
        }

        return betService.createNumericValueBet(wpName, admin, question, isInteger, 
                                               minValue, maxValue, votingMinutes);
    }

    @PostMapping("/ranking")
    @Operation(summary = "Crée un pari de classement ordonné",
               description = "Payload: { \"watchParty\": \"WP1\", \"admin\": \"alice\", \"question\": \"Top 5 joueurs?\", \"items\": [\"Faker\", \"Chovy\", \"ShowMaker\", \"Doran\", \"Zeus\"], \"votingMinutes\": 10 }")
    public String createOrderedRankingBet(@RequestBody Map<String, Object> payload) {
        String wpName = (String) payload.get("watchParty");
        String admin = (String) payload.get("admin");
        String question = (String) payload.get("question");
        @SuppressWarnings("unchecked")
        List<String> items = (List<String>) payload.get("items");
        int votingMinutes = payload.get("votingMinutes") != null 
            ? (int) payload.get("votingMinutes") 
            : 10;

        if (wpName == null || admin == null || question == null || items == null) {
            return "❌ Données manquantes";
        }

        return betService.createOrderedRankingBet(wpName, admin, question, items, votingMinutes);
    }

    // ==================== VOTING ====================

    @PostMapping("/{wpName}/vote")
    @Operation(summary = "Vote sur le pari actif d'une watch party",
               description = "Payload varie selon le type:\n" +
                           "- Discret: { \"user\": \"bob\", \"value\": \"T1\", \"points\": 50 }\n" +
                           "- Numérique: { \"user\": \"bob\", \"value\": 35, \"points\": 50 }\n" +
                           "- Classement: { \"user\": \"bob\", \"value\": [\"Faker\", \"Chovy\", ...], \"points\": 50 }")
    public String vote(@PathVariable String wpName, @RequestBody Map<String, Object> payload) {
        String username = (String) payload.get("user");
        Object value = payload.get("value");
        int points = payload.get("points") != null 
            ? (int) payload.get("points") 
            : 10;

        if (username == null || value == null) {
            return "❌ Données manquantes";
        }

        return betService.vote(wpName, username, value, points);
    }

    // ==================== BET MANAGEMENT ====================

    @PostMapping("/{wpName}/end-voting")
    @Operation(summary = "Ferme la phase de vote (admin uniquement)")
    public String endVoting(@PathVariable String wpName, @RequestBody Map<String, String> payload) {
        String admin = payload.get("admin");
        if (admin == null) {
            return "❌ Admin requis";
        }
        return betService.endVoting(wpName, admin);
    }

    @PostMapping("/{wpName}/resolve")
    @Operation(summary = "Résout le pari avec la valeur correcte (admin uniquement)",
               description = "Payload varie selon le type:\n" +
                           "- Discret: { \"admin\": \"alice\", \"correctValue\": \"T1\" }\n" +
                           "- Numérique: { \"admin\": \"alice\", \"correctValue\": 35 }\n" +
                           "- Classement: { \"admin\": \"alice\", \"correctValue\": [\"Faker\", \"Chovy\", ...] }")
    public String resolveBet(@PathVariable String wpName, @RequestBody Map<String, Object> payload) {
        String admin = (String) payload.get("admin");
        Object correctValue = payload.get("correctValue");

        if (admin == null || correctValue == null) {
            return "❌ Données manquantes";
        }

        return betService.resolveBet(wpName, admin, correctValue);
    }

    @PostMapping("/{wpName}/cancel")
    @Operation(summary = "Annule le pari et rembourse les parieurs (admin uniquement)")
    public String cancelBet(@PathVariable String wpName, @RequestBody Map<String, String> payload) {
        String admin = payload.get("admin");
        if (admin == null) {
            return "❌ Admin requis";
        }
        return betService.cancelBet(wpName, admin);
    }
}