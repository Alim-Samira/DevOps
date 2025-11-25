import java.util.ArrayList;
import java.util.Collection;

public class Choice {
    private String Text;
    private Collection<User> Voters;
    private Integer Points;

    public Choice(String text) {
        this.Text = text;
        this.Voters = new ArrayList<>();
        this.Points = 0;
    }

    public void newVoter(User user) {
        this.Voters.add(user);
    }

    public Collection<User> Voters() {
        return this.Voters;
    }
}
