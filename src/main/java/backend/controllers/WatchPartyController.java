package backend.controllers;

import java.util.List;
import java.util.Map;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import backend.models.AutoType;
import backend.models.User;
import backend.models.WatchParty;
import backend.services.WatchPartyManager;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/watchparties")
@Tag(name = "WatchParty System", description = "Create and manage watchparties ")

public class WatchPartyController {

    private final WatchPartyManager manager;

    @Autowired
    public WatchPartyController(WatchPartyManager manager) {
        this.manager = manager;
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

        if (name == null || typeStr == null) return "Error: Missing name or type";

        // Create a fake admin user (since we have no login system yet)
        User admin = new User("AdminAPI", true);
        
        try {
            AutoType type = AutoType.valueOf(typeStr.toUpperCase());
            WatchParty wp = WatchParty.createAutoWatchParty(admin, name, type);
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
    public String deleteWatchParty(@PathVariable String name) {
        boolean removed = manager.removeWatchParty(name);
        if (removed) {
            return "🗑️ Deleted: ";
        }
        return "⚠️ Not found: ";
    }

    // Helpers
    private String createManualWatchParty(Map<String, String> payload, boolean isPublic) {
        String name = payload.get("name");
        if (name == null || name.isBlank()) return "❌ Missing name";

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
        manager.addWatchParty(wp);
        return (isPublic ? "✅ Public" : "✅ Private") + " watchparty created: " + name;
    }
}