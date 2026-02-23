package backend.models;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type", visible = true)
@JsonSubTypes({
    @JsonSubTypes.Type(value = IcalCalendar.class, name = "ICAL"),
    @JsonSubTypes.Type(value = GoogleCalendar.class, name = "GOOGLE")
})
public abstract class Calendar {
    private String id;
    private String user;
    private CalendarProviderType type;

    protected Calendar(String user, CalendarProviderType type) {
        this.id = UUID.randomUUID().toString();
        this.user = user;
        this.type = type;
    }

    public String getId() {
        return id;
    }

    public String getUser() {
        return user;
    }

    public CalendarProviderType getType() {
        return type;
    }
}
