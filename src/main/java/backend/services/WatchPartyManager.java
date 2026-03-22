package backend.services;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import backend.models.MatchState;
import backend.models.WatchParty;
import backend.repositories.WatchPartyRepository;

@Service
public class WatchPartyManager {

    private static final Logger log = LoggerFactory.getLogger(WatchPartyManager.class);

    private final WatchPartyRepository watchPartyRepository;
    private AutoWatchPartyScheduler scheduler;

    public WatchPartyManager(WatchPartyRepository watchPartyRepository) {
        this.watchPartyRepository = watchPartyRepository;
        this.scheduler = new AutoWatchPartyScheduler(this);
    }

    public void addWatchParty(WatchParty wp) {
        watchPartyRepository.save(wp);
        log.info("WatchParty added: {}", wp.name());
    }

    public void addAutoWatchParty(WatchParty wp) {
        if (!wp.isAutoWatchParty()) {
            log.warn("Not an auto watch party: {}", wp.name());
            return;
        }
        watchPartyRepository.save(wp);
        log.info("Auto WatchParty added: {}", wp.name());
    }

    public boolean removeWatchParty(String name) {
        return watchPartyRepository.findByName(name).map(wp -> {
            watchPartyRepository.delete(wp);
            log.info("WatchParty removed: {}", name);
            return true;
        }).orElseGet(() -> {
            log.warn("WatchParty not found: {}", name);
            return false;
        });
    }

    public WatchParty getWatchPartyByName(String name) {
        return watchPartyRepository.findByName(name).orElse(null);
    }

    public List<WatchParty> getAllWatchParties() {
        return watchPartyRepository.findAll();
    }

    public List<WatchParty> getAllAutoWatchParties() {
        return watchPartyRepository.findAll().stream()
            .filter(WatchParty::isAutoWatchParty)
            .toList();
    }

    public void startScheduler() {
        scheduler.start();
    }

    public void stopScheduler() {
        scheduler.stop();
    }

    public void forceSchedulerUpdate() {
        scheduler.forceUpdate();
    }

    public String forceSchedulerUpdateReport() {
        return scheduler.forceUpdateReport();
    }

    public boolean isSchedulerRunning() {
        return scheduler.isRunning();
    }

    public void changeMatchState(WatchParty wp, MatchState newState, boolean isAdmin) {
        if (!isAdmin) {
            log.warn("Admin required to change match state.");
            return;
        }
        if (wp == null) {
            log.warn("WatchParty is null.");
            return;
        }
        if (wp.matchState() == MatchState.FINISHED) {
            log.warn("Match already finished for: {}", wp.name());
            return;
        }
        wp.setMatchState(newState);
        watchPartyRepository.save(wp);
        log.info("Match state of '{}' changed to: {}", wp.name(), newState);
    }

    public void requestMiniGameLaunch(WatchParty wp, boolean isAdmin) {
        if (!isAdmin) {
            log.warn("Admin required to launch a mini-game.");
            return;
        }
        if (wp == null) {
            log.warn("WatchParty is null.");
            return;
        }
        if (!wp.canLaunchMiniGame()) {
            log.warn("Cannot launch mini-game, current state: {}", wp.matchState());
            return;
        }
        log.info("Mini-game launch authorised for: {}", wp.name());
    }
}
