package backend.models;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public abstract class Chat {
    protected String name;
    protected List<Message> messages;
    protected User admin;
    private MiniGame activeGame;
    private final List<MiniGame> availableGames;

    protected Chat(String name, User admin) {
        this.name = name;
        this.admin = admin;
        this.messages = new ArrayList<>();
        //  Initialisation des jeux disponibles 
        this.availableGames = new ArrayList<>();
        this.availableGames.add(new QuizGame()); 
        this.activeGame = null;
    }

    // Send a message to the Chat
    public void sendMessage(User sender, String content) {
        if (sender == null || content == null || content.isEmpty()) {
            System.out.println("Invalid message details.");
            return;
        }

        String timestamp = new SimpleDateFormat("E MMM dd HH:mm:ss z yyyy").format(new Date());
        Message newMessage = new Message(sender, content, timestamp);
        messages.add(newMessage);
    }

    public void listMessages() {
        System.out.println("\n--- Messages in " + this.name + " ---");
        
        if (activeGame != null) {
            System.out.println("[GAME] **MINI-JEU ACTIF: " + activeGame.getCommandName().toUpperCase() + " en cours!** Utilisez la commande '!" + activeGame.getCommandName() + " exit' pour l'arreter.");
        } else {
            System.out.print("[INFO] Jeux dispos: ");
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
                String messageIdSnippet = String.valueOf(message.getTimestamp().hashCode() & 0xFFFF); 
                String displaySender;
                String senderIcon;

                if (senderName.equals("RB")) {
                    displaySender = "[BOT] RB";
                    senderIcon = ""; 
                } else if (senderName.equals("RB")) {
                    displaySender = "[GAME] RB";
                    senderIcon = ""; 
                } else {
                    displaySender = message.getSender().getName();
                    senderIcon = ""; 
                }

                String replyIndicator = "";
                if (message.getReplyTo() != null) {
                    replyIndicator = " \n    [REPLY to: " + message.getReplyTo().getSender().getName() + ": '" + message.getReplyContentSnippet() + "']";
                }

                System.out.println(String.format(" %s | ID: %s", message.getTimestamp(), messageIdSnippet));
                System.out.println(String.format(" %s %s: %s", senderIcon, displaySender, message.getContent()));
                System.out.println(replyIndicator);
                System.out.println(String.format("   [LIKES] %d | [REPORTS] %d", message.getLikes(), message.getReports()));
                System.out.println("------------------------------------");
            }
        }
        System.out.println("------------------------------------");
    }

    public String launchGame(User launcher, String gameCommand) {
        if (activeGame != null) {
            return "[X] Un mini-jeu (" + activeGame.getCommandName() + ") est deja en cours. Utilisez '!" + activeGame.getCommandName() + " exit' pour l'arreter.";
        }
        
        for (MiniGame game : availableGames) {
            if (game.getCommandName().equalsIgnoreCase(gameCommand)) {
                if (!launcher.isAdmin() && !launcher.isModerator()) {
                    return "[X] Seuls les administrateurs et moderateurs peuvent lancer un mini-jeu.";
                }
                activeGame = game;
                return activeGame.start();
            }
        }
        return "[X] Mini-jeu '" + gameCommand + "' non trouve.";
    }
    
    public String processGameInput(User user, String input) {
        if (activeGame == null) {
            return null;
        }
        
        if (input.trim().equalsIgnoreCase("!" + activeGame.getCommandName() + " exit")) {
            String results = activeGame.getResults();
            activeGame.reset();
            activeGame = null;
            return "🛑 **Mini-jeu arrêté par l'utilisateur.** " + results;
        }

        String gameResponse = activeGame.processInput(user, input);
        
        if (activeGame != null && activeGame.isFinished()) {
            activeGame = null; 
        }
        return gameResponse;
    }
    
    public MiniGame getActiveGame() {
        return activeGame;
    }

    public Message findMessageById(String shortId) {
        if (shortId == null || shortId.isEmpty()) return null;
        for (Message message : messages) {
            String messageIdSnippet = String.valueOf(message.getTimestamp().hashCode() & 0xFFFF);
            if (messageIdSnippet.equals(shortId)) {
                return message;
            }
        }
        return null;
    }

    public void deleteMessage(User remover, String messageShortId) {
        Message message = findMessageById(messageShortId);
        if (message == null) {
            System.out.println("[X] Message non trouve avec l'ID court: " + messageShortId);
            return;
        }

        boolean isSender = message.getSender().equals(remover);
        boolean isAdminOrMod = remover.isAdmin() || remover.isModerator();

        if (isSender || isAdminOrMod) {
            messages.remove(message);
            System.out.println("[OK] Message (ID court: " + messageShortId + ") de " + message.getSender().getName() + " supprime par " + remover.getName() + ".");
        } else {
            System.out.println("[X] Vous n'avez pas la permission de supprimer ce message.");
        }
    }

    public void removeMessage(Message message) {
        if (messages.contains(message)) {
            messages.remove(message);
            System.out.println("[OK] Message removed from chat history.");
        } else {
            System.out.println("[X] Error: Message not found in chat history for removal.");
        }
    }

    public String getName() {
        return name;
    }

    public User getAdmin() {
        return admin;
    }

    public List<Message> getMessages() {
        return messages;
    }
    
    public void registerGame(MiniGame game) {
        this.availableGames.add(game);
        System.out.println("✨ Nouveau jeu ajouté au " + this.name + " : " + game.getCommandName());
    }
    
    public List<MiniGame> getAvailableGames() {
        return availableGames;
    }
}