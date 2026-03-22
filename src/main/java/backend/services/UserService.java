package backend.services;

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
        return userRepository.findByName(username)
            .orElseGet(() -> userRepository.save(new User(username, false)));
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
}
