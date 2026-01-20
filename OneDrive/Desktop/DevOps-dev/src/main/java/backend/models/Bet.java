package backend.models;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Classe abstraite de base pour tous les types de paris.
 * Seuls les admins peuvent créer des paris.
 * Un seul pari actif par watch party.
 */
public abstract class Bet {
    
    public enum State {
        VOTING,    // Phase de vote ouverte
        PENDING,   // Vote fermé, en attente du résultat
        RESOLVED,  // Résolu avec distribution des gains
        CANCELED   // Annulé, points remboursés
    }
    
    protected String question;
    protected User creator;          // Admin qui a créé le pari
    protected WatchParty watchParty; // Watch party associée
    protected State state;
    protected LocalDateTime votingEndTime;
    protected Map<User, Integer> userBets; // User -> points misés
    
    /**
     * Constructeur protégé - utiliser les factory methods des sous-classes
     */
    protected Bet(String question, User creator, WatchParty watchParty, LocalDateTime votingEndTime) {
        if (!creator.isAdmin()) {
            throw new IllegalArgumentException("Seuls les admins peuvent créer des paris");
        }
        this.question = question;
        this.creator = creator;
        this.watchParty = watchParty;
        this.state = State.VOTING;
        this.votingEndTime = votingEndTime;
        this.userBets = new HashMap<>();
    }
    
    /**
     * Soumet un vote pour ce pari
     * @return message de confirmation ou d'erreur
     */
    public abstract String vote(User user, Object votedValue, int points);
    
    /**
     * Résout le pari avec la valeur correcte et distribue les gains
     * @return message de résultat avec détails de distribution
     */
    public abstract String resolve(Object correctValue);
    
    /**
     * Annule le pari et rembourse tous les parieurs
     */
    public String cancel() {
        if (state == State.RESOLVED || state == State.CANCELED) {
            return "❌ Le pari est déjà terminé";
        }
        
        state = State.CANCELED;
        
        // Rembourser tous les parieurs
        for (Map.Entry<User, Integer> entry : userBets.entrySet()) {
            User user = entry.getKey();
            int betAmount = entry.getValue();
            user.setPoints(user.getPoints() + betAmount);
        }
        
        return "✅ Pari annulé, " + userBets.size() + " parieurs remboursés";
    }
    
    /**
     * Ferme la phase de vote
     */
    public String endVoting() {
        if (state != State.VOTING) {
            return "❌ Le vote n'est pas ouvert";
        }
        state = State.PENDING;
        return "✅ Phase de vote terminée, en attente du résultat";
    }
    
    /**
     * Vérifie si la phase de vote est encore ouverte
     */
    public boolean isVotingOpen() {
        return state == State.VOTING && LocalDateTime.now().isBefore(votingEndTime);
    }
    
    /**
     * Calcule le pot total
     */
    protected int getTotalPot() {
        return userBets.values().stream().mapToInt(Integer::intValue).sum();
    }
    
    /**
     * Valide et débite les points d'un parieur
     */
    protected boolean deductPoints(User user, int points) {
        if (points <= 0) {
            return false;
        }
        if (user.getPoints() < points) {
            return false;
        }
        if (userBets.containsKey(user)) {
            return false; // Un seul vote par utilisateur
        }
        
        user.setPoints(user.getPoints() - points);
        userBets.put(user, points);
        return true;
    }
    
    // Getters
    public BetType getType() {
        if (this instanceof DiscreteChoiceBet) return BetType.DISCRETE_CHOICE;
        if (this instanceof NumericValueBet) return BetType.NUMERIC_VALUE;
        if (this instanceof OrderedRankingBet) return BetType.ORDERED_RANKING;
        throw new IllegalStateException("Type de pari inconnu");
    }
    
    public String getQuestion() { return question; }
    public User getCreator() { return creator; }
    public WatchParty getWatchParty() { return watchParty; }
    public State getState() { return state; }
    public LocalDateTime getVotingEndTime() { return votingEndTime; }
    public Map<User, Integer> getUserBets() { return new HashMap<>(userBets); }
    public int getParticipantCount() { return userBets.size(); }
}


