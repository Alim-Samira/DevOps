package backend.models;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class GoogleCalendar extends Calendar {
    private String calendarId;
    private GoogleCalendarDeliveryMode deliveryMode;
    private String inviteEmail;
    @JsonIgnore
    private String oauthAccessToken;

    public GoogleCalendar(
            String user,
            String calendarId,
            String oauthAccessToken,
            GoogleCalendarDeliveryMode deliveryMode,
            String inviteEmail) {
        super(user, CalendarProviderType.GOOGLE);
        this.calendarId = calendarId;
        this.oauthAccessToken = oauthAccessToken;
        this.deliveryMode = deliveryMode;
        this.inviteEmail = inviteEmail;
    }

    public String getCalendarId() {
        return calendarId;
    }

    public String getOauthAccessToken() {
        return oauthAccessToken;
    }

    public GoogleCalendarDeliveryMode getDeliveryMode() {
        return deliveryMode;
    }

    public String getInviteEmail() {
        return inviteEmail;
    }

    public boolean usesGoogleInvites() {
        return deliveryMode == GoogleCalendarDeliveryMode.GOOGLE_INVITE;
    }

    public boolean hasOauthAccessToken() {
        return oauthAccessToken != null && !oauthAccessToken.isBlank();
    }
}
