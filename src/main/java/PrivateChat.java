import java.util.ArrayList;
import java.util.List;

public class PrivateChat extends Chat {
    private int entryCost;
    private List<User> allowedMembers; 

    public PrivateChat(String name, User owner, int entryCost) {
        super(name, owner); // The creator becomes the 'admin' of this chat
        this.entryCost = entryCost;
        this.allowedMembers = new ArrayList<>();
        this.allowedMembers.add(owner); // Owner is automatically a member
    }

    // Attempt to add a member (Logic: Check points -> Pay -> Enter)
    public boolean addMember(User user) {
        if (allowedMembers.contains(user)) {
            System.out.println("⚠️ " + user.getName() + " is already in the chat.");
            return true;
        }

        
        if (user.equals(this.admin) || user.isAdmin() || user.isModerator()) {
            allowedMembers.add(user);
            System.out.println("👑 " + user.getName() + " added to " + this.name + " (VIP/Owner access).");
            return true;
        }

        // Regular users pay points
        if (user.getPoints() >= entryCost) {
            user.setPoints(user.getPoints() - entryCost); // Deduct points
            allowedMembers.add(user);
            System.out.println("✅ " + user.getName() + " added to " + this.name + ".");
            System.out.println("   💰 -" + entryCost + " points. (Remaining: " + user.getPoints() + ")");
            return true;
        } else {
            System.out.println("⛔ " + user.getName() + " cannot be added.");
            System.out.println("   ❌ Insufficient points (Cost: " + entryCost + " | Has: " + user.getPoints() + ")");
            return false;
        }
    }

    public void removeMember(User user) {
        if (allowedMembers.contains(user)) {
            allowedMembers.remove(user);
            System.out.println("👋 " + user.getName() + " removed from " + this.name);
        }
    }

    public boolean isAllowed(User user) {
        // Global Admins always allowed, otherwise must be in the list
        return user.isAdmin() || allowedMembers.contains(user);
    }
    
    public int getEntryCost() {
        return entryCost;
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