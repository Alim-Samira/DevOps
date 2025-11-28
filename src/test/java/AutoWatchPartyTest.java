import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Tests for auto watch party functionality
 */
public class AutoWatchPartyTest {
    
    private WatchPartyManager manager;
    private User admin;
    private User alice;
    private LeaguepediaClient apiClient;
    
    @BeforeEach
    public void setUp() {
        manager = new WatchPartyManager();
        admin = new User("Admin", true);
        alice = new User("Alice", false);
        apiClient = new LeaguepediaClient();
    }
    
    @Test
    public void testCreateAutoWatchPartyForTeam() {
        WatchParty wp = WatchParty.createAutoWatchParty(admin, "T1", AutoType.TEAM);
        
        assertTrue(wp.isAutoWatchParty());
        assertEquals("Auto WP: Team T1", wp.name());
        assertEquals(WatchPartyStatus.WAITING, wp.getStatus());
        assertNotNull(wp.getAutoConfig());
        assertEquals(AutoType.TEAM, wp.getAutoConfig().getType());
        assertEquals("T1", wp.getAutoConfig().getTarget());
    }
    
    @Test
    public void testCreateAutoWatchPartyForTournament() {
        WatchParty wp = WatchParty.createAutoWatchParty(admin, "Worlds 2025", AutoType.TOURNAMENT);
        
        assertTrue(wp.isAutoWatchParty());
        assertEquals("Auto WP: Worlds 2025", wp.name());
        assertEquals(AutoType.TOURNAMENT, wp.getAutoConfig().getType());
    }
    
    @Test
    public void testLeaguepediaClientFetchTeamMatch() {
        Match match = apiClient.getNextTeamMatch("T1");
        
        assertNotNull(match);
        assertTrue(match.getTeam1().equals("T1") || match.getTeam2().equals("T1"));
        assertEquals(MatchStatus.SCHEDULED, match.getStatus());
    }
    
    @Test
    public void testLeaguepediaClientFetchTournamentMatch() {
        Match match = apiClient.getNextTournamentMatch("Worlds 2025");
        
        assertNotNull(match);
        assertEquals("Worlds 2025", match.getTournament());
    }
    
    @Test
    public void testMatchIsStartingSoon() {
        LocalDateTime soon = LocalDateTime.now().plusMinutes(25);
        Match match = new Match("test", "T1", "Gen.G", soon, "Test", "http://test", "BO5");
        
        assertTrue(match.isStartingSoon(30)); // Within 30 minutes
    }
    
    @Test
    public void testMatchNotStartingSoon() {
        LocalDateTime later = LocalDateTime.now().plusHours(2);
        Match match = new Match("test", "T1", "Gen.G", later, "Test", "http://test", "BO5");
        
        assertFalse(match.isStartingSoon(30)); // More than 30 minutes away
    }
    
    @Test
    public void testWatchPartyStatusTransition() {
        WatchParty wp = WatchParty.createAutoWatchParty(admin, "T1", AutoType.TEAM);
        wp.join(admin);
        
        // Initially WAITING
        assertEquals(WatchPartyStatus.WAITING, wp.getStatus());
        
        // Match starting soon should open it
        LocalDateTime soon = LocalDateTime.now().plusMinutes(25);
        Match upcomingMatch = new Match("test", "T1", "Gen.G", soon, "Test", "http://test", "BO5");
        wp.updateStatus(upcomingMatch);
        
        assertEquals(WatchPartyStatus.OPEN, wp.getStatus());
        
        // Finished match should close it
        upcomingMatch.setStatus(MatchStatus.FINISHED);
        wp.updateStatus(upcomingMatch);
        
        assertEquals(WatchPartyStatus.CLOSED, wp.getStatus());
    }
    
    @Test
    public void testCannotJoinClosedAutoWatchParty() {
        WatchParty wp = WatchParty.createAutoWatchParty(admin, "T1", AutoType.TEAM);
        
        // Status is WAITING, should not be able to join
        assertFalse(wp.join(alice));
    }
    
    @Test
    public void testCanJoinOpenAutoWatchParty() {
        WatchParty wp = WatchParty.createAutoWatchParty(admin, "T1", AutoType.TEAM);
        
        // Simulate opening the watch party
        LocalDateTime soon = LocalDateTime.now().plusMinutes(25);
        Match upcomingMatch = new Match("test", "T1", "Gen.G", soon, "Test", "http://test", "BO5");
        wp.updateStatus(upcomingMatch);
        
        // Now it should be OPEN and joinable
        assertEquals(WatchPartyStatus.OPEN, wp.getStatus());
        assertTrue(wp.join(alice));
    }
    
    @Test
    public void testManagerGetAutoWatchParties() {
        WatchParty autoWP1 = WatchParty.createAutoWatchParty(admin, "T1", AutoType.TEAM);
        WatchParty autoWP2 = WatchParty.createAutoWatchParty(admin, "Worlds 2025", AutoType.TOURNAMENT);
        WatchParty manualWP = new WatchParty("Manual WP", LocalDateTime.now().plusDays(1), "LOL");
        
        manager.addAutoWatchParty(autoWP1);
        manager.addAutoWatchParty(autoWP2);
        manager.addWatchParty(manualWP);
        
        List<WatchParty> autoParties = manager.getAllAutoWatchParties();
        
        assertEquals(2, autoParties.size());
        assertTrue(autoParties.stream().allMatch(WatchParty::isAutoWatchParty));
    }
    
    @Test
    public void testSchedulerStartStop() {
        assertFalse(manager.isSchedulerRunning());
        
        manager.startScheduler();
        assertTrue(manager.isSchedulerRunning());
        
        manager.stopScheduler();
        assertFalse(manager.isSchedulerRunning());
    }
}
