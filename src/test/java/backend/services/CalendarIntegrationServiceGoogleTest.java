package backend.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import backend.models.Calendar;
import backend.models.CalendarConnectionRequest;
import backend.models.CalendarEvent;
import backend.models.WatchParty;

class CalendarIntegrationServiceGoogleTest {

    private HttpServer server;
    private CalendarIntegrationService service;
    private String calendarApiBaseUrl;
    private String eventsResponseBody;
    private String createResponseBody;

    @BeforeEach
    void setUp() throws IOException {
        eventsResponseBody = """
                {
                  "items": []
                }
                """;
        createResponseBody = """
                {
                  "id": "google-event-1"
                }
                """;

        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/calendar/v3/calendars/primary/events", this::handleCalendarEvents);
        server.start();

        calendarApiBaseUrl = "http://localhost:" + server.getAddress().getPort() + "/calendar/v3";
        service = new CalendarIntegrationService(RestClient.builder().build(), calendarApiBaseUrl);
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void getEventsForCalendarShouldReturnGoogleEventsInRequestedRange() {
        eventsResponseBody = """
                {
                  "items": [
                    {
                      "summary": "Reunion equipe",
                      "description": "Point weekly",
                      "start": { "dateTime": "2026-04-12T18:30:00+02:00" },
                      "end": { "dateTime": "2026-04-12T19:15:00+02:00" }
                    }
                  ]
                }
                """;

        Calendar connection = connectGoogleCalendar("alice");

        List<CalendarEvent> events = service.getEventsForCalendar(
                "alice",
                connection.getId(),
                LocalDateTime.of(2026, 4, 12, 18, 0),
                LocalDateTime.of(2026, 4, 12, 20, 0));

        assertEquals(1, events.size());
        assertEquals("Reunion equipe", events.get(0).getTitle());
        assertEquals(LocalDateTime.of(2026, 4, 12, 18, 30), events.get(0).getStart());
    }

    @Test
    void checkAvailabilityShouldUseGoogleCalendarEvents() {
        eventsResponseBody = """
                {
                  "items": [
                    {
                      "summary": "Conflit",
                      "start": { "dateTime": "2026-04-13T20:30:00+02:00" },
                      "end": { "dateTime": "2026-04-13T21:00:00+02:00" }
                    }
                  ]
                }
                """;

        connectGoogleCalendar("bob");

        boolean available = service.checkAvailability(
                "bob",
                LocalDateTime.of(2026, 4, 13, 20, 0),
                LocalDateTime.of(2026, 4, 13, 22, 0));

        assertFalse(available);
    }

    @Test
    void acceptWatchPartyCalendarInviteShouldAddEventWhenGoogleCalendarIsFree() {
        connectGoogleCalendar("carol");
        WatchParty watchParty = new WatchParty("MSI Finals", LocalDateTime.of(2026, 4, 14, 20, 0), "LoL");

        Map<String, Object> createdEvent =
                service.acceptWatchPartyCalendarInvite("carol", watchParty, null);

        assertEquals("google-event-1", createdEvent.get("id"));
    }

    @Test
    void acceptWatchPartyCalendarInviteShouldRejectWhenGoogleCalendarHasConflict() {
        eventsResponseBody = """
                {
                  "items": [
                    {
                      "summary": "Dinner",
                      "start": { "dateTime": "2026-04-15T20:15:00+02:00" },
                      "end": { "dateTime": "2026-04-15T21:00:00+02:00" }
                    }
                  ]
                }
                """;

        connectGoogleCalendar("dave");
        WatchParty watchParty = new WatchParty("LCK Night", LocalDateTime.of(2026, 4, 15, 20, 0), "LoL");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.acceptWatchPartyCalendarInvite("dave", watchParty, null));

        assertTrue(exception.getMessage().contains("indisponible"));
    }

    @Test
    void sendWatchPartyInviteViaGoogleShouldCreateGoogleInvitationFromOrganizerCalendar() {
        CalendarConnectionRequest organizerRequest = new CalendarConnectionRequest();
        organizerRequest.setProvider("GOOGLE");
        organizerRequest.setOauthAccessToken("organizer-token");
        organizerRequest.setExternalCalendarId("primary");
        service.connect("organizer", organizerRequest);

        CalendarConnectionRequest attendeeRequest = new CalendarConnectionRequest();
        attendeeRequest.setProvider("GOOGLE");
        attendeeRequest.setExternalCalendarId("primary");
        attendeeRequest.setGoogleDeliveryMode("GOOGLE_INVITE");
        attendeeRequest.setInviteEmail("invitee@example.com");
        service.connect("invitee", attendeeRequest);

        WatchParty watchParty = new WatchParty("Google Invite Finals", LocalDateTime.of(2026, 4, 16, 20, 0), "LoL");

        boolean sent = service.sendWatchPartyInviteViaGoogle("organizer", "invitee", watchParty);

        assertTrue(sent);
    }

    @Test
    void connectGoogleInviteOnlyShouldWorkWithoutOauthToken() {
        CalendarConnectionRequest attendeeRequest = new CalendarConnectionRequest();
        attendeeRequest.setProvider("GOOGLE");
        attendeeRequest.setExternalCalendarId("primary");
        attendeeRequest.setGoogleDeliveryMode("GOOGLE_INVITE");
        attendeeRequest.setInviteEmail("invitee@example.com");

        Calendar connection = service.connect("invitee", attendeeRequest);

        assertEquals("GOOGLE", connection.getType().name());
    }

    private Calendar connectGoogleCalendar(String user) {
        CalendarConnectionRequest request = new CalendarConnectionRequest();
        request.setProvider("GOOGLE");
        request.setOauthAccessToken("test-token");
        request.setExternalCalendarId("primary");
        return service.connect(user, request);
    }

    private void handleCalendarEvents(HttpExchange exchange) throws IOException {
        if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            respond(exchange, eventsResponseBody);
            return;
        }
        if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            respond(exchange, createResponseBody);
            return;
        }

        exchange.sendResponseHeaders(405, -1);
        exchange.close();
    }

    private void respond(HttpExchange exchange, String body) throws IOException {
        byte[] bytes = body.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(bytes);
        }
    }
}
