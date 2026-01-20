package backend.models;

/**
 * Interface de base pour tous les mini-jeux.
 */
public interface MiniGame {
   
    String getCommandName();

    String start();

    /**
     * @param user L'utilisateur qui a envoyé le message.
     * @param input L'entrée de l'utilisateur (le message envoyé).
     * @return Le message de réponse du jeu (peut être une mise à jour du score, une nouvelle question, ou un message de fin). 
     * A voire
     */
    String processInput(User user, String input);

    boolean isFinished();

    String getResults();

    void reset();
}