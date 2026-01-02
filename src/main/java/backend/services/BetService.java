package backend.services;

import java.sql.Time;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import backend.models.Choice;
import backend.models.PublicBet;
import backend.models.User;

@Service
public class BetService {
    private List<PublicBet> activeBets = new ArrayList<>();
    private final UserService userService;

    public BetService(UserService userService) {
        this.userService = userService;
    }

    public PublicBet createBet(String question, List<String> optionTexts) {
        List<Choice> choices = new ArrayList<>();
        for (String text : optionTexts) {
            choices.add(new Choice(text));
        }

        // Set voting time to 10 minutes from now
        Time votingTime = new Time(System.currentTimeMillis() + 10 * 60 * 1000);
        
        PublicBet bet = new PublicBet(question, choices, votingTime);
        activeBets.add(bet);
        return bet;
    }

    public List<PublicBet> getActiveBets() {
        return activeBets;
    }

    public boolean vote(int betIndex, String username, int choiceIndex, int points) {
        if (betIndex < 0 || betIndex >= activeBets.size()) {
            return false;
        }
        
        PublicBet bet = activeBets.get(betIndex);
        User user = userService.getUser(username);
        
        // Get the choice from the bet's options and vote
        List<Choice> choiceList = new ArrayList<>(bet.getOptions());
        if (choiceIndex < 0 || choiceIndex >= choiceList.size()) {
            return false;
        }
        
        Choice selectedChoice = choiceList.get(choiceIndex);
        bet.vote(user, selectedChoice, points);
        return true;
    }
}