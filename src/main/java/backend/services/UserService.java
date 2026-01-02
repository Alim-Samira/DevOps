package backend.services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import backend.models.User;

@Service
public class UserService {
    private Map<String, User> users = new HashMap<>();

    public UserService() {
        // Create a default admin
        users.put("admin", new User("admin", true));
        // Create a default user
        users.put("alice", new User("alice", false));
    }

    // Get or Create a user
    public User getUser(String username) {
        String key = username.toLowerCase();
        return users.computeIfAbsent(key, k -> new User(username, false));
    }

    public List<User> getAllUsers() {
        return new ArrayList<>(users.values());
    }
}