package backend.models;

import java.time.LocalDateTime;

public class AvailabilityCheckRequest {
    private LocalDateTime start;
    private LocalDateTime end;

    public AvailabilityCheckRequest() {
    }

    public AvailabilityCheckRequest(LocalDateTime start, LocalDateTime end) {
        this.start = start;
        this.end = end;
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

    @Override
    public String toString() {
        return "AvailabilityCheckRequest{" +
                "start=" + start +
                ", end=" + end +
                '}';
    }
}
