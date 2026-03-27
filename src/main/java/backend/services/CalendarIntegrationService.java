package backend.services;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import backend.models.Calendar;
import backend.models.CalendarConnectionRequest;
import backend.models.CalendarEvent;
import backend.models.CalendarProviderType;
import backend.models.GoogleCalendar;
import backend.models.GoogleCalendarDeliveryMode;
import backend.models.IcalCalendar;
import backend.models.WatchParty;

@Service
public class CalendarIntegrationService {

    private static final Logger log = LoggerFactory.getLogger(CalendarIntegrationService.class);
    private static final int DEFAULT_WATCH_PARTY_DURATION_HOURS = 2;
    private static final String DEFAULT_GOOGLE_CALENDAR_API_BASE_URL = "https://www.googleapis.com/calendar/v3";

    private final Map<String, List<Calendar>> connectionsByUser = new HashMap<>();
    private final RestClient restClient;
    private final String googleCalendarApiBaseUrl;

    public CalendarIntegrationService() {
        this(RestClient.builder().build(), DEFAULT_GOOGLE_CALENDAR_API_BASE_URL);
    }

    @Autowired
    public CalendarIntegrationService(ObjectProvider<RestClient.Builder> restClientBuilderProvider) {
        this(restClientBuilderProvider.getIfAvailable(RestClient::builder).build(), DEFAULT_GOOGLE_CALENDAR_API_BASE_URL);
    }

    CalendarIntegrationService(RestClient restClient) {
        this(restClient, DEFAULT_GOOGLE_CALENDAR_API_BASE_URL);
    }

    CalendarIntegrationService(RestClient restClient, String googleCalendarApiBaseUrl) {
        this.restClient = restClient;
        this.googleCalendarApiBaseUrl = googleCalendarApiBaseUrl;
    }

