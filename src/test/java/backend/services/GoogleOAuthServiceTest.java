package backend.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import backend.models.Calendar;
import backend.models.CalendarConnectionRequest;
import backend.models.GoogleCalendar;

class GoogleOAuthServiceTest {

    private HttpServer server;
    private GoogleOAuthService googleOAuthService;
    private CalendarIntegrationService calendarIntegrationService;
    private String tokenUrl;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/oauth/token", this::handleTokenExchange);
        server.start();

        tokenUrl = "http://localhost:" + server.getAddress().getPort() + "/oauth/token";
        calendarIntegrationService = new CalendarIntegrationService(RestClient.builder().build(), "http://localhost:1/calendar/v3");
        googleOAuthService = new GoogleOAuthService(
                calendarIntegrationService,
                RestClient.builder().build(),
                "client-id",
                "client-secret",
                "http://localhost:8080/api/oauth/google/callback",
                "https://www.googleapis.com/auth/calendar",
                "https://accounts.google.com/o/oauth2/v2/auth",
                tokenUrl);
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void beginAndCompleteAuthorizationShouldCreateGoogleCalendarConnection() {
        CalendarConnectionRequest request = new CalendarConnectionRequest();
        request.setProvider("GOOGLE");
        request.setExternalCalendarId("primary");
        request.setGoogleDeliveryMode("GOOGLE_INVITE");
        request.setInviteEmail("invitee@example.com");

        Map<String, Object> authorization = googleOAuthService.beginAuthorization("alice", request);

        assertTrue(String.valueOf(authorization.get("authorizationUrl")).contains("client_id=client-id"));
        String state = String.valueOf(authorization.get("state"));

        Calendar connection = googleOAuthService.completeAuthorization("auth-code-123", state);

        assertTrue(connection instanceof GoogleCalendar);
        GoogleCalendar googleCalendar = (GoogleCalendar) connection;
        assertEquals("primary", googleCalendar.getCalendarId());
        assertEquals("invitee@example.com", googleCalendar.getInviteEmail());
        assertEquals(1, calendarIntegrationService.getConnectionsForUser("alice").size());
    }

    private void handleTokenExchange(HttpExchange exchange) throws IOException {
        String body = """
                {
                  "access_token": "oauth-access-token",
                  "token_type": "Bearer",
                  "expires_in": 3600
                }
                """;
        byte[] bytes = body.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(bytes);
        }
    }
}
