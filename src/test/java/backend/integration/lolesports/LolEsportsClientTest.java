package backend.integration.lolesports;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import backend.models.Match;

class LolEsportsClientTest {

    private LolEsportsClient client;
    private HttpServer server;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/gw/getEventDetails", exchange -> respond(exchange, """
                {
                  "event": {
                    "games": [
                      { "id": "game-1" },
                      { "id": "game-2" }
                    ]
                  }
                }
                """));
        server.createContext("/gw/getLive", exchange -> respond(exchange, """
                {
                  "data": {
                    "schedule": {
                      "events": [
                        {
                          "id": "event-123",
                          "league": { "name": "MSI 2026" },
                          "match": {
                            "id": "match-456",
                            "teams": [
                              { "name": "T1" },
                              { "name": "G2" }
                            ]
                          }
                        }
                      ]
                    }
                  }
                }
                """));
        server.createContext("/live/window/game-live", exchange -> respond(exchange, """
                {
                  "esportsGameId": "game-live",
                  "gameMetadata": { "patchVersion": "14.5" },
                  "frames": [
                    {
                      "gameTime": 120000,
                      "blueTeam": { "totalGold": 12000, "totalKills": 2, "participants": [] },
                      "redTeam": { "totalGold": 11800, "totalKills": 1, "participants": [] },
                      "events": [
                        { "eventType": "KILL", "teamID": "blue", "gameTime": 95000 }
                      ]
                    }
                  ]
                }
                """));
        server.start();

        String baseUrl = "http://localhost:" + server.getAddress().getPort();
        client = new LolEsportsClient("key", baseUrl + "/gw", baseUrl + "/live");
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void getFirstGameIdShouldCallGateway() {
        assertEquals(Optional.of("game-1"), client.getFirstGameId("match-live"));
    }

    @Test
    void findLiveEventIdAndWindowShouldUseHttpEndpoints() {
        Optional<String> eventId = client.findLiveEventId("T1", "G2", "MSI");

        assertTrue(eventId.isPresent());
        assertEquals("match-456", eventId.get());
        assertEquals("game-live", client.getWindow("game-live").esportsGameId());
        assertEquals(1, client.getWindow("game-live").frames().get(0).events().size());
    }

    @Test
    void findLiveEventIdShouldAcceptMatchWrapper() {
        Match match = new Match("lp-match", "T1", "G2", java.time.LocalDateTime.now(), "MSI 2026", "", "BO1");
        assertEquals(Optional.of("match-456"), client.findLiveEventId(match));
    }

    @Test
    void clientShouldReturnEmptyForInvalidLookupInputs() {
        assertEquals(Optional.empty(), client.getFirstGameId(""));
        assertEquals(Optional.empty(), client.findLiveEventId((Match) null));
        assertEquals(Optional.empty(), client.findLiveEventId("T1", "", "MSI 2026"));
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
