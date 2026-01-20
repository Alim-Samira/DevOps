package backend.services;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import backend.models.User;

@Service
public class RankingService {

    private final UserService userService;

    public RankingService(UserService userService) {
        this.userService = userService;
    }

    public Map<String, Integer> getRanking() {
        List<User> users = userService.getAllUsers();
        
        return users.stream()
                .sorted((u1, u2) -> Integer.compare(u2.getPoints(), u1.getPoints()))
                .collect(Collectors.toMap(
                    User::getName,
                    User::getPoints,
                    (e1, e2) -> e1,
                    LinkedHashMap::new
                ));
    }

    public List<Map<String, Object>> getRankingWithDetails() {
        List<User> users = userService.getAllUsers();
        
        return users.stream()
                .sorted((u1, u2) -> Integer.compare(u2.getPoints(), u1.getPoints()))
                .map(user -> {
                    Map<String, Object> entry = new LinkedHashMap<>();
                    entry.put("username", user.getName());
                    entry.put("points", user.getPoints());
                    entry.put("isAdmin", user.isAdmin());
                    entry.put("isModerator", user.isModerator());
                    return entry;
                })
                .toList();
    }
}