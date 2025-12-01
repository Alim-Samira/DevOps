import java.time.LocalDateTime;

/**
 * Represents a League of Legends esports match
 */
public class Match {
    private String id;
    private String team1;
    private String team2;
    private LocalDateTime scheduledTime;
    private String tournament;
    private String streamUrl;
    private MatchStatus status;
    private String bestOf; // "BO1", "BO3", "BO5"

    public Match(String id, String team1, String team2, LocalDateTime scheduledTime, 
                 String tournament, String streamUrl, String bestOf) {
        this.id = id;
        this.team1 = team1;
        this.team2 = team2;
        this.scheduledTime = scheduledTime;
        this.tournament = tournament;
        this.streamUrl = streamUrl;
        this.bestOf = bestOf;
        this.status = MatchStatus.SCHEDULED;
    }

    public String getId() { return id; }
    public String getTeam1() { return team1; }
    public String getTeam2() { return team2; }
    public LocalDateTime getScheduledTime() { return scheduledTime; }
    public String getTournament() { return tournament; }
    public String getStreamUrl() { return streamUrl; }
    public MatchStatus getStatus() { return status; }
    public String getBestOf() { return bestOf; }

    public void setStatus(MatchStatus status) { this.status = status; }

    public boolean isFinished() {
        return status == MatchStatus.FINISHED;
    }
    
    /**
     * Check if this match is in the past (scheduled time has passed)
     */
    public boolean isPast() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime matchEnd = scheduledTime.plusHours(getEstimatedDurationHours());
        return now.isAfter(matchEnd);
    }

    public boolean isStartingSoon(int minutesBefore) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime openTime = scheduledTime.minusMinutes(minutesBefore);
        return now.isAfter(openTime) && now.isBefore(scheduledTime.plusHours(getEstimatedDurationHours()));
    }

    public int getEstimatedDurationHours() {
        switch (bestOf) {
            case "BO1": return 1;
            case "BO3": return 2;
            case "BO5": return 4;
            default: return 2;
        }
    }

    @Override
    public String toString() {
        return String.format("%s vs %s | %s | %s | %s", 
            team1, team2, tournament, scheduledTime, status);
    }
}
