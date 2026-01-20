package backend.models;

import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType; // For auto-generating IDs
import jakarta.persistence.Id; // For primary key 
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.persistence.Transient;

@Entity
@Table(name = "users") // Maps this class to the 'users' table in Neon
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // Unique ID required by the database

    @Column(name = "created_at")
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt; // To track when the user was created


    @Column(name = "username", nullable = false, unique = true)
    private String name;

    @Column(name = "is_admin")
    private boolean isAdmin;

    // @Transient tells Java: to Keep this in memory, but don't try to save it to the DB
    @Transient
    private boolean isModerator; 

    @Column(columnDefinition = "INTEGER DEFAULT 200")
    private int points;

    // Master Constructor (Sets everything)
    public User(String name, boolean isAdmin, boolean isModerator) {
        this.name = name;
        this.isAdmin = isAdmin;
        this.isModerator = isModerator;
        this.points = 200;
        this.createdAt = new Date();
    }


    public User(String name, boolean isAdmin) {
        this(name, isAdmin, false); 
    }

    public User() {
        this("Anonymous", false, false); 
    }

    // Added Getter for ID (useful for database operations)
    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isAdmin() {
        return isAdmin;
    }

    public void setAdmin(boolean isAdmin) {
        this.isAdmin = isAdmin;
    }

    public boolean isModerator() {
        return isModerator;
    }

    public void setModerator(boolean isModerator) {
        this.isModerator = isModerator;
    }

    public int getPoints() {
        return points;
    }

    public void setPoints(int points) {
        this.points = points;
    }
}