package backend.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import backend.models.User;
import backend.models.WatchParty;
import backend.services.CalendarIntegrationService;
import backend.services.RankingService;
import backend.services.UserService;
import backend.services.WatchPartyManager;

class WatchPartyControllerTest {

    private WatchPartyManager manager;
    private UserService userService;
    private WatchPartyController controller;
    private User alice;
    private CalendarIntegrationService calendarIntegrationService;

    @BeforeEach
    void setUp() {
        manager = new WatchPartyManager();
        userService = new UserService();
        calendarIntegrationService = new CalendarIntegrationService();
        controller = new WatchPartyController(manager, userService, new RankingService(userService, manager), calendarIntegrationService);
        alice = userService.getUser("alice");
    }

    @Test
    void getWatchPartyChatShouldReturnFlatSerializableMessages() {
        WatchParty wp = new WatchParty("Chat WP", LocalDateTime.now().plusDays(1), "LoL");
        wp.setCreator(alice);
        wp.getChat().sendMessage(alice, "Salut le chat");
        manager.addWatchParty(wp);

        List<WatchPartyController.ChatMessageResponse> messages = controller.getWatchPartyChat(wp.getName());

        assertEquals(1, messages.size());
        WatchPartyController.ChatMessageResponse message = messages.get(0);
        assertEquals("alice", message.senderName());
        assertEquals("Salut le chat", message.content());
        assertFalse(message.timestamp().isBlank());
    }

    @Test
    void createPublicWatchPartyShouldAlsoAddEventToGoogleCalendarWhenRequested() {
        TrackingCalendarIntegrationService trackingCalendarIntegrationService = new TrackingCalendarIntegrationService();
        controller = new WatchPartyController(
                manager,
                userService,
                new RankingService(userService, manager),
                trackingCalendarIntegrationService);

        String response = controller.createPublicWatchParty(Map.of(
                "name", "Calendar WP",
                "game", "LoL",
                "date", "2026-04-02T20:00:00",
                "user", "alice",
                "addToCalendar", "true"));

        assertTrue(response.contains("watchparty created"));
        assertEquals("alice", trackingCalendarIntegrationService.lastUser);
        assertEquals("Calendar WP", trackingCalendarIntegrationService.lastWatchPartyName);
        assertEquals(1, trackingCalendarIntegrationService.callCount);
    }

    @Test
    void createPrivateWatchPartyShouldAlsoAddEventToGoogleCalendarWhenRequested() {
        TrackingCalendarIntegrationService trackingCalendarIntegrationService = new TrackingCalendarIntegrationService();
        controller = new WatchPartyController(
                manager,
                userService,
                new RankingService(userService, manager),
                trackingCalendarIntegrationService);

        String response = controller.createPrivateWatchParty(Map.of(
                "name", "Private Calendar WP",
                "game", "LoL",
                "date", "2026-04-03T21:00:00",
                "user", "alice",
                "addToCalendar", "true",
                "calendarConnectionId", "google-conn-1"));

        assertTrue(response.contains("watchparty created"));
        assertEquals("alice", trackingCalendarIntegrationService.lastUser);
        assertEquals("Private Calendar WP", trackingCalendarIntegrationService.lastWatchPartyName);
        assertEquals("google-conn-1", trackingCalendarIntegrationService.lastConnectionId);
        assertEquals(1, trackingCalendarIntegrationService.callCount);
    }

    @Test
    void createWatchPartyShouldNotAddEventWhenCalendarOptionIsDisabled() {
        TrackingCalendarIntegrationService trackingCalendarIntegrationService = new TrackingCalendarIntegrationService();
        controller = new WatchPartyController(
                manager,
                userService,
                new RankingService(userService, manager),
                trackingCalendarIntegrationService);

        String response = controller.createPublicWatchParty(Map.of(
                "name", "No Calendar WP",
                "game", "LoL",
                "date", "2026-04-04T20:00:00",
                "user", "alice",
                "addToCalendar", "false"));

        assertTrue(response.contains("watchparty created"));
        assertEquals(0, trackingCalendarIntegrationService.callCount);
    }

    @Test
    void createWatchPartyShouldRejectCalendarAdditionWithoutCreator() {
        TrackingCalendarIntegrationService trackingCalendarIntegrationService = new TrackingCalendarIntegrationService();
        controller = new WatchPartyController(
                manager,
                userService,
                new RankingService(userService, manager),
                trackingCalendarIntegrationService);

        String response = controller.createPublicWatchParty(Map.of(
                "name", "Missing Creator WP",
                "game", "LoL",
                "date", "2026-04-05T20:00:00",
                "addToCalendar", "true"));

        assertTrue(response.contains("Impossible d'ajouter au calendrier sans utilisateur createur"));
        assertEquals(0, trackingCalendarIntegrationService.callCount);
    }

