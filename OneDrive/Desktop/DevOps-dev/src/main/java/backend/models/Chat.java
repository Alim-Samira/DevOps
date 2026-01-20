package backend.models;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "chats") // links this class to the 'chats' table in the DB
@Inheritance(strategy = InheritanceType.SINGLE_TABLE) // manage all subclasses in one table ie Public/Private
@DiscriminatorColumn(name = "type", discriminatorType = DiscriminatorType.STRING) //to distinguish between chat types
public abstract class Chat {

    // BDD Fields
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    protected Long id;

    @Column(name = "created_at")
    @CreationTimestamp
    protected Date createdAt;
    
    @Column(name = "name")
    protected String name;

    //Un Chat a un Admin (User)
    @ManyToOne
    @JoinColumn(name = "admin_id")
    protected User admin;

    // 'mappedBy = "chat"' signifie que c'est la classe Message qui porte la clé étrangère
    @OneToMany(mappedBy = "chat", cascade = CascadeType.ALL, orphanRemoval = true)
    protected List<Message> messages;

    // @Transient = basically keeps memory for API but don't put it inside SQL base 
    @Transient
    private MiniGame activeGame;
    
    @Transient
    private final List<MiniGame> availableGames;

    // empty constructor so JPA reads th BDD correctly
    public Chat() {
        this.messages = new ArrayList<>();
        this.availableGames = new ArrayList<>();
        this.availableGames.add(new QuizGame());
    }

    protected Chat(String name, User admin) {
        this(); // Appelle le constructeur vide pour initier les listes
        this.name = name;
        this.admin = admin;
        this.activeGame = null;
    }

    public void sendMessage(User sender, String content) {
        if (sender == null || content == null || content.isEmpty()) {
            return;
        }

        String timestamp = new SimpleDateFormat("E MMM dd HH:mm:ss z yyyy").format(new Date());
        Message newMessage = new Message(sender, content, timestamp);
        
        // tells to a message to which chat it belongs for the BDD
        newMessage.setChat(this); 
        
        messages.add(newMessage);
    }

    // Toutes vos autres méthodes restent exactement pareilles
    
    public void listMessages() {
        // Messages listing delegated to UI layer
    }

    public String launchGame(User launcher, String gameCommand) {
        if (activeGame != null) {
            return "[X] Un mini-jeu (" + activeGame.getCommandName() + ") est deja en cours.";
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
        if (activeGame == null) return null;
        
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
    
    public MiniGame getActiveGame() { return activeGame; }

    public Message findMessageById(String shortId) {
        if (shortId == null || shortId.isEmpty()) return null;
        for (Message message : messages) {
            String messageIdSnippet = String.valueOf(message.getTimestamp().hashCode() & 0xFFFF);
            if (messageIdSnippet.equals(shortId)) return message;
        }
        return null;
    }

    public void deleteMessage(User remover, String messageShortId) {
        Message message = findMessageById(messageShortId);
        if (message == null) return;

        boolean isSender = message.getSender().equals(remover);
        boolean isAdminOrMod = remover.isAdmin() || remover.isModerator();

        if (isSender || isAdminOrMod) {
            messages.remove(message);
            message.setChat(null);
        }
    }

    public void removeMessage(Message message) {
        messages.remove(message);
    }

    // Getters standards
    public String getName() { return name; }
    public User getAdmin() { return admin; }
    public List<Message> getMessages() { return messages; }
    public void registerGame(MiniGame game) { this.availableGames.add(game); }
    public List<MiniGame> getAvailableGames() { return availableGames; }

    // Getter for the ID BDD (optional)
    public Long getId() { return id; }
}