package backend.models;

import jakarta.persistence.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "chats")
public class Chat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    // IN-MEMORY :so the messages do not pollute database
    @Transient
    protected List<Message> messages;

    @Transient
    protected User admin;

    @Transient
    private MiniGame activeGame;

    @Transient
    private final List<MiniGame> availableGames = new ArrayList<>();

    //constructors

    public Chat() {
        this.messages = new ArrayList<>();
        initGames();
    }

    public Chat(String name) {
        this.name = name;
        this.messages = new ArrayList<>();
        initGames();
    }

    public Chat(String name, User admin) {
        this.name = name;
        this.admin = admin;
        this.messages = new ArrayList<>();
        initGames();
    }

    private void initGames() {
        // Add games (verify quiz is mounted)
        this.availableGames.add(new QuizGame());
    }

    public void sendMessage(User sender, String content) {
        if (sender == null || content == null || content.isEmpty()) return;
        String timestamp = new SimpleDateFormat("HH:mm").format(new Date());
        messages.add(new Message(sender, content, timestamp));
    }

    public String launchGame(User launcher, String gameCommand) {
        if (activeGame != null) return "[X] Jeu en cours.";
        
        for (MiniGame game : availableGames) {
            if (game.getCommandName().equalsIgnoreCase(gameCommand)) {
                activeGame = game;
                return activeGame.start();
            }
        }
        return "[X] Jeu introuvable.";
    }

    public String processGameInput(User user, String input) {
        if (activeGame == null) return null;
        
        if (input.trim().equalsIgnoreCase("!" + activeGame.getCommandName() + " exit")) {
            activeGame.reset();
            activeGame = null;
            return " Jeu arrêté par l'utilisateur.";
        }
        
        String response = activeGame.processInput(user, input);
        if (activeGame.isFinished()) activeGame = null;
        return response;
    }

    public MiniGame getActiveGame() { return activeGame; }

    // Just some helpers tu find/delete a message (Logic In-Memory)
    public Message findMessageById(String shortId) {
        if (shortId == null) return null;
        for (Message m : messages) {
            if (String.valueOf(m.getTimestamp().hashCode() & 0xFFFF).equals(shortId)) return m;
        }
        return null;
    }

    public void deleteMessage(User remover, String messageShortId) {
        Message m = findMessageById(messageShortId);
        if (m != null) messages.remove(m);
    }
    
    public void removeMessage(Message message) { messages.remove(message); }

    // Getters DB
    public Long getId() { return id; }
    public String getName() { return name; }
    public List<Message> getMessages() { return messages; }
    public User getAdmin() { return admin; }
}