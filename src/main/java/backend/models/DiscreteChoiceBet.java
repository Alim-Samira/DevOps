package backend.models;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Pari classique avec choix discrets (2 à 4 options)
 * Exemple: Quelle équipe va gagner? T1 / GenG / DRX
 * 
 * Résolution: Le pot total est réparti équitablement entre tous les parieurs
 * ayant choisi la bonne réponse.
 */
public class DiscreteChoiceBet extends Bet {
    
    private List<String> choices;                    // Les options disponibles
    private Map<User, String> userChoices;           // User -> choix sélectionné
    private String correctChoice;                    // La réponse correcte (après résolution)
    
    /**
     * Crée un pari à choix discrets
     * @param choices Liste de 2 à 4 options
     */
    public DiscreteChoiceBet(String question, User creator, WatchParty watchParty, 
                             LocalDateTime votingEndTime, List<String> choices) {
        super(question, creator, watchParty, votingEndTime);
        
        if (choices == null || choices.size() < 2 || choices.size() > 4) {
            throw new IllegalArgumentException("Un pari doit avoir entre 2 et 4 choix");
        }
        
        this.choices = new ArrayList<>(choices);
        this.userChoices = new HashMap<>();
        this.correctChoice = null;
    }
    
    @Override
    public String vote(User user, Object votedValue, int points) {
        if (!isVotingOpen()) {
            return "❌ Le vote est fermé";
        }
        
        if (!(votedValue instanceof String)) {
            return "❌ Le choix doit être une chaîne de caractères";
        }
        
        String choice = (String) votedValue;
        if (!choices.contains(choice)) {
            return "❌ Choix invalide. Options disponibles: " + String.join(", ", choices);
        }
        
        if (!deductPoints(user, points)) {
            return "❌ Points insuffisants ou vote déjà enregistré";
        }
        
        userChoices.put(user, choice);
        return "✅ Vote enregistré: " + choice + " (" + points + " points)";
    }
    
    @Override
    public String resolve(Object correctValue) {
        if (state != State.PENDING) {
            return "❌ Le pari doit être en attente pour être résolu";
        }
        
        if (!(correctValue instanceof String)) {
            return "❌ La réponse doit être une chaîne de caractères";
        }
        
        String correct = (String) correctValue;
        if (!choices.contains(correct)) {
            return "❌ Réponse invalide. Options: " + String.join(", ", choices);
        }
        
        this.correctChoice = correct;
        state = State.RESOLVED;
        
        // Trouver tous les gagnants
        List<User> winners = new ArrayList<>();
        for (Map.Entry<User, String> entry : userChoices.entrySet()) {
            if (entry.getValue().equals(correctChoice)) {
                winners.add(entry.getKey());
            }
        }
        
        int totalPot = getTotalPot();
        
        if (winners.isEmpty()) {
            return "⚠️ Aucun gagnant! Pot de " + totalPot + " points perdu";
        }
        
        // Répartition équitable du pot
        int rewardPerWinner = totalPot / winners.size();
        for (User winner : winners) {
            creditUserPoints(winner, rewardPerWinner);
            // Tickets
            if (isOffersTicket()) {
                watchParty.grantTicket(winner, TicketType.DISCRETE_CHOICE);
                // 10% de chance d'un ticket IN_OR_OUT additionnel
                if (Math.random() < 0.10) {
                    watchParty.grantTicket(winner, TicketType.IN_OR_OUT);
                }
            }
        }
        
        return String.format("✅ Pari résolu! Réponse: %s | %d gagnants | %d points chacun",
                           correctChoice, winners.size(), rewardPerWinner);
    }
    
    /**
     * Obtient la distribution des votes (pour affichage)
     */
    public Map<String, Integer> getVoteDistribution() {
        Map<String, Integer> distribution = new HashMap<>();
        for (String choice : choices) {
            distribution.put(choice, 0);
        }
        
        for (String choice : userChoices.values()) {
            distribution.put(choice, distribution.get(choice) + 1);
        }
        
        return distribution;
    }
    
    // Getters
    public List<String> getChoices() { return new ArrayList<>(choices); }
    public String getCorrectChoice() { return correctChoice; }
    public Map<User, String> getUserChoices() { return new HashMap<>(userChoices); }

    /**
     * Permet de modifier le choix d'un utilisateur pendant PENDING via ticket.
     */
    public String modifyChoice(User user, String newChoice) {
        if (state != State.PENDING) return "❌ Le pari doit être en attente (PENDING)";
        if (!userChoices.containsKey(user)) return "❌ Aucun vote enregistré";
        if (newChoice == null || !choices.contains(newChoice)) return "❌ Choix invalide";
        userChoices.put(user, newChoice);
        return "✅ Choix modifié";
    }
}
