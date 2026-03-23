package backend;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import backend.models.User;
import backend.models.UserNotification;
import backend.models.WatchParty;
import backend.repositories.WatchPartyRepository;
import backend.services.CalendarIntegrationService;
import backend.services.NotificationService;
import backend.services.UserService;
import backend.services.WatchPartyManager;

class WatchPartyManagerNotificationTest {

    @Test
    @DisplayName("Planifying a watch party notifies available participants")
    void testPlanifyWatchPartyNotifiesAvailableParticipants() {
        WatchPartyRepository watchPartyRepository = mock(WatchPartyRepository.class);
        CalendarIntegrationService calendarIntegrationService = mock(CalendarIntegrationService.class);
        UserService userService = mock(UserService.class);
        NotificationService notificationService = mock(NotificationService.class);

        WatchPartyManager manager = new WatchPartyManager(watchPartyRepository, calendarIntegrationService, userService, notificationService);

        User alice = new User("alice", false);
        User bob = new User("bob", false);

        // Setup calendar availability
        when(calendarIntegrationService.hasConnectedCalendar("alice")).thenReturn(true);
        when(calendarIntegrationService.hasConnectedCalendar("bob")).thenReturn(true);
        when(calendarIntegrationService.canAttendWatchParty(eq("alice"), any(), any())).thenReturn(true);
        when(calendarIntegrationService.canAttendWatchParty(eq("bob"), any(), any())).thenReturn(false);

        // Create watch party with alice and bob as participants
        WatchParty wp = new WatchParty("IRL Finals", LocalDateTime.now().plusDays(1), "LoL");
        wp.join(alice);
        wp.join(bob);

        manager.planifyWatchParty(wp);

        // Verify: alice (available) gets notified, bob (unavailable) doesn't
        ArgumentCaptor<UserNotification> captor = ArgumentCaptor.forClass(UserNotification.class);
        verify(notificationService).addNotification(eq("alice"), captor.capture());
        verify(notificationService, never()).addNotification(eq("bob"), any());
        assertEquals("IRL Finals", captor.getValue().getWatchPartyName());
    }
}
