package backend.services;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import backend.models.Match;
import backend.models.MatchState;

/**
 * Leaguepedia client that queries the Liquipedia/Leaguepedia Cargo API.
 *
 * NOTE: This is a simple implementation that attempts to map Cargo query
 * results into the project's `Match` model. Field names on the Cargo side
 * vary between wikis; this client tries several common keys and falls back
 * gracefully when values are missing.
 */
public class LeaguepediaClient {

    private static final String DEFAULT_API = "https://liquipedia.net/leagueoflegends/api.php";
    private static final String CARGO_QUERY_KEY = "cargoquery";
    private static final String FULLTEXT_KEY = "fulltext";
    
    private final String apiEndpoint;
    private final HttpClient http;
    private final Gson gson;

    public LeaguepediaClient() {
        this(DEFAULT_API);
    }

    public LeaguepediaClient(String apiEndpoint) {
        this.apiEndpoint = apiEndpoint;
        this.http = HttpClient.newHttpClient();
        this.gson = new Gson();
    }

    /**
     * Get the next upcoming match for a specific team using a Cargo query.
     * This method will return null if no match can be found or on errors.
     */
    public Match getNextTeamMatch(String teamName) {
        try {
            List<Match> all = fetchUpcomingMatchesForTeam(teamName);
            LocalDateTime now = LocalDateTime.now();
            Match best = null;
            for (Match m : all) {
                if (isValidUpcomingMatch(m, now) && (best == null || m.getScheduledTime().isBefore(best.getScheduledTime()))) {
                    best = m;
                }
            }
            return best;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while fetching team matches", e);
        }
    }

    /**
     * Get the next upcoming match in a tournament
     */
    public Match getNextTournamentMatch(String tournamentName) {
        try {
            List<Match> all = fetchUpcomingMatchesForTournament(tournamentName);
            LocalDateTime now = LocalDateTime.now();
            Match best = null;
            for (Match m : all) {
                if (isValidUpcomingMatch(m, now) && (best == null || m.getScheduledTime().isBefore(best.getScheduledTime()))) {
                    best = m;
                }
            }
            return best;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while fetching tournament matches", e);
        }
    }

    private boolean isValidUpcomingMatch(Match m, LocalDateTime now) {
        return m != null 
            && !m.isFinished() 
            && !m.isPast() 
            && m.getScheduledTime() != null 
            && m.getScheduledTime().isAfter(now);
    }

    /**
     * Fetch upcoming matches for a team.
     * Uses a simple Cargo query to Liquipedia's API and attempts to map results.
     */
    public List<Match> fetchUpcomingMatchesForTeam(String teamName) throws InterruptedException {
        try {
            String where = URLEncoder.encode(String.format("(Team1 LIKE '%%%s%%' OR Team2 LIKE '%%%s%%')", teamName, teamName), StandardCharsets.UTF_8);
            String q = "action=cargoquery&format=json&tables=match&fields=Start,Team1,Team2,Event,Stream,BestOf&page=Match&where=" + where + "&limit=50";
            return queryCargoForMatches(q);
        } catch (IOException e) {
            return new ArrayList<>();
        }
    }

    /**
     * Fetch upcoming matches for a tournament.
     */
    public List<Match> fetchUpcomingMatchesForTournament(String tournamentName) throws InterruptedException {
        try {
            String where = URLEncoder.encode(String.format("Event LIKE '%%%s%%'", tournamentName), StandardCharsets.UTF_8);
            String q = "action=cargoquery&format=json&tables=match&fields=Start,Team1,Team2,Event,Stream,BestOf&page=Match&where=" + where + "&limit=50";
            return queryCargoForMatches(q);
        } catch (IOException e) {
            return new ArrayList<>();
        }
    }

    private List<Match> queryCargoForMatches(String query) throws IOException, InterruptedException {
        String uri = apiEndpoint + "?" + query;
                HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(uri))
                    .GET()
                    .header("User-Agent", "DevOps-Client/0.2.0")
                    .header("Accept", "application/json, text/javascript, */*; q=0.01")
                    .header("Accept-Encoding", "gzip, deflate, br")
                    .header("Referer", "https://liquipedia.net/leagueoflegends/")
                    .header("X-Requested-With", "XMLHttpRequest")
                    .build();

        HttpResponse<byte[]> resp = http.send(req, HttpResponse.BodyHandlers.ofByteArray());
        int status = resp.statusCode();
        byte[] respBytes = resp.body();

