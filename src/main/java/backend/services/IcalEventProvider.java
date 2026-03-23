package backend.services;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import backend.models.CalendarEvent;

public class IcalEventProvider {

    private static final DateTimeFormatter ICAL_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss");
    private static final DateTimeFormatter ICAL_FORMATTER_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final long CACHE_TTL_MINUTES = 30;
    
    // Cache en mémoire : URL → événements + timestamp
    private static final Map<String, CachedCalendarData> cache = new HashMap<>();

    /**
     * Données en cache avec timestamp pour gérer l'expiration (30min)
     */
    private static class CachedCalendarData {
        final List<CalendarEvent> events;
        final long cachedAt;

        CachedCalendarData(List<CalendarEvent> events) {
            this.events = events;
            this.cachedAt = System.currentTimeMillis();
        }

        boolean isExpired() {
            long ageMs = System.currentTimeMillis() - cachedAt;
            long ttlMs = CACHE_TTL_MINUTES * 60 * 1000;
            return ageMs > ttlMs;
        }
    }

    /**
     * Récupère les événements d'une URL iCal (avec cache 30min)
     */
    public static List<CalendarEvent> fetchEventsFromUrl(String sourceUrl) throws Exception {
        // Vérifie si c'est en cache
        CachedCalendarData cached = cache.get(sourceUrl);
        if (cached != null && !cached.isExpired()) {
            return cached.events;
        }

        // Télécharge et parse le fichier
        List<CalendarEvent> events = downloadAndParseCalendar(sourceUrl);
        
        // Stocke en cache
        cache.put(sourceUrl, new CachedCalendarData(events));
        
        return events;
    }

    /**
     * Télécharge et parse le fichier .ics
     */
    private static List<CalendarEvent> downloadAndParseCalendar(String sourceUrl) throws Exception {
        List<CalendarEvent> events = new ArrayList<>();

        // Télécharge le fichier
        URL url = new URL(sourceUrl);
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        }

        String icsContent = content.toString();

        // Extrait les blocs VEVENT
        Pattern vEventPattern = Pattern.compile("BEGIN:VEVENT(.*?)END:VEVENT", Pattern.DOTALL);
        Matcher vEventMatcher = vEventPattern.matcher(icsContent);

        while (vEventMatcher.find()) {
            String vEventBlock = vEventMatcher.group(1);
            CalendarEvent event = parseVEvent(vEventBlock);
            if (event != null) {
                events.add(event);
            }
        }

        return events;
    }

    /**
     * Récupère les événements, filtrés sur le jour demandé (+ lendemain si multi-jours)
     */
    public static List<CalendarEvent> fetchEventsFromUrl(String sourceUrl, LocalDateTime referenceDate) throws Exception {
        List<CalendarEvent> allEvents = fetchEventsFromUrl(sourceUrl);
        
        // Filtre sur la date et le jour suivant (évite de charger tous les événements)
        java.time.LocalDate targetDate = referenceDate.toLocalDate();
        java.time.LocalDate nextDate = targetDate.plusDays(1);
        
        List<CalendarEvent> filtered = new ArrayList<>();
        for (CalendarEvent event : allEvents) {
            java.time.LocalDate eventDate = event.getStart().toLocalDate();
            if (eventDate.equals(targetDate) || eventDate.equals(nextDate)) {
                filtered.add(event);
            }
        }
        
        return filtered;
    }

    /**
     * Parse un bloc VEVENT (titre, dates, description)
     */
    private static CalendarEvent parseVEvent(String vEventBlock) {
        String title = extractField(vEventBlock, "SUMMARY");
        String startStr = extractField(vEventBlock, "DTSTART");
        String endStr = extractField(vEventBlock, "DTEND");
        String description = extractField(vEventBlock, "DESCRIPTION");

        if (title == null || startStr == null || endStr == null) {
            return null;
        }

        try {
            LocalDateTime start = parseICalDateTime(startStr);
            LocalDateTime end = parseICalDateTime(endStr);
            return new CalendarEvent(title, start, end, description != null ? description : "");
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Extrait un champ du VEVENT (ex: SUMMARY, DTSTART, DTEND)
     */
    private static String extractField(String vEventBlock, String fieldName) {
        Pattern pattern = Pattern.compile(fieldName + "(?:;[^:]*)?:([^\n\r]*)");
        Matcher matcher = pattern.matcher(vEventBlock);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return null;
    }

    /**
     * Parse une date iCal (format yyyyMMdd'T'HHmmss ou yyyyMMdd seul)
     */
    private static LocalDateTime parseICalDateTime(String dateStr) {
        dateStr = dateStr.trim();
        try {
            if (dateStr.contains("T")) {
                if (dateStr.endsWith("Z")) {
                    dateStr = dateStr.substring(0, dateStr.length() - 1);
                }
                return LocalDateTime.parse(dateStr, ICAL_FORMATTER);
            } else {
                return LocalDateTime.parse(dateStr, ICAL_FORMATTER_DATE).withHour(0).withMinute(0);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse date: " + dateStr, e);
        }
    }

    /**
     * Retourne les événements qui chevauchent le créneau demandé
     */
    public static List<CalendarEvent> getEventsInTimeRange(List<CalendarEvent> allEvents, LocalDateTime start, LocalDateTime end) {
        List<CalendarEvent> result = new ArrayList<>();
        for (CalendarEvent event : allEvents) {
            if (event.getStart().isBefore(end) && event.getEnd().isAfter(start)) {
                result.add(event);
            }
        }
        return result;
    }

    /**
     * Vérifie si la personne est libre (pas d'événements conflictuels)
     */
    public static boolean isAvailable(List<CalendarEvent> allEvents, LocalDateTime start, LocalDateTime end) {
        return getEventsInTimeRange(allEvents, start, end).isEmpty();
    }
}
