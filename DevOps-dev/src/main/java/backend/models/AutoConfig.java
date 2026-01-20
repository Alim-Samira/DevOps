package backend.models;
import java.time.LocalDateTime;

/**
 * Configuration for automatic watch party management
 */
public class AutoConfig {
    private AutoType type;
    private String target; // Team name or tournament name
    private Match currentMatch; // The current or upcoming match
    private LocalDateTime lastChecked;

    public AutoConfig(AutoType type, String target) {
        this.type = type;
        this.target = target;
        this.currentMatch = null;
        this.lastChecked = LocalDateTime.now();
    }

    public AutoType getType() { return type; }
    public String getTarget() { return target; }
    public Match getCurrentMatch() { return currentMatch; }
    public LocalDateTime getLastChecked() { return lastChecked; }

    public void setCurrentMatch(Match match) { 
        this.currentMatch = match; 
    }

    public void updateLastChecked() {
        this.lastChecked = LocalDateTime.now();
    }

    public boolean isTeamBased() {
        return type == AutoType.TEAM;
    }

    public boolean isTournamentBased() {
        return type == AutoType.TOURNAMENT;
    }

    @Override
    public String toString() {
        return String.format("%s: %s (Last checked: %s)", 
            type, target, lastChecked);
    }
}
