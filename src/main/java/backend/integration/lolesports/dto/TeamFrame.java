package backend.integration.lolesports.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TeamFrame(
    int totalGold,
    int totalKills,
    List<ParticipantFrame> participants
) {
    @Override
    public List<ParticipantFrame> participants() {
        return participants == null ? List.of() : participants;
    }
}
