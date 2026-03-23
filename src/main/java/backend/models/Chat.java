package backend.models;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

@Entity
@Table(name = "chats")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "dtype", discriminatorType = DiscriminatorType.STRING)
@DiscriminatorValue("Chat")
public class Chat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @OneToMany(mappedBy = "chat", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("dbTimestamp ASC")
    protected List<Message> messages = new ArrayList<>();

    @Transient
    protected User admin;

    @Transient
    private MiniGame activeGame;

    @Transient
    private final List<MiniGame> availableGames = new ArrayList<>();

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
        this.availableGames.add(new QuizGame());
    }

    public void sendMessage(User sender, String content) {
        if (sender == null || content == null || content.isEmpty()) return;
        String timestamp = new SimpleDateFormat("HH:mm").format(new Date());
        Message msg = new Message(sender, content, timestamp);
        msg.setChat(this);
        messages.add(msg);
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
            return "Jeu arrete par l'utilisateur.";
        }
        String response = activeGame.processInput(user, input);
        if (activeGame.isFinished()) activeGame = null;
        return response;
    }

    public MiniGame getActiveGame() { return activeGame; }

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

    public Long getId() { return id; }
    public String getName() { return name; }
    public List<Message> getMessages() { return messages; }
    public User getAdmin() { return admin; }
}
