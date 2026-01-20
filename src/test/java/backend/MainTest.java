package backend;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import backend.models.AutoType;
import backend.models.DiscreteChoiceBet;
import backend.models.NumericValueBet;
import backend.models.OrderedRankingBet;
import backend.models.Chat;
import backend.models.QuizGame;
import backend.models.User;
import backend.models.WatchParty;
import backend.models.WatchPartyStatus;
import backend.services.WatchPartyManager;
import backend.services.RankingService;
import backend.services.UserService;
import backend.services.RewardService;

/**
 * Suite de tests complète pour le système DevOps
 * Tests: Chat, Paris (3 types), Watch Parties, Auto Watch Parties, Quiz
 */
class MainTest {

    private User admin;
    private User alice;
    private User bob;
    private WatchParty watchParty;

    @BeforeEach
    void setUp() {
        admin = new User("Admin", true);
        alice = new User("Alice", false);
        bob = new User("Bob", false);
        watchParty = new WatchParty("Test WP", LocalDateTime.now().plusDays(1), "LoL");
    }

    // ==================== CHAT TESTS ====================

    @Test
    @DisplayName("Chat should store messages correctly")
    void testPublicChatMessaging() {
        Chat chat = new Chat("Chat", admin);

        chat.sendMessage(alice, "Hello everyone");

        assertNotNull(chat.getMessages());
        assertEquals(1, chat.getMessages().size());
        assertEquals("Hello everyone", chat.getMessages().get(0).getContent());
        assertEquals("Alice", chat.getMessages().get(0).getSender().getName());
    }

    // ==================== DISCRETE CHOICE BET TESTS ====================

    @Test
    @DisplayName("Discrete bet should deduct points when voting")
    void testDiscreteBetVotingDeductsPoints() {
        List<String> choices = Arrays.asList("T1", "GenG");
        DiscreteChoiceBet bet = new DiscreteChoiceBet(
            "Qui va gagner?", admin, watchParty, 
            LocalDateTime.now().plusMinutes(10), choices
        );
        watchParty.createBet(bet);

        assertEquals(200, alice.getPublicPoints());
        bet.vote(alice, "T1", 50);
        assertEquals(150, alice.getPublicPoints());
    }

    @Test
    @DisplayName("Discrete bet should distribute winnings equally")
    void testDiscreteBetEqualDistribution() {
        List<String> choices = Arrays.asList("A", "B");
        DiscreteChoiceBet bet = new DiscreteChoiceBet(
            "Test?", admin, watchParty,
            LocalDateTime.now().plusMinutes(10), choices
        );

        bet.vote(alice, "A", 50);
        bet.vote(bob, "A", 50);
        
        assertEquals(150, alice.getPublicPoints());
        assertEquals(150, bob.getPublicPoints());

        bet.endVoting();
        bet.resolve("A");

        // Total pot = 100, split equally between 2 winners = 50 each
        assertEquals(200, alice.getPublicPoints()); // 150 + 50
        assertEquals(200, bob.getPublicPoints());   // 150 + 50
    }

    @Test
    @DisplayName("Discrete bet should refund points when canceled")
    void testDiscreteBetCancellation() {
        List<String> choices = Arrays.asList("A", "B");
        DiscreteChoiceBet bet = new DiscreteChoiceBet(
            "Test?", admin, watchParty,
            LocalDateTime.now().plusMinutes(10), choices
        );

        bet.vote(alice, "A", 50);
        assertEquals(150, alice.getPublicPoints());

        bet.cancel();
        assertEquals(200, alice.getPublicPoints());
    }

    @Test
    @DisplayName("Only admin can create discrete bet")
    void testDiscreteBetAdminOnly() {
        List<String> choices = Arrays.asList("A", "B");
        LocalDateTime votingEnd = LocalDateTime.now().plusMinutes(10);
        
        assertThrows(IllegalArgumentException.class, () -> 
            new DiscreteChoiceBet("Test?", alice, watchParty, votingEnd, choices)
        );
    }

    // ==================== NUMERIC VALUE BET TESTS ====================

    @Test
    @DisplayName("Numeric bet should accept integer values")
    void testNumericBetIntegerVoting() {
        NumericValueBet bet = new NumericValueBet(
            "Combien de kills?", admin, watchParty,
            LocalDateTime.now().plusMinutes(10), true, 0.0, 100.0
        );

        String result = bet.vote(alice, 35, 50);
        assertTrue(result.contains("✅"));
        assertEquals(150, alice.getPublicPoints());
    }

