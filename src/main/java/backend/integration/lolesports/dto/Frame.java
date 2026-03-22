package backend.integration.lolesports.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Frame(
    @JsonAlias({"timestamp", "gameTime"}) long timestamp,
    TeamFrame blueTeam,
    TeamFrame redTeam,
    List<GameEvent> events
) {
    @Override
    public List<GameEvent> events() {
        return events == null ? List.of() : events;
    }
}
