package backend.models;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Pari sur une valeur numérique (entier ou flottant)
 * Exemple: Combien de kills au total? Durée du match en minutes?
 * 
 * Résolution:
 * - Si valeur exacte trouvée: pot réparti équitablement entre eux
 * - Sinon: top 30% des plus proches partagent le pot, pondéré par proximité
 */
public class NumericValueBet extends Bet {
    
    private static final String ERROR_VOTING_CLOSED = "❌ Le vote est fermé";
    private static final String ERROR_INVALID_NUMBER = "❌ La valeur doit être un nombre";
    private static final String ERROR_NUMBER_FORMAT = "❌ Format de nombre invalide";
    private static final String ERROR_MUST_BE_INTEGER = "❌ La valeur doit être un entier";
    private static final String ERROR_VALUE_TOO_SMALL = "❌ Valeur trop petite (min: %s)";
    private static final String ERROR_VALUE_TOO_LARGE = "❌ Valeur trop grande (max: %s)";
    private static final String ERROR_INSUFFICIENT_POINTS = "❌ Points insuffisants ou vote déjà enregistré";
    private static final String ERROR_NOT_PENDING = "❌ Le pari doit être en attente pour être résolu";
    private static final String SUCCESS_VOTE = "✅ Vote enregistré: %s (%d points)";
    private static final String SUCCESS_EXACT_MATCH = "✅ Pari résolu! Valeur exacte: %s | %d gagnants exacts | %d points chacun";
    private static final String SUCCESS_PROXIMITY = "✅ Pari résolu! Valeur: %s | Top %d parieurs (30%%):\n";
    private static final String WINNER_DETAIL_FORMAT = "  • %s: %s (écart: %s) → +%d pts%n";
    private static final double TOLERANCE = 0.0001;
    private static final double TOP_PERCENT = 0.30;
    
    private Map<User, Double> userValues;      // User -> valeur prédite (LinkedHashMap pour ordre d'insertion)
    private Double correctValue;               // La valeur correcte (après résolution)
    private boolean isInteger;                 // true = entier, false = flottant
    private Double minValue;                   // Valeur minimale acceptée (optionnel)
    private Double maxValue;                   // Valeur maximale acceptée (optionnel)
    
    /**
     * Crée un pari sur une valeur numérique
     * @param isInteger true pour entiers uniquement, false pour flottants
     * @param minValue valeur minimale acceptée (null = pas de limite)
     * @param maxValue valeur maximale acceptée (null = pas de limite)
     */
    public NumericValueBet(String question, User creator, WatchParty watchParty,
                          LocalDateTime votingEndTime, boolean isInteger,
                          Double minValue, Double maxValue) {
        super(question, creator, watchParty, votingEndTime);
        this.userValues = new LinkedHashMap<>();
        this.isInteger = isInteger;
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.correctValue = null;
    }
    
    @Override
    public String vote(User user, Object votedValue, int points) {
        if (!isVotingOpen()) {
            return ERROR_VOTING_CLOSED;
        }
        
        Double value = parseNumericValue(votedValue);
        if (value == null) {
            return ERROR_INVALID_NUMBER;
        }
        
        String validationError = validateValue(value);
        if (validationError != null) {
            return validationError;
        }
        
        if (!deductPoints(user, points)) {
            return ERROR_INSUFFICIENT_POINTS;
        }
        
        userValues.put(user, value);
        return String.format(SUCCESS_VOTE, formatValue(value), points);
    }
    
    /**
     * Parse une valeur numérique depuis différents types
     */
    private Double parseNumericValue(Object value) {
        try {
            if (value instanceof Number number) {
                return number.doubleValue();
            } else if (value instanceof String str) {
                return Double.parseDouble(str);
            }
        } catch (NumberFormatException e) {
            return null;
        }
        return null;
    }
    
    /**
     * Valide les contraintes sur la valeur
     */
    private String validateValue(double value) {
        if (isInteger && value != Math.floor(value)) {
            return ERROR_MUST_BE_INTEGER;
        }
        
        if (minValue != null && value < minValue) {
            return String.format(ERROR_VALUE_TOO_SMALL, minValue);
        }
        
        if (maxValue != null && value > maxValue) {
            return String.format(ERROR_VALUE_TOO_LARGE, maxValue);
        }
        
        return null;
    }
    
    @Override
    public String resolve(Object correctValue) {
        if (state != State.PENDING) {
            return ERROR_NOT_PENDING;
        }
        
        Double value = parseNumericValue(correctValue);
        if (value == null) {
            return ERROR_NUMBER_FORMAT;
        }
        
        this.correctValue = value;
        state = State.RESOLVED;
        
        int totalPot = getTotalPot();
        List<User> exactMatches = findExactMatches(value);
        
        if (!exactMatches.isEmpty()) {
            return distributeExactMatchRewards(exactMatches, totalPot, value);
        }
        
        return resolveByProximity(value, totalPot);
    }
    
