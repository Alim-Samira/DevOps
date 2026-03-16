package backend.services;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;

import backend.models.Calendar;
import backend.models.CalendarConnectionRequest;
import backend.models.CalendarEvent;
import backend.models.CalendarProviderType;
import backend.models.GoogleCalendar;
import backend.models.IcalCalendar;

@Service
public class CalendarIntegrationService {
    private final Map<String, List<Calendar>> connectionsByUser = new HashMap<>();

    public List<Map<String, Object>> supportedProviders() {
        List<Map<String, Object>> providers = new ArrayList<>();
        providers.add(providerInfo(CalendarProviderType.ICAL, "Lien public .ics", List.of("sourceUrl")));
        providers.add(providerInfo(CalendarProviderType.GOOGLE, "Google Calendar OAuth", List.of("oauthAccessToken", "externalCalendarId (optionnel)")));
        return providers;
    }

    public Calendar connect(String user, CalendarConnectionRequest request) {
        validateRequest(user, request);
        String normalizedUser = user.trim().toLowerCase();
        CalendarProviderType provider = CalendarProviderType.valueOf(request.getProvider().trim().toUpperCase());

        Calendar calendar;
        if (provider == CalendarProviderType.ICAL) {
            String sourceUrl = normalize(request.getSourceUrl());
            calendar = new IcalCalendar(normalizedUser, sourceUrl);
        } else {
            String calendarId = defaultIfBlank(normalize(request.getExternalCalendarId()), "primary");
            calendar = new GoogleCalendar(normalizedUser, calendarId, normalize(request.getOauthAccessToken()));
        }

        connectionsByUser.computeIfAbsent(normalizedUser, ignored -> new ArrayList<>()).add(calendar);
        return calendar;
    }

    public List<Calendar> getConnectionsForUser(String user) {
        if (user == null || user.isBlank()) {
            return new ArrayList<>();
        }
        return new ArrayList<>(connectionsByUser.getOrDefault(user.trim().toLowerCase(), new ArrayList<>()));
    }

    public boolean removeConnection(String user, String connectionId) {
        if (user == null || user.isBlank() || connectionId == null || connectionId.isBlank()) {
            return false;
        }

        String key = user.trim().toLowerCase();
        List<Calendar> userConnections = connectionsByUser.get(key);
        if (userConnections == null) {
            return false;
        }

        boolean removed = userConnections.removeIf(connection -> connection.getId().equals(connectionId));
        if (userConnections.isEmpty()) {
            connectionsByUser.remove(key);
        }
        return removed;
    }

    private Map<String, Object> providerInfo(CalendarProviderType type, String description, List<String> requiredFields) {
        Map<String, Object> provider = new HashMap<>();
        provider.put("provider", type.name());
        provider.put("description", description);
        provider.put("requiredFields", requiredFields);
        return provider;
    }

    private void validateRequest(String user, CalendarConnectionRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Body manquant");
        }
        if (user == null || user.isBlank()) {
            throw new IllegalArgumentException("Le champ 'user' est requis");
        }
        if (request.getProvider() == null || request.getProvider().isBlank()) {
            throw new IllegalArgumentException("Le champ 'provider' est requis");
        }

        CalendarProviderType provider;
        try {
            provider = CalendarProviderType.valueOf(request.getProvider().trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Provider invalide. Valeurs supportees: " + java.util.Arrays.toString(CalendarProviderType.values()));
        }

        switch (provider) {
            case ICAL -> validateIcal(request);
            case GOOGLE -> validateGoogle(request);
            default -> throw new IllegalArgumentException("Provider non supporte");
        }
    }

    private void validateIcal(CalendarConnectionRequest request) {
        if (isBlank(request.getSourceUrl())) {
            throw new IllegalArgumentException("Pour ICAL, le champ 'sourceUrl' est requis");
        }
        String sourceUrl = request.getSourceUrl().trim();
        if (!sourceUrl.startsWith("http://") && !sourceUrl.startsWith("https://")) {
            throw new IllegalArgumentException("'sourceUrl' doit commencer par http:// ou https://");
        }
    }

    private void validateGoogle(CalendarConnectionRequest request) {
        if (isBlank(request.getOauthAccessToken())) {
            throw new IllegalArgumentException("Pour GOOGLE, le champ 'oauthAccessToken' est requis");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String defaultIfBlank(String value, String fallback) {
        return isBlank(value) ? fallback : value;
    }

    private String normalize(String value) {
        return Optional.ofNullable(value)
                .map(String::trim)
                .filter(v -> !v.isEmpty())
                .orElse(null);
    }

    /**
     * Récupère les événements d'un calendrier ICAL pour un créneau donné
     */
    public List<CalendarEvent> getEventsForCalendar(String user, String connectionId, LocalDateTime start, LocalDateTime end) {
        List<Calendar> userConnections = getConnectionsForUser(user);
        
        for (Calendar connection : userConnections) {
            if (connection.getId().equals(connectionId) && connection instanceof IcalCalendar) {
                IcalCalendar icalCal = (IcalCalendar) connection;
                try {
                    List<CalendarEvent> allEvents = IcalEventProvider.fetchEventsFromUrl(icalCal.getSourceUrl(), start);
                    return IcalEventProvider.getEventsInTimeRange(allEvents, start, end);
                } catch (Exception e) {
                    throw new RuntimeException("Erreur lors de la recuperation des evenements: " + e.getMessage());
                }
            }
        }
        
        throw new IllegalArgumentException("Calendrier " + connectionId + " non trouve pour l'utilisateur " + user);
    }

    /**
     * Vérifie si un utilisateur est libre sur tous ses calendriers ICAL
     */
    public boolean checkAvailability(String user, LocalDateTime start, LocalDateTime end) {
        List<Calendar> userConnections = getConnectionsForUser(user);
        
        for (Calendar connection : userConnections) {
            if (connection instanceof IcalCalendar) {
                IcalCalendar icalCal = (IcalCalendar) connection;
                try {
                    List<CalendarEvent> allEvents = IcalEventProvider.fetchEventsFromUrl(icalCal.getSourceUrl(), start);
                    if (!IcalEventProvider.isAvailable(allEvents, start, end)) {
                        return false;  // Conflit trouvé
                    }
                } catch (Exception e) {
                    System.err.println("Erreur lors de la vérification de disponibilité: " + e.getMessage());
                }
            }
        }
        
        return true;  // Aucun conflit
    }
}
