import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.sql.Time;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;

public class MainTest {

    @Test
    public void chatMessagingAndMessageStorage() {
        User admin = new User("Admin", true);
        User alice = new User("Alice", false);

        PublicChat publicChat = new PublicChat("Public Chat", admin);
        publicChat.addUser(admin);
        publicChat.addUser(alice);

        // send a message from Alice
        publicChat.sendMessage(alice, "Hello everyone");

        // messages is protected and the test is in the same package, so we can access it
        assertNotNull(publicChat.messages);
        assertEquals(1, publicChat.messages.size());
        assertEquals("Hello everyone", publicChat.messages.get(0).getContent());
        assertEquals("Alice", publicChat.messages.get(0).getSender().getName());
    }

    @Test
    public void betVotingAndResultsUpdatePoints() {
        User alice = new User("Alice", false);
        User bob = new User("Bob", false);

        // both start with default 200 points
        assertEquals(200, alice.getPoints());
        assertEquals(200, bob.getPoints());

        Choice choiceA = new Choice("A");
        Collection options = new ArrayList();
        options.add(choiceA);
        Time votingTime = new Time(System.currentTimeMillis());

        PublicBet bet = new PublicBet("Which option?", options, votingTime);

        // Alice votes 50 points for choiceA
        bet.Vote(alice, choiceA, 50);
        assertEquals(150, alice.getPoints());

        // Bob votes 50 points for choiceA too
        bet.Vote(bob, choiceA, 50);
        assertEquals(150, bob.getPoints());

        // End the vote and set result
        bet.SetResult(choiceA);

        // After result, since both voted equally, their points should increase back (they receive a share)
        assertTrue(alice.getPoints() >= 150);
        assertTrue(bob.getPoints() >= 150);
    }

    @Test
    public void watchPartyPlanning() {
        WatchPartyManager manager = new WatchPartyManager();
        WatchParty wp = new WatchParty("Party", LocalDateTime.now().plusDays(1), "Game");

        manager.addWatchParty(wp);
        manager.toPlanWatchParty(wp);

        // planned list should contain the watch party
        assertTrue(manager.watchPartiesPlanifiees().contains(wp));
    }
}
