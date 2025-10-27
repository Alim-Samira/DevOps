import java.util.Date;

public class Message {
    private User sender;
    private String content;
    private String timestamp;

    public Message(User sender, String content, String timestamp) {
        this.sender = sender;
        this.content = content;
        this.timestamp = timestamp;
    }

    public User getSender() {
        return sender;
    }

    public String getContent() {
        return content;
    }

    public String getTimestamp() {
        return timestamp;
    }
}