package backend.services.betresolution;

import backend.integration.lolesports.dto.Frame;
import backend.models.WatchParty;

public record BetResolutionContext(
        WatchParty watchParty,
        Frame previousFrame,
        Frame currentFrame,
        MilestoneRaceTracker milestoneRaceTracker
) {}
