package backend.controllers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import backend.models.Choice;
import backend.models.PublicBet;
import backend.models.User;
import backend.services.BetService;
import backend.services.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/bets")
@Tag(name = "Betting System", description = "Create bets and vote")
public class BetController {

    private final BetService betService;
    private final UserService userService;

    public BetController(BetService betService, UserService userService) {
        this.betService = betService;
        this.userService = userService;
    }

    @GetMapping
    public List<PublicBet> getBets() {
        return betService.getActiveBets();
    }

    @GetMapping("/users")
    public List<User> getUsersPoints() {
        return userService.getAllUsers();
    }

    @Operation(summary = "Create a Bet", description = "Payload: { \"question\": \"Who wins?\", \"options\": [\"T1\", \"GenG\"] }")
    @PostMapping
    public String createBet(@RequestBody Map<String, Object> payload) {
        String question = (String) payload.get("question");
        List<String> options = (List<String>) payload.get("options");

        if (question == null || options == null || options.isEmpty()) return "Invalid data";

        betService.createBet(question, options);
        return "Bet created!";
    }

    @Operation(summary = "Vote on a Bet", description = "Payload: { \"user\": \"alice\", \"choiceIndex\": 0, \"points\": 50 }")
    @PostMapping("/{betIndex}/vote")
    public String vote(@PathVariable int betIndex, @RequestBody Map<String, Object> payload) {
        String username = (String) payload.get("user");
        int choiceIndex = (int) payload.get("choiceIndex");
        int points = (int) payload.get("points");

        List<PublicBet> bets = betService.getActiveBets();
        if (betIndex >= bets.size()) return "Bet not found";

        PublicBet bet = bets.get(betIndex);
        User user = userService.getUser(username);

        // Get options to find the choice object
        List<Choice> choiceList = new ArrayList<>(bet.getOptions());
        if (choiceIndex >= choiceList.size()) return "Invalid option index";

        bet.vote(user, choiceList.get(choiceIndex), points);
        
        return "Voted " + points + " points on " + choiceList.get(choiceIndex).toString();
    }
}