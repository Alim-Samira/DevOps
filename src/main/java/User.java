public class User {
    private String name;
    private boolean isAdmin;
    private boolean isModerator; 
    private int points;          


    //Master Constructor (Sets everything)
    public User(String name, boolean isAdmin, boolean isModerator) {
        this.name = name;
        this.isAdmin = isAdmin;
        this.isModerator = isModerator;
        this.points = 200; // Default starting points for everyone
    }

    
    // Automatically sets isModerator to false.
    public User(String name, boolean isAdmin) {
        this(name, isAdmin, false); // Chains to the Master Constructor
    }

    // Creates an Anonymous user with no permissions. 
    public User() {
        this("Anonymous", false, false); 
    }


    public String getName() {
        return name;
    }

    public boolean isAdmin() {
        return isAdmin;
    }

    public boolean isModerator() {
        return isModerator;
    }

    public int getPoints() {
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
    
    public void setModerator(boolean isModerator) {
        this.isModerator = isModerator;
    }
}
// 