package backend;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Time;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;

import backend.models.AutoType;
import backend.models.Choice;
import backend.models.PublicBet;
import backend.models.PublicChat;
import backend.models.QuizGame;
import backend.models.User;
import backend.models.WatchParty;
import backend.models.WatchPartyStatus;
import backend.services.WatchPartyManager;

/**
 * Comprehensive test suite for DevOps System
 * Tests:  Chat, Betting, Watch Parties, Auto Watch Parties, Quiz
 */
class MainTest {

    private User admin;
    private User alice;
    private User bob;

    @BeforeEach
    void setUp() {
        admin = new User("Admin", true);
        alice = new User("Alice", false);
        bob = new User("Bob", false);
    }

    // ==================== CHAT TESTS ====================

    @Test
    @DisplayName("Public chat should store messages correctly")
    void testPublicChatMessaging() {
        PublicChat chat = new PublicChat("Public Chat", admin);

        chat.sendMessage(alice, "Hello everyone");

        assertNotNull(chat.getMessages());
        assertEquals(1, chat.getMessages().size());
        assertEquals("Hello everyone", chat.getMessages().get(0).getContent());
        assertEquals("Alice", chat.getMessages().get(0).getSender().getName());
    }

    // ==================== BETTING TESTS ====================

    @Test
    @DisplayName("Betting should deduct points when voting")
    void testBetVotingDeductsPoints() {
        Choice choiceA = new Choice("Option A");
        Choice choiceB = new Choice("Option B");
        
        Collection<Choice> options = new ArrayList<>();
        options.add(choiceA);
        options.add(choiceB);
        
        Time votingTime = new Time(System.currentTimeMillis() + 10000);
        PublicBet bet = new PublicBet("Test Question? ", options, votingTime);

        assertEquals(200, alice.getPoints());
        bet.vote(alice, choiceA, 50);
        assertEquals(150, alice. getPoints());
    }

    @Test
    @DisplayName("Betting should distribute winnings correctly")
    void testBetResultDistribution() {
        Choice choiceA = new Choice("A");
        Choice choiceB = new Choice("B");
        
        Collection<Choice> options = new ArrayList<>();
        options.add(choiceA);
        options.add(choiceB);
        
        Time votingTime = new Time(System. currentTimeMillis());
        PublicBet bet = new PublicBet("Which option?", options, votingTime);

        bet.vote(alice, choiceA, 50);
        bet.vote(bob, choiceA, 50);
        
        assertEquals(150, alice.getPoints());
        assertEquals(150, bob.getPoints());

        bet.setResult(choiceA);

        assertTrue(alice.getPoints() >= 150);
        assertTrue(bob.getPoints() >= 150);
    }

    @Test
    @DisplayName("Betting should refund points when canceled")
    void testBetCancellation() {
        Choice choiceA = new Choice("A");
        Collection<Choice> options = new ArrayList<>();
        options.add(choiceA);
        
        Time votingTime = new Time(System.currentTimeMillis());
        PublicBet bet = new PublicBet("Test? ", options, votingTime);

        bet.vote(alice, choiceA, 50);
        assertEquals(150, alice. getPoints());

        bet.cancel();
        assertEquals(200, alice.getPoints());
    }

    // ==================== WATCH PARTY TESTS ====================

    @Test
    @DisplayName("Watch party should be added to planned list")
    void testWatchPartyPlanning() {
        WatchPartyManager manager = new WatchPartyManager();
        WatchParty wp = new WatchParty("Party", LocalDateTime.now().plusDays(1), "Game");

        manager.addWatchParty(wp);
        manager.planifyWatchParty(wp);

        assertTrue(manager.watchPartiesPlanifiees().contains(wp));
    }

    @Test
    @DisplayName("Manual watch party should be joinable")
    void testManualWatchPartyJoin() {
        WatchPartyManager manager = new WatchPartyManager();
        WatchParty wp = new WatchParty("Manual Party", LocalDateTime.now().plusDays(1), "Game");
        
        manager.addWatchParty(wp);
        wp.join(alice);

        assertTrue(wp. getParticipants().contains(alice));
        assertEquals(1, wp. getParticipants().size());
    }

    // ==================== AUTO WATCH PARTY TESTS ====================

    @Test
    @DisplayName("Auto watch party should be created for team")
    void testAutoWatchPartyCreation() {
        WatchParty wp = WatchParty.createAutoWatchParty(alice, "T1", AutoType. TEAM);

        assertTrue(wp.isAutoWatchParty());
        assertEquals(AutoType.TEAM, wp.getAutoConfig().getType());
        assertEquals("T1", wp. getAutoConfig().getTarget());
        assertEquals(WatchPartyStatus. WAITING, wp.getStatus());
    }

    @Test
    @DisplayName("Auto watch party should transition through states")
    void testAutoWatchPartyStateTransitions() {
        WatchParty wp = WatchParty. createAutoWatchParty(alice, "Gen. G", AutoType.TEAM);
        
        assertEquals(WatchPartyStatus.WAITING, wp.getStatus());

        boolean joined = wp.join(bob);
        assertFalse(joined);
        assertFalse(wp. getParticipants().contains(bob));

        assertEquals(alice, wp.getCreator());
        assertTrue(wp.isAutoWatchParty());
        assertEquals("Gen. G", wp.getAutoConfig().getTarget());
    }

