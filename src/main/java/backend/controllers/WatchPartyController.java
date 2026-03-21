package backend.controllers;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import backend.models.AutoType;
import backend.models.Message;
import backend.models.User;
import backend.models.WatchParty;
import backend.services.RankingService;
import backend.services.UserService;
import backend.services.WatchPartyManager;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/watchparties")
@Tag(name = "WatchParty System", description = "Create and manage watchparties ")

public class WatchPartyController {

    private final WatchPartyManager manager;
    private final UserService userService;
    private final RankingService rankingService;

    @Autowired
    public WatchPartyController(WatchPartyManager manager, UserService userService, RankingService rankingService) {
        this.manager = manager;
        this.userService = userService;
        this.rankingService = rankingService;
    }

    // 1. GET (Read all)
    @GetMapping
    public List<WatchParty> getAllWatchParties() {
        return manager.getAllWatchParties();
    }

    // 2. POST (Create new)
    @PostMapping
    public String createWatchParty(@RequestBody Map<String, String> payload) {
        String name = payload.get("name");
        String typeStr = payload.get("type");
        String userName = payload.get("user");

        if (name == null || typeStr == null || userName == null) return "Error: Missing name, type or user";

        // Use the caller (payload.user) as the creator of the watchparty.
        // Do NOT promote the creator to a global admin here — WatchParty-scoped admin checks use `WatchParty.isAdmin(User)`.
        User creator = userService.getUser(userName);

        try {
            AutoType type = AutoType.valueOf(typeStr.toUpperCase());
            WatchParty wp = WatchParty.createAutoWatchParty(creator, name, type);
            manager.addAutoWatchParty(wp);
            return "✅ Created WatchParty: " + name;
        } catch (IllegalArgumentException e) {
            return "❌ Error: Invalid Type. Use 'TEAM' or 'TOURNAMENT'";
        }
    }

    // 2b. POST manual public watchparty
    @PostMapping("/public")
    public String createPublicWatchParty(@RequestBody Map<String, String> payload) {
        return createManualWatchParty(payload, true);
    }

    // 2c. POST manual private watchparty
    @PostMapping("/private")
    public String createPrivateWatchParty(@RequestBody Map<String, String> payload) {
        return createManualWatchParty(payload, false);
    }

    // 3. DELETE (Remove)
    @DeleteMapping(value = "/{name}", produces = "text/plain")
    public String deleteWatchParty(@PathVariable("name") String name) {
        boolean removed = manager.removeWatchParty(name);
        if (removed) {
            return "🗑️ Deleted: ";
        }
        return "⚠️ Not found: ";
    }

    // 4. JOIN a watchparty (user becomes participant and gets initial WP points)
    @PostMapping("/{name}/join")
    public String joinWatchParty(@PathVariable("name") String name, @RequestBody Map<String, String> payload) {
        String userName = payload.get("user");
        if (userName == null || userName.isBlank()) return "❌ Missing user";
        WatchParty wp = manager.getWatchPartyByName(name);
        if (wp == null) return "❌ WatchParty introuvable: " + name;
        boolean joined = wp.join(userService.getUser(userName));
        if (joined && wp.isPublic()) {
            // Refresh global ranking cache when user joins a public WP
            rankingService.refreshAll();
        }
        return joined ? "✅ " + userName + " a rejoint " + name : "⚠️ " + userName + " est déjà participant";
    }

    // 5. LEAVE a watchparty
    @PostMapping("/{name}/leave")
    public String leaveWatchParty(@PathVariable("name") String name, @RequestBody Map<String, String> payload) {
        String userName = payload.get("user");
        if (userName == null || userName.isBlank()) return "❌ Missing user";
        WatchParty wp = manager.getWatchPartyByName(name);
        if (wp == null) return "❌ WatchParty introuvable: " + name;
        boolean removed = wp.leave(userService.getUser(userName));
        return removed ? "✅ " + userName + " a quitté " + name : "⚠️ " + userName + " n'est pas participant";
    }

    // 6. CHAT for a watchparty
    @GetMapping("/{name}/chat")
    public List<Message> getWatchPartyChat(@PathVariable("name") String name) {
        WatchParty wp = manager.getWatchPartyByName(name);
        if (wp == null) return List.of();
        return wp.getChat().getMessages();
    }

    @PostMapping("/{name}/chat")
    public String sendWatchPartyMessage(@PathVariable("name") String name, @RequestBody Map<String, String> payload) {
        String user = payload.get("user");
        String text = payload.get("text");

        if (user == null || text == null) {
            return "❌ Missing 'user' or 'text'";
        }

        WatchParty wp = manager.getWatchPartyByName(name);
        if (wp == null) {
            return "❌ Watch party introuvable: " + name;
        }

        User sender = userService.getUser(user);
        if (sender == null) {
            return "❌ Utilisateur introuvable: " + user;
        }

        // Vérifier que l'utilisateur est participant de la WatchParty
        if (!wp.getParticipants().contains(sender)) {
            return "❌ Vous devez être participant de la watch party pour envoyer des messages";
        }

        wp.getChat().sendMessage(sender, text);
        return "✅ Message sent";
    }

    // Helpers
    private String createManualWatchParty(Map<String, String> payload, boolean isPublic) {
        String name = payload.get("name");
        if (name == null || name.isBlank()) return "❌ Missing name";

        // Optional creator supplied by caller (useful for manual creation from UI)
        String userName = payload.get("user");
        User creator = null;
        if (userName != null && !userName.isBlank()) {
            creator = userService.getUser(userName);
        }

        String game = payload.getOrDefault("game", "League of Legends");
        String dateStr = payload.get("date");
        LocalDateTime date = null;
        if (dateStr != null && !dateStr.isBlank()) {
            try {
                date = LocalDateTime.parse(dateStr);
            } catch (DateTimeParseException e) {
                return "❌ Invalid date format. Use ISO-8601 (e.g., 2026-01-17T20:00:00)";
            }
        } else {
            date = LocalDateTime.now().plusDays(1);
        }

        WatchParty wp = new WatchParty(name, date, game);
        wp.setPublic(isPublic);
        if (creator != null) wp.setCreator(creator);
        manager.addWatchParty(wp);
        manager.planifyWatchParty(wp);
        
        // Refresh global ranking cache when a public WP is created (especially with a creator)
        if (isPublic) {
            rankingService.refreshAll();
        }
        
        return (isPublic ? "✅ Public" : "✅ Private") + " watchparty created: " + name;
    }
}