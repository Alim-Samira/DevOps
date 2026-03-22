package backend.services;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import backend.models.MatchState;
import backend.models.User;
import backend.models.UserNotification;
import backend.models.WatchParty;
import backend.repositories.WatchPartyRepository;

@Service
public class WatchPartyManager {

    private static final Logger log = LoggerFactory.getLogger(WatchPartyManager.class);
    private static final int DEFAULT_WATCH_PARTY_DURATION_HOURS = 2;

    private final WatchPartyRepository watchPartyRepository;
    private final CalendarIntegrationService calendarIntegrationService;
    private final UserService userService;
    private final NotificationService notificationService;
    private AutoWatchPartyScheduler scheduler;
    
    // In-memory fallback lists (for tests/legacy support)
    private final List<WatchParty> watchParties;
    private final List<WatchParty> watchPartiesPlanned;

    @Autowired
    public WatchPartyManager(
            WatchPartyRepository watchPartyRepository,
            CalendarIntegrationService calendarIntegrationService,
            UserService userService,
            NotificationService notificationService) {
        this.watchPartyRepository = watchPartyRepository;
        this.calendarIntegrationService = calendarIntegrationService;
        this.userService = userService;
        this.notificationService = notificationService;
        this.watchParties = new ArrayList<>();
        this.watchPartiesPlanned = new ArrayList<>();
        this.scheduler = new AutoWatchPartyScheduler(this);
    }

    public WatchPartyManager() {
        this(null, new CalendarIntegrationService(), new UserService(null), new NotificationService());
    }

    public void addWatchParty(WatchParty wp) {
        if (watchPartyRepository != null) {
            watchPartyRepository.save(wp);
        } else {
            watchParties.add(wp);
        }
        log.info("WatchParty ajoutée : {}", wp.name());
    }

    public void planifyWatchParty(WatchParty wp) {
        if (wp.date().isAfter(LocalDateTime.now())) {
            watchPartiesPlanned.add(wp);
            wp.planify(); 
        } else {
            log.warn("Impossible de planifier une WatchParty passée : {}", wp.name());
        }
    }

    public List<WatchParty> watchPartiesPlanifiees() {
        return watchPartiesPlanned;
    }

    public void displayAllWatchParties() {
        if (watchParties.isEmpty()) {
            log.info("Aucune WatchParty enregistrée pour le moment.");
        } else {
            log.info("Liste des WatchParties :");
            for (WatchParty wp : watchParties) {
                log.info("- {}", wp.name());
            }
        }
    }
    
    // === Auto Watch Party Methods ===
    
    /**
     * Get all auto watch parties
     */
    public List<WatchParty> getAllAutoWatchParties() {
        if (watchPartyRepository != null) {
            return watchPartyRepository.findAll().stream()
                .filter(WatchParty::isAutoWatchParty)
                .toList();
        }
        List<WatchParty> autoWPs = new ArrayList<>();
        for (WatchParty wp : watchParties) {
            if (wp.isAutoWatchParty()) {
                autoWPs.add(wp);
            }
        }
        return autoWPs;
    }

    public boolean removeWatchParty(String name) {
        if (watchPartyRepository != null) {
            return watchPartyRepository.findByName(name).map(wp -> {
                watchPartyRepository.delete(wp);
                log.info("WatchParty removed: {}", name);
                return true;
            }).orElseGet(() -> {
                log.warn("WatchParty not found: {}", name);
                return false;
            });
        }
        boolean removed = watchParties.removeIf(wp -> wp.name().equals(name));
        if (removed) {
            log.info("WatchParty removed: {}", name);
        } else {
            log.warn("WatchParty not found: {}", name);
        }
        return removed;
    }

    public WatchParty getWatchPartyByName(String name) {
        if (watchPartyRepository != null) {
            return watchPartyRepository.findByName(name).orElse(null);
        }
        return watchParties.stream()
            .filter(wp -> wp.name().equals(name))
            .findFirst()
            .orElse(null);
    }

    public List<WatchParty> getAllWatchParties() {
        if (watchPartyRepository != null) {
            return watchPartyRepository.findAll();
        }
        return new ArrayList<>(watchParties);
    }

    public void addAutoWatchParty(WatchParty wp) {
        if (!wp.isAutoWatchParty()) {
            log.warn("Not an auto watch party: {}", wp.name());
            return;
        }
        addWatchParty(wp);
        log.info("Auto WatchParty added: {}", wp.name());
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

    public void notifyAvailableUsersForPresentiel(WatchParty wp) {
        if (wp == null || wp.date() == null) {
            return;
        }

        LocalDateTime start = wp.date();
        LocalDateTime end = start.plusHours(DEFAULT_WATCH_PARTY_DURATION_HOURS);

        for (User user : wp.getParticipants()) {
            if (user == null) {
                continue;
            }

            if (!calendarIntegrationService.hasConnectedCalendar(user.getName())) {
                continue;
            }

            if (!calendarIntegrationService.canAttendWatchParty(user.getName(), start, end)) {
                continue;
            }

            String message = "Tu es dispo pour la watch party '" + wp.name() + "' le " + start + ". On peut te proposer du presentiel.";
            notificationService.addNotification(
                    user.getName(),
                    new UserNotification("Watch party en presentiel", message, wp.name(), LocalDateTime.now()));
            log.info("Notification envoyee a {} pour {}", user.getName(), wp.name());
        }
    }
}
