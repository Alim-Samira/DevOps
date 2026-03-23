package backend.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import backend.integration.lolesports.LiveMatchMonitorService;
import backend.integration.lolesports.LolEsportsClient;
import backend.models.AutoType;
import backend.models.Match;
import backend.models.MatchState;
import backend.models.User;
import backend.models.WatchParty;

class AutoWatchPartySchedulerTest {

    @Test
    void forceUpdateShouldResolveLiveEventIdAndStartMonitoring() throws Exception {
        WatchPartyManager manager = new WatchPartyManager();
        LeaguepediaClient apiClient = mock(LeaguepediaClient.class);
        LolEsportsClient lolClient = mock(LolEsportsClient.class);
        LiveMatchMonitorService liveMonitor = mock(LiveMatchMonitorService.class);
        AutoWatchPartyScheduler scheduler = new AutoWatchPartyScheduler(manager, apiClient, lolClient, liveMonitor);

        WatchParty watchParty = WatchParty.createAutoWatchParty(new User("alice", false), "T1", AutoType.TEAM);
        manager.addAutoWatchParty(watchParty);

        Match liveMatch = new Match(
                "lp-match",
                "T1",
                "G2",
                LocalDateTime.now().minusMinutes(20),
                "MSI 2026",
                "",
                "BO1");
        liveMatch.setStatus(MatchState.IN_PROGRESS);

        when(apiClient.getNextTeamMatch("T1")).thenReturn(liveMatch);
        when(lolClient.findLiveEventId(liveMatch)).thenReturn(Optional.of("riot-match"));
        when(lolClient.getFirstGameId("riot-match")).thenReturn(Optional.of("riot-game-1"));

        scheduler.forceUpdate();

        assertEquals("riot-match", liveMatch.getRiotEventId());
        verify(liveMonitor).startMonitoring(eq(watchParty), eq("riot-game-1"));
    }

    @Test
    void forceUpdateReportShouldListUpcomingMatches() throws Exception {
        WatchPartyManager manager = new WatchPartyManager();
        LeaguepediaClient apiClient = mock(LeaguepediaClient.class);
        AutoWatchPartyScheduler scheduler = new AutoWatchPartyScheduler(manager, apiClient, null, null);

        WatchParty watchParty = WatchParty.createAutoWatchParty(new User("alice", false), "T1", AutoType.TEAM);
        manager.addAutoWatchParty(watchParty);

        Match nextMatch = new Match(
                "match-2",
                "T1",
                "G2",
                LocalDateTime.now().plusHours(2),
                "MSI 2026",
                "",
                "BO3");

        when(apiClient.fetchUpcomingMatchesForTeam("T1")).thenReturn(List.of(nextMatch));

        String report = scheduler.forceUpdateReport();

        assertTrue(report.contains("[MATCHES]"));
        assertTrue(report.contains("T1 vs G2"));
        assertTrue(report.contains("MSI 2026"));
    }
}