    @Test
    @DisplayName("Numeric bet should reward exact matches equally")
    void testNumericBetExactMatch() {
        NumericValueBet bet = new NumericValueBet(
            "Nombre de kills?", admin, watchParty,
            LocalDateTime.now().plusMinutes(10), true, 0.0, 100.0
        );

        bet.vote(alice, 35, 50);
        bet.vote(bob, 35, 50);

        bet.endVoting();
        String result = bet.resolve(35);

        assertTrue(result.contains("✅"));
        assertEquals(200, alice.getPublicPoints()); // 150 + 50
        assertEquals(200, bob.getPublicPoints());
    }

    @Test
    @DisplayName("Numeric bet should reward top 30% by proximity")
    void testNumericBetProximityReward() {
        User charlie = new User("Charlie", false);
        User david = new User("David", false);

        NumericValueBet bet = new NumericValueBet(
            "Durée du match?", admin, watchParty,
            LocalDateTime.now().plusMinutes(10), true, 0.0, 60.0
        );

        bet.vote(alice, 30, 50);   // Distance = 5
        bet.vote(bob, 33, 50);     // Distance = 2 (plus proche)
        bet.vote(charlie, 40, 50); // Distance = 5
        bet.vote(david, 20, 50);   // Distance = 15 (plus loin)

        bet.endVoting();
        String result = bet.resolve(35);

        assertTrue(result.contains("✅"));
        // Top 30% de 4 = 2 gagnants (bob et alice ou charlie)
        assertTrue(bob.getPublicPoints() > 150); // Bob devrait gagner plus (plus proche)
    }

    // ==================== ORDERED RANKING BET TESTS ====================

    @Test
    @DisplayName("Ranking bet should accept valid rankings")
    void testRankingBetValidVoting() {
        List<String> players = Arrays.asList("Faker", "Chovy", "ShowMaker");
        OrderedRankingBet bet = new OrderedRankingBet(
            "Top 3 joueurs?", admin, watchParty,
            LocalDateTime.now().plusMinutes(10), players
        );

        List<String> ranking = Arrays.asList("Faker", "Chovy", "ShowMaker");
        String result = bet.vote(alice, ranking, 50);

        assertTrue(result.contains("✅"));
        assertEquals(150, alice.getPublicPoints());
    }

    @Test
    @DisplayName("Ranking bet should reward perfect matches equally")
    void testRankingBetPerfectMatch() {
        List<String> players = Arrays.asList("A", "B", "C");
        OrderedRankingBet bet = new OrderedRankingBet(
            "Classement?", admin, watchParty,
            LocalDateTime.now().plusMinutes(10), players
        );

        List<String> perfect = Arrays.asList("A", "B", "C");
        bet.vote(alice, perfect, 50);
        bet.vote(bob, perfect, 50);

        bet.endVoting();
        String result = bet.resolve(perfect);

        assertTrue(result.contains("parfait"));
        assertEquals(200, alice.getPublicPoints());
        assertEquals(200, bob.getPublicPoints());
    }

    @Test
    @DisplayName("Ranking bet should calculate Kendall tau distance")
    void testRankingBetKendallDistance() {
        List<String> players = Arrays.asList("A", "B", "C", "D");
        OrderedRankingBet bet = new OrderedRankingBet(
            "Classement?", admin, watchParty,
            LocalDateTime.now().plusMinutes(10), players
        );

        bet.vote(alice, Arrays.asList("A", "B", "C", "D"), 50); // Parfait
        bet.vote(bob, Arrays.asList("B", "A", "C", "D"), 50);   // 1 inversion

        bet.endVoting();
        String result = bet.resolve(Arrays.asList("A", "B", "C", "D"));

        assertTrue(result.contains("✅"));
        // Alice devrait gagner plus (distance = 0)
        assertTrue(alice.getPublicPoints() >= bob.getPublicPoints());
    }

    // ==================== WATCH PARTY BETTING TESTS ====================

    @Test
    @DisplayName("Watch party should allow only one active bet")
    void testWatchPartyOneActiveBet() {
        DiscreteChoiceBet bet1 = new DiscreteChoiceBet(
            "Question 1?", admin, watchParty,
            LocalDateTime.now().plusMinutes(10), Arrays.asList("A", "B")
        );
        DiscreteChoiceBet bet2 = new DiscreteChoiceBet(
            "Question 2?", admin, watchParty,
            LocalDateTime.now().plusMinutes(10), Arrays.asList("C", "D")
        );

        String result1 = watchParty.createBet(bet1);
        assertTrue(result1.contains("✅"));
        assertTrue(watchParty.hasActiveBet());

        String result2 = watchParty.createBet(bet2);
        assertTrue(result2.contains("❌"));
    }

