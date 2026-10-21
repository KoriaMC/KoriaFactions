package fr.koriamc.factions.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Données persistantes d'une faction. L'appartenance des joueurs est portée par
 * {@link FPlayer} ; l'ensemble {@link #members} est un index reconstruit au chargement.
 */
public final class Faction {

    public static final String WILDERNESS = "wilderness";
    public static final String SAFEZONE = "safezone";
    public static final String WARZONE = "warzone";

    private String id;
    private String tag;
    private String description;
    private String motd;
    private long createdAt;
    private long lastRenamed;
    private SerialLoc home;
    private Map<String, Warp> warps = new LinkedHashMap<String, Warp>();
    private Map<String, Boolean> flags = new HashMap<String, Boolean>();
    private Map<String, Map<String, Boolean>> permissions = new HashMap<String, Map<String, Boolean>>();
    /** Permissions personnalisées d'un membre (UUID -> action -> valeur), prioritaires sur son rôle. */
    private Map<String, Map<String, Boolean>> playerPermissions = new HashMap<String, Map<String, Boolean>>();
    private Map<String, Long> invites = new HashMap<String, Long>();
    private Set<String> bans = new HashSet<String>();
    private Map<String, Integer> upgrades = new HashMap<String, Integer>();
    /** Points de faction (quêtes, événements...), servent au classement /f top POINTS. */
    private long points;
    private String chest;
    private List<LogEntry> logs = new ArrayList<LogEntry>();

    private transient Set<UUID> members = new HashSet<UUID>();
    private transient boolean raidable;

    public Faction() {
    }

    public Faction(String id, String tag) {
        this.id = id;
        this.tag = tag;
        this.createdAt = System.currentTimeMillis();
    }

    /** Corrige les collections nulles après une désérialisation. */
    public void ensureInit() {
        if (warps == null) warps = new LinkedHashMap<String, Warp>();
        if (flags == null) flags = new HashMap<String, Boolean>();
        if (permissions == null) permissions = new HashMap<String, Map<String, Boolean>>();
        if (playerPermissions == null) playerPermissions = new HashMap<String, Map<String, Boolean>>();
        if (invites == null) invites = new HashMap<String, Long>();
        if (bans == null) bans = new HashSet<String>();
        if (upgrades == null) upgrades = new HashMap<String, Integer>();
        if (logs == null) logs = new ArrayList<LogEntry>();
        if (members == null) members = new HashSet<UUID>();
        // permissions de sujets ou d'actions qui n'existent plus
        permissions.keySet().removeIf(subject -> PermSubject.parse(subject) == null);
        for (Map<String, Boolean> map : permissions.values()) {
            map.keySet().removeIf(action -> PermAction.parse(action) == null);
        }
        for (Map<String, Boolean> map : playerPermissions.values()) {
            map.keySet().removeIf(action -> PermAction.parse(action) == null);
        }
        playerPermissions.values().removeIf(Map::isEmpty);
    }

    // ------------------------------------------------------------------ Type

    public boolean isWilderness() {
        return WILDERNESS.equals(id);
    }

    public boolean isSafeZone() {
        return SAFEZONE.equals(id);
    }

    public boolean isWarZone() {
        return WARZONE.equals(id);
    }

    public boolean isSystem() {
        return isWilderness() || isSafeZone() || isWarZone();
    }

    public boolean isNormal() {
        return !isSystem();
    }

    // ------------------------------------------------------------- Identité

