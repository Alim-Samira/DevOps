import java.sql.Time;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;


public class PublicBet implements Bet {
    private Map<User,Integer> users;
    private String question;
    private Collection options;
    private State state;
    private Time votingTime;

    public PublicBet( String question, Collection options, Time votingTime) 
    {
        this.question = question;
        this.users = new HashMap<>();
        this.options = new ArrayList<>(options);
        this.state = State.VOTING;
        this.votingTime = votingTime;
    }

    public void options(Collection options){
        this.options = options;
    }

    public void setResult(Choice choice){
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
                if(winningPoints!=0){
                    int reward = (int) ((double) userBet / winningPoints * totalPoints);
                    u.setPoints(u.getPoints() + reward);
                }
            } 
        }
    }

    
    public void vote(User user, Choice choice,Integer points){
        if (this.state == State.VOTING && options.contains(choice)) {
            choice.newVoter(user);
            this.users.put(user, points);
            user.setPoints(user.getPoints() - points); // a modifier car manque point quand privé
        }
    }

    public void cancel(){
        this.state = State.CANCELED;
        for (User u : users.keySet()) {
            int userBet = users.get(u);
            u.setPoints(u.getPoints() + userBet);
        }
    }

    public void endVoteTime(){
        this.state = State.PENDING;
    }

}

