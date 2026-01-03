package backend.controllers;

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

    // 3. DELETE (Remove)
    @DeleteMapping("/{name}")
    public String deleteWatchParty(@PathVariable String name) {
        boolean removed = manager.removeWatchParty(name);
        if (removed) {
            return "🗑️ Deleted: " + sanitize(name);
        }
        return "⚠️ Not found: " + sanitize(name);
    }

    private String sanitize(String input) {
        if (input == null) {
            return "";
        }
        return input.replaceAll("[<>\"'&]", "");
    }
}