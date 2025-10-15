import java.util.ArrayList;
import java.util.Collection;

public class Choice {
    String Text;
    Collection Voters;
    Integer Points;
    
    public Choice(String text){
        this.Text = text;
        this.Voters = new ArrayList<>();
        this.Points = 0;
    }
    public void newVoter(User user){
        this.Voters.add(user);
    }
}
