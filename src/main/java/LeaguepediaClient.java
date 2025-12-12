import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

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
import java.io.ByteArrayInputStream;
import java.util.zip.GZIPInputStream;

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
        List<Match> all = fetchUpcomingMatchesForTeam(teamName, 30);
        LocalDateTime now = LocalDateTime.now();
        Match best = null;
        for (Match m : all) {
            if (m == null) continue;
            if (m.isFinished() || m.isPast()) continue;
            if (m.getScheduledTime() == null) continue;
            if (m.getScheduledTime().isAfter(now)) {
                if (best == null || m.getScheduledTime().isBefore(best.getScheduledTime())) {
                    best = m;
                }
            }
        }
        return best;
    }

    /**
     * Get the next upcoming match in a tournament
     */
    public Match getNextTournamentMatch(String tournamentName) {
        List<Match> all = fetchUpcomingMatchesForTournament(tournamentName, 30);
        LocalDateTime now = LocalDateTime.now();
        Match best = null;
        for (Match m : all) {
            if (m == null) continue;
            if (m.isFinished() || m.isPast()) continue;
            if (m.getScheduledTime() == null) continue;
            if (m.getScheduledTime().isAfter(now)) {
                if (best == null || m.getScheduledTime().isBefore(best.getScheduledTime())) {
                    best = m;
                }
            }
        }
        return best;
    }

    /**
     * Fetch upcoming matches for a team within `daysAhead` days.
     * Uses a simple Cargo query to Liquipedia's API and attempts to map results.
     */
    public List<Match> fetchUpcomingMatchesForTeam(String teamName, int daysAhead) {
        String where = URLEncoder.encode(String.format("(Team1 LIKE '%%%s%%' OR Team2 LIKE '%%%s%%')", teamName, teamName), StandardCharsets.UTF_8);
        String q = "action=cargoquery&format=json&tables=match&fields=Start,Team1,Team2,Event,Stream,BestOf&page=Match&where=" + where + "&limit=50";
        return queryCargoForMatches(q);
    }

    /**
     * Fetch upcoming matches for a tournament.
     */
    public List<Match> fetchUpcomingMatchesForTournament(String tournamentName, int daysAhead) {
        String where = URLEncoder.encode(String.format("Event LIKE '%%%s%%'", tournamentName), StandardCharsets.UTF_8);
        String q = "action=cargoquery&format=json&tables=match&fields=Start,Team1,Team2,Event,Stream,BestOf&page=Match&where=" + where + "&limit=50";
        return queryCargoForMatches(q);
    }

    private List<Match> queryCargoForMatches(String query) {
        try {
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
            JsonArray cargo = null;
            if (root.has("cargoquery") && root.get("cargoquery").isJsonArray()) {
                cargo = root.getAsJsonArray("cargoquery");
            } else if (root.has("query") && root.getAsJsonObject("query").has("pages")) {
                // fallback: some wikis return different structures
                // not implemented in-depth here
            }

            List<Match> result = new ArrayList<>();
            if (cargo == null) return result;

            for (JsonElement el : cargo) {
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

                    if ((team1 == null || team2 == null) && title != null && title.has("fulltext")) {
                        String ft = title.get("fulltext").getAsString();
                        // try simple "Team A vs Team B" parse
                        if (ft.contains(" vs ")) {
                            String[] parts = ft.split(" vs ", 2);
                            if (team1 == null) team1 = parts[0].trim();
                            if (team2 == null && parts.length > 1) team2 = parts[1].trim();
                        }
                    }

                    LocalDateTime scheduled = parseDateTime(start);
                    String id = title != null && title.has("fulltext") ? title.get("fulltext").getAsString() : Integer.toString(obj.hashCode());
                    if (team1 == null || team2 == null || scheduled == null) {
                        // skip incomplete entries
                        continue;
                    }

                    Match m = new Match(id, team1, team2, scheduled, event == null ? "Unknown" : event, stream == null ? "" : stream, bo == null ? "BO3" : bo);
                    result.add(m);
                } catch (Exception e) {
                    // Continue on parse errors for individual entries
                }
            }

            return result;
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            // Error querying API
            return new ArrayList<>();
        }
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
