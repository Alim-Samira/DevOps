package backend.controllers;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import backend.services.RankingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/ranking")
@Tag(name = "Ranking / Leaderboard", description = "View user rankings by points")
public class RankingController {

    private final RankingService rankingService;

    public RankingController(RankingService rankingService) {
        this.rankingService = rankingService;
    }

    @Operation(summary = "Get simple ranking", description = "Returns a map of username -> points, sorted by points descending")
    @GetMapping
    public Map<String, Integer> getRanking() {
        return rankingService.getRanking();
    }

    @Operation(summary = "Get detailed ranking", description = "Returns a list with username, points, and roles for each user")
    @GetMapping("/detailed")
    public List<Map<String, Object>> getDetailedRanking() {
        return rankingService.getRankingWithDetails();
    }
}