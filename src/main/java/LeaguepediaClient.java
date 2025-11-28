import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
    public Match getNextTeamMatch(String teamName) {
        LocalDateTime now = LocalDateTime.now();
        
        return mockMatches.stream()
            .filter(m -> !m.isFinished())
            .filter(m -> m.getTeam1().equalsIgnoreCase(teamName) || 
                        m.getTeam2().equalsIgnoreCase(teamName))
            .filter(m -> m.getScheduledTime().isAfter(now))
            .min((m1, m2) -> m1.getScheduledTime().compareTo(m2.getScheduledTime()))
            .orElse(null);
    }
    
    /**
     * Get the next upcoming match in a tournament
     */
    public Match getNextTournamentMatch(String tournamentName) {
        LocalDateTime now = LocalDateTime.now();
        
        return mockMatches.stream()
            .filter(m -> !m.isFinished())
            .filter(m -> m.getTournament().equalsIgnoreCase(tournamentName))
            .filter(m -> m.getScheduledTime().isAfter(now))
            .min((m1, m2) -> m1.getScheduledTime().compareTo(m2.getScheduledTime()))
            .orElse(null);
    }
    
    /**
     * Get all upcoming matches for a team
     */
    public List<Match> getAllUpcomingTeamMatches(String teamName, int daysAhead) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime deadline = now.plusDays(daysAhead);
        
        List<Match> result = new ArrayList<>();
        for (Match m : mockMatches) {
            if (!m.isFinished() && 
                (m.getTeam1().equalsIgnoreCase(teamName) || m.getTeam2().equalsIgnoreCase(teamName)) &&
                m.getScheduledTime().isAfter(now) &&
                m.getScheduledTime().isBefore(deadline)) {
                result.add(m);
            }
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
        mockMatches.get(mockMatches.size() - 1).setStatus(MatchStatus.FINISHED);
    }
    
    /**
     * Simulate checking if a match status has changed
     * In production, this would query live match data
     */
    public void updateMatchStatus(Match match) {
        if (match == null) return;
        
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime matchEnd = match.getScheduledTime().plusHours(match.getEstimatedDurationHours());
        
        if (now.isAfter(match.getScheduledTime()) && now.isBefore(matchEnd)) {
            match.setStatus(MatchStatus.LIVE);
        } else if (now.isAfter(matchEnd)) {
            match.setStatus(MatchStatus.FINISHED);
        }
    }
    
    /**
     * Get all mock matches (for debugging)
     */
    public List<Match> getAllMatches() {
        return new ArrayList<>(mockMatches);
    }
}
