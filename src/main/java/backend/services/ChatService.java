package backend.services;

import java.util.List;

import org.springframework.stereotype.Service;

import backend.models.Message;
import backend.models.Chat;
import backend.models.User;

@Service
public class ChatService {
    private Chat publicChat;
    private final UserService userService;

    public ChatService(UserService userService) {
        this.userService = userService;
        User admin = userService.getUser("admin");
        this.publicChat = new Chat("Global Chat", admin);
    }

    public List<Message> getMessages() {
        return publicChat.getMessages();
    }

    public void addMessage(String username, String content) {
        User sender = userService.getUser(username);
        publicChat.sendMessage(sender, content);
    }
}