package backend.models;

public class CalendarConnectionRequest {
    private String provider;
    private String label;
    private String sourceUrl;
    private String oauthAccessToken;
    private String externalCalendarId;
    private String googleDeliveryMode;
    private String inviteEmail;

    public CalendarConnectionRequest() {
        // Default constructor required by Jackson for request deserialization.
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }

    public String getOauthAccessToken() {
        return oauthAccessToken;
    }

    public void setOauthAccessToken(String oauthAccessToken) {
        this.oauthAccessToken = oauthAccessToken;
    }

    public String getExternalCalendarId() {
        return externalCalendarId;
    }

    public void setExternalCalendarId(String externalCalendarId) {
        this.externalCalendarId = externalCalendarId;
    }

    public String getGoogleDeliveryMode() {
        return googleDeliveryMode;
    }

    public void setGoogleDeliveryMode(String googleDeliveryMode) {
        this.googleDeliveryMode = googleDeliveryMode;
    }

    public String getInviteEmail() {
        return inviteEmail;
    }

    public void setInviteEmail(String inviteEmail) {
        this.inviteEmail = inviteEmail;
    }
}
