package backend.integration.lolesports;

import backend.models.WatchParty;
import backend.services.BetService;
import backend.integration.lolesports.dto.Frame;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
public class LiveMatchMonitorService {

    private final LolEsportsClient client;
    private final BetService betService;
    private final WatchPartyManager manager; // ton manager existant

    // gameId → WatchParty (pour savoir quel pari régler)
    private final Map<String, WatchParty> activeMonitors = new ConcurrentHashMap<>();
    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(5);

    public LiveMatchMonitorService(LolEsportsClient client, BetService betService, WatchPartyManager manager) {
        this.client = client;
        this.betService = betService;
        this.manager = manager;
    }

    // Appelé par ton AutoWatchPartyScheduler quand un match passe "inProgress"
    public void startMonitoring(WatchParty wp, String gameId) {
        if (activeMonitors.containsKey(gameId)) return;

        wp.setCurrentRiotGameId(gameId);
        activeMonitors.put(gameId, wp);

        executor.scheduleAtFixedRate(() -> pollAndResolve(gameId), 0, 8, TimeUnit.SECONDS);
    }

    private void pollAndResolve(String gameId) {
        try {
            WindowResponse window = client.getWindow(gameId);
            if (window.frames().isEmpty()) return;

            Frame latest = window.frames().get(window.frames().size() - 1);
            WatchParty wp = activeMonitors.get(gameId);

            betService.tryAutoResolveLiveBet(wp, latest);

            // Optionnel : détecter fin de match → stop polling
            if (isGameFinished(latest)) {
                stopMonitoring(gameId);
            }
        } catch (Exception e) {
            // log silencieux
        }
    }

    public void stopMonitoring(String gameId) {
        activeMonitors.remove(gameId);
        // tu peux annuler la task si besoin
    }

    private boolean isGameFinished(Frame frame) {
        // logique selon tes events (GAME_END ou gold = 0 etc.)
        return false;
    }
}