package backend.services;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import backend.models.CalendarEvent;

public class IcalEventProvider {

    private static final DateTimeFormatter ICAL_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss");
    private static final DateTimeFormatter ICAL_FORMATTER_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

    /**
     * Fetch and parse events from an iCalendar URL
     */
    public static List<CalendarEvent> fetchEventsFromUrl(String sourceUrl) throws Exception {
        List<CalendarEvent> events = new ArrayList<>();

        // Fetch the .ics file
        URL url = new URL(sourceUrl);
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        }

        String icsContent = content.toString();

        // Extract VEVENT blocks
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
     * Fetch and parse events from an iCalendar URL, filtered by date range
     * Only returns events that start on referenceDate or the next day
     */
    public static List<CalendarEvent> fetchEventsFromUrl(String sourceUrl, LocalDateTime referenceDate) throws Exception {
        List<CalendarEvent> allEvents = fetchEventsFromUrl(sourceUrl);
        
        // Filter to only include events on the reference date or next day
        java.time.LocalDate targetDate = referenceDate.toLocalDate();
        java.time.LocalDate nextDate = targetDate.plusDays(1);
        
        List<CalendarEvent> filtered = new ArrayList<>();
        for (CalendarEvent event : allEvents) {
            java.time.LocalDate eventDate = event.getStart().toLocalDate();
            // Keep event if it starts on targetDate or nextDate
            if (eventDate.equals(targetDate) || eventDate.equals(nextDate)) {
                filtered.add(event);
            }
        }
        
        return filtered;
    }

    /**
     * Parse a single VEVENT block
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
     * Extract field value from VEVENT (handles DTSTART;TZID=...:value)
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
     * Parse iCalendar datetime (yyyyMMdd'T'HHmmss or yyyyMMdd)
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
     * Get events that overlap with the given time range
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
     * Check if person is available (no conflicting events)
     */
    public static boolean isAvailable(List<CalendarEvent> allEvents, LocalDateTime start, LocalDateTime end) {
        return getEventsInTimeRange(allEvents, start, end).isEmpty();
    }
}
