package backend.integration.lolesports.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ParticipantFrame(
    String championName,
    int kills,
    int deaths,
    int assists,
    int gold,
    int level
) {}
