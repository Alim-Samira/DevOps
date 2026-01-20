package backend.services;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

@Service
public class RewardScheduler {

    private static final Logger log = Logger.getLogger(RewardScheduler.class.getName());

    private final RewardService rewardService;
    private final ScheduledExecutorService scheduler;

    public RewardScheduler(RewardService rewardService) {
        this.rewardService = rewardService;
        this.scheduler = Executors.newScheduledThreadPool(1);
    }

    @PostConstruct
    public void start() {
        // Run daily
        scheduler.scheduleAtFixedRate(this::runDailySafe, 0, 24, TimeUnit.HOURS);
    }

    @PreDestroy
    public void stop() {
        scheduler.shutdownNow();
    }

    private void runDailySafe() {
        try {
            List<String> msgs = rewardService.runDailyJob();
            if (!msgs.isEmpty()) {
                msgs.forEach(m -> log.info("[Rewards] " + m));
            } else {
                log.fine("[Rewards] Daily job executed, no rewards granted");
            }
        } catch (Exception e) {
            log.warning("[Rewards] Daily job failed: " + e.getMessage());
        }
    }
}
