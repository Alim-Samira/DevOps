package backend.models;

public class UserCreateRequest {
    private String name;

    public UserCreateRequest() {}

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
