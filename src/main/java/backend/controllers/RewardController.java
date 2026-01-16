package backend.controllers;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import backend.services.RewardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/rewards")
@Tag(name = "Rewards", description = "Récompenses par paliers et top mensuel")
public class RewardController {

    private final RewardService rewardService;

    public RewardController(RewardService rewardService) {
        this.rewardService = rewardService;
    }

    @Operation(summary = "Lister les paliers de points publics")
    @GetMapping("/thresholds")
    public List<Integer> getThresholds() {
        return rewardService.getThresholds();
    }

    @Operation(summary = "Forcer l'évaluation des paliers")
    @PostMapping("/thresholds/run")
    public List<String> runThresholds() {
        return rewardService.evaluateThresholdRewards();
    }

    @Operation(summary = "Forcer le calcul du top 3 mensuel (public points & wins)")
    @PostMapping("/monthly/run")
    public List<String> runMonthly() {
        return rewardService.computeMonthlyTop3();
    }
}
