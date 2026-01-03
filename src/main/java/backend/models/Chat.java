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
            return;
        }

        String timestamp = new SimpleDateFormat("E MMM dd HH:mm:ss z yyyy").format(new Date());
        Message newMessage = new Message(sender, content, timestamp);
        messages.add(newMessage);
    }

    public void listMessages() {
        // Messages listing delegated to UI layer
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
        
        if (activeGame.isFinished()) {
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
            return;
        }

        boolean isSender = message.getSender().equals(remover);
        boolean isAdminOrMod = remover.isAdmin() || remover.isModerator();

        if (isSender || isAdminOrMod) {
            messages.remove(message);
        }
    }

    public void removeMessage(Message message) {
        messages.remove(message);
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
    }
    
    public List<MiniGame> getAvailableGames() {
        return availableGames;
    }
}