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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.GZIPInputStream;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import backend.models.Match;
import backend.models.MatchState;

/**
 * Client to fetch League of Legends esports match data
 * Uses mock data for MVP, will connect to Leaguepedia API later
 */
public class LeaguepediaClient {
    
    private List<Match> mockMatches;
    
    public LeaguepediaClient() {
        initializeMockData();
    }
    
    /**
     * Get the next upcoming match for a specific team
     */
    public Match getNextTeamMatch(String teamName) throws InterruptedException {
        List<Match> all = fetchUpcomingMatchesForTeam(teamName);
        LocalDateTime now = LocalDateTime.now();
        
        return mockMatches.stream()
            .filter(m -> !m.isFinished())
            .filter(m -> !m.isPast()) 
            .filter(m -> m.getTeam1().equalsIgnoreCase(teamName) || 
                        m.getTeam2().equalsIgnoreCase(teamName))
            .filter(m -> m.getScheduledTime().isAfter(now))
            .min((m1, m2) -> m1.getScheduledTime().compareTo(m2.getScheduledTime()))
            .orElse(null);
    }
    
    /**
     * Get the next upcoming match in a tournament
     */
    public Match getNextTournamentMatch(String tournamentName) throws InterruptedException {
        List<Match> all = fetchUpcomingMatchesForTournament(tournamentName);
        LocalDateTime now = LocalDateTime.now();
        Match best = null;
        for (Match m : all) {
            if (isValidUpcomingMatch(m, now) && (best == null || m.getScheduledTime().isBefore(best.getScheduledTime()))) {
                best = m;
            }
        }
        return best;
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
     * Get all upcoming matches for a team
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
    
    /**
     * Initialize mock data for testing
     * In production, this would query Leaguepedia's Cargo API
     */
    private void initializeMockData() {
        LocalDateTime now = LocalDateTime.now();
        
        mockMatches = new ArrayList<>(Arrays.asList(
            // Upcoming matches (for testing auto watch parties)
            new Match(
                "worlds2025_t1_geng_1",
                "T1",
                "Gen.G",
                now.plusMinutes(25), // 25min from now - should open soon!
                "Worlds 2025",
                "https://twitch.tv/riotgames",
                "BO5"
            ),
            new Match(
                "lck2025_t1_dk_1",
                "T1",
                "Dplus KIA",
                now.plusHours(3),
                "LCK Spring 2025",
                "https://twitch.tv/lck",
                "BO3"
            ),
            new Match(
                "lec2025_g2_fnc_1",
                "G2 Esports",
                "Fnatic",
                now.plusHours(5),
                "LEC Spring 2025",
                "https://twitch.tv/lec",
                "BO1"
            ),
            new Match(
                "worlds2025_geng_blg_1",
                "Gen.G",
                "BiliBili Gaming",
                now.plusDays(1),
                "Worlds 2025",
                "https://twitch.tv/riotgames",
                "BO5"
            ),
            new Match(
                "lck2025_dk_kt_1",
                "Dplus KIA",
                "KT Rolster",
                now.plusDays(2),
                "LCK Spring 2025",
                "https://twitch.tv/lck",
                "BO3"
            ),
            
            // Past matches (finished)
            new Match(
                "worlds2024_t1_blg_finals",
                "T1",
                "BiliBili Gaming",
                now.minusDays(30),
                "Worlds 2024",
                "https://twitch.tv/riotgames",
                "BO5"
            )
        ));
        
        // Mark past match as finished
        mockMatches.get(mockMatches.size() - 1).setStatus(MatchState.FINISHED);
    }
    
    /**
     * Simulate checking if a match status has changed
     * In production, this would query live match data
     */
    public void updateMatchStatus(Match match) {
        if (match == null) return;
        
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime matchEnd = match.getScheduledTime().plusHours(match.getEstimatedDurationHours());
        
        // Auto-mark past matches as finished to prevent stale data
        if (match.isPast() && match.getStatus() != MatchState.FINISHED) {
            match.setStatus(MatchState.FINISHED);
            System.out.println("[!] Auto-marked past match as finished: " + match.getId());
        } else if (now.isAfter(match.getScheduledTime()) && now.isBefore(matchEnd)) {
            match.setStatus(MatchState.IN_PROGRESS);
        } else if (now.isAfter(matchEnd)) {
            match.setStatus(MatchState.FINISHED);
        }
    }
    
    /**
     * Get all mock matches (for debugging)
     */
    public List<Match> getAllMatches() {
        return new ArrayList<>(mockMatches);
    }
}
