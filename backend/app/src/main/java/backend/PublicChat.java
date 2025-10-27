import java.util.ArrayList;
import java.util.List;

public class PublicChat extends Chat {
    public PublicChat(String name, User admin) {
        super(name, admin);  // Pass both name and admin to the superclass constructor
    }

    // Method to add a user to the public chat
    public void addUser(User user) {
        System.out.println(user.getName() + " has joined the public chat.");
    }
}