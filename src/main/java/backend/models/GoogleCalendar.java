package backend.models;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class GoogleCalendar extends Calendar {
    private String calendarId;
    @JsonIgnore
    private String oauthAccessToken;

    public GoogleCalendar(String user, String calendarId, String oauthAccessToken) {
        super(user, CalendarProviderType.GOOGLE);
        this.calendarId = calendarId;
        this.oauthAccessToken = oauthAccessToken;
    }

    public String getCalendarId() {
        return calendarId;
    }

    public String getOauthAccessToken() {
        return oauthAccessToken;
    }
}
