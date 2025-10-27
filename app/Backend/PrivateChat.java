import java.util.ArrayList;
import java.util.List;

public class PrivateChat extends Chat {
    public PrivateChat(String name, User admin) {
        super(name, admin);  // Pass both name and admin to the superclass constructor
    }

    // Method to add a user to the private chat
    public void addUser(User user) {
        System.out.println(user.getName() + " has joined the private chat.");
    }
}