    public List<Map<String, Object>> supportedProviders() {
        List<Map<String, Object>> providers = new ArrayList<>();
        providers.add(providerInfo(CalendarProviderType.ICAL, "Lien public .ics", List.of("sourceUrl")));
        providers.add(providerInfo(
                CalendarProviderType.GOOGLE,
                "Google Calendar (connexion OAuth ou invitation par email)",
                List.of(
                        "externalCalendarId (optionnel)",
                        "googleDeliveryMode=APP_ONLY|GOOGLE_INVITE",
                        "inviteEmail (requis si GOOGLE_INVITE)")));
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
            GoogleCalendarDeliveryMode deliveryMode = parseGoogleDeliveryMode(request.getGoogleDeliveryMode());
            calendar = new GoogleCalendar(
                    normalizedUser,
                    calendarId,
                    normalize(request.getOauthAccessToken()),
                    deliveryMode,
                    normalize(request.getInviteEmail()));
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

    public boolean hasConnectedCalendar(String user) {
        return !getConnectionsForUser(user).isEmpty();
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

    public Map<String, Object> addWatchPartyToGoogleCalendar(String user, WatchParty watchParty, String connectionId) {
        if (watchParty == null) {
            throw new IllegalArgumentException("Watch party introuvable");
        }
        if (watchParty.getDate() == null) {
            throw new IllegalArgumentException("La watch party doit avoir une date pour etre ajoutee au calendrier");
        }

        LocalDateTime start = watchParty.getDate();
        LocalDateTime end = start.plusHours(DEFAULT_WATCH_PARTY_DURATION_HOURS);
        String description = "WatchParty " + watchParty.getName() + " sur " + watchParty.getGame();

        CalendarEvent event = new CalendarEvent(watchParty.getName(), start, end, description);
        return createGoogleCalendarEvent(user, connectionId, event);
    }

    public Map<String, Object> createGoogleCalendarEvent(String user, String connectionId, CalendarEvent event) {
        validateEventCreationRequest(user, event);
        GoogleCalendar googleCalendar = findGoogleCalendarConnection(user, connectionId);

        Map<String, Object> payload = new HashMap<>();
        payload.put("summary", event.getTitle());
        payload.put("description", defaultIfBlank(normalize(event.getDescription()), ""));
        payload.put("start", googleDateTimePayload(event.getStart()));
        payload.put("end", googleDateTimePayload(event.getEnd()));

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restClient.post()
                    .uri(googleCalendarApiBaseUrl + "/calendars/{calendarId}/events",
                            googleCalendar.getCalendarId())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + googleCalendar.getOauthAccessToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .body(Map.class);

            return response != null ? response : Map.of();
        } catch (RestClientResponseException ex) {
            String responseBody = ex.getResponseBodyAsString();
            String detail = isBlank(responseBody) ? ex.getMessage() : responseBody;
            throw new IllegalArgumentException("Erreur Google Calendar: " + detail);
        }
    }

    public List<CalendarEvent> getEventsForCalendar(
            String user, String connectionId, LocalDateTime start, LocalDateTime end) {
        for (Calendar connection : getConnectionsForUser(user)) {
            if (connection.getId().equals(connectionId)) {
                return getEventsForConnection(connection, start, end);
            }
        }

        throw new IllegalArgumentException("Calendrier " + connectionId + " non trouve pour l'utilisateur " + user);
    }

    public boolean checkAvailability(String user, LocalDateTime start, LocalDateTime end) {
        for (Calendar connection : getConnectionsForUser(user)) {
            try {
                if (!getEventsForConnection(connection, start, end).isEmpty()) {
                    return false;
                }
            } catch (CalendarAccessException e) {
                log.warn("Erreur lors de la verification de disponibilite pour {}", user, e);
            }
        }

        return true;
    }

    public boolean canAttendWatchParty(String user, LocalDateTime start, LocalDateTime end) {
        List<Calendar> userConnections = getConnectionsForUser(user);
        if (userConnections.isEmpty()) {
            return false;
        }

        boolean checkedCalendar = false;
        for (Calendar connection : userConnections) {
            checkedCalendar = true;
            try {
                if (!getEventsForConnection(connection, start, end).isEmpty()) {
                    return false;
                }
            } catch (CalendarAccessException e) {
                log.debug("Impossible de verifier la disponibilite de {} sur {}", user, connection.getId(), e);
                return false;
            }
        }

        return checkedCalendar;
    }

    public boolean canCheckAvailability(String user) {
        for (Calendar connection : getConnectionsForUser(user)) {
            if (connection instanceof IcalCalendar) {
                return true;
            }
            if (connection instanceof GoogleCalendar googleCalendar && googleCalendar.hasOauthAccessToken()) {
                return true;
            }
        }
        return false;
    }

    public Map<String, Object> acceptWatchPartyCalendarInvite(String user, WatchParty watchParty, String connectionId) {
        if (watchParty == null) {
            throw new IllegalArgumentException("Watch party introuvable");
        }
        if (watchParty.getDate() == null) {
            throw new IllegalArgumentException("La watch party doit avoir une date");
        }

        LocalDateTime start = watchParty.getDate();
        LocalDateTime end = start.plusHours(DEFAULT_WATCH_PARTY_DURATION_HOURS);
        if (!canAttendWatchParty(user, start, end)) {
            throw new IllegalArgumentException("Utilisateur indisponible sur ce creneau");
        }

        return addWatchPartyToGoogleCalendar(user, watchParty, connectionId);
    }

    public boolean prefersGoogleInvite(String user) {
        for (Calendar connection : getConnectionsForUser(user)) {
            if (connection instanceof GoogleCalendar googleCalendar && googleCalendar.usesGoogleInvites()) {
                return true;
            }
        }
        return false;
    }

    public boolean sendWatchPartyInviteViaGoogle(String organizerUser, String attendeeUser, WatchParty watchParty) {
        if (watchParty == null || organizerUser == null || organizerUser.isBlank()
                || attendeeUser == null || attendeeUser.isBlank()) {
            return false;
        }

        GoogleCalendar organizerCalendar = findGoogleCalendarConnection(organizerUser, null);
        GoogleCalendar attendeeCalendar = findGoogleInviteConnection(attendeeUser);

        Map<String, Object> payload = new HashMap<>();
        payload.put("summary", watchParty.getName());
        payload.put("description", "Invitation WatchParty " + watchParty.getName() + " sur " + watchParty.getGame());
        payload.put("start", googleDateTimePayload(watchParty.getDate()));
        payload.put("end", googleDateTimePayload(watchParty.getDate().plusHours(DEFAULT_WATCH_PARTY_DURATION_HOURS)));
        payload.put("attendees", List.of(Map.of("email", attendeeCalendar.getInviteEmail())));

        try {
            restClient.post()
                    .uri(
                            googleCalendarApiBaseUrl
                                    + "/calendars/{calendarId}/events?sendUpdates=all",
                            organizerCalendar.getCalendarId())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + organizerCalendar.getOauthAccessToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .body(Map.class);
            return true;
        } catch (RestClientResponseException ex) {
            throw new IllegalArgumentException("Erreur Google Calendar: " + extractGoogleErrorDetail(ex));
        }
    }

    private Map<String, Object> providerInfo(
            CalendarProviderType type, String description, List<String> requiredFields) {
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
            throw new IllegalArgumentException(
                    "Provider invalide. Valeurs supportees: " + java.util.Arrays.toString(CalendarProviderType.values()));
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
        GoogleCalendarDeliveryMode deliveryMode = parseGoogleDeliveryMode(request.getGoogleDeliveryMode());
        if (deliveryMode == GoogleCalendarDeliveryMode.APP_ONLY && isBlank(request.getOauthAccessToken())) {
            throw new IllegalArgumentException("Pour GOOGLE en mode APP_ONLY, le champ 'oauthAccessToken' est requis");
        }
        if (deliveryMode == GoogleCalendarDeliveryMode.GOOGLE_INVITE) {
            if (isBlank(request.getInviteEmail())) {
                throw new IllegalArgumentException("Pour GOOGLE_INVITE, le champ 'inviteEmail' est requis");
            }
            if (!request.getInviteEmail().contains("@")) {
                throw new IllegalArgumentException("'inviteEmail' doit etre une adresse email valide");
            }
        }
    }

    private void validateEventCreationRequest(String user, CalendarEvent event) {
        if (user == null || user.isBlank()) {
            throw new IllegalArgumentException("Le champ 'user' est requis");
        }
        if (event == null) {
            throw new IllegalArgumentException("Le corps de la requete est requis");
        }
        if (isBlank(event.getTitle())) {
            throw new IllegalArgumentException("Le titre de l'evenement est requis");
        }
        if (event.getStart() == null || event.getEnd() == null) {
            throw new IllegalArgumentException("Les dates de debut et de fin sont requises");
        }
        if (!event.getEnd().isAfter(event.getStart())) {
            throw new IllegalArgumentException("La date de fin doit etre apres la date de debut");
        }
    }

    private GoogleCalendar findGoogleCalendarConnection(String user, String connectionId) {
        List<Calendar> userConnections = getConnectionsForUser(user);

        for (Calendar connection : userConnections) {
            boolean connectionMatches = connectionId == null
                    || connectionId.isBlank()
                    || connection.getId().equals(connectionId);
            if (connectionMatches && connection instanceof GoogleCalendar googleCalendar) {
                return googleCalendar;
            }
        }

        if (connectionId == null || connectionId.isBlank()) {
            throw new IllegalArgumentException("Aucun calendrier Google connecte pour l'utilisateur " + user);
        }
        throw new IllegalArgumentException("Calendrier Google " + connectionId + " non trouve pour l'utilisateur " + user);
    }

    private GoogleCalendar findGoogleInviteConnection(String user) {
        for (Calendar connection : getConnectionsForUser(user)) {
            if (connection instanceof GoogleCalendar googleCalendar && googleCalendar.usesGoogleInvites()) {
                if (isBlank(googleCalendar.getInviteEmail())) {
                    throw new IllegalArgumentException("Aucune adresse d'invitation Google configuree pour " + user);
                }
                return googleCalendar;
            }
        }
        throw new IllegalArgumentException("Aucune connexion Google en mode invitation pour l'utilisateur " + user);
    }

    private List<CalendarEvent> getEventsForConnection(Calendar connection, LocalDateTime start, LocalDateTime end) {
        try {
            if (connection instanceof IcalCalendar icalCal) {
                List<CalendarEvent> allEvents = IcalEventProvider.fetchEventsFromUrl(icalCal.getSourceUrl(), start);
                return IcalEventProvider.getEventsInTimeRange(allEvents, start, end);
            }
            if (connection instanceof GoogleCalendar googleCalendar) {
                if (!googleCalendar.hasOauthAccessToken()) {
                    throw new CalendarAccessException(
                            "Ce calendrier Google est configure pour les invitations seulement et ne permet pas la lecture des evenements",
                            null);
                }
                return fetchGoogleCalendarEvents(googleCalendar, start, end);
            }
        } catch (IOException e) {
            throw new CalendarAccessException("Erreur lors de la recuperation des evenements", e);
        } catch (RestClientResponseException e) {
            throw googleCalendarAccessException(e);
        } catch (RuntimeException e) {
            throw new CalendarAccessException("Erreur lors de la recuperation des evenements", e);
        }

        throw new IllegalArgumentException("Provider calendrier non supporte");
    }

    private List<CalendarEvent> fetchGoogleCalendarEvents(
            GoogleCalendar googleCalendar, LocalDateTime start, LocalDateTime end) {
        @SuppressWarnings("unchecked")
        Map<String, Object> response = restClient.get()
                .uri(googleCalendarApiBaseUrl
                                + "/calendars/{calendarId}/events?singleEvents=true&orderBy=startTime&timeMin={timeMin}&timeMax={timeMax}",
                        googleCalendar.getCalendarId(),
                        toGoogleDateTimeValue(start),
                        toGoogleDateTimeValue(end))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + googleCalendar.getOauthAccessToken())
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(Map.class);

        return mapGoogleEvents(response);
    }

