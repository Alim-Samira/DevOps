import java.sql.Time;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;


public class PrivateBet implements Bet {
    private Map<User,Integer> users;
    private String question;
    private Collection options;
    private State state;
    private Time votingTime;
    private PrivateChat chat;

    public PrivateBet(String question, Collection options, Time votingTime, PrivateChat chat) 
    {
        this.question = question;
        this.options = new ArrayList<>(options);
        this.state = State.VOTING;
        this.votingTime = votingTime;
        this.chat=chat;
        this.users=new HashMap<>();
    }

    public void Options(Collection options){
        this.options = options;
    }

    public void SetResult(Choice choice){
        this.state = State.END;
        int totalPoints = users.values().stream().mapToInt(Integer::intValue).sum();
        int winningPoints = 0;
        for (User u : users.keySet()) {
            if (choice.Voters().contains(u)) {
                winningPoints += users.get(u);
            }
        }
        for (User u : users.keySet()) {
            if (choice.Voters().contains(u)) {
                int userBet = users.get(u);
                int reward = (int) ((double) userBet / winningPoints * totalPoints);
                int current = chat.Users().get(u);
                chat.setPoints(u, current + reward);            } 
        }
    }

    
    public void Vote(User user, Choice choice,Integer points){
        if (this.state == State.VOTING && options.contains(choice)) {
            choice.newVoter(user);
            this.users.put(user, points);
           int current = chat.Users().get(user);
            chat.setPoints(user, current - points);        }
    }

    public void Cancel(){
        this.state = State.CANCELED;
        for (User u : users.keySet()) {
            int userBet = users.get(u);
            int current = chat.Users().get(u);
            chat.setPoints(u, current + userBet);
        }
    }

    public void EndVoteTime(){
        this.state = State.PENDING;
    }

}

