package backend.models;

public class UserCreateRequest {
    private String name;

    public UserCreateRequest() {
        // Default constructor required by Jackson for request deserialization.
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
