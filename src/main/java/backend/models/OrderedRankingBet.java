package backend.models;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Pari sur un classement ordonné d'éléments
 * Exemple: Classement des 5 meilleurs joueurs par kills
 * 
 * Résolution:
 * - Si classement(s) parfait(s): pot réparti équitablement entre eux
 * - Sinon: top 30% par distance de Kendall tau partagent le pot pondéré
 */
public class OrderedRankingBet extends Bet {
    
    private List<String> items;                          // Les éléments à classer (ex: joueurs)
    private Map<User, List<String>> userRankings;        // User -> son classement
    private List<String> correctRanking;                 // Le classement correct
    
    /**
     * Crée un pari de classement ordonné
     * @param items Liste des éléments à classer (ordre n'a pas d'importance ici)
     */
    public OrderedRankingBet(String question, User creator, WatchParty watchParty,
                            LocalDateTime votingEndTime, List<String> items) {
        super(question, creator, watchParty, votingEndTime);
        
        if (items == null || items.size() < 2) {
            throw new IllegalArgumentException("Au moins 2 éléments requis pour un classement");
        }
        
        this.items = new ArrayList<>(items);
        this.userRankings = new HashMap<>();
        this.correctRanking = null;
    }
    
    @Override
    public String vote(User user, Object votedValue, int points) {
        if (!isVotingOpen()) {
            return "❌ Le vote est fermé";
        }
        
        if (!(votedValue instanceof List)) {
            return "❌ Le classement doit être une liste";
        }
        
        @SuppressWarnings("unchecked")
        List<String> ranking = (List<String>) votedValue;
        
        // Valider que le classement contient exactement les mêmes éléments
        if (ranking.size() != items.size()) {
            return "❌ Le classement doit contenir " + items.size() + " éléments";
        }
        
        List<String> sortedRanking = new ArrayList<>(ranking);
        List<String> sortedItems = new ArrayList<>(items);
        Collections.sort(sortedRanking);
        Collections.sort(sortedItems);
        
        if (!sortedRanking.equals(sortedItems)) {
            return "❌ Le classement contient des éléments invalides. Éléments attendus: " 
                   + String.join(", ", items);
        }
        
        if (!deductPoints(user, points)) {
            return "❌ Points insuffisants ou vote déjà enregistré";
        }
        
        userRankings.put(user, new ArrayList<>(ranking));
        return "✅ Classement enregistré: " + String.join(" > ", ranking) + " (" + points + " points)";
    }
    
    @Override
    public String resolve(Object correctValue) {
        if (state != State.PENDING) {
            return "❌ Le pari doit être en attente pour être résolu";
        }
        
        if (!(correctValue instanceof List)) {
            return "❌ Le classement correct doit être une liste";
        }
        
        @SuppressWarnings("unchecked")
        List<String> ranking = (List<String>) correctValue;
        
        // Valider le classement correct
        List<String> sortedRanking = new ArrayList<>(ranking);
        List<String> sortedItems = new ArrayList<>(items);
        Collections.sort(sortedRanking);
        Collections.sort(sortedItems);
        
        if (!sortedRanking.equals(sortedItems)) {
            return "❌ Le classement correct est invalide";
        }
        
        this.correctRanking = new ArrayList<>(ranking);
        state = State.RESOLVED;
        
        int totalPot = getTotalPot();
        
        // Vérifier s'il y a des classements parfaits
        List<User> perfectMatches = new ArrayList<>();
        for (Map.Entry<User, List<String>> entry : userRankings.entrySet()) {
            if (entry.getValue().equals(correctRanking)) {
                perfectMatches.add(entry.getKey());
            }
        }
        
        // Si classements parfaits trouvés, répartition équitable
        if (!perfectMatches.isEmpty()) {
            int rewardPerWinner = totalPot / perfectMatches.size();
            for (User winner : perfectMatches) {
                winner.setPoints(winner.getPoints() + rewardPerWinner);
            }
            return String.format("✅ Pari résolu! %d classement(s) parfait(s) | %d points chacun\nClassement: %s",
                               perfectMatches.size(), rewardPerWinner, String.join(" > ", correctRanking));
        }
        
        // Sinon, calcul des distances Kendall tau et répartition top 30%
        return resolveByKendallDistance(totalPot);
    }
    
