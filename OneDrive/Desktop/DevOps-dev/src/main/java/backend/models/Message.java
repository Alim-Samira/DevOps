package backend.models;

import java.util.Date;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

@Entity
@Table(name = "messages")
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // link to Chat 
    @ManyToOne
    @JoinColumn(name = "chat_id")
    private Chat chat;

    // actual date stored in DB
    @Column(name = "timestamp")
    @CreationTimestamp
    private Date dbTimestamp;

    @ManyToOne
    @JoinColumn(name = "sender_id") //link object User to column sender_id
    private User sender;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    // @Transient = dtells to not touch to it in the DB.
    @Transient
    private String timestamp;

    @Column(name = "likes_count") // Mappe 'likes' Java to 'likes_count' SQL
    private int likes;

    @Column(name = "report_count") // Mappe 'reports' Java to 'report_count' SQL
    private int reports;

    @ManyToOne
    @JoinColumn(name = "reply_to_id") 
    private Message replyTo;

    // necessity for JPA
    public Message() {}

    public Message(User sender, String content, String timestamp) {
        this.sender = sender;
        this.content = content;
        this.timestamp = timestamp;
        this.likes = 0;
        this.reports = 0;
        this.replyTo = null;
    }
    
    public void setChat(Chat chat) {
        this.chat = chat;
    }

    public User getSender() {
        return sender;
    }

    public String getContent() {
        return content;
    }

    public String getTimestamp() {
        //if the timestamp is empty (at start) we can recreate it from the DB date
        if (timestamp == null && dbTimestamp != null) {
            return dbTimestamp.toString();
        }
        return timestamp;
    }

    public int getLikes() {
        return likes;
    }

    public void like() {
        likes++;
    }

    public int getReports() {
        return reports;
    }

    public void report() {
        reports++;
    }

    public Message getReplyTo() {
        return replyTo;
    }

    public void setReplyTo(Message message) {
        this.replyTo = message;
    }

    // Votre logique existante
    public String getMessageId() {
        return this.timestamp; 
    }

    public String getReplyContentSnippet() {
        if (this.replyTo == null) {
            return "";
        }
        String contenu = this.replyTo.getContent();
        return contenu.length() > 20 ? contenu.substring(0, 17) + "..." : contenu;
    }
    
    // Getter technique pour l'ID BDD (optionnel mais utile)
    public Long getId() {
        return id;
    }
}