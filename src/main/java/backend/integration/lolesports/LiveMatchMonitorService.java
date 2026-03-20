package backend.integration.lolesports;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import backend.integration.lolesports.dto.Frame;
import backend.integration.lolesports.dto.WindowResponse;
import backend.models.WatchParty;
import backend.services.BetService;
import backend.services.WatchPartyManager;

@Service
public class LiveMatchMonitorService {

    private static final Logger log = LoggerFactory.getLogger(LiveMatchMonitorService.class);

    private final LolEsportsClient client;
    private final BetService betService;
    private final WatchPartyManager manager;

    private final Map<String, WatchParty> activeMonitors = new ConcurrentHashMap<>();
    private final Map<String, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();
    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(5);

    public LiveMatchMonitorService(LolEsportsClient client, BetService betService, WatchPartyManager manager) {
        this.client = client;
        this.betService = betService;
        this.manager = manager;
    }

    public void startMonitoring(WatchParty wp, String gameId) {
        if (wp == null || gameId == null || gameId.isBlank() || activeMonitors.containsKey(gameId)) {
            return;
        }

        wp.setCurrentRiotGameId(gameId);
        activeMonitors.put(gameId, wp);

        ScheduledFuture<?> task = executor.scheduleAtFixedRate(() -> pollAndResolve(gameId), 0, 8, TimeUnit.SECONDS);
        scheduledTasks.put(gameId, task);
    }

    private void pollAndResolve(String gameId) {
        try {
            WatchParty wp = activeMonitors.get(gameId);
            if (wp == null || manager.getWatchPartyByName(wp.getName()) == null) {
                stopMonitoring(gameId);
                return;
            }

            WindowResponse window = client.getWindow(gameId);
            if (window.frames().isEmpty()) {
                return;
            }

            Frame latest = window.frames().get(window.frames().size() - 1);
            wp.setLastFrameProcessed(LocalDateTime.now());
            betService.tryAutoResolveLiveBet(wp, latest);

            if (isGameFinished(latest)) {
                stopMonitoring(gameId);
            }
        } catch (Exception e) {
            log.debug("Unable to poll live game {}", gameId, e);
        }
    }

    public void stopMonitoring(String gameId) {
        WatchParty wp = activeMonitors.remove(gameId);
        ScheduledFuture<?> task = scheduledTasks.remove(gameId);
        if (task != null) {
            task.cancel(false);
        }
        if (wp != null && gameId.equals(wp.getCurrentRiotGameId())) {
            wp.setCurrentRiotGameId(null);
            wp.setLastFrameProcessed(null);
        }
    }

    private boolean isGameFinished(Frame frame) {
        return frame.events().stream().anyMatch(event -> "GAME_END".equalsIgnoreCase(event.type()));
    }
}
