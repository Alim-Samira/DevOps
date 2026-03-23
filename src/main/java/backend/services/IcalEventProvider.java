package backend.services;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import backend.models.CalendarEvent;

public final class IcalEventProvider {

    private static final DateTimeFormatter ICAL_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss");
    private static final DateTimeFormatter ICAL_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final Pattern VEVENT_PATTERN = Pattern.compile("BEGIN:VEVENT(.*?)END:VEVENT", Pattern.DOTALL);
    private static final long CACHE_TTL_MINUTES = 30;

    private static final Map<String, CachedCalendarData> CACHE = new ConcurrentHashMap<>();

    private IcalEventProvider() {
        // Utility class.
    }

    public static List<CalendarEvent> fetchEventsFromUrl(String sourceUrl) throws IOException {
        CachedCalendarData cached = CACHE.get(sourceUrl);
        if (cached != null && !cached.isExpired()) {
            return cached.events();
        }

        List<CalendarEvent> events = downloadAndParseCalendar(sourceUrl);
        CACHE.put(sourceUrl, new CachedCalendarData(events));
        return events;
    }

    public static List<CalendarEvent> fetchEventsFromUrl(String sourceUrl, LocalDateTime referenceDate) throws IOException {
        List<CalendarEvent> allEvents = fetchEventsFromUrl(sourceUrl);
        LocalDate targetDate = referenceDate.toLocalDate();
        LocalDate nextDate = targetDate.plusDays(1);

        List<CalendarEvent> filtered = new ArrayList<>();
        for (CalendarEvent event : allEvents) {
            LocalDate eventDate = event.getStart().toLocalDate();
            if (eventDate.equals(targetDate) || eventDate.equals(nextDate)) {
                filtered.add(event);
            }
        }

        return filtered;
    }

    public static List<CalendarEvent> getEventsInTimeRange(List<CalendarEvent> allEvents, LocalDateTime start, LocalDateTime end) {
        List<CalendarEvent> result = new ArrayList<>();
        for (CalendarEvent event : allEvents) {
            if (event.getStart().isBefore(end) && event.getEnd().isAfter(start)) {
                result.add(event);
            }
        }
        return result;
    }

    public static boolean isAvailable(List<CalendarEvent> allEvents, LocalDateTime start, LocalDateTime end) {
        return getEventsInTimeRange(allEvents, start, end).isEmpty();
    }

    private static List<CalendarEvent> downloadAndParseCalendar(String sourceUrl) throws IOException {
        List<CalendarEvent> events = new ArrayList<>();
        StringBuilder content = new StringBuilder();

        URI uri = URI.create(sourceUrl);
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(uri.toURL().openStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append('\n');
            }
        }

        Matcher matcher = VEVENT_PATTERN.matcher(content);
        while (matcher.find()) {
            CalendarEvent event = parseVEvent(matcher.group(1));
            if (event != null) {
                events.add(event);
            }
        }

        return events;
    }

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
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static String extractField(String vEventBlock, String fieldName) {
        Pattern fieldPattern = Pattern.compile(fieldName + "(?:;[^:]*)?:([^\\n\\r]*)");
        Matcher matcher = fieldPattern.matcher(vEventBlock);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return null;
    }

    private static LocalDateTime parseICalDateTime(String dateStr) {
        String normalizedDate = dateStr.trim();
        try {
            if (normalizedDate.contains("T")) {
                if (normalizedDate.endsWith("Z")) {
                    normalizedDate = normalizedDate.substring(0, normalizedDate.length() - 1);
                }
                return LocalDateTime.parse(normalizedDate, ICAL_FORMATTER);
            }
            return LocalDate.parse(normalizedDate, ICAL_DATE_FORMATTER).atStartOfDay();
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("Failed to parse date: " + dateStr, ex);
        }
    }

    private record CachedCalendarData(List<CalendarEvent> events, long cachedAt) {

        private CachedCalendarData(List<CalendarEvent> events) {
            this(events, System.currentTimeMillis());
        }

        private boolean isExpired() {
            long ageMs = System.currentTimeMillis() - cachedAt;
            long ttlMs = CACHE_TTL_MINUTES * 60 * 1000;
            return ageMs > ttlMs;
        }
    }
}
