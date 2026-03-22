package backend.integration.lolesports.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record WindowResponse(
    String esportsGameId,
    GameMetadata gameMetadata,
    List<Frame> frames
) {
    @Override
    public List<Frame> frames() {
        return frames == null ? List.of() : frames;
    }
}
