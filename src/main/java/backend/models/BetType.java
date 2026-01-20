package backend.models;

/**
 * Types de paris disponibles dans le système
 */
public enum BetType {
    /**
     * Pari classique avec choix discrets (ex: équipe gagnante)
     * Résolution: pot réparti équitablement entre tous les gagnants
     */
    DISCRETE_CHOICE,
    
    /**
     * Pari sur une valeur numérique (ex: nombre de kills, durée du match)
     * Résolution: top 30% des plus proches partagent le pot proportionnellement
     */
    NUMERIC_VALUE,
    
    /**
     * Pari sur un classement ordonné (ex: top 5 joueurs par kills)
     * Résolution: top 30% par distance de Kendall tau partagent le pot
     */
    ORDERED_RANKING
}
