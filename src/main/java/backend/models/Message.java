package backend.models;


public class Message {
    private User sender;
    private String content;
    private String timestamp;
    private int likes;
    private int reports;
    private Message replyTo;

    public Message(User sender, String content, String timestamp) {
        this.sender = sender;
        this.content = content;
        this.timestamp = timestamp;
        this.likes = 0;
        this.reports = 0;
        this.replyTo = null;  // No reply initially
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

    // Method to get unique message ID
    public String getMessageId() {
        return this.timestamp; // Use full timestamp internally
    }

    // get a snippet of the message being replied to for display
    public String getReplyContentSnippet() {
        if (this.replyTo == null) {
            return "";
        }
        String contenu = this.replyTo.getContent();
        // Truncate the content to a max of 20 characters for a clean reply reference
        return contenu.length() > 20 ? contenu.substring(0, 17) + "..." : contenu;
    }
}
