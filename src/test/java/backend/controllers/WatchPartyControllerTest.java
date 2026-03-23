package backend.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import backend.models.User;
import backend.models.WatchParty;
import backend.services.RankingService;
import backend.services.UserService;
import backend.services.WatchPartyManager;

class WatchPartyControllerTest {

    private WatchPartyManager manager;
    private UserService userService;
    private WatchPartyController controller;
    private User alice;

    @BeforeEach
    void setUp() {
        manager = new WatchPartyManager();
        userService = new UserService();
        controller = new WatchPartyController(manager, userService, new RankingService(userService, manager));
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
}
