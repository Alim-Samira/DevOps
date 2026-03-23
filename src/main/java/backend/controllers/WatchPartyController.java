package backend.controllers;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
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
import backend.services.CalendarIntegrationService;
import backend.services.RankingService;
import backend.services.UserService;
import backend.services.WatchPartyManager;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/watchparties")
@Tag(name = "WatchParty System", description = "Create and manage watchparties ")
public class WatchPartyController {

    private static final String KEY_SUCCESS = "success";
    private static final String KEY_ERROR = "error";
    private static final String KEY_USER = "user";
    private static final String KEY_NAME = "name";
    private static final String KEY_GAME = "game";
    private static final String KEY_DATE = "date";
    private static final String KEY_TEXT = "text";
    private static final String KEY_CONNECTION_ID = "connectionId";
    private static final String KEY_TYPE = "type";
    private static final String KEY_EVENT = "event";
    private static final String KEY_ADD_TO_CALENDAR = "addToCalendar";
    private static final String KEY_CALENDAR_CONNECTION_ID = "calendarConnectionId";
    private static final String DEFAULT_GAME = "League of Legends";

    private final WatchPartyManager manager;
    private final UserService userService;
    private final RankingService rankingService;
    private final CalendarIntegrationService calendarIntegrationService;

    @Autowired
    public WatchPartyController(
            WatchPartyManager manager,
            UserService userService,
            RankingService rankingService,
            CalendarIntegrationService calendarIntegrationService) {
        this.manager = manager;
        this.userService = userService;
        this.rankingService = rankingService;
        this.calendarIntegrationService = calendarIntegrationService;
    }

    @GetMapping
    public List<WatchParty> getAllWatchParties() {
        return manager.getAllWatchParties();
    }

