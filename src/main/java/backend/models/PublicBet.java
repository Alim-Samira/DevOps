package backend.models;
import java.sql.Time;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;


public class PublicBet implements Bet {
    private Map<User, Integer> users;
    private String question;
    private Collection<Choice> options;
    private State state;
    private Time votingTime;

    public PublicBet(String question, Collection<Choice> options, Time votingTime)
    {
        this.question = question;
        this.users = new HashMap<>();
        this.options = new ArrayList<>(options);
        this.state = State.VOTING;
        this.votingTime = votingTime;
    }

    @Override
    public void options(Collection<Choice> options) {
        this.options = options;
    }

    @Override
    public void setResult(Choice choice) {
        this.state = State.END;
        int totalPoints = users.values().stream().mapToInt(Integer::intValue).sum();
        int winningPoints = 0;
        for (Map.Entry<User, Integer> entry : users.entrySet()) {
            User user = entry.getKey();
            int userBet = entry.getValue();
            if (choice.voters().contains(user)) {
                winningPoints += userBet;
            }
        }
        for (Map.Entry<User, Integer> entry : users.entrySet()) {
            User user = entry.getKey();
            int userBet = entry.getValue();
            if (choice.voters().contains(user) && winningPoints != 0) {
                    int reward = (int) ((double) userBet / winningPoints * totalPoints);
                    user.setPoints(user.getPoints() + reward);
                }
            } 
        }

    
    @Override
    public void vote(User user, Choice choice, Integer points) {
        if (this.state == State.VOTING && options.contains(choice)) {
            choice.newVoter(user);
            this.users.put(user, points);
            user.setPoints(user.getPoints() - points); // a modifier car manque point quand privé
        }
    }

    @Override
    public void cancel() {
        this.state = State.CANCELED;
        for (Map.Entry<User, Integer> entry : users.entrySet()) {
            User user = entry.getKey();
            int userBet = entry.getValue();
            user.setPoints(user.getPoints() + userBet);
        }
    }

    @Override
    public void endVoteTime() {
        this.state = State.PENDING;
    }

    public Collection<Choice> getOptions() {
        return this.options;
    }

    public String getQuestion() {
        return question;
    }

    public State getState() {
        return state;
    }

    public Time getVotingTime() {
        return votingTime;
    }
}