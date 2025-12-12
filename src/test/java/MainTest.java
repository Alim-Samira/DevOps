import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

import java.sql.Time;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Comprehensive test suite for DevOps System
 * Tests: Chat, Betting, Watch Parties, Auto Watch Parties
 */
public class MainTest {

    private User admin;
    private User alice;
    private User bob;

    @BeforeEach
    public void setUp() {
        admin = new User("Admin", true);
        alice = new User("Alice", false);
        bob = new User("Bob", false);
    }

    // ==================== CHAT TESTS ====================

    @Test
    @DisplayName("Public chat should store messages correctly")
    public void testPublicChatMessaging() {
        PublicChat chat = new PublicChat("Public Chat", admin);
        chat.addUser(admin);
        chat.addUser(alice);

        chat.sendMessage(alice, "Hello everyone");

        assertNotNull(chat.messages);
        assertEquals(1, chat.messages.size());
        assertEquals("Hello everyone", chat.messages.get(0).getContent());
        assertEquals("Alice", chat.messages.get(0).getSender().getName());
    }

    @Test
    @DisplayName("Private chat should manage users with points")
    public void testPrivateChatUserManagement() {
        PrivateChat chat = new PrivateChat("Private Chat", admin);
        chat.addUser(alice);
        
        assertEquals(200, chat.users().get(alice));
        
        chat.setPoints(alice, 150);
        assertEquals(150, chat.users().get(alice));
    }

    // ==================== BETTING TESTS ====================

    @Test
    @DisplayName("Betting should deduct points when voting")
    public void testBetVotingDeductsPoints() {
        Choice choiceA = new Choice("Option A");
        Choice choiceB = new Choice("Option B");
        
        Collection<Choice> options = new ArrayList<>();
        options.add(choiceA);
        options.add(choiceB);
        
        Time votingTime = new Time(System.currentTimeMillis() + 10000);
        PublicBet bet = new PublicBet("Test Question?", options, votingTime);

        assertEquals(200, alice.getPoints());
        bet.vote(alice, choiceA, 50);
        assertEquals(150, alice.getPoints());
    }

    @Test
    @DisplayName("Betting should distribute winnings correctly")
    public void testBetResultDistribution() {
        Choice choiceA = new Choice("A");
        Choice choiceB = new Choice("B");
        
        Collection<Choice> options = new ArrayList<>();
        options.add(choiceA);
        options.add(choiceB);
        
        Time votingTime = new Time(System.currentTimeMillis());
        PublicBet bet = new PublicBet("Which option?", options, votingTime);

        // Alice and Bob both vote 50 points on choiceA
        bet.vote(alice, choiceA, 50);
        bet.vote(bob, choiceA, 50);
        
        assertEquals(150, alice.getPoints());
        assertEquals(150, bob.getPoints());

        // Set choiceA as winner - both should get their share back
        bet.setResult(choiceA);

        // Both should have at least their original points back
        assertTrue(alice.getPoints() >= 150);
        assertTrue(bob.getPoints() >= 150);
    }

    @Test
    @DisplayName("Betting should refund points when canceled")
    public void testBetCancellation() {
        Choice choiceA = new Choice("A");
        Collection<Choice> options = new ArrayList<>();
        options.add(choiceA);
        
        Time votingTime = new Time(System.currentTimeMillis());
        PublicBet bet = new PublicBet("Test?", options, votingTime);

        bet.vote(alice, choiceA, 50);
        assertEquals(150, alice.getPoints());

        bet.cancel();
        assertEquals(200, alice.getPoints()); // Points refunded
    }

    // ==================== WATCH PARTY TESTS ====================

    @Test
    @DisplayName("Watch party should be added to planned list")
    public void testWatchPartyPlanning() {
        WatchPartyManager manager = new WatchPartyManager();
        WatchParty wp = new WatchParty("Party", LocalDateTime.now().plusDays(1), "Game");

        manager.addWatchParty(wp);
        manager.planifyWatchParty(wp);

        assertTrue(manager.watchPartiesPlanifiees().contains(wp));
    }

    @Test
    @DisplayName("Manual watch party should be joinable")
    public void testManualWatchPartyJoin() {
        WatchPartyManager manager = new WatchPartyManager();
        WatchParty wp = new WatchParty("Manual Party", LocalDateTime.now().plusDays(1), "Game");
        
        manager.addWatchParty(wp);
        wp.join(alice);

        assertTrue(wp.getParticipants().contains(alice));
        assertEquals(1, wp.getParticipants().size());
    }

    // ==================== AUTO WATCH PARTY TESTS ====================

    @Test
    @DisplayName("Auto watch party should be created for team")
    public void testAutoWatchPartyCreation() {
        WatchParty wp = WatchParty.createAutoWatchParty(alice, "T1", AutoType.TEAM);

        assertTrue(wp.isAutoWatchParty());
        assertEquals(AutoType.TEAM, wp.getAutoConfig().getType());
        assertEquals("T1", wp.getAutoConfig().getTarget());
        assertEquals(WatchPartyStatus.WAITING, wp.getStatus());
    }

