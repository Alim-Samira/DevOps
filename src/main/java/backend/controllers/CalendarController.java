package backend.controllers;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import backend.models.Calendar;
import backend.models.CalendarConnectionRequest;
import backend.models.CalendarEvent;
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

    @Operation(summary = "Get events from a calendar in a time range")
    @GetMapping("/users/{user}/calendars/{connectionId}/events")
    public Map<String, Object> getEvents(
            @PathVariable("user") String user,
            @PathVariable("connectionId") String connectionId,
            @RequestParam("start") String startStr,
            @RequestParam("end") String endStr) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
            LocalDateTime start = LocalDateTime.parse(startStr, formatter);
            LocalDateTime end = LocalDateTime.parse(endStr, formatter);

            List<CalendarEvent> events = calendarIntegrationService.getEventsForCalendar(user, connectionId, start, end);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("events", events);
            response.put("count", events.size());
            return response;
        } catch (Exception ex) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", ex.getMessage());
            return response;
        }
    }

    @Operation(summary = "Create an event in a Google calendar")
    @PostMapping("/users/{user}/calendars/{connectionId}/events")
    public Map<String, Object> createEvent(
            @PathVariable("user") String user,
            @PathVariable("connectionId") String connectionId,
            @RequestBody CalendarEvent event) {
        try {
            Map<String, Object> createdEvent = calendarIntegrationService.createGoogleCalendarEvent(user, connectionId, event);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("event", createdEvent);
            return response;
        } catch (Exception ex) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", ex.getMessage());
            return response;
        }
    }

    @Operation(summary = "Check if a user is available during a time slot")
    @GetMapping("/users/{user}/availability")
    public Map<String, Object> checkAvailability(
            @PathVariable("user") String user,
            @RequestParam("start") String startStr,
            @RequestParam("end") String endStr) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
            LocalDateTime start = LocalDateTime.parse(startStr, formatter);
            LocalDateTime end = LocalDateTime.parse(endStr, formatter);

            boolean available = calendarIntegrationService.checkAvailability(user, start, end);
            
            Map<String, Object> response = new HashMap<>();
            response.put("user", user);
            response.put("start", start);
            response.put("end", end);
            response.put("available", available);
            return response;
        } catch (Exception ex) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", ex.getMessage());
            return response;
        }
    }
}
