package backend.models;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
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
    protected String watchPartyName; // Nom de la watch party
    protected boolean isPublic;      // Si la watch party est publique
    protected State state;
    protected LocalDateTime votingEndTime;
    protected Map<User, Integer> userBets; // User -> points misés
    protected boolean offersTicket;        // Ce pari offre-t-il un ticket aux gagnants ?
    
    /**
     * Constructeur protégé - utiliser les factory methods des sous-classes
     */
    protected Bet(String question, User creator, WatchParty watchParty, LocalDateTime votingEndTime) {
        if (!creator.isAdmin()) {
            throw new IllegalArgumentException("Seuls les admins peuvent créer des paris");
        }
        this.question = question;
        this.creator = creator;
        this.watchPartyName = watchParty.name();
        this.isPublic = watchParty.isPublic();
        this.state = State.VOTING;
        this.votingEndTime = votingEndTime;
        this.userBets = new HashMap<>();
        this.offersTicket = false;
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
     * Récupère la liste des gagnants après résolution
     * @return liste des utilisateurs gagnants (vide si pas encore résolu)
     */
    public abstract List<User> getLastWinners();
    
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
            creditUserPoints(user, betAmount);
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
        if (!hasSufficientPoints(user, points)) {
            return false;
        }
        if (userBets.containsKey(user)) {
            return false; // Un seul vote par utilisateur
        }
        
        debitUserPoints(user, points);
        userBets.put(user, points);
        return true;
    }

    /**
     * Permet d'ajuster la mise d'un utilisateur pendant l'état PENDING (utilisation de ticket IN_OR_OUT).
     */
    public String adjustBetPoints(User user, int newPoints) {
        if (state != State.PENDING) {
            return "❌ Le pari doit être en attente (PENDING)";
        }
        Integer current = userBets.get(user);
        if (current == null) {
            return "❌ Aucun vote enregistré pour l'utilisateur";
        }
        if (newPoints < 0) {
            return "❌ La mise doit être >= 0";
        }
        if (newPoints == current) {
            return "ℹ️ Mise inchangée";
        }
        if (newPoints == 0) {
            // Se retirer du pari: rembourser la mise actuelle
            creditUserPoints(user, current);
            userBets.remove(user);
            return "✅ Retrait du pari, " + current + " points remboursés";
        }
        if (newPoints > current) {
            int delta = newPoints - current;
            if (!hasSufficientPoints(user, delta)) {
                return "❌ Points insuffisants pour augmenter la mise";
            }
            debitUserPoints(user, delta);
            userBets.put(user, newPoints);
            return "✅ Mise augmentée de " + delta + " points";
        } else {
            int delta = current - newPoints;
            // Rembourser la différence
            creditUserPoints(user, delta);
            userBets.put(user, newPoints);
            return "✅ Mise diminuée de " + delta + " points";
        }
    }

    protected boolean hasSufficientPoints(User user, int points) {
        if (isPublic) {
            return user.getPublicPoints() >= points;
        }
        return user.getPointsForWatchParty(watchPartyName) >= points;
    }

    protected void debitUserPoints(User user, int points) {
        if (isPublic) {
            user.addPublicPoints(-points);
        } else {
            user.addPointsForWatchParty(watchPartyName, -points);
        }
    }

    protected void creditUserPoints(User user, int points) {
        if (isPublic) {
            user.addPublicPoints(points);
        } else {
            user.addPointsForWatchParty(watchPartyName, points);
        }
    }

    /**
     * Enregistre une victoire pour l'utilisateur selon le type de watchparty.
     */
    protected void recordWin(User user) {
        if (isPublic) {
            user.addPublicWin();
        } else {
            user.addWinForWatchParty(watchPartyName);
        }
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
    public State getState() { return state; }
    public LocalDateTime getVotingEndTime() { return votingEndTime; }
    public Map<User, Integer> getUserBets() { return new HashMap<>(userBets); }
    public int getParticipantCount() { return userBets.size(); }
    public boolean isOffersTicket() { return offersTicket; }
    public void setOffersTicket(boolean offersTicket) { this.offersTicket = offersTicket; }
    public String getWatchPartyName() { return watchPartyName; }
    public boolean isPublic() { return isPublic; }
}


