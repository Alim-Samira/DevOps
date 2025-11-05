public class User {
    private String name;
    private boolean isAdmin;
    private int points;

    public User(String name, boolean isAdmin) {
        this.name = name;
        this.isAdmin = isAdmin;
        this.points = 200; // Default points from the original points-based version
    }

    // Default constructor from points-based version
    public User() {
        this.points = 200;
        this.name = "Anonymous";
        this.isAdmin = false;
    }

    public String getName() {
        return name;
    }

    public boolean isAdmin() {
        return isAdmin;
    }

    public int getPoints() {  // Renamed from Points() to follow Java naming conventions
        return points;
    }

    public void setPoints(int points) {
        this.points = points;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setAdmin(boolean isAdmin) {
        this.isAdmin = isAdmin;
    }
}
