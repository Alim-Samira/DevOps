package backend.integration.lolesports;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import backend.integration.lolesports.dto.WindowResponse;
import backend.models.Match;

@Service
public class LolEsportsClient {

    private static final Logger log = LoggerFactory.getLogger(LolEsportsClient.class);
    private static final String DEFAULT_GW_BASE_URL = "https://esports-api.lolesports.com/persisted/gw";
    private static final String DEFAULT_LIVE_BASE_URL = "https://feed.lolesports.com/livestats/v1";
    private static final String HEADER_AUTH_TOKEN = "x-api-key";
    private static final String STRING_MATCH = "match";
    private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^a-z0-9]+");
    private static final Gson GSON = new Gson();

    private final RestClient gwClient;
    private final RestClient liveClient;

    public LolEsportsClient(
            @Value("${lolesports.auth-token:}") String authToken,
            @Value("${lolesports.gw-base-url:" + DEFAULT_GW_BASE_URL + "}") String gwBaseUrl,
            @Value("${lolesports.live-base-url:" + DEFAULT_LIVE_BASE_URL + "}") String liveBaseUrl) {
        this.gwClient = buildClient(gwBaseUrl, authToken);
        this.liveClient = buildClient(liveBaseUrl, authToken);
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

            return extractFirstGameId(json);
        } catch (RuntimeException ex) {
            log.debug("Unable to fetch first game id for event {}", eventId, ex);
            return Optional.empty();
        }
    }

    public Optional<String> findLiveEventId(Match match) {
        if (match == null) {
            return Optional.empty();
        }
        return findLiveEventId(match.getTeam1(), match.getTeam2(), match.getTournament());
    }

    public Optional<String> findLiveEventId(String team1, String team2, String tournamentName) {
        if (isBlank(team1) || isBlank(team2)) {
            return Optional.empty();
        }

        try {
            String json = gwClient.get()
                    .uri("/getLive?hl=fr-FR")
                    .retrieve()
                    .body(String.class);

            return extractLiveEventId(json, team1, team2, tournamentName);
        } catch (RuntimeException ex) {
            log.debug("Unable to find live event id for {} vs {}", team1, team2, ex);
            return Optional.empty();
        }
    }

    public WindowResponse getWindow(String gameId) {
        return liveClient.get()
                .uri("/window/{gameId}", gameId)
                .retrieve()
                .body(WindowResponse.class);
    }

    Optional<String> extractFirstGameId(String json) {
        EventDetailsResponse response = GSON.fromJson(json, EventDetailsResponse.class);
        if (response != null
                && response.event() != null
                && response.event().games() != null
                && !response.event().games().isEmpty()) {
            return Optional.ofNullable(response.event().games().get(0).id());
        }
        return Optional.empty();
    }

    Optional<String> extractLiveEventId(String json, String team1, String team2, String tournamentName) {
        JsonObject root = GSON.fromJson(json, JsonObject.class);
        JsonArray events = getNestedArray(root, "data", "schedule", "events");
        if (events == null) {
            return Optional.empty();
        }

        for (JsonElement element : events) {
            JsonObject event = safeObject(element);
            if (event == null || !matchesLiveEvent(event, team1, team2, tournamentName)) {
                continue;
            }

            String id = extractEventIdentifier(event);
            if (!isBlank(id)) {
                return Optional.of(id);
            }
        }

        return Optional.empty();
    }

    private RestClient buildClient(String baseUrl, String authToken) {
        RestClient.Builder builder = RestClient.builder().baseUrl(baseUrl);
        if (!isBlank(authToken)) {
            builder.defaultHeader(HEADER_AUTH_TOKEN, authToken);
        }
        return builder.build();
    }

    private boolean matchesLiveEvent(JsonObject event, String team1, String team2, String tournamentName) {
        JsonObject match = event.has(STRING_MATCH) ? safeObject(event.get(STRING_MATCH)) : null;
        JsonArray teams = match != null && match.has("teams") ? match.getAsJsonArray("teams") : null;
        if (teams == null || teams.size() < 2) {
            return false;
        }

        String liveTeam1 = extractName(safeObject(teams.get(0)));
        String liveTeam2 = extractName(safeObject(teams.get(1)));
        if (isBlank(liveTeam1) || isBlank(liveTeam2)) {
            return false;
        }

        boolean sameTeams = namesMatch(team1, liveTeam1) && namesMatch(team2, liveTeam2);
        boolean swappedTeams = namesMatch(team1, liveTeam2) && namesMatch(team2, liveTeam1);
        if (!sameTeams && !swappedTeams) {
            return false;
        }

        if (isBlank(tournamentName)) {
            return true;
        }

        JsonObject league = event.has("league") ? safeObject(event.get("league")) : null;
        String liveTournament = extractName(league);
        return isBlank(liveTournament) || namesClose(tournamentName, liveTournament);
    }

    private String extractEventIdentifier(JsonObject event) {
        JsonObject match = event.has(STRING_MATCH) ? safeObject(event.get(STRING_MATCH)) : null;
        String matchId = match == null ? null : getString(match, "id");
        return !isBlank(matchId) ? matchId : getString(event, "id");
    }

    private String extractName(JsonObject object) {
        if (object == null) {
            return null;
        }
        String name = getString(object, "name");
        return !isBlank(name) ? name : getString(object, "slug");
    }

    private JsonArray getNestedArray(JsonObject root, String... path) {
        JsonElement current = root;
        for (String segment : path) {
            if (current == null || !current.isJsonObject()) {
                return null;
            }
            JsonObject object = current.getAsJsonObject();
            if (!object.has(segment)) {
                return null;
            }
            current = object.get(segment);
        }
        return current != null && current.isJsonArray() ? current.getAsJsonArray() : null;
    }

    private JsonObject safeObject(JsonElement element) {
        return element != null && element.isJsonObject() ? element.getAsJsonObject() : null;
    }

    private String getString(JsonObject object, String key) {
        return object.has(key) && !object.get(key).isJsonNull() ? object.get(key).getAsString() : null;
    }

    private boolean namesMatch(String left, String right) {
        return normalize(left).equals(normalize(right));
    }

    private boolean namesClose(String left, String right) {
        String normalizedLeft = normalize(left);
        String normalizedRight = normalize(right);
        return normalizedLeft.equals(normalizedRight)
                || normalizedLeft.contains(normalizedRight)
                || normalizedRight.contains(normalizedLeft);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String normalize(String value) {
        return value == null ? "" : NON_ALPHANUMERIC.matcher(value.toLowerCase(Locale.ROOT)).replaceAll("");
    }
}

record EventDetailsResponse(EventData event) {}
record EventData(List<GameData> games) {}
record GameData(String id) {}