    @Test
    void createWatchPartyShouldReturnCalendarServiceError() {
        FailingCalendarIntegrationService failingCalendarIntegrationService = new FailingCalendarIntegrationService();
        controller = new WatchPartyController(
                manager,
                userService,
                new RankingService(userService, manager),
                failingCalendarIntegrationService);

        String response = controller.createPublicWatchParty(Map.of(
                "name", "Failing Calendar WP",
                "game", "LoL",
                "date", "2026-04-06T20:00:00",
                "user", "alice",
                "addToCalendar", "true"));

        assertTrue(response.contains("Impossible d'ajouter au Google Calendar"));
    }

    @Test
    void addWatchPartyToCalendarShouldReturnCreatedEvent() {
        TrackingCalendarIntegrationService trackingCalendarIntegrationService = new TrackingCalendarIntegrationService();
        controller = new WatchPartyController(
                manager,
                userService,
                new RankingService(userService, manager),
                trackingCalendarIntegrationService);

        WatchParty wp = new WatchParty("Calendar Endpoint WP", LocalDateTime.of(2026, 4, 7, 20, 0), "LoL");
        wp.setCreator(alice);
        manager.addWatchParty(wp);

        Map<String, Object> response = controller.addWatchPartyToCalendar(wp.getName(), Map.of(
                "user", "alice",
                "connectionId", "google-conn-2"));

        assertEquals(true, response.get("success"));
        assertNotNull(response.get("event"));
        assertEquals("alice", trackingCalendarIntegrationService.lastUser);
        assertEquals("Calendar Endpoint WP", trackingCalendarIntegrationService.lastWatchPartyName);
        assertEquals("google-conn-2", trackingCalendarIntegrationService.lastConnectionId);
    }

    @Test
    void addWatchPartyToCalendarShouldReturnErrorWhenWatchPartyDoesNotExist() {
        Map<String, Object> response = controller.addWatchPartyToCalendar("Unknown WP", Map.of(
                "user", "alice",
                "connectionId", "google-conn-2"));

        assertEquals(false, response.get("success"));
        assertTrue(String.valueOf(response.get("error")).contains("Watch party introuvable"));
    }

    @Test
    void acceptWatchPartyCalendarInviteShouldReturnCreatedEvent() {
        TrackingCalendarIntegrationService trackingCalendarIntegrationService = new TrackingCalendarIntegrationService();
        controller = new WatchPartyController(
                manager,
                userService,
                new RankingService(userService, manager),
                trackingCalendarIntegrationService);

        WatchParty wp = new WatchParty("Accept Calendar WP", LocalDateTime.of(2026, 4, 10, 20, 0), "LoL");
        wp.setCreator(alice);
        manager.addWatchParty(wp);

        Map<String, Object> response = controller.acceptWatchPartyCalendarInvite(wp.getName(), Map.of(
                "user", "alice",
                "connectionId", "google-conn-3"));

        assertEquals(true, response.get("success"));
        assertNotNull(response.get("event"));
        assertEquals("alice", trackingCalendarIntegrationService.lastAcceptedUser);
        assertEquals("Accept Calendar WP", trackingCalendarIntegrationService.lastAcceptedWatchPartyName);
        assertEquals("google-conn-3", trackingCalendarIntegrationService.lastAcceptedConnectionId);
        assertEquals(1, trackingCalendarIntegrationService.acceptCallCount);
    }

    @Test
    void acceptWatchPartyCalendarInviteShouldReturnErrorWhenWatchPartyDoesNotExist() {
        Map<String, Object> response = controller.acceptWatchPartyCalendarInvite("Unknown Accept WP", Map.of(
                "user", "alice",
                "connectionId", "google-conn-3"));

        assertEquals(false, response.get("success"));
        assertTrue(String.valueOf(response.get("error")).contains("Watch party introuvable"));
    }

    private static class TrackingCalendarIntegrationService extends CalendarIntegrationService {
        private String lastUser;
        private String lastWatchPartyName;
        private String lastConnectionId;
        private int callCount;
        private String lastAcceptedUser;
        private String lastAcceptedWatchPartyName;
        private String lastAcceptedConnectionId;
        private int acceptCallCount;

        @Override
        public Map<String, Object> addWatchPartyToGoogleCalendar(String user, WatchParty watchParty, String connectionId) {
            this.lastUser = user;
            this.lastWatchPartyName = watchParty.getName();
            this.lastConnectionId = connectionId;
            this.callCount++;
            return Map.of("id", "event-123");
        }

        @Override
        public Map<String, Object> acceptWatchPartyCalendarInvite(String user, WatchParty watchParty, String connectionId) {
            this.lastAcceptedUser = user;
            this.lastAcceptedWatchPartyName = watchParty.getName();
            this.lastAcceptedConnectionId = connectionId;
            this.acceptCallCount++;
            return Map.of("id", "accepted-event-123");
        }
    }

    private static class FailingCalendarIntegrationService extends CalendarIntegrationService {
        @Override
        public Map<String, Object> addWatchPartyToGoogleCalendar(String user, WatchParty watchParty, String connectionId) {
            throw new IllegalArgumentException("token Google invalide");
        }
    }
}
