package backend.integration.lolesports;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import backend.integration.lolesports.dto.Frame;
import backend.integration.lolesports.dto.GameEvent;
import backend.integration.lolesports.dto.TeamFrame;
import backend.integration.lolesports.dto.WindowResponse;
import backend.models.User;
import backend.models.WatchParty;
import backend.services.BetService;
import backend.services.WatchPartyManager;

class LiveMatchMonitorServiceTest {

    @Test
    void pollAndResolveOnceShouldResolveAndStopWhenGameEnds() {
        LolEsportsClient client = mock(LolEsportsClient.class);
        BetService betService = mock(BetService.class);
        WatchPartyManager manager = mock(WatchPartyManager.class);
        LiveMatchMonitorService service = new LiveMatchMonitorService(client, betService, manager);

        WatchParty watchParty = new WatchParty("Monitor WP", LocalDateTime.now().plusDays(1), "LoL");
        watchParty.setCreator(new User("admin", true));
        Frame latestFrame = new Frame(
                1_800_000L,
                new TeamFrame(50000, 10, List.of()),
                new TeamFrame(48000, 8, List.of()),
                List.of(new GameEvent("GAME_END", null, null, null, "blue", 1_800_000L)));
        when(manager.getWatchPartyByName("Monitor WP")).thenReturn(watchParty);
        when(client.getWindow("game-1")).thenReturn(new WindowResponse(
                "game-1",
                null,
                List.of(latestFrame)));

        service.startMonitoring(watchParty, "game-1");
        service.pollAndResolveOnce("game-1");

        verify(betService, atLeastOnce()).tryAutoResolveLiveBet(eq(watchParty), isNull(), eq(latestFrame));
        assertNull(watchParty.getCurrentRiotGameId());
    }

    @Test
    void shutdownOnContextCloseShouldStopActiveMonitors() {
        LolEsportsClient client = mock(LolEsportsClient.class);
        BetService betService = mock(BetService.class);
        WatchPartyManager manager = mock(WatchPartyManager.class);
        LiveMatchMonitorService service = new LiveMatchMonitorService(client, betService, manager);

        WatchParty watchParty = new WatchParty("Shutdown WP", LocalDateTime.now().plusDays(1), "LoL");
        watchParty.setCreator(new User("admin", true));

        service.startMonitoring(watchParty, "game-shutdown");
        service.shutdownOnContextClose();

        assertNull(watchParty.getCurrentRiotGameId());
    }
}