        String respBody;
        String contentEncoding = resp.headers().firstValue("Content-Encoding").orElse("").toLowerCase();
        if (contentEncoding.contains("gzip")) {
            try (ByteArrayInputStream bais = new ByteArrayInputStream(respBytes);
                 GZIPInputStream gzis = new GZIPInputStream(bais)) {
                respBody = new String(gzis.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            }
        } else {
            respBody = new String(respBytes, java.nio.charset.StandardCharsets.UTF_8);
        }

        if (status != 200) {
            // Non-200 response - log details for debugging
            return new ArrayList<>();
        }

        JsonObject root = gson.fromJson(respBody, JsonObject.class);
        JsonArray cargo = extractCargoArray(root);

        List<Match> result = new ArrayList<>();
        if (cargo == null) return result;

        for (JsonElement el : cargo) {
            Match m = parseMatchFromElement(el);
            if (m != null) {
                result.add(m);
            }
        }

        return result;
    }

    private JsonArray extractCargoArray(JsonObject root) {
        if (root.has(CARGO_QUERY_KEY) && root.get(CARGO_QUERY_KEY).isJsonArray()) {
            return root.getAsJsonArray(CARGO_QUERY_KEY);
        }
        return null;
    }

    private Match parseMatchFromElement(JsonElement el) {
        try {
            JsonObject obj = el.getAsJsonObject();
            JsonObject title = obj.has("title") ? obj.getAsJsonObject("title") : null;
            JsonObject fields = obj.has("fields") ? obj.getAsJsonObject("fields") : null;

            String team1 = getFirstNonNull(fields, "Team1", "team1", "team_a", "team1_name");
            String team2 = getFirstNonNull(fields, "Team2", "team2", "team_b", "team2_name");
            String start = getFirstNonNull(fields, "Start", "start", "StartTime", "start_time");
            String event = getFirstNonNull(fields, "Event", "event", "tournament");
            String stream = getFirstNonNull(fields, "Stream", "stream", "stream_url");
            String bo = getFirstNonNull(fields, "BestOf", "bestof", "BO");

            if ((team1 == null || team2 == null) && title != null && title.has(FULLTEXT_KEY)) {
                String[] teams = parseTeamsFromFulltext(title.get(FULLTEXT_KEY).getAsString());
                if (team1 == null) team1 = teams[0];
                if (team2 == null) team2 = teams[1];
            }

            LocalDateTime scheduled = parseDateTime(start);
            String id = extractMatchId(title, obj);
            
            if (team1 == null || team2 == null || scheduled == null) {
                return null;
            }

            return new Match(id, team1, team2, scheduled, 
                event == null ? "Unknown" : event, 
                stream == null ? "" : stream, 
                bo == null ? "BO3" : bo);
        } catch (Exception e) {
            return null;
        }
    }

    private String[] parseTeamsFromFulltext(String fulltext) {
        String[] result = {null, null};
        if (fulltext.contains(" vs ")) {
            String[] parts = fulltext.split(" vs ", 2);
            result[0] = parts[0].trim();
            if (parts.length > 1) result[1] = parts[1].trim();
        }
        return result;
    }

    private String extractMatchId(JsonObject title, JsonObject obj) {
        if (title != null && title.has(FULLTEXT_KEY)) {
            return title.get(FULLTEXT_KEY).getAsString();
        }
        return Integer.toString(obj.hashCode());
    }

    private String getFirstNonNull(JsonObject obj, String... keys) {
        if (obj == null) return null;
        for (String k : keys) {
            if (obj.has(k) && !obj.get(k).isJsonNull()) {
                return obj.get(k).getAsString();
            }
        }
        return null;
    }

    private LocalDateTime parseDateTime(String s) {
        if (s == null) return null;
        s = s.trim();
        // Try several common formats
        DateTimeFormatter[] fmts = new DateTimeFormatter[] {
            DateTimeFormatter.ISO_DATE_TIME,
            DateTimeFormatter.ISO_LOCAL_DATE_TIME,
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")
        };
        for (DateTimeFormatter f : fmts) {
            try {
                return LocalDateTime.parse(s, f);
            } catch (DateTimeParseException ignored) {
                // Format not matching - try next format
            }
        }

        // Try parsing epoch seconds
        try {
            long epoch = Long.parseLong(s);
            return LocalDateTime.ofEpochSecond(epoch, 0, java.time.ZoneOffset.UTC);
        } catch (Exception ignored) {
            // Not a valid epoch timestamp
        }

        return null;
    }

    /**
     * Update match status locally based on current time. Kept for compatibility.
     */
    public void updateMatchStatus(Match match) {
        if (match == null) return;
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime matchEnd = match.getScheduledTime().plusHours(match.getEstimatedDurationHours());
        if (match.isPast() && match.getStatus() != MatchState.FINISHED) {
            match.setStatus(MatchState.FINISHED);
        } else if (now.isAfter(match.getScheduledTime()) && now.isBefore(matchEnd)) {
            match.setStatus(MatchState.IN_PROGRESS);
        } else if (now.isAfter(matchEnd)) {
            match.setStatus(MatchState.FINISHED);
        }
    }

}
