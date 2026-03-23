package backend.controllers;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import backend.models.User;
import backend.models.UserCreateRequest;
import backend.models.UserNotification;
import backend.services.NotificationService;
import backend.services.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/users")
@Tag(name = "Users", description = "Manage users and view points")
public class UserController {

    private final UserService userService;
    private final NotificationService notificationService;
    private final backend.services.RankingService rankingService;

    public UserController(UserService userService, NotificationService notificationService, backend.services.RankingService rankingService) {
        this.userService = userService;
        this.notificationService = notificationService;
        this.rankingService = rankingService;
    }

    @Operation(summary = "Get all users", description = "Returns list of users")
    @GetMapping
    public List<User> getAllUsers() {
        return userService.getAllUsers();
    }

    @Operation(summary = "Get specific user")
    @GetMapping("/{username}")
    public java.util.Map<String, Object> getUser(@PathVariable("username") String username) {
        User user = userService.getUser(username);
        java.util.Map<String, Object> body = new java.util.HashMap<>();
        body.put("name", user.getName());
        body.put("admin", user.isAdmin());
        body.put("moderator", user.isModerator());
        body.put("publicPoints", user.getPublicPoints());
        body.put("pointsByWatchParty", user.getPointsByWatchParty());
        body.put("publicWins", user.getPublicWins());
        body.put("winsByWatchParty", user.getWinsByWatchParty());

        // computed global points = rankingService sum for public WPs
        java.util.Map<String, Integer> globalRanking = rankingService.getGlobalPublicPoints(false);
        body.put("globalPoints", globalRanking.getOrDefault(username, 0));

        return body;
    }

    @Operation(summary = "Register/Reset User", description = "Resets a user to 200 points or creates them.")
    @PostMapping
    public User createUser(@RequestBody UserCreateRequest payload) {
        String username = payload.getName();
        if(username == null) return null;
        
        // Since your UserService.getUser() creates one if missing, 
        // we can just call that.
        return userService.getUser(username);
    }

    @Operation(summary = "Get user notifications")
    @GetMapping("/{username}/notifications")
    public List<UserNotification> getNotifications(@PathVariable("username") String username) {
        return notificationService.getNotifications(username);
    }
}