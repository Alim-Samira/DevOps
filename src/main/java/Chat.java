import java.util.ArrayList;
import java.util.List;
import java.text.SimpleDateFormat;
import java.util.Date;
import minigames.MiniGame;
import minigames.QuizGame;

public abstract class Chat {
    protected String name;
    protected List<Message> messages;
    protected User admin;
    //Gestion des mini-jeux
    private MiniGame activeGame;
    private final List<MiniGame> availableGames;

    public Chat(String name, User admin) {
        this.name = name;
        this.admin = admin;
        this.messages = new ArrayList<>();
        //  Initialisation des jeux disponibles (extensible)
        this.availableGames = new ArrayList<>();
        this.availableGames.add(new QuizGame()); // Ajout du QuizGame
        this.activeGame = null;
    }

    // Send a message to the Chat
    public void sendMessage(User sender, String content) {
        if (sender == null || content == null || content.isEmpty()) {
            System.out.println("Invalid message details.");
            return;
        }

        // Get the current timestamp
        String timestamp = new SimpleDateFormat("E MMM dd HH:mm:ss z yyyy").format(new Date());

        // Create the message and add it to the list
        Message newMessage = new Message(sender, content, timestamp);
        messages.add(newMessage);
    }

    // List all messages in the Chat
    public void listMessages() {
        System.out.println("\n--- Messages in " + this.name + " ---");
        // Affichage de l'état du jeu
        if (activeGame != null) {
            System.out.println("🚨 **MINI-JEU ACTIF: " + activeGame.getCommandName().toUpperCase() + " en cours!** Utilisez la commande '!" + activeGame.getCommandName() + " exit' pour l'arrêter.");
        } else {
            System.out.print("💡Jeux dispos: ");
            for (MiniGame game : availableGames) {
                System.out.print(game.getCommandName() + " ");
            }
            System.out.println("\n------------------------------------");
        }
        
        if (messages.isEmpty()) {
            System.out.println("No messages yet.");
        } else {
            for (Message message : messages) {
                String senderName = message.getSender().getName();
                
                // Truncate the timestamp for a shorter, more readable ID (hash code snippet)
                String messageIdSnippet = String.valueOf(message.getTimestamp().hashCode() & 0xFFFF); 
                
                String displaySender;
                String senderIcon;

                if (senderName.equals("RB")) {
                    displaySender = "🤖 RB";
                    senderIcon = ""; // Already handled by the name
                } else if (senderName.equals("RB")) {
                    displaySender = "🎮 RB";
                    senderIcon = ""; 
                } else {
                    displaySender = message.getSender().getName();
                    // On ne met pas d'icône pour l'utilisateur courant, c'est mieux dans le contexte de la console.
                    senderIcon = ""; 
                }

                String replyIndicator = "";
                if (message.getReplyTo() != null) {
                   
                    replyIndicator = " \n    [REPLY to: " + message.getReplyTo().getSender().getName() + ": '" + message.getReplyContentSnippet() + "']";
                }

                // Display the message with the short ID, sender, content, and reply info
                System.out.println(String.format(" %s | ID: %s", message.getTimestamp(), messageIdSnippet));
                System.out.println(String.format(" %s %s: %s", senderIcon, displaySender, message.getContent()));
                System.out.println(replyIndicator);
                
                // Display likes and reports on a separate line
                System.out.println(String.format("   👍 %d | 🚩 %d", message.getLikes(), message.getReports()));
                System.out.println("------------------------------------");
            }
        }
        System.out.println("------------------------------------");
    }

    // Méthode pour lancer un mini-jeu
    public String launchGame(User launcher, String gameCommand) {
        if (activeGame != null) {
            return "⛔ Un mini-jeu (" + activeGame.getCommandName() + ") est déjà en cours. Utilisez '!" + activeGame.getCommandName() + " exit' pour l'arrêter.";
        }
        
        // Trouver le jeu correspondant à la commande
        for (MiniGame game : availableGames) {
            if (game.getCommandName().equalsIgnoreCase(gameCommand)) {
                // Seul un admin ou un modérateur peut lancer un jeu
                if (!launcher.isAdmin() && !launcher.isModerator()) {
                    return "⛔ Seuls les administrateurs et modérateurs peuvent lancer un mini-jeu.";
                }
                
                activeGame = game;
                return activeGame.start();
            }
        }
        
        return "❌ Mini-jeu '" + gameCommand + "' non trouvé.";
    }
    
    // Méthode pour traiter les entrées pendant un jeu
    public String processGameInput(User user, String input) {
        if (activeGame == null) {
            return null;
        }
        
        // Commande d'arrêt du jeu
        if (input.trim().equalsIgnoreCase("!" + activeGame.getCommandName() + " exit")) {
            String results = activeGame.getResults();
            activeGame.reset();
            activeGame = null;
            return "🛑 **Mini-jeu arrêté par l'utilisateur.** " + results;
        }

        // Laisser le jeu actif traiter l'entrée
        String gameResponse = activeGame.processInput(user, input);
        
        // Si le jeu est terminé après le traitement, réinitialiser
        if (activeGame != null && activeGame.isFinished()) {
            activeGame = null; // Le jeu a renvoyé les résultats dans sa réponse
        }
        
        return gameResponse;
    }
    
    // Getter pour l'état du jeu
    public MiniGame getActiveGame() {
        return activeGame;
    }

    // Uniformisation de la recherche par ID court (hash code)
    public Message findMessageById(String shortId) {
        if (shortId == null || shortId.isEmpty()) return null;
        // L'ID court est le hash code du timestamp tronqué
        for (Message message : messages) {
            String messageIdSnippet = String.valueOf(message.getTimestamp().hashCode() & 0xFFFF);
            if (messageIdSnippet.equals(shortId)) {
                return message;
            }
        }
        return null;
    }

    // Utilise la méthode findMessageById uniformisée pour la suppression
    public void deleteMessage(User remover, String messageShortId) {
        Message message = findMessageById(messageShortId);
        if (message == null) {
            System.out.println("❌ Message non trouvé avec l'ID court: " + messageShortId);
            return;
        }

        boolean isSender = message.getSender().equals(remover);
        boolean isAdminOrMod = remover.isAdmin() || remover.isModerator();

        if (isSender || isAdminOrMod) {
            messages.remove(message);
            System.out.println("✅ Message (ID court: " + messageShortId + ") de " + message.getSender().getName() + " supprimé par " + remover.getName() + ".");
        } else {
            System.out.println("⛔ Vous n'avez pas la permission de supprimer ce message.");
        }
    }

    // Cette méthode a été conservée pour la compatibilité, mais deleteMessage est préférée.
    public void removeMessage(Message message) {
        if (messages.contains(message)) {
            messages.remove(message);
            // Note: The success message is handled in Main.java, but we add a local confirmation here too.
            System.out.println("✅ Message removed from chat history.");
        } else {
            System.out.println("❌ Error: Message not found in chat history for removal.");
        }
    }

    // Getter for the name of the Chat
    public String getName() {
        return name;
    }

    // Getter for the admin
    public User getAdmin() {
        return admin;
    }

    // Getter for messages (to allow findMessageById to work)
    public List<Message> getMessages() {
        return messages;
    }
    
    //  Allow admin to add custom games (needed for the Game Menu feature in Main.java)
    public void registerGame(MiniGame game) {
        this.availableGames.add(game);
        System.out.println("✨ Nouveau jeu ajouté au " + this.name + " : " + game.getCommandName());
    }

}