    @Test
    @DisplayName("Watch party should allow new bet after resolution")
    void testWatchPartyNewBetAfterResolution() {
        DiscreteChoiceBet bet1 = new DiscreteChoiceBet(
            "Question 1?", admin, watchParty,
            LocalDateTime.now().plusMinutes(10), Arrays.asList("A", "B")
        );

        watchParty.createBet(bet1);
        bet1.endVoting();
        bet1.resolve("A");

        assertFalse(watchParty.hasActiveBet());

        DiscreteChoiceBet bet2 = new DiscreteChoiceBet(
            "Question 2?", admin, watchParty,
            LocalDateTime.now().plusMinutes(10), Arrays.asList("C", "D")
        );

        String result = watchParty.createBet(bet2);
        assertTrue(result.contains("✅"));
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

        assertTrue(wp.getParticipants().contains(alice));
        assertEquals(1, wp.getParticipants().size());
    }

    // ==================== AUTO WATCH PARTY TESTS ====================

    @Test
    @DisplayName("Auto watch party should be created for team")
    void testAutoWatchPartyCreation() {
        WatchParty wp = WatchParty.createAutoWatchParty(alice, "T1", AutoType.TEAM);

        assertTrue(wp.isAutoWatchParty());
        assertEquals(AutoType.TEAM, wp.getAutoConfig().getType());
        assertEquals("T1", wp.getAutoConfig().getTarget());
        assertEquals(WatchPartyStatus.WAITING, wp.getStatus());
    }

    @Test
    @DisplayName("Auto watch party should transition through states")
    void testAutoWatchPartyStateTransitions() {
        WatchParty wp = WatchParty.createAutoWatchParty(alice, "Gen.G", AutoType.TEAM);
        
        assertEquals(WatchPartyStatus.WAITING, wp.getStatus());

        boolean joined = wp.join(bob);
        assertFalse(joined);
        assertFalse(wp.getParticipants().contains(bob));

        assertEquals(alice, wp.getCreator());
        assertTrue(wp.isAutoWatchParty());
        assertEquals("Gen.G", wp.getAutoConfig().getTarget());
    }

    @Test
    @DisplayName("Manager should track auto watch parties")
    void testManagerAutoWatchPartyTracking() {
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
        WatchParty wp = WatchParty.createAutoWatchParty(alice, "FNC", AutoType.TEAM);
        
        manager.addAutoWatchParty(wp);
        assertEquals(1, manager.getAllWatchParties().size());
        
        manager.removeWatchParty(wp.name());
        assertEquals(0, manager.getAllWatchParties().size());
    }

    // ==================== QUIZ TESTS ====================

    @Test
    @DisplayName("Quiz should start correctly")
    void testQuizStart() {
        QuizGame quiz = new QuizGame();
        
        assertFalse(quiz.isActive());
        
        String startMessage = quiz.start();
        
        assertTrue(quiz.isActive());
        assertNotNull(startMessage);
        assertTrue(startMessage.contains("DEMARRE"));
    }

    @Test
    @DisplayName("Quiz should accept correct answers")
    void testQuizCorrectAnswer() {
        QuizGame quiz = new QuizGame();
        quiz.start();
        
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
        
        assertNull(result);
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
    @DisplayName("System should handle multiple concurrent watch parties")
    void testMultipleWatchPartiesCoexist() {
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

    // ==================== USER POINTS AND WINS TESTS ====================

    @Test
    @DisplayName("User points accumulation should work")
    void testUserPointsAccumulation() {
        User pointUser = new User("PointAccumulator", false);

        int initialPoints = pointUser.getPublicPoints();
        pointUser.addPublicPoints(100);

        assertEquals(initialPoints + 100, pointUser.getPublicPoints());
    }

    @Test
    @DisplayName("User wins tracking should work")
    void testUserWinsTracking() {
        User winUser = new User("WinTracker", false);

        int initialWins = winUser.getPublicWins();
        winUser.addPublicWin();

        assertEquals(initialWins + 1, winUser.getPublicWins());
    }

    @Test
    @DisplayName("User per-watch-party wins should work")
    void testUserWinsPerWatchParty() {
        User partyUser = new User("PartyWinner", false);

        partyUser.addWinForWatchParty("TestParty");
        int wins = partyUser.getWinsForWatchParty("TestParty");

        assertEquals(1, wins);
    }

    @Test
    @DisplayName("User per-watch-party points should work")
    void testUserPointsPerWatchParty() {
        User partyPointUser = new User("PartyPoints", false);

        partyPointUser.addPointsForWatchParty("Party2", 250);
        int points = partyPointUser.getPointsForWatchParty("Party2");

        assertEquals(250, points);
    }
}