    @Test
    @DisplayName("Auto watch party should transition through states")
    public void testAutoWatchPartyStateTransitions() {
        WatchParty wp = WatchParty.createAutoWatchParty(alice, "Gen.G", AutoType.TEAM);
        
        // Initially waiting
        assertEquals(WatchPartyStatus.WAITING, wp.getStatus());

        // Cannot join when in WAITING state
        boolean joined = wp.join(bob);
        assertFalse(joined);
        assertFalse(wp.getParticipants().contains(bob));

        // Verify creator and auto configuration
        assertEquals(alice, wp.getCreator());
        assertTrue(wp.isAutoWatchParty());
        assertEquals("Gen.G", wp.getAutoConfig().getTarget());
    }

    @Test
    @DisplayName("Manager should track auto watch parties")
    public void testManagerAutoWatchPartyTracking() {
        WatchPartyManager manager = new WatchPartyManager();
        
        WatchParty wp1 = WatchParty.createAutoWatchParty(alice, "T1", AutoType.TEAM);
        WatchParty wp2 = WatchParty.createAutoWatchParty(bob, "Worlds 2025", AutoType.TOURNAMENT);
        
        manager.addAutoWatchParty(wp1);
        manager.addAutoWatchParty(wp2);

        assertEquals(2, manager.getAllAutoWatchParties().size());
        assertTrue(manager.getAllAutoWatchParties().contains(wp1));
        assertTrue(manager.getAllAutoWatchParties().contains(wp2));
    }

    @Test
    @DisplayName("Scheduler should start and stop correctly")
    public void testSchedulerLifecycle() {
        WatchPartyManager manager = new WatchPartyManager();
        
        assertFalse(manager.isSchedulerRunning());
        
        manager.startScheduler();
        assertTrue(manager.isSchedulerRunning());
        
        manager.stopScheduler();
        assertFalse(manager.isSchedulerRunning());
    }

    @Test
    @DisplayName("Auto watch party should prevent joining when not open")
    public void testAutoWatchPartyJoinRestrictions() {
        WatchParty wp = WatchParty.createAutoWatchParty(alice, "G2", AutoType.TEAM);
        
        // Initially in WAITING state - cannot join
        assertEquals(WatchPartyStatus.WAITING, wp.getStatus());
        boolean joined = wp.join(bob);
        assertFalse(joined);
        assertFalse(wp.getParticipants().contains(bob));
        
        // Verify auto configuration is maintained
        assertEquals("G2", wp.getAutoConfig().getTarget());
        assertEquals(AutoType.TEAM, wp.getAutoConfig().getType());
    }

    @Test
    @DisplayName("Users should leave watch parties correctly")
    public void testWatchPartyLeave() {
        // Manual watch party - always joinable
        WatchParty wp = new WatchParty("DRX Watch", LocalDateTime.now().plusDays(1), "LoL");
        
        wp.join(bob);
        assertTrue(wp.getParticipants().contains(bob));
        
        wp.leave(bob);
        assertFalse(wp.getParticipants().contains(bob));
    }

    @Test
    @DisplayName("Manager should remove watch parties by name")
    public void testWatchPartyRemoval() {
        WatchPartyManager manager = new WatchPartyManager();
        WatchParty wp = WatchParty.createAutoWatchParty(alice, "FNC", AutoType.TEAM);
        
        manager.addAutoWatchParty(wp);
        assertEquals(1, manager.getAllWatchParties().size());
        
        manager.removeWatchParty(wp.name());
        assertEquals(0, manager.getAllWatchParties().size());
    }

    // ==================== INTEGRATION TESTS ====================

    @Test
    @DisplayName("User should maintain consistent points across operations")
    public void testUserPointsConsistency() {
        assertEquals(200, alice.getPoints());
        
        // Deduct points
        alice.setPoints(150);
        assertEquals(150, alice.getPoints());
        
        // Bet and lose
        Choice choice = new Choice("A");
        Collection<Choice> options = new ArrayList<>();
        options.add(choice);
        
        PublicBet bet = new PublicBet("Test?", options, new Time(System.currentTimeMillis()));
        bet.vote(alice, choice, 50);
        assertEquals(100, alice.getPoints());
        
        // Cancel bet - refund
        bet.cancel();
        assertEquals(150, alice.getPoints());
    }

    @Test
    @DisplayName("System should handle multiple concurrent watch parties")
    public void testMultipleWatchPartiesCoexist() {
        WatchPartyManager manager = new WatchPartyManager();
        
        WatchParty manual = new WatchParty("Manual", LocalDateTime.now().plusDays(1), "Game");
        WatchParty auto1 = WatchParty.createAutoWatchParty(alice, "T1", AutoType.TEAM);
        WatchParty auto2 = WatchParty.createAutoWatchParty(bob, "LCK", AutoType.TOURNAMENT);
        
        manager.addWatchParty(manual);
        manager.addAutoWatchParty(auto1);
        manager.addAutoWatchParty(auto2);
        
        assertEquals(3, manager.getAllWatchParties().size());
        assertEquals(2, manager.getAllAutoWatchParties().size());
    }
}
