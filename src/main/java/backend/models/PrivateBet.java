package backend.models;
import java.sql.Time;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;


public class PrivateBet implements Bet {
    private Map<User, Integer> users;
    private String question;
    private Collection<Choice> options;
    private State state;
    private Time votingTime;
    private PrivateChat chat;

    public PrivateBet(String question, Collection<Choice> options, Time votingTime, PrivateChat chat)
    {
        this.question = question;
        this.options = new ArrayList<>(options);
        this.state = State.VOTING;
        this.votingTime = votingTime;
        this.chat = chat;
        this.users = new HashMap<>();
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
            if (choice.voters().contains(user)) {
                int reward = (int) ((double) userBet / winningPoints * totalPoints);
                int current = chat.users().get(user);
                chat.setPoints(user, current + reward);
            } 
        }
    }

    @Override
    public void vote(User user, Choice choice, Integer points) {
        if (this.state == State.VOTING && options.contains(choice)) {
            choice.newVoter(user);
            this.users.put(user, points);
            int current = chat.users().get(user);
            chat.setPoints(user, current - points);
        }
    }

    @Override
    public void cancel() {
        this.state = State.CANCELED;
        for (Map.Entry<User, Integer> entry : users.entrySet()) {
            User user = entry.getKey();
            int userBet = entry.getValue();
            int current = chat.users().get(user);
            chat.setPoints(user, current + userBet);
        }
    }

    @Override
    public void endVoteTime() {
        this.state = State.PENDING;
    }

    public String getQuestion() {
        return question;
    }

    public Collection<Choice> getOptions() {
        return options;
    }

    public State getState() {
        return state;
    }

    public Time getVotingTime() {
        return votingTime;
    }

    public PrivateChat getChat() {
        return chat;
    }
}

