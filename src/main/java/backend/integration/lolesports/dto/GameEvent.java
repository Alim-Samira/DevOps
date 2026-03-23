package backend.integration.lolesports.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GameEvent(
    @JsonAlias({"type", "eventType"}) String type,
    @JsonAlias({"killer", "killerName"}) String killer,
    @JsonAlias({"victim", "victimName"}) String victim,
    @JsonAlias({"dragonType"}) String dragonType,
    @JsonAlias({"team", "teamID"}) String team,
    @JsonAlias({"timestamp", "gameTime"}) long timestamp
) {}
