import java.sql.Time;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

//done by AI
public class Main_bet {
    public static void main(String[] args) {
        // Create users
        User alice = new User();
        alice.points = 100;
        User bob = new User();
        bob.points = 100;
        User carol = new User();
        carol.points = 100;

        System.out.println("Initial points:");
        System.out.println("Alice: " + alice.points + ", Bob: " + bob.points + ", Carol: " + carol.points);

        // Create choices
        Choice c1 = new Choice("Team A wins");
        Choice c2 = new Choice("Team B wins");

    ArrayList<Choice> choices = new ArrayList<>();
    choices.add(c1);
    choices.add(c2);

        // Prepare users map (empty initially)
    Map<User,Integer> usersMap = new HashMap<>();

        // Create a Bet with a dummy Time
        Bet bet = new Bet(usersMap, choices, new Time(System.currentTimeMillis()));

        // Users vote
        bet.Vote(alice, c1, 30); // Alice bets 30 on c1
        bet.Vote(bob, c2, 20);   // Bob bets 20 on c2
        bet.Vote(carol, c1, 50); // Carol bets 50 on c1

        System.out.println("\nAfter votes (points deducted):");
        System.out.println("Alice: " + alice.points + ", Bob: " + bob.points + ", Carol: " + carol.points);

        // End voting period
        bet.EndVoteTime();

        // Set result: c1 wins
        bet.SetResult(c1);

        System.out.println("\nAfter setting result (rewards distributed):");
        System.out.println("Alice: " + alice.points + ", Bob: " + bob.points + ", Carol: " + carol.points);

        // Now test cancel behavior on a new bet
    Map<User,Integer> usersMap2 = new HashMap<>();
    ArrayList<Choice> choices2 = new ArrayList<>();
        Choice d1 = new Choice("Option 1");
        choices2.add(d1);
        Bet bet2 = new Bet(usersMap2, choices2, new Time(System.currentTimeMillis()));

        bet2.Vote(alice, d1, 10);
        System.out.println("\nAfter new bet vote (Alice deducted 10): Alice: " + alice.points);
        bet2.Cancel();
        System.out.println("After cancel (Alice refunded 10): Alice: " + alice.points);
    }
}
