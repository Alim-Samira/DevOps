package backend;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.sql.Time;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class BetTest {

	@Test
	public void testUserInitialPoints() {
		User u = new User();
		// default int field should be 0
		assertEquals(0, u.points);
	}

	@Test
	public void testChoiceNewVoter() {
		Choice c = new Choice("option");
		User u = new User();
		c.newVoter(u);
		assertTrue(c.Voters.contains(u));
	}

	@Test
	public void testBetVoteAndCancel() {
		User u = new User();
		u.points = 100;

	Map<User,Integer> initialUsers = new HashMap<>();
	Collection<Choice> options = new ArrayList<>();
		Choice c = new Choice("opt1");
		options.add(c);
		Time t = new Time(System.currentTimeMillis());

		Bet bet = new Bet(initialUsers, options, t);

		bet.Vote(u, c, 30);

		// user points decreased by bet amount
		assertEquals(70, u.points);

		// bet recorded the user's bet
		assertEquals(30, bet.users.get(u));

		// choice recorded voter
		assertTrue(c.Voters.contains(u));

		// cancel should return the points and set state
		bet.Cancel();
		assertEquals(100, u.points);
		assertEquals(Bet.State.CANCELED, bet.state);
	}

	@Test
	public void testSetResultRewardDistribution() {
		// Setup three users
		User u1 = new User(); u1.points = 100;
		User u2 = new User(); u2.points = 100;
		User u3 = new User(); u3.points = 100;

	Map<User,Integer> initialUsers = new HashMap<>();
	Collection<Choice> options = new ArrayList<>();
		Choice winning = new Choice("win");
		Choice losing = new Choice("lose");
		options.add(winning);
		options.add(losing);
		Time t = new Time(System.currentTimeMillis());

		Bet bet = new Bet(initialUsers, options, t);

		// votes: u1 and u2 on winning, u3 on losing
		bet.Vote(u1, winning, 40); // u1 now 60
		bet.Vote(u2, winning, 60); // u2 now 40
		bet.Vote(u3, losing, 30);  // u3 now 70

		// Sanity before result
		assertEquals(60, u1.points);
		assertEquals(40, u2.points);
		assertEquals(70, u3.points);

		// End vote and set result
		bet.EndVoteTime();
		bet.SetResult(winning);

		// After distributing rewards:
		// totalPoints = 40 + 60 + 30 = 130
		// winningPoints = 40 + 60 = 100
		// u1 reward = 40/100 * 130 = 52 -> u1 final = 60 + 52 = 112
		// u2 reward = 60/100 * 130 = 78 -> u2 final = 40 + 78 = 118
		// u3 should not get reward and stays at 70

		assertEquals(112, u1.points);
		assertEquals(118, u2.points);
		assertEquals(70, u3.points);
		assertEquals(Bet.State.END, bet.state);
	}

}
