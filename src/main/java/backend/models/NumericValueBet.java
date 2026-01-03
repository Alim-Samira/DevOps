package backend.models;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
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
    
    private Map<User, Double> userValues;      // User -> valeur prédite
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
        this.userValues = new HashMap<>();
        this.isInteger = isInteger;
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.correctValue = null;
    }
    
    @Override
    public String vote(User user, Object votedValue, int points) {
        if (!isVotingOpen()) {
            return "❌ Le vote est fermé";
        }
        
        Double value;
        try {
            if (votedValue instanceof Number) {
                value = ((Number) votedValue).doubleValue();
            } else if (votedValue instanceof String) {
                value = Double.parseDouble((String) votedValue);
            } else {
                return "❌ La valeur doit être un nombre";
            }
        } catch (NumberFormatException e) {
            return "❌ Format de nombre invalide";
        }
        
        // Validation des contraintes
        if (isInteger && value != Math.floor(value)) {
            return "❌ La valeur doit être un entier";
        }
        
        if (minValue != null && value < minValue) {
            return "❌ Valeur trop petite (min: " + minValue + ")";
        }
        
        if (maxValue != null && value > maxValue) {
            return "❌ Valeur trop grande (max: " + maxValue + ")";
        }
        
        if (!deductPoints(user, points)) {
            return "❌ Points insuffisants ou vote déjà enregistré";
        }
        
        userValues.put(user, value);
        return "✅ Vote enregistré: " + formatValue(value) + " (" + points + " points)";
    }
    
    @Override
    public String resolve(Object correctValue) {
        if (state != State.PENDING) {
            return "❌ Le pari doit être en attente pour être résolu";
        }
        
        Double value;
        try {
            if (correctValue instanceof Number) {
                value = ((Number) correctValue).doubleValue();
            } else if (correctValue instanceof String) {
                value = Double.parseDouble((String) correctValue);
            } else {
                return "❌ La valeur doit être un nombre";
            }
        } catch (NumberFormatException e) {
            return "❌ Format de nombre invalide";
        }
        
        this.correctValue = value;
        state = State.RESOLVED;
        
        int totalPot = getTotalPot();
        
        // Vérifier s'il y a des valeurs exactes
        List<User> exactMatches = new ArrayList<>();
        for (Map.Entry<User, Double> entry : userValues.entrySet()) {
            if (Math.abs(entry.getValue() - value) < 0.0001) { // Tolérance pour flottants
                exactMatches.add(entry.getKey());
            }
        }
        
        // Si valeurs exactes trouvées, répartition équitable
        if (!exactMatches.isEmpty()) {
            int rewardPerWinner = totalPot / exactMatches.size();
            for (User winner : exactMatches) {
                winner.setPoints(winner.getPoints() + rewardPerWinner);
            }
            return String.format("✅ Pari résolu! Valeur exacte: %s | %d gagnants exacts | %d points chacun",
                               formatValue(value), exactMatches.size(), rewardPerWinner);
        }
        
        // Sinon, calcul des distances et répartition top 30%
        return resolveByProximity(value, totalPot);
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
        Collections.sort(distances, Comparator.comparingDouble(ud -> ud.distance));
        
        // Sélectionner le top 30% (arrondi supérieur)
        int winnerCount = (int) Math.ceil(distances.size() * 0.30);
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
        result.append(String.format("✅ Pari résolu! Valeur: %s | Top %d parieurs (30%%):\n",
                                   formatValue(correctValue), winners.size()));
        
        for (UserDistance ud : winners) {
            double weight = weights.get(ud.user);
            int reward = (int) ((weight / totalWeight) * totalPot);
            ud.user.setPoints(ud.user.getPoints() + reward);
            
            result.append(String.format("  • %s: %s (écart: %s) → +%d pts\n",
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
}