    @Test
    @DisplayName("Manager should track auto watch parties")
    void testManagerAutoWatchPartyTracking() {
        WatchPartyManager manager = new WatchPartyManager();
        
        WatchParty wp1 = WatchParty.createAutoWatchParty(alice, "T1", AutoType. TEAM);
        WatchParty wp2 = WatchParty.createAutoWatchParty(bob, "Worlds 2025", AutoType. TOURNAMENT);
        
        manager. addAutoWatchParty(wp1);
        manager.addAutoWatchParty(wp2);

        assertEquals(2, manager. getAllAutoWatchParties().size());
        assertTrue(manager.getAllAutoWatchParties().contains(wp1));
        assertTrue(manager.getAllAutoWatchParties().contains(wp2));
    }

    @Test
    @DisplayName("Scheduler should start and stop correctly")
    void testSchedulerLifecycle() {
        WatchPartyManager manager = new WatchPartyManager();
        
        assertFalse(manager.isSchedulerRunning());
        
        manager.startScheduler();
        assertTrue(manager.isSchedulerRunning());
        
        manager.stopScheduler();
        assertFalse(manager.isSchedulerRunning());
    }

    @Test
    @DisplayName("Users should leave watch parties correctly")
    void testWatchPartyLeave() {
        WatchParty wp = new WatchParty("DRX Watch", LocalDateTime.now().plusDays(1), "LoL");
        
        wp.join(bob);
        assertTrue(wp.getParticipants().contains(bob));
        
        wp.leave(bob);
        assertFalse(wp.getParticipants().contains(bob));
    }

    @Test
    @DisplayName("Manager should remove watch parties by name")
    void testWatchPartyRemoval() {
        WatchPartyManager manager = new WatchPartyManager();
        WatchParty wp = WatchParty. createAutoWatchParty(alice, "FNC", AutoType. TEAM);
        
        manager.addAutoWatchParty(wp);
        assertEquals(1, manager. getAllWatchParties().size());
        
        manager. removeWatchParty(wp.name());
        assertEquals(0, manager.getAllWatchParties().size());
    }

    // ==================== QUIZ TESTS ====================

    @Test
    @DisplayName("Quiz should start correctly")
    void testQuizStart() {
        QuizGame quiz = new QuizGame();
        
        assertFalse(quiz.isActive());
        
        String startMessage = quiz. start();
        
        assertTrue(quiz.isActive());
        assertNotNull(startMessage);
        assertTrue(startMessage.contains("DEMARRE"));
    }

    @Test
    @DisplayName("Quiz should accept correct answers")
    void testQuizCorrectAnswer() {
        QuizGame quiz = new QuizGame();
        quiz.start();
        
        // First question answer is "Leonardo DiCaprio"
        String result = quiz.processInput(alice, "Leonardo DiCaprio");
        
        assertNotNull(result);
        assertTrue(result.contains("BRAVO"));
    }

    @Test
    @DisplayName("Quiz should reject wrong answers")
    void testQuizWrongAnswer() {
        QuizGame quiz = new QuizGame();
        quiz.start();
        
        String result = quiz.processInput(alice, "Wrong answer");
        
        assertNull(result); // Wrong answers return null
    }

    @Test
    @DisplayName("Quiz should track scores")
    void testQuizScoring() {
        QuizGame quiz = new QuizGame();
        quiz.start();
        
        quiz.processInput(alice, "Leonardo DiCaprio");
        quiz.processInput(bob, "2020");
        quiz.processInput(alice, "Canberra");
        
        String results = quiz.getResults();
        
        assertTrue(results.contains("Alice"));
        assertTrue(results.contains("Bob"));
    }

    @Test
    @DisplayName("Quiz should reset correctly")
    void testQuizReset() {
        QuizGame quiz = new QuizGame();
        quiz.start();
        quiz.processInput(alice, "Leonardo DiCaprio");
        
        quiz.reset();
        
        assertFalse(quiz.isActive());
        assertFalse(quiz.isFinished());
    }

    @Test
    @DisplayName("Custom quiz should accept custom questions")
    void testCustomQuiz() {
        QuizGame quiz = new QuizGame("custom-quiz");
        
        quiz.addQuestion("What is 2+2?", "4");
        quiz.addQuestion("Capital of France?", "Paris");
        
        String startMessage = quiz.start();
        
        assertTrue(startMessage.contains("CUSTOM-QUIZ"));
        assertTrue(quiz.isActive());
    }

    // ==================== INTEGRATION TESTS ====================

    @Test
    @DisplayName("User should maintain consistent points across operations")
    void testUserPointsConsistency() {
        assertEquals(200, alice. getPoints());
        
        alice.setPoints(150);
        assertEquals(150, alice.getPoints());
        
        Choice choice = new Choice("A");
        Collection<Choice> options = new ArrayList<>();
        options.add(choice);
        
        PublicBet bet = new PublicBet("Test? ", options, new Time(System.currentTimeMillis()));
        bet.vote(alice, choice, 50);
        assertEquals(100, alice.getPoints());
        
        bet.cancel();
        assertEquals(150, alice.getPoints());
    }

    @Test
    @DisplayName("System should handle multiple concurrent watch parties")
    void testMultipleWatchPartiesCoexist() {
        WatchPartyManager manager = new WatchPartyManager();
        
        WatchParty manual = new WatchParty("Manual", LocalDateTime. now().plusDays(1), "Game");
        WatchParty auto1 = WatchParty.createAutoWatchParty(alice, "T1", AutoType.TEAM);
        WatchParty auto2 = WatchParty. createAutoWatchParty(bob, "LCK", AutoType.TOURNAMENT);
        
        manager. addWatchParty(manual);
        manager.addAutoWatchParty(auto1);
        manager.addAutoWatchParty(auto2);
        
        assertEquals(3, manager.getAllWatchParties().size());
        assertEquals(2, manager. getAllAutoWatchParties().size());
    }
}