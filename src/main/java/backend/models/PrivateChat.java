package backend.models;

import jakarta.persistence.*;
import java.util.HashMap;
import java.util.Map;

@Entity
@DiscriminatorValue("PRIVATE")
public class PrivateChat extends Chat {

    @ElementCollection
    @CollectionTable(
        name = "private_chat_users",
        joinColumns = @JoinColumn(name = "chat_id")
    )
    @MapKeyJoinColumn(name = "user_id")
    @Column(name = "points")
    private Map<User, Integer> users = new HashMap<>();

    public PrivateChat() {
        super();
    }

    public PrivateChat(String name, User admin) {
        super(name, admin);
        this.users = new HashMap<>();
        this.addUser(admin);
    }

    public void addUser(User user) {
        users.put(user, 200);
    }

    public void setPoints(User user, Integer points) {
        if (users.containsKey(user)) {
            users.replace(user, points);
        }
    }

    public Map<User, Integer> users() {
        return this.users;
    }
}
