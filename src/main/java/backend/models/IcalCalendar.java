package backend.models;

public class IcalCalendar extends Calendar {
    private String sourceUrl;

    public IcalCalendar(String user, String sourceUrl) {
        super(user, CalendarProviderType.ICAL);
        this.sourceUrl = sourceUrl;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }
}
