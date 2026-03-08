package backend.integration.lolesports;

import backend.integration.lolesports.dto.WindowResponse;
import com.google.gson.Gson;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class LolEsportsClient {

    private static final String KEY = "0TvQnueqKa5mxJntVWt0w4LpLfEkrV1Ta8rQBb9Z";

    private final RestClient gwClient = RestClient.builder()
            .baseUrl("https://esports-api.lolesports.com/persisted/gw")
            .defaultHeader("x-api-key", KEY)
            .build();

    private final RestClient liveClient = RestClient.builder()
            .baseUrl("https://feed.lolesports.com/livestats/v1")
            .defaultHeader("x-api-key", KEY)
            .build();

    private final Gson gson = new Gson(); // si tu préfères Gson comme Leaguepedia

    // Récupère un gameId à partir d'un event Riot (appelé par le scheduler)
    public Optional<String> getFirstGameId(String eventId) {
        // appel /getEventDetails puis retourne le premier game.id
        // implémente selon besoin (je peux te donner la version complète)
        return Optional.empty(); // placeholder
    }

    // CORE : Frame live (appelée toutes les 8-10s)
    public WindowResponse getWindow(String gameId) {
        return liveClient.get()
                .uri("/window/{gameId}", gameId)
                .retrieve()
                .body(WindowResponse.class);
    }

    // Bonus : détecter les matchs live pour mapping auto
    public List<LiveEvent> getLiveEvents() {
        // /getLive → parsing
        return List.of();
    }
}