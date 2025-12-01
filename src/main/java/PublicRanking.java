import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.stream.Collectors;

public class PublicRanking implements Ranking {

    @Override
    public Map<User, Integer> getRanking(List<User> allUsers) {
        if (allUsers == null || allUsers.isEmpty()) {
            return Collections.emptyMap();
        }

        return allUsers.stream()
                .collect(Collectors.toMap(
                    user -> user, 
                    User::getPoints // Use public points
                ))
                .entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .collect(Collectors.toMap(
                    Map.Entry::getKey, 
                    Map.Entry::getValue, 
                    (e1, e2) -> e1, 
                    LinkedHashMap::new 
                ));
    }
}