package fr.koriamc.factions.model;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Chunk revendiqué : propriétaire et accès accordés (/f access).
 */
public final class Claim {

    private String factionId;
    private long claimedAt;
    private String claimedBy;
    private Set<String> accessPlayers = new HashSet<String>();
    private Set<String> accessFactions = new HashSet<String>();

    public Claim() {
    }

    public Claim(String factionId, UUID claimedBy) {
        this.factionId = factionId;
        this.claimedAt = System.currentTimeMillis();
        this.claimedBy = claimedBy == null ? null : claimedBy.toString();
    }

    public void ensureInit() {
        if (accessPlayers == null) {
            accessPlayers = new HashSet<String>();
        }
        if (accessFactions == null) {
            accessFactions = new HashSet<String>();
        }
    }

    public String getFactionId() {
        return factionId;
    }

    public long getClaimedAt() {
        return claimedAt;
    }

    public String getClaimedBy() {
        return claimedBy;
    }

    public Set<String> getAccessPlayers() {
        return accessPlayers;
    }

    public Set<String> getAccessFactions() {
        return accessFactions;
    }

    public boolean hasAccess(UUID player, String factionId) {
        return accessPlayers.contains(player.toString()) || (factionId != null && accessFactions.contains(factionId));
    }
}
