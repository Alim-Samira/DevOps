package backend.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;

import backend.services.RewardService;

class RewardControllerTest {

    @Test
    void rewardEndpointsShouldDelegateToService() {
        RewardService rewardService = mock(RewardService.class);
        RewardController controller = new RewardController(rewardService);

        when(rewardService.getThresholds()).thenReturn(List.of(1000, 2500));
        when(rewardService.evaluateThresholdRewards()).thenReturn(List.of("threshold reward"));
        when(rewardService.computeMonthlyTop3()).thenReturn(List.of("monthly reward"));

        assertEquals(List.of(1000, 2500), controller.getThresholds());
        assertEquals(List.of("threshold reward"), controller.runThresholds());
        assertEquals(List.of("monthly reward"), controller.runMonthly());
    }
}
