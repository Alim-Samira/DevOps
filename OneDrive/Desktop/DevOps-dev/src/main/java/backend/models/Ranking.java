package backend.models;
import java.util.List;
import java.util.Map;

public interface Ranking {
    
    Map<User, Integer> getRanking(List<User> allUsers);
}