package backend;

import java.sql.Time;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;


public class Bet {
    private Map<User,Integer> users;
    private String question;
    private Collection options;
    private State state;
    private Time votingTime;

    public enum State {
        VOTING,PENDING,END,CANCELED 
    }


    public Bet( String question, Collection options, Time votingTime) 
    {
        this.question = question;
        this.users = new HashMap<>();
        this.options = new ArrayList<>(options);
        this.state = State.VOTING;
        this.votingTime = votingTime;
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
                u.points += reward;
            } 
        }
    }

    
    public void Vote(User user, Choice choice,Integer points){
        if (this.state == State.VOTING && options.contains(choice)) {
            choice.newVoter(user);
            this.users.put(user, points);
            user.points -= points;
        }
    }

    public void Cancel(){
        this.state = State.CANCELED;
        for (User u : users.keySet()) {
            int userBet = users.get(u);
            u.points += userBet;
        }
    }

    public void EndVoteTime(){
        this.state = State.PENDING;
    }

}

