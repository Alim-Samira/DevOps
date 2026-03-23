package backend.services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import backend.models.UserNotification;

@Service
public class NotificationService {
    private final Map<String, List<UserNotification>> notificationsByUser = new HashMap<>();

    public void addNotification(String username, UserNotification notification) {
        if (username == null || username.isBlank() || notification == null) {
            return;
        }

        String key = username.trim().toLowerCase();
        notificationsByUser.computeIfAbsent(key, ignored -> new ArrayList<>()).add(notification);
    }

    public List<UserNotification> getNotifications(String username) {
        if (username == null || username.isBlank()) {
            return new ArrayList<>();
        }

        String key = username.trim().toLowerCase();
        return new ArrayList<>(notificationsByUser.getOrDefault(key, new ArrayList<>()));
    }
}
