package backend.models;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PrivateRanking implements Ranking {
    
    private final PrivateChat chat;

    public PrivateRanking(PrivateChat chat) {
        this.chat = chat;
    }

    @Override
    public Map<User, Integer> getRanking(List<User> allUsers) {
        if (chat == null) {
            return Collections.emptyMap();
        }
        
        Map<User, Integer> chatUsers = chat.users();

        return chatUsers.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .collect(Collectors.toMap(
                    Map.Entry::getKey, 
                    Map.Entry::getValue, 
                    (e1, e2) -> e1, 
                    LinkedHashMap::new 
                ));
    }
}