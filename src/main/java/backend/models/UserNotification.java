package backend.models;

import java.time.LocalDateTime;

public class UserNotification {
    private final String title;
    private final String message;
    private final String watchPartyName;
    private final LocalDateTime createdAt;

    public UserNotification(String title, String message, String watchPartyName, LocalDateTime createdAt) {
        this.title = title;
        this.message = message;
        this.watchPartyName = watchPartyName;
        this.createdAt = createdAt;
    }

    public String getTitle() {
        return title;
    }

    public String getMessage() {
        return message;
    }

    public String getWatchPartyName() {
        return watchPartyName;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
