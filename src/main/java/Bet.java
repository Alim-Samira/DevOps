import java.util.Collection;

public interface Bet {
    public enum State {
        VOTING,PENDING,END,CANCELED 
    }


    void setResult(Choice choice);

    void vote(User user, Choice choice, Integer points);

    void cancel();

    void endVoteTime();

    void options(Collection options);

}


