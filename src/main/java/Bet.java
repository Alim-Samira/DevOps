import java.util.Collection;

public interface Bet {
    public enum State {
        VOTING,PENDING,END,CANCELED 
    }


    void SetResult(Choice choice);

    void Vote(User user, Choice choice, Integer points);

    void Cancel();

    void EndVoteTime();


    void Options(Collection options);

}