    public String getId() {
        return id;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getMotd() {
        return motd;
    }

    public void setMotd(String motd) {
        this.motd = motd;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getLastRenamed() {
        return lastRenamed;
    }

    public void setLastRenamed(long lastRenamed) {
        this.lastRenamed = lastRenamed;
    }

    // ------------------------------------------------------------ Membres

    public Set<UUID> getMemberIds() {
        return members;
    }

    public int getSize() {
        return members.size();
    }

    // -------------------------------------------------------- Home / warps

    public SerialLoc getHome() {
        return home;
    }

    public void setHome(SerialLoc home) {
        this.home = home;
    }

    public Map<String, Warp> getWarps() {
        return warps;
    }

    public Warp getWarp(String name) {
        return name == null ? null : warps.get(name.toLowerCase(Locale.ROOT));
    }

    public void putWarp(Warp warp) {
        warps.put(warp.getName().toLowerCase(Locale.ROOT), warp);
    }

    public Warp removeWarp(String name) {
        return warps.remove(name.toLowerCase(Locale.ROOT));
    }

    // -------------------------------------------------- Flags / permissions

    /** @return la valeur explicite du flag, ou null si la valeur par défaut s'applique. */
    public Boolean getFlagValue(FactionFlag flag) {
        return flags.get(flag.name());
    }

    public void setFlag(FactionFlag flag, Boolean value) {
        if (value == null) {
            flags.remove(flag.name());
        } else {
            flags.put(flag.name(), value);
        }
    }

    /** @return la valeur personnalisée, ou null si la valeur par défaut s'applique. */
    public Boolean getPermission(PermSubject subject, PermAction action) {
        Map<String, Boolean> map = permissions.get(subject.name());
        return map == null ? null : map.get(action.name());
    }

    public void setPermission(PermSubject subject, PermAction action, Boolean value) {
        Map<String, Boolean> map = permissions.get(subject.name());
        if (map == null) {
            if (value == null) {
                return;
            }
            map = new HashMap<String, Boolean>();
            permissions.put(subject.name(), map);
        }
        if (value == null) {
            map.remove(action.name());
        } else {
            map.put(action.name(), value);
        }
    }

    /** @return la valeur personnalisée pour ce membre, ou null si son rôle décide. */
    public Boolean getPlayerPermission(UUID player, PermAction action) {
        Map<String, Boolean> map = playerPermissions.get(player.toString());
        return map == null ? null : map.get(action.name());
    }

    public void setPlayerPermission(UUID player, PermAction action, Boolean value) {
        String key = player.toString();
        Map<String, Boolean> map = playerPermissions.get(key);
        if (map == null) {
            if (value == null) {
                return;
            }
            map = new HashMap<String, Boolean>();
            playerPermissions.put(key, map);
        }
        if (value == null) {
            map.remove(action.name());
            if (map.isEmpty()) {
                playerPermissions.remove(key);
            }
        } else {
            map.put(action.name(), value);
        }
    }

    /** Nombre de permissions personnalisées d'un membre. */
    public int countPlayerPermissions(UUID player) {
        Map<String, Boolean> map = playerPermissions.get(player.toString());
        return map == null ? 0 : map.size();
    }

    public void clearPlayerPermissions(UUID player) {
        playerPermissions.remove(player.toString());
    }

    // ----------------------------------------------------- Invitations / bans

    public Map<String, Long> getInvites() {
        return invites;
    }

    public boolean isInvited(UUID uuid) {
        Long expiry = invites.get(uuid.toString());
        if (expiry == null) {
            return false;
        }
        if (expiry > 0 && expiry < System.currentTimeMillis()) {
            invites.remove(uuid.toString());
            return false;
        }
        return true;
    }

    public void invite(UUID uuid, long expiry) {
        invites.put(uuid.toString(), expiry);
    }

    public boolean deinvite(UUID uuid) {
        return invites.remove(uuid.toString()) != null;
    }

    /** Supprime les invitations expirées. */
    public void purgeInvites() {
        long now = System.currentTimeMillis();
        invites.values().removeIf(expiry -> expiry != null && expiry > 0 && expiry < now);
    }

    public Set<String> getBans() {
        return bans;
    }

    public boolean isBanned(UUID uuid) {
        return bans.contains(uuid.toString());
    }

    // ---------------------------------------------------------------- Points

    public long getPoints() {
        return points;
    }

    public void setPoints(long points) {
        this.points = Math.max(0, points);
    }

    // ---------------------------------------------------------- Améliorations

    public int getUpgradeLevel(String upgradeId) {
        Integer level = upgrades.get(upgradeId);
        return level == null ? 0 : level;
    }

    public void setUpgradeLevel(String upgradeId, int level) {
        if (level <= 0) {
            upgrades.remove(upgradeId);
        } else {
            upgrades.put(upgradeId, level);
        }
    }

    public Map<String, Integer> getUpgrades() {
        return upgrades;
    }

    public String getChestData() {
        return chest;
    }

    public void setChestData(String chest) {
        this.chest = chest;
    }

    public List<LogEntry> getLogs() {
        return logs;
    }

    public void addLog(LogEntry entry, int max) {
        logs.add(entry);
        while (max > 0 && logs.size() > max) {
            logs.remove(0);
        }
    }

    public boolean wasRaidable() {
        return raidable;
    }

    public void setWasRaidable(boolean raidable) {
        this.raidable = raidable;
    }
}
