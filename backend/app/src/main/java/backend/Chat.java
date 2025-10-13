import java.util.ArrayList;
import java.util.List;
import java.text.SimpleDateFormat;
import java.util.Date;

public abstract class Chat {
    protected String name;
    protected List<Message> messages;
    protected User admin;

    public Chat(String name, User admin) {
        this.name = name;
        this.admin = admin;
        this.messages = new ArrayList<>();
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
        if (messages.isEmpty()) {
            System.out.println("No messages yet.");
        } else {
            for (Message message : messages) {
                // Display the message with timestamp
                String senderName = message.getSender().getName();
                String timestamp = message.getTimestamp();
                
                if (senderName.equals("RB")) {
                    // Display RB's message as the robot (🤖)
                    System.out.println("[" + timestamp + "] 🤖: " + message.getContent());
                } else {
                    // Display user messages with "{You}"
                    System.out.println("[" + timestamp + "] {You}: " + message.getContent());
                }
            }
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
}
