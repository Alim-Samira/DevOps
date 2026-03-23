package backend.models;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.CollectionTable; // Importe all (Entity, Column, Id, ElementCollection...)
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

@Entity
@Table(name = "users") // Maps this class to the 'users' table in Neon
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // Unique ID required by the database

    @Column(name = "created_at", updatable = false)
    private Date createdAt; // To track when the user was created

    @Column(name = "username", nullable = false, unique = true)
    private String name;

    @Column(name = "is_admin", nullable = false, columnDefinition = "boolean default false")
    private boolean isAdmin;

    // @Transient tells Java: to Keep this in memory, but don't try to save it to the DB
    @Transient
    private boolean isModerator;

    @Column(name = "public_points", nullable = false, columnDefinition = "int default 200")
    private int publicPoints;

    // save pts inside watchparty in liked table
    @JsonIgnore
    @ElementCollection
    @CollectionTable(name = "user_wp_points", joinColumns = @JoinColumn(name = "user_id"))
    @MapKeyColumn(name = "wp_name")
    @Column(name = "points")
    private Map<String, Integer> pointsByWatchParty = new HashMap<>();

    @Column(name = "ical_url")
    private String icalUrl;

    public String getIcalUrl() { return icalUrl; }
    public void setIcalUrl(String url) { this.icalUrl = url; }

    // Add wins
    @Column(name = "public_wins", nullable = false, columnDefinition = "int default 0")
    private int publicWins;

    @JsonIgnore
    @ElementCollection
    @CollectionTable(name = "user_wp_wins", joinColumns = @JoinColumn(name = "user_id"))
    @MapKeyColumn(name = "wp_name")
    @Column(name = "wins")
    private Map<String, Integer> winsByWatchParty = new HashMap<>();

    // Master Constructor (Sets everything)
    public User(String name, boolean isAdmin, boolean isModerator) {
        this.name = name;
        this.isAdmin = isAdmin;
        this.isModerator = isModerator;
        this.publicPoints = 200; // Default points
        this.createdAt = new Date();
        // Initialisation des Maps
        this.pointsByWatchParty = new HashMap<>();
        this.winsByWatchParty = new HashMap<>();
        this.publicWins = 0;
    }

    public User(String name, boolean isAdmin) {
        this(name, isAdmin, false);
    }

    public User() {
        this("Anonymous", false, false);
    }

    // Added Getter for ID (useful for DB operations)
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

    // MANAGE PUBLIC PTS
    public int getPublicPoints() {
        return publicPoints;
    }

    public void addPublicPoints(int delta) {
        this.publicPoints = Math.max(0, this.publicPoints + delta);
    }

    // Alias
    public int getPoints() {
        return getPublicPoints();
    }

    public void setPoints(int points) {
        this.publicPoints = points;
    }

    // manage pts for watchparty
    public void addPointsForWatchParty(String wpName, int delta) {
        int current = pointsByWatchParty.getOrDefault(wpName, 0);
        pointsByWatchParty.put(wpName, Math.max(0, current + delta));
    }

    public int getPointsForWatchParty(String wpName) {
        return pointsByWatchParty.getOrDefault(wpName, 0);
    }

    public void setPointsForWatchParty(String wpName, int points) {
        pointsByWatchParty.put(wpName, points);
    }

    public Map<String, Integer> getPointsByWatchParty() {
        return new HashMap<>(pointsByWatchParty);
    }

    // manage wins
    public int getPublicWins() {
        return publicWins;
    }

    public void addPublicWin() {
        this.publicWins++;
    }

    public void addWinForWatchParty(String wpName) {
        winsByWatchParty.put(wpName, winsByWatchParty.getOrDefault(wpName, 0) + 1);
    }

    public int getWinsForWatchParty(String wpName) {
        return winsByWatchParty.getOrDefault(wpName, 0);
    }

    public Map<String, Integer> getWinsByWatchParty() {
        return new HashMap<>(winsByWatchParty);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof User other)) return false;
        // Compare by username (case-insensitive) or by ID if both are persisted
        if (this.name != null && other.name != null) {
            return this.name.equalsIgnoreCase(other.name);
        }
        return this.id != null && this.id.equals(other.id);
    }

    @Override
    public int hashCode() {
        // Use username for hashCode to be consistent with equals
        if (this.name != null) {
            return this.name.toLowerCase().hashCode();
        }
        if (this.id != null) {
            return this.id.hashCode();
        }
        return 0;
    }
}
