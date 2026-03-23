package backend.models;

import java.time.LocalDateTime;

public class CalendarEvent {
    private String title;
    private LocalDateTime start;
    private LocalDateTime end;
    private String description;

    public CalendarEvent(String title, LocalDateTime start, LocalDateTime end, String description) {
        this.title = title;
        this.start = start;
        this.end = end;
        this.description = description;
    }

    public CalendarEvent() {
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public LocalDateTime getStart() {
        return start;
    }

    public void setStart(LocalDateTime start) {
        this.start = start;
    }

    public LocalDateTime getEnd() {
        return end;
    }

    public void setEnd(LocalDateTime end) {
        this.end = end;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
