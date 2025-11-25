package minigames;

import backend.User;

import java.util.List;

/**
 * Interface de base pour tous les mini-jeux.
 * Cette interface garantit que tout nouveau jeu ajouté aura des méthodes
 * pour démarrer, gérer les entrées des utilisateurs et déterminer s'il est terminé.
 */
public interface MiniGame {
   
    String getCommandName();

    String start();

    /**
     * Traite l'entrée d'un utilisateur pendant que le jeu est actif.
     * @param user L'utilisateur qui a envoyé le message.
     * @param input L'entrée de l'utilisateur (le message envoyé).
     * @return Le message de réponse du jeu (peut être une mise à jour du score, une nouvelle question, ou un message de fin).
     */
    String processInput(User user, String input);

    boolean isFinished();

    String getResults();

    void reset();
}