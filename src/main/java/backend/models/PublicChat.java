package backend.models;
public class PublicChat extends Chat {
    public PublicChat(String name, User admin) {
        super(name, admin);  // Pass both name and admin to the superclass constructor
    }
}