package backend.services;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
    private static final String WATCH_PARTY_REMOVED_LOG = "WatchParty removed";
    private static final String WATCH_PARTY_NOT_FOUND_LOG = "WatchParty not found";

    private final WatchPartyRepository watchPartyRepository;
    private final CalendarIntegrationService calendarIntegrationService;
    private final NotificationService notificationService;
    private AutoWatchPartyScheduler scheduler;

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
        this.notificationService = notificationService;
        this.watchParties = new ArrayList<>();
        this.watchPartiesPlanned = new ArrayList<>();
        this.scheduler = new AutoWatchPartyScheduler(this);
    }

    public WatchPartyManager() {
        this(null, new CalendarIntegrationService(), new UserService(null), new NotificationService());
    }

    public void setScheduler(AutoWatchPartyScheduler scheduler) {
        if (scheduler != null) {
            this.scheduler = scheduler;
        }
    }

    public void addWatchParty(WatchParty wp) {
        if (wp == null) {
            return;
        }

        replaceInMemoryWatchParty(wp);

        if (watchPartyRepository != null) {
            watchPartyRepository.findAll().stream()
                .filter(existing -> existing.getName().equals(wp.getName()))
                .filter(existing -> wp.getId() == null || !wp.getId().equals(existing.getId()))
                .forEach(watchPartyRepository::delete);
            watchPartyRepository.save(wp);
        }

        log.info("WatchParty ajoutee : {}", wp.name());
    }

    public void saveWatchParty(WatchParty wp) {
        if (wp == null) {
            return;
        }

        replaceInMemoryWatchParty(wp);
        if (watchPartyRepository != null) {
            watchPartyRepository.save(wp);
        }
    }

    public void planifyWatchParty(WatchParty wp) {
        if (wp.date().isAfter(LocalDateTime.now())) {
            if (!watchPartiesPlanned.contains(wp)) {
                watchPartiesPlanned.add(wp);
            }
            wp.planify();
            saveWatchParty(wp);
            notifyAvailableUsersForPresentiel(wp);
        } else {
            log.warn("Impossible de planifier une WatchParty passee : {}", wp.name());
        }
    }

    public List<WatchParty> watchPartiesPlanifiees() {
        return watchPartiesPlanned;
    }

    public void displayAllWatchParties() {
        List<WatchParty> all = getAllWatchParties();
        if (all.isEmpty()) {
            log.info("Aucune WatchParty enregistree pour le moment.");
        } else {
            log.info("Liste des WatchParties :");
            for (WatchParty wp : all) {
                log.info("- {}", wp.name());
            }
        }
    }

    public List<WatchParty> getAllAutoWatchParties() {
        return getAllWatchParties().stream()
            .filter(WatchParty::isAutoWatchParty)
            .toList();
    }

    public void addAutoWatchParty(WatchParty wp) {
        if (!wp.isAutoWatchParty()) {
            log.warn("Not an auto watch party: {}", wp.name());
            return;
        }
        addWatchParty(wp);
        log.info("Auto WatchParty added: {}", wp.name());
    }

    public boolean removeWatchParty(String name) {
        boolean removedInMemory = watchParties.removeIf(wp -> wp.getName().equals(name));
        watchPartiesPlanned.removeIf(wp -> wp.getName().equals(name));

        boolean removedInRepository = false;
        if (watchPartyRepository != null) {
            List<WatchParty> matches = watchPartyRepository.findAll().stream()
                .filter(wp -> wp.getName().equals(name))
                .toList();
            matches.forEach(watchPartyRepository::delete);
            removedInRepository = !matches.isEmpty();
        }

        boolean removed = removedInMemory || removedInRepository;
        if (removed) {
            log.info(WATCH_PARTY_REMOVED_LOG);
        } else {
            log.warn(WATCH_PARTY_NOT_FOUND_LOG);
        }
        return removed;
    }

    public WatchParty getWatchPartyByName(String name) {
        WatchParty inMemory = watchParties.stream()
            .filter(wp -> wp.getName().equals(name))
            .findFirst()
            .orElse(null);
        if (inMemory != null) {
            return inMemory;
        }

        if (watchPartyRepository == null) {
            return null;
        }

        WatchParty fromRepository = watchPartyRepository.findAll().stream()
            .filter(wp -> wp.getName().equals(name))
            .reduce((first, second) -> second)
            .orElse(null);

        if (fromRepository != null) {
            replaceInMemoryWatchParty(fromRepository);
        }
        return fromRepository;
    }

    public List<WatchParty> getAllWatchParties() {
        Map<String, WatchParty> merged = new LinkedHashMap<>();
        for (WatchParty wp : watchParties) {
            merged.put(wp.getName(), wp);
        }
        if (watchPartyRepository != null) {
            for (WatchParty wp : watchPartyRepository.findAll()) {
                merged.putIfAbsent(wp.getName(), wp);
            }
        }
        return new ArrayList<>(merged.values());
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
        saveWatchParty(wp);
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
            if (shouldNotifyUserForPresentiel(user, start, end)) {
                String message = "Tu es dispo pour la watch party '" + wp.name() + "' le " + start
                        + ". On peut te proposer du presentiel.";
                notificationService.addNotification(
                        user.getName(),
                        new UserNotification("Watch party en presentiel", message, wp.name(), LocalDateTime.now()));
                log.info("Notification envoyee pour une watch party en presentiel");
            }
        }
    }

    private boolean shouldNotifyUserForPresentiel(User user, LocalDateTime start, LocalDateTime end) {
        return user != null
                && calendarIntegrationService.hasConnectedCalendar(user.getName())
                && calendarIntegrationService.canAttendWatchParty(user.getName(), start, end);
    }

    private void replaceInMemoryWatchParty(WatchParty wp) {
        watchParties.removeIf(existing -> existing.getName().equals(wp.getName()));
        watchParties.add(wp);
    }
}
