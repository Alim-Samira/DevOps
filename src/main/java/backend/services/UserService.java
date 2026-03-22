package backend.services;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import backend.models.User;
import backend.repositories.UserRepository;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User getUser(String username) {
        if (userRepository == null) {
            return new User(username, false);
        }
        return userRepository.findByName(username)
            .orElseGet(() -> userRepository.save(new User(username, false)));
    }

    public List<User> getAllUsers() {
        if (userRepository == null) {
            return new ArrayList<>();
        }
        return userRepository.findAll();
    }
}
