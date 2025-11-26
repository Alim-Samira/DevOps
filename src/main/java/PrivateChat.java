import java.util.ArrayList;
import java.util.List;

public class PrivateChat extends Chat {

    private List<User> allowedMembers; 

    // Standard Constructor
    public PrivateChat(String name, User owner) {
        super(name, owner); 
        this.allowedMembers = new ArrayList<>();
        this.allowedMembers.add(owner); // Owner is automatically a member
    }

    // Compatibility Constructor 
    public PrivateChat(String name, User owner, int ignoredCost) {
        this(name, owner);
    }

    // Attempt to add a member 
    public boolean addMember(User user) {
        if (allowedMembers.contains(user)) {
            System.out.println("⚠️ " + user.getName() + " is already in the chat.");
            return true;
        }

       
        allowedMembers.add(user);
        System.out.println("✅ " + user.getName() + " added to " + this.name + ".");
        return true;
    }

    public void removeMember(User user) {
        if (allowedMembers.contains(user)) {
            allowedMembers.remove(user);
            System.out.println("👋 " + user.getName() + " removed from " + this.name);
        }
    }

    public boolean isAllowed(User user) {
        // Global Admins and Moderators always allowed, others must be in allowedMembers TO BE MODIFIED
        return user.isAdmin() || user.isModerator() || allowedMembers.contains(user);
    }

    public int getEntryCost() {
        return 0;
    }
}

// public class PrivateChat extends Chat {
//     public PrivateChat(String name, User admin) {
//         super(name, admin);  // Pass both name and admin to the superclass constructor
//     }

//     // Method to add a user to the private chat
//     public void addUser(User user) {
//         System.out.println(user.getName() + " has joined the private chat.");
//     }
// }