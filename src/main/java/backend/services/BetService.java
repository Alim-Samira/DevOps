package backend.services;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import backend.models.Bet;
import backend.models.DiscreteChoiceBet;
import backend.models.NumericValueBet;
import backend.models.OrderedRankingBet;
import backend.models.User;
import backend.models.WatchParty;

/**
 * Service de gestion des paris
 * - Seuls les admins peuvent créer des paris
 * - Un seul pari actif par watch party
 * - Support de 3 types de paris: discret, numérique, classement
 */
@Service
public class BetService {
    
    private final WatchPartyManager watchPartyManager;
    private final UserService userService;

    public BetService(WatchPartyManager watchPartyManager, UserService userService) {
        this.watchPartyManager = watchPartyManager;
        this.userService = userService;
    }

    /**
     * Crée un pari à choix discrets (2-4 options)
     */
    public String createDiscreteChoiceBet(String watchPartyName, String adminName, 
                                         String question, List<String> choices, 
                                         int votingMinutes) {
        User admin = userService.getUser(adminName);
        if (!admin.isAdmin()) {
            return "❌ Seuls les admins peuvent créer des paris";
        }
        
        WatchParty wp = watchPartyManager.getWatchPartyByName(watchPartyName);
        if (wp == null) {
            return "❌ Watch party introuvable: " + watchPartyName;
        }
        
        if (wp.hasActiveBet()) {
            return "❌ Un pari est déjà actif pour cette watch party";
        }
        
        LocalDateTime votingEndTime = LocalDateTime.now().plusMinutes(votingMinutes);
        DiscreteChoiceBet bet = new DiscreteChoiceBet(question, admin, wp, votingEndTime, choices);
        
        return wp.createBet(bet);
    }
    
    /**
     * Crée un pari sur une valeur numérique
     */
    public String createNumericValueBet(String watchPartyName, String adminName,
                                       String question, boolean isInteger,
                                       Double minValue, Double maxValue,
                                       int votingMinutes) {
        User admin = userService.getUser(adminName);
        if (!admin.isAdmin()) {
            return "❌ Seuls les admins peuvent créer des paris";
        }
        
        WatchParty wp = watchPartyManager.getWatchPartyByName(watchPartyName);
        if (wp == null) {
            return "❌ Watch party introuvable: " + watchPartyName;
        }
        
        if (wp.hasActiveBet()) {
            return "❌ Un pari est déjà actif pour cette watch party";
        }
        
        LocalDateTime votingEndTime = LocalDateTime.now().plusMinutes(votingMinutes);
        NumericValueBet bet = new NumericValueBet(question, admin, wp, votingEndTime, 
                                                  isInteger, minValue, maxValue);
        
        return wp.createBet(bet);
    }
    
    /**
     * Crée un pari de classement ordonné
     */
    public String createOrderedRankingBet(String watchPartyName, String adminName,
                                         String question, List<String> items,
                                         int votingMinutes) {
        User admin = userService.getUser(adminName);
        if (!admin.isAdmin()) {
            return "❌ Seuls les admins peuvent créer des paris";
        }
        
        WatchParty wp = watchPartyManager.getWatchPartyByName(watchPartyName);
        if (wp == null) {
            return "❌ Watch party introuvable: " + watchPartyName;
        }
        
        if (wp.hasActiveBet()) {
            return "❌ Un pari est déjà actif pour cette watch party";
        }
        
        LocalDateTime votingEndTime = LocalDateTime.now().plusMinutes(votingMinutes);
        OrderedRankingBet bet = new OrderedRankingBet(question, admin, wp, votingEndTime, items);
        
        return wp.createBet(bet);
    }
    
    /**
     * Soumet un vote sur le pari actif d'une watch party
     */
    public String vote(String watchPartyName, String username, Object votedValue, int points) {
        WatchParty wp = watchPartyManager.getWatchPartyByName(watchPartyName);
        if (wp == null) {
            return "❌ Watch party introuvable: " + watchPartyName;
        }
        
        if (!wp.hasActiveBet()) {
            return "❌ Aucun pari actif pour cette watch party";
        }
        
        User user = userService.getUser(username);
        Bet bet = wp.getActiveBet();
        
        return bet.vote(user, votedValue, points);
    }
    
    /**
     * Ferme la phase de vote du pari actif
     */
    public String endVoting(String watchPartyName, String adminName) {
        User admin = userService.getUser(adminName);
        if (!admin.isAdmin()) {
            return "❌ Seuls les admins peuvent fermer le vote";
        }
        
        WatchParty wp = watchPartyManager.getWatchPartyByName(watchPartyName);
        if (wp == null) {
            return "❌ Watch party introuvable: " + watchPartyName;
        }
        
        return wp.closeActiveBet();
    }
    
    /**
     * Résout un pari avec la valeur correcte
     */
    public String resolveBet(String watchPartyName, String adminName, Object correctValue) {
        User admin = userService.getUser(adminName);
        if (!admin.isAdmin()) {
            return "❌ Seuls les admins peuvent résoudre un pari";
        }
        
        WatchParty wp = watchPartyManager.getWatchPartyByName(watchPartyName);
        if (wp == null) {
            return "❌ Watch party introuvable: " + watchPartyName;
        }
        
        if (!wp.hasActiveBet()) {
            return "❌ Aucun pari actif pour cette watch party";
        }
        
        Bet bet = wp.getActiveBet();
        return bet.resolve(correctValue);
    }
    
    /**
     * Annule un pari et rembourse les parieurs
     */
    public String cancelBet(String watchPartyName, String adminName) {
        User admin = userService.getUser(adminName);
        if (!admin.isAdmin()) {
            return "❌ Seuls les admins peuvent annuler un pari";
        }
        
        WatchParty wp = watchPartyManager.getWatchPartyByName(watchPartyName);
        if (wp == null) {
            return "❌ Watch party introuvable: " + watchPartyName;
        }
        
        if (!wp.hasActiveBet()) {
            return "❌ Aucun pari actif pour cette watch party";
        }
        
        Bet bet = wp.getActiveBet();
        return bet.cancel();
    }
    
    /**
     * Récupère tous les paris actifs (pour toutes les watch parties)
     */
    public List<Bet> getAllActiveBets() {
        List<Bet> activeBets = new ArrayList<>();
        for (WatchParty wp : watchPartyManager.getAllWatchParties()) {
            if (wp.hasActiveBet()) {
                activeBets.add(wp.getActiveBet());
            }
        }
        return activeBets;
    }
    
    /**
     * Récupère le pari actif d'une watch party spécifique
     */
    public Bet getActiveBet(String watchPartyName) {
        WatchParty wp = watchPartyManager.getWatchPartyByName(watchPartyName);
        if (wp == null) {
            return null;
        }
        return wp.getActiveBet();
    }
}