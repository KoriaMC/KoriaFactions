package fr.koriamc.factions.model;

/**
 * Warp de faction, éventuellement protégé par un mot de passe.
 */
public final class Warp {

    private String name;
    private SerialLoc location;
    private String password;
    private String createdBy;
    private long createdAt;

    public Warp() {
    }

    public Warp(String name, SerialLoc location, String password, String createdBy) {
        this.name = name;
        this.location = location;
        this.password = password;
        this.createdBy = createdBy;
        this.createdAt = System.currentTimeMillis();
    }

    public String getName() {
        return name;
    }

    public SerialLoc getLocation() {
        return location;
    }

    public void setLocation(SerialLoc location) {
        this.location = location;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean hasPassword() {
        return password != null && !password.isEmpty();
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public long getCreatedAt() {
        return createdAt;
    }
}
