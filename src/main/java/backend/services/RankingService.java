package backend.services;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import backend.models.User;
import backend.services.WatchPartyManager;

@Service
public class RankingService {

    private final UserService userService;
    private final WatchPartyManager watchPartyManager;

    private CachedRanking globalPublicPoints;
    private CachedRanking globalPublicWins;
    private final Map<String, CachedRanking> watchPartyPoints = new ConcurrentHashMap<>();
    private final Map<String, CachedRanking> watchPartyWins = new ConcurrentHashMap<>();

    public RankingService(UserService userService, WatchPartyManager watchPartyManager) {
        this.userService = userService;
        this.watchPartyManager = watchPartyManager;
    }

    public Map<String, Integer> getGlobalPublicPoints(boolean refresh) {
        if (globalPublicPoints == null || refresh) {
            globalPublicPoints = new CachedRanking(computeGlobalPublicPoints());
        }
        return globalPublicPoints.data;
    }

    public Map<String, Integer> getGlobalPublicWins(boolean refresh) {
        if (globalPublicWins == null || refresh) {
            globalPublicWins = new CachedRanking(computeGlobalPublicWins());
        }
        return globalPublicWins.data;
    }

    public Map<String, Integer> getWatchPartyPoints(String watchPartyName, boolean refresh) {
        if (!watchPartyExists(watchPartyName)) {
            return Map.of();
        }
        CachedRanking cached = watchPartyPoints.get(watchPartyName);
        if (cached == null || refresh) {
            CachedRanking updated = new CachedRanking(computeWatchPartyPoints(watchPartyName));
            watchPartyPoints.put(watchPartyName, updated);
            cached = updated;
        }
        return cached.data;
    }

    public Map<String, Integer> getWatchPartyWins(String watchPartyName, boolean refresh) {
        if (!watchPartyExists(watchPartyName)) {
            return Map.of();
        }
        CachedRanking cached = watchPartyWins.get(watchPartyName);
        if (cached == null || refresh) {
            CachedRanking updated = new CachedRanking(computeWatchPartyWins(watchPartyName));
            watchPartyWins.put(watchPartyName, updated);
            cached = updated;
        }
        return cached.data;
    }

    public void refreshAll() {
        globalPublicPoints = null;
        globalPublicWins = null;
        watchPartyPoints.clear();
        watchPartyWins.clear();
    }

    public void refreshWatchParty(String watchPartyName) {
        watchPartyPoints.remove(watchPartyName);
        watchPartyWins.remove(watchPartyName);
    }

    private boolean watchPartyExists(String name) {
        return watchPartyManager.getWatchPartyByName(name) != null;
    }

    private Map<String, Integer> computeGlobalPublicPoints() {
        return sortDescending(userService.getAllUsers().stream()
            .collect(Collectors.toMap(User::getName, User::getPublicPoints, (a, b) -> a, LinkedHashMap::new)));
    }

    private Map<String, Integer> computeGlobalPublicWins() {
        return sortDescending(userService.getAllUsers().stream()
            .collect(Collectors.toMap(User::getName, User::getPublicWins, (a, b) -> a, LinkedHashMap::new)));
    }

    private Map<String, Integer> computeWatchPartyPoints(String watchPartyName) {
        return sortDescending(userService.getAllUsers().stream()
            .collect(Collectors.toMap(User::getName, u -> u.getPointsForWatchParty(watchPartyName), (a, b) -> a, LinkedHashMap::new)));
    }

    private Map<String, Integer> computeWatchPartyWins(String watchPartyName) {
        return sortDescending(userService.getAllUsers().stream()
            .collect(Collectors.toMap(User::getName, u -> u.getWinsForWatchParty(watchPartyName), (a, b) -> a, LinkedHashMap::new)));
    }

    private Map<String, Integer> sortDescending(Map<String, Integer> input) {
        return input.entrySet().stream()
            .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue,
                (e1, e2) -> e1,
                LinkedHashMap::new
            ));
    }

    private static class CachedRanking {
        private final Map<String, Integer> data;
        private final LocalDateTime lastUpdated;

        CachedRanking(Map<String, Integer> data) {
            this.data = data;
            this.lastUpdated = LocalDateTime.now();
        }

        public LocalDateTime getLastUpdated() {
            return lastUpdated;
        }
    }
}