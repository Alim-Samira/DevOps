package backend.services;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import org.springframework.stereotype.Service;

@Service
public class RewardService {

    private static final Logger log = Logger.getLogger(RewardService.class.getName());

    private static final List<Integer> THRESHOLDS = List.of(1000, 2500, 5000, 10000, 20000, 50000, 100000);

    private final UserService userService;
    private final RankingService rankingService;

    // Tracks thresholds already granted per user to avoid duplicate rewards in memory
    private final Map<String, Set<Integer>> awardedThresholds = new ConcurrentHashMap<>();

    public RewardService(UserService userService, RankingService rankingService) {
        this.userService = userService;
        this.rankingService = rankingService;
    }

    public List<Integer> getThresholds() {
        return THRESHOLDS;
    }

    /**
     * Evaluate public point thresholds for all users and grant rewards (placeholder logging).
     * Uses the sum of points in all public watch parties (same calculation as global ranking).
     */
    public List<String> evaluateThresholdRewards() {
        List<String> rewards = new ArrayList<>();
        // Get global points (sum of all public WP points) from ranking service
        Map<String, Integer> globalPoints = rankingService.getGlobalPublicPoints(true);
        
        for (Map.Entry<String, Integer> entry : globalPoints.entrySet()) {
            String username = entry.getKey();
            int points = entry.getValue();
            Set<Integer> granted = awardedThresholds.computeIfAbsent(username, k -> ConcurrentHashMap.newKeySet());
            for (int threshold : THRESHOLDS) {
                if (points >= threshold && !granted.contains(threshold)) {
                    granted.add(threshold);
                    String msg = "🎁 Reward threshold reached: " + username + " -> " + threshold + " pts (global public)";
                    rewards.add(msg);
                    log.info(msg);
                }
            }
        }
        return rewards;
    }

    /**
     * Compute monthly top 3 (public points and wins) and grant placeholder rewards.
     * Runs typically once per month (scheduler checks day-of-month == 1).
     */
    public List<String> computeMonthlyTop3() {
        rankingService.refreshAll();
        Map<String, Integer> points = rankingService.getGlobalPublicPoints(true);
        Map<String, Integer> wins = rankingService.getGlobalPublicWins(true);

        List<String> messages = new ArrayList<>();
        messages.addAll(awardTop3("points", points));
        messages.addAll(awardTop3("wins", wins));
        return messages;
    }

    private List<String> awardTop3(String category, Map<String, Integer> ranking) {
        if (ranking == null || ranking.isEmpty()) return Collections.emptyList();
        List<Map.Entry<String, Integer>> top = ranking.entrySet().stream().limit(3).toList();
        List<String> msgs = new ArrayList<>();
        for (int i = 0; i < top.size(); i++) {
            Map.Entry<String, Integer> entry = top.get(i);
            String msg = String.format("🏅 [%s] #%d %s (%d)", category, i + 1, entry.getKey(), entry.getValue());
            msgs.add(msg);
            log.info(() -> msg + " — reward placeholder (color/title TBD)");
        }
        return msgs;
    }

    /**
     * Daily job: refresh ranking caches and thresholds; run monthly awards on day 1.
     */
    public List<String> runDailyJob() {
        rankingService.refreshAll();
        List<String> output = new ArrayList<>();
        output.addAll(evaluateThresholdRewards());
        if (LocalDate.now().getDayOfMonth() == 1) {
            output.addAll(computeMonthlyTop3());
        }
        return output;
    }
}