    @PostMapping
    public String createWatchParty(@RequestBody Map<String, String> payload) {
        String name = payload.get(KEY_NAME);
        String typeStr = payload.get(KEY_TYPE);
        String userName = payload.get(KEY_USER);

        if (name == null || typeStr == null || userName == null) {
            return "Error: Missing name, type or user";
        }

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

    @PostMapping("/public")
    public String createPublicWatchParty(@RequestBody Map<String, String> payload) {
        return createManualWatchParty(payload, true);
    }

    @PostMapping("/private")
    public String createPrivateWatchParty(@RequestBody Map<String, String> payload) {
        return createManualWatchParty(payload, false);
    }

    @DeleteMapping(value = "/{name}", produces = "text/plain")
    public String deleteWatchParty(@PathVariable("name") String name) {
        boolean removed = manager.removeWatchParty(name);
        return removed ? "🗑️ Deleted: " : "⚠️ Not found: ";
    }

    @PostMapping("/{name}/join")
    public String joinWatchParty(@PathVariable("name") String name, @RequestBody Map<String, String> payload) {
        String userName = payload.get(KEY_USER);
        if (userName == null || userName.isBlank()) {
            return "❌ Missing user";
        }

        WatchParty wp = manager.getWatchPartyByName(name);
        if (wp == null) {
            return "❌ WatchParty introuvable: " + name;
        }

        User user = userService.getUser(userName);
        boolean joined = wp.join(user);
        if (joined) {
            userService.saveUser(user);
            manager.saveWatchParty(wp);
            if (wp.isPublic()) {
                rankingService.refreshAll();
            }
            if (wp.isPlanned()) {
                manager.notifyAvailableUsersForPresentiel(wp);
            }
        }

        return joined
                ? "✅ " + userName + " a rejoint " + name
                : "⚠️ " + userName + " est déjà participant";
    }

    @PostMapping("/{name}/leave")
    public String leaveWatchParty(@PathVariable("name") String name, @RequestBody Map<String, String> payload) {
        String userName = payload.get(KEY_USER);
        if (userName == null || userName.isBlank()) {
            return "❌ Missing user";
        }

        WatchParty wp = manager.getWatchPartyByName(name);
        if (wp == null) {
            return "❌ WatchParty introuvable: " + name;
        }

        boolean removed = wp.leave(userService.getUser(userName));
        if (removed) {
            manager.saveWatchParty(wp);
            if (wp.isPublic()) {
                rankingService.refreshAll();
            }
        }

        return removed
                ? "✅ " + userName + " a quitté " + name
                : "⚠️ " + userName + " n'est pas participant";
    }

    @GetMapping("/{name}/chat")
    @Transactional(readOnly = true)
    public List<ChatMessageResponse> getWatchPartyChat(@PathVariable("name") String name) {
        WatchParty wp = manager.getWatchPartyByName(name);
        if (wp == null) {
            return List.of();
        }

        return wp.getChat().getMessages().stream()
                .filter(Objects::nonNull)
                .map(message -> new ChatMessageResponse(
                        message.getId(),
                        message.getSender() != null ? message.getSender().getName() : "System",
                        message.getContent(),
                        message.getTimestamp()))
                .toList();
    }

    @PostMapping("/{name}/chat")
    @Transactional
    public String sendWatchPartyMessage(@PathVariable("name") String name, @RequestBody Map<String, String> payload) {
        String user = payload.get(KEY_USER);
        String text = payload.get(KEY_TEXT);

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
        if (!wp.getParticipants().contains(sender)) {
            return "❌ Vous devez être participant de la watch party pour envoyer des messages";
        }

        wp.getChat().sendMessage(sender, text);
        manager.saveWatchParty(wp);
        return "✅ Message sent";
    }

    @PostMapping("/{name}/calendar")
    public Map<String, Object> addWatchPartyToCalendar(
            @PathVariable("name") String name,
            @RequestBody Map<String, String> payload) {
        Map<String, Object> response = new HashMap<>();
        String user = payload.get(KEY_USER);
        String connectionId = payload.get(KEY_CONNECTION_ID);

        WatchParty wp = manager.getWatchPartyByName(name);
        if (wp == null) {
            response.put(KEY_SUCCESS, false);
            response.put(KEY_ERROR, "Watch party introuvable: " + name);
            return response;
        }

        try {
            Map<String, Object> createdEvent =
                    calendarIntegrationService.addWatchPartyToGoogleCalendar(user, wp, connectionId);
            response.put(KEY_SUCCESS, true);
            response.put(KEY_EVENT, createdEvent);
            return response;
        } catch (Exception ex) {
            response.put(KEY_SUCCESS, false);
            response.put(KEY_ERROR, ex.getMessage());
            return response;
        }
    }

    private String createManualWatchParty(Map<String, String> payload, boolean isPublic) {
        String name = payload.get(KEY_NAME);
        if (name == null || name.isBlank()) {
            return "❌ Missing name";
        }

        User creator = resolveCreator(payload.get(KEY_USER));
        String game = payload.getOrDefault(KEY_GAME, DEFAULT_GAME);
        LocalDateTime date = parseWatchPartyDate(payload.get(KEY_DATE));
        if (date == null) {
            return "❌ Invalid date format. Use ISO-8601 (e.g., 2026-01-17T20:00:00)";
        }

        WatchParty wp = buildWatchParty(name, game, date, isPublic, creator);
        String calendarError = addToCalendarIfRequested(payload, creator, wp);
        if (calendarError != null) {
            return calendarError;
        }

        manager.addWatchParty(wp);
        manager.planifyWatchParty(wp);
        if (isPublic) {
            rankingService.refreshAll();
        }

        return (isPublic ? "✅ Public" : "✅ Private") + " watchparty created: " + name;
    }

    private User resolveCreator(String userName) {
        if (userName == null || userName.isBlank()) {
            return null;
        }
        return userService.getUser(userName);
    }

    private LocalDateTime parseWatchPartyDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) {
            return LocalDateTime.now().plusDays(1);
        }
        try {
            return LocalDateTime.parse(dateStr);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private WatchParty buildWatchParty(
            String name, String game, LocalDateTime date, boolean isPublic, User creator) {
        WatchParty wp = new WatchParty(name, date, game);
        wp.setPublic(isPublic);
        if (creator != null) {
            wp.setCreator(creator);
            userService.saveUser(creator);
        }
        return wp;
    }

    private String addToCalendarIfRequested(Map<String, String> payload, User creator, WatchParty wp) {
        if (!Boolean.parseBoolean(payload.getOrDefault(KEY_ADD_TO_CALENDAR, "false"))) {
            return null;
        }
        if (creator == null) {
            return "❌ Impossible d'ajouter au calendrier sans utilisateur createur";
        }

        try {
            calendarIntegrationService.addWatchPartyToGoogleCalendar(
                    creator.getName(),
                    wp,
                    payload.get(KEY_CALENDAR_CONNECTION_ID));
            return null;
        } catch (IllegalArgumentException ex) {
            return "❌ Impossible d'ajouter au Google Calendar: " + ex.getMessage();
        }
    }

    public record ChatMessageResponse(Long id, String senderName, String content, String timestamp) {
    }
}
