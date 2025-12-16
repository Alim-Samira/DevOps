package backend.controllers;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import backend.models.Message;
import backend.services.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/chat")
@Tag(name = "Chat System", description = "Public chat functionality")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @Operation(summary = "Get all messages")
    @GetMapping
    public List<Message> getHistory() {
        return chatService.getMessages();
    }

    @Operation(summary = "Send a message", description = "Send JSON: { \"user\": \"bob\", \"text\": \"hello\" }")
    @PostMapping
    public String sendMessage(@RequestBody Map<String, String> payload) {
        String user = payload.get("user");
        String text = payload.get("text");

        if (user == null || text == null) return "Error: 'user' and 'text' required";

        chatService.addMessage(user, text);
        return "Message sent";
    }
}