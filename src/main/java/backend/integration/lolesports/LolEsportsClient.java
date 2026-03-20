package backend.integration.lolesports;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.google.gson.Gson;

import backend.integration.lolesports.dto.WindowResponse;

@Service
public class LolEsportsClient {

    private static final String DEFAULT_API_KEY = "0TvQnueqKa5mxJntVWt0w4LpLfEkrV1Ta8rQBb9Z";
    private static final String DEFAULT_GW_BASE_URL = "https://esports-api.lolesports.com/persisted/gw";
    private static final String DEFAULT_LIVE_BASE_URL = "https://feed.lolesports.com/livestats/v1";

    private final RestClient gwClient;
    private final RestClient liveClient;
    private final Gson gson = new Gson();

    public LolEsportsClient(
            @Value("${lolesports.api-key:" + DEFAULT_API_KEY + "}") String apiKey,
            @Value("${lolesports.gw-base-url:" + DEFAULT_GW_BASE_URL + "}") String gwBaseUrl,
            @Value("${lolesports.live-base-url:" + DEFAULT_LIVE_BASE_URL + "}") String liveBaseUrl) {
        this.gwClient = RestClient.builder()
                .baseUrl(gwBaseUrl)
                .defaultHeader("x-api-key", apiKey)
                .build();

        this.liveClient = RestClient.builder()
                .baseUrl(liveBaseUrl)
                .defaultHeader("x-api-key", apiKey)
                .build();
    }

    public Optional<String> getFirstGameId(String eventId) {
        if (eventId == null || eventId.isBlank()) {
            return Optional.empty();
        }

        try {
            String json = gwClient.get()
                    .uri("/getEventDetails?id={eventId}&hl=fr-FR", eventId)
                    .retrieve()
                    .body(String.class);

            EventDetailsResponse response = gson.fromJson(json, EventDetailsResponse.class);
            if (response != null
                    && response.event() != null
                    && response.event().games() != null
                    && !response.event().games().isEmpty()) {
                return Optional.of(response.event().games().get(0).id());
            }
        } catch (Exception ignored) {
            // Best effort client: the caller decides whether to retry or fall back.
        }
        return Optional.empty();
    }

    public WindowResponse getWindow(String gameId) {
        return liveClient.get()
                .uri("/window/{gameId}", gameId)
                .retrieve()
                .body(WindowResponse.class);
    }
}

record EventDetailsResponse(EventData event) {}
record EventData(List<GameData> games) {}
record GameData(String id) {}
