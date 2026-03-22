package backend.services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import backend.models.User;
import backend.repositories.UserRepository;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final Map<String, User> inMemoryUsers;

    @Autowired
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
        this.inMemoryUsers = new HashMap<>();
        this.inMemoryUsers.put("admin", new User("admin", true));
    }

    public UserService() {
        this(null);
    }

    public User getUser(String username) {
        boolean shouldBeAdmin = "admin".equalsIgnoreCase(username);
        if (userRepository == null) {
            String key = username.toLowerCase();
            User user = inMemoryUsers.computeIfAbsent(key, ignored -> new User(username, shouldBeAdmin));
            if (shouldBeAdmin && !user.isAdmin()) {
                user.setAdmin(true);
            }
            return user;
        }
        return userRepository.findByName(username)
            .map(user -> {
                if (shouldBeAdmin && !user.isAdmin()) {
                    user.setAdmin(true);
                    return userRepository.save(user);
                }
                return user;
            })
            .orElseGet(() -> userRepository.save(new User(username, shouldBeAdmin)));
    }

    public List<User> getAllUsers() {
        if (userRepository == null) {
            return new ArrayList<>(inMemoryUsers.values());
        }
        return userRepository.findAll();
    }

    public User saveUser(User user) {
        if (user == null) {
            return null;
        }
        if (userRepository == null) {
            inMemoryUsers.put(user.getName().toLowerCase(), user);
            return user;
        }
        return userRepository.save(user);
    }
}
