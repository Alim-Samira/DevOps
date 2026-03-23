package backend.models;

import java.time.LocalDateTime;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Transient;

@Embeddable
public class AutoConfig {

    private AutoType type;
    private String target;

    @Transient
    private Match currentMatch;

    private LocalDateTime lastChecked;

    public AutoConfig() {}

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