    private List<CalendarEvent> mapGoogleEvents(Map<String, Object> response) {
        List<CalendarEvent> events = new ArrayList<>();
        if (response == null) {
            return events;
        }

        Object rawItems = response.get("items");
        if (!(rawItems instanceof List<?> items)) {
            return events;
        }

        for (Object rawItem : items) {
            if (!(rawItem instanceof Map<?, ?> item)) {
                continue;
            }

            LocalDateTime eventStart = extractGoogleEventBoundary(item.get("start"));
            LocalDateTime eventEnd = extractGoogleEventBoundary(item.get("end"));
            if (eventStart == null || eventEnd == null || !eventEnd.isAfter(eventStart)) {
                continue;
            }

            String title = stringifyGoogleField(item.get("summary"), "Evenement Google Calendar");
            String description = stringifyGoogleField(item.get("description"), "");
            events.add(new CalendarEvent(title, eventStart, eventEnd, description));
        }

        return events;
    }

    private LocalDateTime extractGoogleEventBoundary(Object rawBoundary) {
        if (!(rawBoundary instanceof Map<?, ?> boundary)) {
            return null;
        }

        Object dateTime = boundary.get("dateTime");
        if (dateTime instanceof String dateTimeValue && !dateTimeValue.isBlank()) {
            return OffsetDateTime.parse(dateTimeValue).toLocalDateTime();
        }

        Object date = boundary.get("date");
        if (date instanceof String dateValue && !dateValue.isBlank()) {
            return LocalDate.parse(dateValue).atStartOfDay();
        }

        return null;
    }

