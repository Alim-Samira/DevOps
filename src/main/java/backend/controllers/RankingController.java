package backend.controllers;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import backend.services.RankingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api")
@Tag(name = "Ranking / Leaderboard", description = "Classements points & victoires (public & par watchparty)")
public class RankingController {

    private final RankingService rankingService;

    public RankingController(RankingService rankingService) {
        this.rankingService = rankingService;
    }

    @Operation(summary = "Classement global public par points")
    @GetMapping("/rankings/public/points")
    public Map<String, Integer> getGlobalPublicPoints(@RequestParam(name = "refresh", defaultValue = "false") boolean refresh) {
        return rankingService.getGlobalPublicPoints(refresh);
    }

    @Operation(summary = "Classement global public par victoires")
    @GetMapping("/rankings/public/wins")
    public Map<String, Integer> getGlobalPublicWins(@RequestParam(name = "refresh", defaultValue = "false") boolean refresh) {
        return rankingService.getGlobalPublicWins(refresh);
    }

    @Operation(summary = "Classement par points d'une watchparty (public ou privée)")
    @GetMapping("/watchparties/{name}/rankings/points")
    public Map<String, Integer> getWatchPartyPoints(@PathVariable String name,
                                                    @RequestParam(name = "refresh", defaultValue = "false") boolean refresh) {
        return rankingService.getWatchPartyPoints(name, refresh);
    }

    @Operation(summary = "Classement par victoires d'une watchparty (public ou privée)")
    @GetMapping("/watchparties/{name}/rankings/wins")
    public Map<String, Integer> getWatchPartyWins(@PathVariable String name,
                                                  @RequestParam(name = "refresh", defaultValue = "false") boolean refresh) {
        return rankingService.getWatchPartyWins(name, refresh);
    }

    @Operation(summary = "Rafraîchir tous les caches de classement")
    @PostMapping("/rankings/refresh")
    public String refreshAll() {
        rankingService.refreshAll();
        return "✅ Caches de classement rafraîchis";
    }

    @Operation(summary = "Rafraîchir les caches d'une watchparty")
    @PostMapping("/watchparties/{name}/rankings/refresh")
    public String refreshWatchParty(@PathVariable String name) {
        rankingService.refreshWatchParty(name);
        return "✅ Cache de classement rafraîchi";
    }
}