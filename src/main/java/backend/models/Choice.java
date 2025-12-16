package backend.models;
import java.util.ArrayList;
import java.util.Collection;

public class Choice {
    private String text;
    private Collection<User> voters;

    public Choice(String text) {
        this.text = text;
        this.voters = new ArrayList<>();
    }

    public void newVoter(User user) {
        this.voters.add(user);
    }

    public Collection<User> voters() {
        return this.voters;
    }

    @Override
    public String toString() {
        return this.text;
    }
}