    private String stringifyGoogleField(Object rawValue, String fallback) {
        return rawValue instanceof String stringValue && !stringValue.isBlank() ? stringValue : fallback;
    }

    private String toGoogleDateTimeValue(LocalDateTime dateTime) {
        return dateTime.atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }

    private CalendarAccessException googleCalendarAccessException(RestClientResponseException ex) {
        return new CalendarAccessException("Erreur Google Calendar: " + extractGoogleErrorDetail(ex), ex);
    }

    private String extractGoogleErrorDetail(RestClientResponseException ex) {
        String responseBody = ex.getResponseBodyAsString();
        return isBlank(responseBody) ? ex.getMessage() : responseBody;
    }

    private GoogleCalendarDeliveryMode parseGoogleDeliveryMode(String rawMode) {
        if (isBlank(rawMode)) {
            return GoogleCalendarDeliveryMode.APP_ONLY;
        }
        try {
            return GoogleCalendarDeliveryMode.valueOf(rawMode.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(
                    "Mode Google invalide. Valeurs supportees: " + java.util.Arrays.toString(GoogleCalendarDeliveryMode.values()));
        }
    }

    private Map<String, String> googleDateTimePayload(LocalDateTime dateTime) {
        ZoneId zone = ZoneId.systemDefault();
        Map<String, String> payload = new HashMap<>();
        payload.put("dateTime", toGoogleDateTimeValue(dateTime));
        payload.put("timeZone", zone.getId());
        return payload;
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

    private static final class CalendarAccessException extends IllegalStateException {
        private CalendarAccessException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
