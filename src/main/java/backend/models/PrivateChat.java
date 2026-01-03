package backend.models;
import java.util.Map;

public class PrivateChat extends Chat {
    private Map<User,Integer> users;

    public PrivateChat(String name, User admin) {
        super(name, admin);  // Pass both name and admin to the superclass constructor
        this.users = new java.util.HashMap<>();
    }

    // Method to add a user to the private chat
    public void addUser(User user) {
        users.put(user,200);
        System.out.println(user.getName() + " has joined the private chat.");
    }
    public void setPoints(User user, Integer points){
        if(users.containsKey(user)){
            users.replace(user, points);
        }
    }
    public Map<User,Integer> users(){
        return this.users;
    }
}