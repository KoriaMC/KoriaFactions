package fr.koriamc.factions.model;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Données d'un joueur (persistantes et temporaires).
 */
public final class FPlayer {

    private String uuid;
    private String name;
    private String factionId;
    private Role role;
    private String title;
    private double power;
    private double cachedMaxPower;
    private long firstJoin;
    private long lastSeen;
    private long lastFactionLeave;
    private long joinedFactionAt;
    private int kills;
    private int deaths;
    private ChatMode chatMode = ChatMode.PUBLIC;
    private boolean territoryNotifications = true;
    private boolean autoMap;

    // ------------------------------------------------------------ Temporaire
    private transient UUID cachedUuid;
    private transient boolean bypass;
    private transient boolean spy;
    private transient AutoClaimMode autoClaim = AutoClaimMode.NONE;
    private transient String autoClaimTarget;
    private transient ChunkPos lastChunk;
    private transient String lastTerritoryId;
    private transient boolean seeChunk;
    private transient long lastDenyMessage;

    public FPlayer() {
    }

    public FPlayer(UUID uuid, String name, double defaultPower, boolean notifications) {
        this.uuid = uuid.toString();
        this.cachedUuid = uuid;
        this.name = name;
        this.power = defaultPower;
        this.firstJoin = System.currentTimeMillis();
        this.lastSeen = firstJoin;
        this.territoryNotifications = notifications;
    }

    public void ensureInit() {
        if (chatMode == null) {
            chatMode = ChatMode.PUBLIC;
        }
        if (autoClaim == null) {
            autoClaim = AutoClaimMode.NONE;
        }
    }

    public UUID getUniqueId() {
        if (cachedUuid == null) {
            cachedUuid = UUID.fromString(uuid);
        }
        return cachedUuid;
    }

    public Player getPlayer() {
        return Bukkit.getPlayer(getUniqueId());
    }

    public boolean isOnline() {
        Player p = getPlayer();
        return p != null && p.isOnline();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    // --------------------------------------------------------------- Faction

    public String getFactionId() {
        return factionId;
    }

    public boolean hasFaction() {
        return factionId != null;
    }

    public void setFaction(String factionId, Role role) {
        this.factionId = factionId;
        this.role = factionId == null ? null : role;
        this.joinedFactionAt = factionId == null ? 0L : System.currentTimeMillis();
        if (factionId == null) {
            this.title = null;
            if (chatMode != ChatMode.PUBLIC) {
                chatMode = ChatMode.PUBLIC;
            }
        }
    }

    public Role getRole() {
        return role == null ? Role.RECRUIT : role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public long getJoinedFactionAt() {
        return joinedFactionAt;
    }

    public long getLastFactionLeave() {
        return lastFactionLeave;
    }

    public void setLastFactionLeave(long lastFactionLeave) {
        this.lastFactionLeave = lastFactionLeave;
    }

    // --------------------------------------------------------------- Power

    public double getPower() {
        return power;
    }

    public void setPower(double power) {
        this.power = power;
    }

    public double getCachedMaxPower() {
        return cachedMaxPower;
    }

    public void setCachedMaxPower(double cachedMaxPower) {
        this.cachedMaxPower = cachedMaxPower;
    }

    // --------------------------------------------------------------- Stats

    public long getFirstJoin() {
        return firstJoin;
    }

    public long getLastSeen() {
        return lastSeen;
    }

    public void setLastSeen(long lastSeen) {
        this.lastSeen = lastSeen;
    }

    public int getKills() {
        return kills;
    }

    public void setKills(int kills) {
        this.kills = kills;
    }

    public int getDeaths() {
        return deaths;
    }

    public void setDeaths(int deaths) {
        this.deaths = deaths;
    }

    // ------------------------------------------------------------ Préférences

    public ChatMode getChatMode() {
        return chatMode;
    }

    public void setChatMode(ChatMode chatMode) {
        this.chatMode = chatMode;
    }

    public boolean isTerritoryNotifications() {
        return territoryNotifications;
    }

    public void setTerritoryNotifications(boolean territoryNotifications) {
        this.territoryNotifications = territoryNotifications;
    }

    public boolean isAutoMap() {
        return autoMap;
    }

    public void setAutoMap(boolean autoMap) {
        this.autoMap = autoMap;
    }

    // ------------------------------------------------------------ Temporaire

    public boolean isBypass() {
        return bypass;
    }

    public void setBypass(boolean bypass) {
        this.bypass = bypass;
    }

    public boolean isSpy() {
        return spy;
    }

    public void setSpy(boolean spy) {
        this.spy = spy;
    }

    public AutoClaimMode getAutoClaim() {
        return autoClaim;
    }

    public String getAutoClaimTarget() {
        return autoClaimTarget;
    }

    public void setAutoClaim(AutoClaimMode mode, String targetFactionId) {
        this.autoClaim = mode;
        this.autoClaimTarget = targetFactionId;
    }

    public ChunkPos getLastChunk() {
        return lastChunk;
    }

    public void setLastChunk(ChunkPos lastChunk) {
        this.lastChunk = lastChunk;
    }

    public String getLastTerritoryId() {
        return lastTerritoryId;
    }

    public void setLastTerritoryId(String lastTerritoryId) {
        this.lastTerritoryId = lastTerritoryId;
    }

    public boolean isSeeChunk() {
        return seeChunk;
    }

    public void setSeeChunk(boolean seeChunk) {
        this.seeChunk = seeChunk;
    }

    public long getLastDenyMessage() {
        return lastDenyMessage;
    }

    public void setLastDenyMessage(long lastDenyMessage) {
        this.lastDenyMessage = lastDenyMessage;
    }
}
