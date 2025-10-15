import java.util.ArrayList;
import java.util.List;

import backend.*;

public class App {
    public static void main(String[] args) {
        List<Choice> options=new ArrayList<>();
        options.add(new Choice("Equipe1"));
        options.add(new Choice("Equipe2"));
        Bet b1=new Bet("Qui va gagner le BO3 ?",options,null);
        User u1=new User();
        User u2=new User();
        User u3=new User();
        b1.Vote(u1, options.get(0), 100);
        b1.Vote(u2, options.get(0), 50);
        b1.Vote(u3, options.get(1), 200);
        b1.SetResult(options.get(0));
        System.out.println(u1.Points());
        System.out.println(u2.Points());
        System.out.println(u3.Points());
    }
}