    /**
     * Résolution par distance de Kendall tau: top 30% partagent le pot pondéré
     */
    private String resolveByKendallDistance(int totalPot) {
        // Calculer la distance de Kendall tau pour chaque utilisateur
        List<UserRankingDistance> distances = new ArrayList<>();
        for (Map.Entry<User, List<String>> entry : userRankings.entrySet()) {
            double distance = calculateKendallTauDistance(entry.getValue(), correctRanking);
            distances.add(new UserRankingDistance(entry.getKey(), entry.getValue(), distance));
        }
        
        // Trier par distance (plus proche en premier)
        Collections.sort(distances, Comparator.comparingDouble(urd -> urd.distance));
        
        // Sélectionner le top 30% (arrondi supérieur)
        int winnerCount = (int) Math.ceil(distances.size() * 0.30);
        List<UserRankingDistance> winners = distances.subList(0, Math.min(winnerCount, distances.size()));
        
        // Calculer les poids (inverse de la distance normalisée)
        double maxDistance = calculateMaxKendallDistance(items.size());
        double totalWeight = 0.0;
        Map<User, Double> weights = new HashMap<>();
        
        for (UserRankingDistance urd : winners) {
            // Poids = 1 / (1 + distance_normalisée)
            double normalizedDistance = maxDistance > 0 ? urd.distance / maxDistance : 0;
            double weight = 1.0 / (1.0 + normalizedDistance);
            weights.put(urd.user, weight);
            totalWeight += weight;
        }
        
        // Distribuer le pot proportionnellement aux poids
        StringBuilder result = new StringBuilder();
        result.append(String.format("✅ Pari résolu! Classement: %s\nTop %d parieurs (30%%):\n",
                                   String.join(" > ", correctRanking), winners.size()));
        
        for (UserRankingDistance urd : winners) {
            double weight = weights.get(urd.user);
            int reward = (int) ((weight / totalWeight) * totalPot);
            urd.user.setPoints(urd.user.getPoints() + reward);
            
            result.append(String.format("  • %s: %s (distance: %.0f) → +%d pts\n",
                                      urd.user.getName(),
                                      String.join(" > ", urd.ranking),
                                      urd.distance,
                                      reward));
        }
        
        return result.toString().trim();
    }
    
    /**
     * Calcule la distance de Kendall tau entre deux classements
     * Compte le nombre de paires d'éléments inversées entre les deux listes
     */
    private double calculateKendallTauDistance(List<String> ranking1, List<String> ranking2) {
        int n = ranking1.size();
        int inversions = 0;
        
        // Créer des mappings position -> élément pour accès rapide
        Map<String, Integer> pos1 = new HashMap<>();
        Map<String, Integer> pos2 = new HashMap<>();
        
        for (int i = 0; i < n; i++) {
            pos1.put(ranking1.get(i), i);
            pos2.put(ranking2.get(i), i);
        }
        
        // Compter les inversions
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                String item1 = ranking1.get(i);
                String item2 = ranking1.get(j);
                
                // Dans ranking1: item1 est avant item2 (i < j)
                // Vérifier l'ordre dans ranking2
                int pos1InRank2 = pos2.get(item1);
                int pos2InRank2 = pos2.get(item2);
                
                if (pos1InRank2 > pos2InRank2) {
                    inversions++; // Ordre inversé dans ranking2
                }
            }
        }
        
        return inversions;
    }
    
    /**
     * Calcule la distance maximale possible pour un classement de taille n
     */
    private double calculateMaxKendallDistance(int n) {
        // Distance max = nombre total de paires = n * (n-1) / 2
        return (n * (n - 1)) / 2.0;
    }
    
    /**
     * Classe interne pour stocker user + classement + distance
     */
    private static class UserRankingDistance {
        User user;
        List<String> ranking;
        double distance;
        
        UserRankingDistance(User user, List<String> ranking, double distance) {
            this.user = user;
            this.ranking = ranking;
            this.distance = distance;
        }
    }
    
    // Getters
    public List<String> getItems() { return new ArrayList<>(items); }
    public Map<User, List<String>> getUserRankings() { return new HashMap<>(userRankings); }
    public List<String> getCorrectRanking() { 
        return correctRanking != null ? new ArrayList<>(correctRanking) : null; 
    }
}
