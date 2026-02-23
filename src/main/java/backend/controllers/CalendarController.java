package backend.controllers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import backend.models.Calendar;
import backend.models.CalendarConnectionRequest;
import backend.services.CalendarIntegrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api")
@Tag(name = "Calendars", description = "Connect user calendars (simple version)")
public class CalendarController {

    private final CalendarIntegrationService calendarIntegrationService;

    public CalendarController(CalendarIntegrationService calendarIntegrationService) {
        this.calendarIntegrationService = calendarIntegrationService;
    }

    @Operation(summary = "Get supported calendar providers")
    @GetMapping("/calendars/providers")
    public List<Map<String, Object>> getProviders() {
        return calendarIntegrationService.supportedProviders();
    }

    @Operation(summary = "Connect a user calendar")
    @PostMapping("/users/{user}/calendars")
    public Map<String, Object> connect(
            @PathVariable("user") String user,
            @RequestBody CalendarConnectionRequest request) {
        try {
            Calendar connection = calendarIntegrationService.connect(user, request);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("connection", connection);
            return response;
        } catch (IllegalArgumentException ex) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", ex.getMessage());
            return response;
        }
    }

    @Operation(summary = "List all calendar connections for a user")
    @GetMapping("/users/{user}/calendars")
    public List<Calendar> getUserConnections(@PathVariable("user") String user) {
        return calendarIntegrationService.getConnectionsForUser(user);
    }

    @Operation(summary = "Delete one calendar connection for a user")
    @DeleteMapping("/users/{user}/calendars/{connectionId}")
    public Map<String, Object> deleteConnection(
            @PathVariable("user") String user,
            @PathVariable("connectionId") String connectionId) {
        boolean removed = calendarIntegrationService.removeConnection(user, connectionId);
        Map<String, Object> response = new HashMap<>();
        response.put("success", removed);
        if (!removed) {
            response.put("error", "Connexion introuvable");
        }
        return response;
    }
}