    /**
     * Trouve les utilisateurs avec une valeur exacte
     */
    private List<User> findExactMatches(double correctValue) {
        List<User> exactMatches = new ArrayList<>();
        for (Map.Entry<User, Double> entry : userValues.entrySet()) {
            if (Math.abs(entry.getValue() - correctValue) < TOLERANCE) {
                exactMatches.add(entry.getKey());
            }
        }
        return exactMatches;
    }
    
    /**
     * Distribue les récompenses pour les matchs exacts
     */
    private String distributeExactMatchRewards(List<User> winners, int totalPot, double value) {
        int rewardPerWinner = totalPot / winners.size();
        for (User winner : winners) {
            creditUserPoints(winner, rewardPerWinner);
            recordWin(winner);
            if (isOffersTicket()) {
                watchParty.grantTicket(winner, TicketType.NUMERIC_VALUE);
                if (Math.random() < 0.10) { //NOSONAR S2245: Random is acceptable for game mechanics
                    watchParty.grantTicket(winner, TicketType.IN_OR_OUT);
                }
            }
        }
        return String.format(SUCCESS_EXACT_MATCH, formatValue(value), winners.size(), rewardPerWinner);
    }
    
    /**
     * Résolution par proximité: top 30% partagent le pot pondéré
     */
    private String resolveByProximity(double correctValue, int totalPot) {
        // Calculer les écarts pour chaque utilisateur
        List<UserDistance> distances = new ArrayList<>();
        for (Map.Entry<User, Double> entry : userValues.entrySet()) {
            double distance = Math.abs(entry.getValue() - correctValue);
            distances.add(new UserDistance(entry.getKey(), entry.getValue(), distance));
        }
        
        // Trier par distance (plus proche en premier)
        // L'ordre d'insertion est préservé via LinkedHashMap: en cas d'égalité,
        // le premier à avoir voté est prioritaire (tri stable)
        Collections.sort(distances, Comparator.comparingDouble(ud -> ud.distance));
        
        // Sélectionner le top 30% (arrondi supérieur)
        int winnerCount = (int) Math.ceil(distances.size() * TOP_PERCENT);
        List<UserDistance> winners = distances.subList(0, Math.min(winnerCount, distances.size()));
        
        // Calculer les poids (inverse de la distance normalisée)
        double maxDistance = distances.get(distances.size() - 1).distance;
        double totalWeight = 0.0;
        Map<User, Double> weights = new HashMap<>();
        
        for (UserDistance ud : winners) {
            // Poids = 1 / (1 + distance_normalisée)
            double normalizedDistance = maxDistance > 0 ? ud.distance / maxDistance : 0;
            double weight = 1.0 / (1.0 + normalizedDistance);
            weights.put(ud.user, weight);
            totalWeight += weight;
        }
        
        // Distribuer le pot proportionnellement aux poids
        StringBuilder result = new StringBuilder();
        result.append(String.format(SUCCESS_PROXIMITY, formatValue(correctValue), winners.size()));
        
        for (UserDistance ud : winners) {
            double weight = weights.get(ud.user);
            int reward = (int) ((weight / totalWeight) * totalPot);
            creditUserPoints(ud.user, reward);
            recordWin(ud.user);
            if (isOffersTicket()) {
                watchParty.grantTicket(ud.user, TicketType.NUMERIC_VALUE);
                if (Math.random() < 0.10) { //NOSONAR S2245: Random is acceptable for game mechanics
                    watchParty.grantTicket(ud.user, TicketType.IN_OR_OUT);
                }
            }
            
            result.append(String.format(WINNER_DETAIL_FORMAT,
                                      ud.user.getName(),
                                      formatValue(ud.predictedValue),
                                      formatValue(ud.distance),
                                      reward));
        }
        
        return result.toString().trim();
    }
    
    /**
     * Classe interne pour stocker user + distance
     */
    private static class UserDistance {
        User user;
        double predictedValue;
        double distance;
        
        UserDistance(User user, double predictedValue, double distance) {
            this.user = user;
            this.predictedValue = predictedValue;
            this.distance = distance;
        }
    }
    
    /**
     * Formate une valeur selon son type (entier ou flottant)
     */
    private String formatValue(double value) {
        if (isInteger) {
            return String.valueOf((int) value);
        }
        return String.format("%.2f", value);
    }
    
    // Getters
    public Map<User, Double> getUserValues() { return new HashMap<>(userValues); }
    public Double getCorrectValue() { return correctValue; }
    public boolean isInteger() { return isInteger; }
    public Double getMinValue() { return minValue; }
    public Double getMaxValue() { return maxValue; }

    /**
     * Permet de modifier la valeur d'un utilisateur pendant PENDING via ticket.
     */
    public String modifyValue(User user, Double newValue) {
        if (state != State.PENDING) return "❌ Le pari doit être en attente (PENDING)";
        if (!userValues.containsKey(user)) return "❌ Aucun vote enregistré";
        if (newValue == null) return "❌ Valeur invalide";
        String err = validateValue(newValue);
        if (err != null) return err;
        userValues.put(user, newValue);
        return "✅ Valeur modifiée";
    }
}
