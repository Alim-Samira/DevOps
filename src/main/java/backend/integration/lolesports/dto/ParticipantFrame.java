package backend.integration.lolesports.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ParticipantFrame(
    @JsonAlias("championName") String championName,
    @JsonAlias({"summonerName", "playerName", "name"}) String summonerName,
    int kills,
    int deaths,
    int assists,
    int gold,
    int level
) {
    public ParticipantFrame(String championName, int kills, int deaths, int assists, int gold, int level) {
        this(championName, championName, kills, deaths, assists, gold, level);
    }

    public String displayName() {
        return summonerName == null || summonerName.isBlank() ? championName : summonerName;
    }
}
