package fr.koriamc.factions.manager;

import fr.koriamc.factions.KoriaFactions;
import fr.koriamc.factions.api.event.LandClaimEvent;
import fr.koriamc.factions.api.event.LandUnclaimEvent;
import fr.koriamc.factions.config.Settings;
import fr.koriamc.factions.model.ChunkPos;
import fr.koriamc.factions.model.Claim;
import fr.koriamc.factions.model.FPlayer;
import fr.koriamc.factions.model.Faction;
import fr.koriamc.factions.model.PermAction;
import fr.koriamc.factions.model.Warp;
import fr.koriamc.factions.util.Sounds;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Carte des territoires et règles de revendication.
 */
public final class ClaimManager {

    private final KoriaFactions plugin;
    private final Map<ChunkPos, Claim> claims = new HashMap<ChunkPos, Claim>();
    private final Map<String, Set<ChunkPos>> byFaction = new HashMap<String, Set<ChunkPos>>();

    public ClaimManager(KoriaFactions plugin) {
        this.plugin = plugin;
    }

    public void load(Map<ChunkPos, Claim> loaded) {
        claims.clear();
        byFaction.clear();
        int ignored = 0;
        for (Map.Entry<ChunkPos, Claim> entry : loaded.entrySet()) {
            Claim claim = entry.getValue();
            Faction f = plugin.factions().getById(claim.getFactionId());
            if (f == null || f.isWilderness()) {
                ignored++;
                continue;
            }
            claim.ensureInit();
            put(entry.getKey(), claim);
        }
        if (ignored > 0) {
            plugin.getLogger().warning(ignored + " claim(s) appartenant à des factions inexistantes ont été ignorés.");
        }
    }

    // ================================================================= Accès

    public Map<ChunkPos, Claim> getAll() {
        return Collections.unmodifiableMap(claims);
    }

    public int size() {
        return claims.size();
    }

    public Claim getClaim(ChunkPos pos) {
        return claims.get(pos);
    }

    public String getOwnerId(ChunkPos pos) {
        Claim claim = claims.get(pos);
        return claim == null ? Faction.WILDERNESS : claim.getFactionId();
    }

    /** Propriétaire du chunk (jamais null : la wilderness par défaut). */
    public Faction getOwner(ChunkPos pos) {
        Claim claim = claims.get(pos);
        if (claim == null) {
            return plugin.factions().getWilderness();
        }
        Faction f = plugin.factions().getById(claim.getFactionId());
        return f == null ? plugin.factions().getWilderness() : f;
    }

    public Faction getOwner(Location location) {
        return getOwner(ChunkPos.of(location));
    }

    public int count(String factionId) {
        Set<ChunkPos> set = byFaction.get(factionId);
        return set == null ? 0 : set.size();
    }

    public Set<ChunkPos> getClaims(String factionId) {
        Set<ChunkPos> set = byFaction.get(factionId);
        return set == null ? Collections.<ChunkPos>emptySet() : Collections.unmodifiableSet(set);
    }

    // =========================================================== Écriture brute

    private void put(ChunkPos pos, Claim claim) {
        Claim previous = claims.put(pos, claim);
        if (previous != null) {
            Set<ChunkPos> old = byFaction.get(previous.getFactionId());
            if (old != null) {
                old.remove(pos);
            }
        }
        Set<ChunkPos> set = byFaction.get(claim.getFactionId());
        if (set == null) {
            set = new HashSet<ChunkPos>();
            byFaction.put(claim.getFactionId(), set);
        }
        set.add(pos);
    }

    public void setOwner(ChunkPos pos, Faction faction, UUID by) {
        Claim claim = new Claim(faction.getId(), by);
        put(pos, claim);
        plugin.top().invalidate();
    }

    public boolean remove(ChunkPos pos) {
        Claim claim = claims.remove(pos);
        if (claim == null) {
            return false;
        }
        Set<ChunkPos> set = byFaction.get(claim.getFactionId());
        if (set != null) {
            set.remove(pos);
            if (set.isEmpty()) {
                byFaction.remove(claim.getFactionId());
            }
        }
        plugin.top().invalidate();
        return true;
    }

    /** Supprime tous les claims d'une faction et renvoie les chunks libérés. */
    public List<ChunkPos> unclaimAll(String factionId) {
        Set<ChunkPos> set = byFaction.remove(factionId);
        if (set == null) {
            return Collections.emptyList();
        }
        List<ChunkPos> removed = new ArrayList<ChunkPos>(set);
        for (ChunkPos pos : removed) {
            claims.remove(pos);
        }
        plugin.top().invalidate();
        return removed;
    }

    /** Retire le home et les warps d'une faction situés dans un chunk perdu. */
    public void cleanupAfterLoss(Faction f, ChunkPos pos) {
        Settings s = plugin.settings();
        if (s.removeHomeWhenUnclaimed && f.getHome() != null && f.getHome().chunk().equals(pos)) {
            f.setHome(null);
            plugin.factions().notifyMembers(f, "home.removed-unclaimed");
        }
        if (s.removeWarpsWhenUnclaimed) {
            Iterator<Warp> it = f.getWarps().values().iterator();
            while (it.hasNext()) {
                Warp warp = it.next();
                if (warp.getLocation() != null && warp.getLocation().chunk().equals(pos)) {
                    it.remove();
                    plugin.factions().notifyMembers(f, "warp.removed-unclaimed", "warp", warp.getName());
                }
            }
        }
    }

    // ============================================================ Voisinage

    public boolean isConnected(ChunkPos pos, String factionId) {
        return factionId.equals(getOwnerId(pos.relative(1, 0)))
                || factionId.equals(getOwnerId(pos.relative(-1, 0)))
                || factionId.equals(getOwnerId(pos.relative(0, 1)))
                || factionId.equals(getOwnerId(pos.relative(0, -1)));
    }

    /** Le chunk touche-t-il un chunk qui n'appartient pas à la faction ? */
    public boolean isBorder(ChunkPos pos, String factionId) {
        return !factionId.equals(getOwnerId(pos.relative(1, 0)))
                || !factionId.equals(getOwnerId(pos.relative(-1, 0)))
                || !factionId.equals(getOwnerId(pos.relative(0, 1)))
                || !factionId.equals(getOwnerId(pos.relative(0, -1)));
    }

    /** Première faction (hors {@code ownId} et zones système) à moins de {@code radius} chunks. */
    public Faction findOtherFactionNear(ChunkPos pos, String ownId, int radius) {
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                String id = getOwnerId(pos.relative(dx, dz));
                if (!id.equals(ownId) && !id.equals(Faction.WILDERNESS) && !id.equals(Faction.SAFEZONE) && !id.equals(Faction.WARZONE)) {
                    Faction f = plugin.factions().getById(id);
                    if (f != null) {
                        return f;
                    }
                }
            }
        }
        return null;
    }

    public boolean isNearOwner(ChunkPos pos, String ownerId, int radius) {
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (ownerId.equals(getOwnerId(pos.relative(dx, dz)))) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isNearWorldSpawn(ChunkPos pos, int radius) {
        World world = pos.getWorld();
        if (world == null) {
            return false;
        }
        Location spawn = world.getSpawnLocation();
        return Math.abs((spawn.getBlockX() >> 4) - pos.getX()) <= radius
                && Math.abs((spawn.getBlockZ() >> 4) - pos.getZ()) <= radius;
    }

    /** Chunks d'un carré de rayon donné, du plus proche au plus éloigné du centre. */
    public static List<ChunkPos> square(final ChunkPos center, int radius) {
        List<ChunkPos> list = new ArrayList<ChunkPos>();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                list.add(center.relative(dx, dz));
            }
        }
        Collections.sort(list, new Comparator<ChunkPos>() {
            @Override
            public int compare(ChunkPos a, ChunkPos b) {
                return Integer.compare(a.distanceSquared(center), b.distanceSquared(center));
            }
        });
        return list;
    }

    // ============================================================ Revendiquer

    private String colored(FPlayer viewer, Faction f) {
        return plugin.factions().getColor(viewer, f) + f.getTag();
    }

    /**
     * Tente de revendiquer un chunk pour une faction normale.
     *
     * @param player joueur à l'origine, peut être null
     */
    public ClaimResult claim(Player player, FPlayer fp, Faction target, ChunkPos pos) {
        Settings s = plugin.settings();
        FactionManager fm = plugin.factions();
        boolean bypass = fp.isBypass();
        int blockX = (pos.getX() << 4) + 8;
        int blockZ = (pos.getZ() << 4) + 8;

        if (!bypass && !s.isClaimWorld(pos.getWorldName())) {
            return ClaimResult.fail("claim.world-denied");
        }
        Faction current = getOwner(pos);
        if (current == target) {
            return ClaimResult.fail("claim.already-own");
        }
        if (current.isSafeZone() || current.isWarZone()) {
            return ClaimResult.fail("claim.system-zone", "faction", colored(fp, current));
        }
        if (!fm.hasPermission(fp, target, PermAction.CLAIM)) {
            return ClaimResult.fail("general.no-faction-permission", "action", plugin.roles().actionName(PermAction.CLAIM));
        }
        int owned = count(target.getId());
        if (!bypass) {
            int cap = fm.getClaimCap(target);
            if (cap > 0 && owned >= cap) {
                return ClaimResult.fail("claim.max-reached", "max", cap);
            }
            if (owned >= fm.getLandFromPower(target)) {
                return ClaimResult.fail("claim.not-enough-power");
            }
        }
        boolean overclaim = current.isNormal();
        if (!bypass) {
            if (overclaim) {
                if (!s.overclaimEnabled) {
                    return ClaimResult.fail("claim.overclaim-disabled");
                }
                if (s.overclaimRequireRaidable && !fm.isRaidable(current)) {
                    return ClaimResult.fail("claim.overclaim-not-raidable", "faction", current.getTag());
                }
                if (s.overclaimRequireBorder && !isBorder(pos, current.getId())) {
                    return ClaimResult.fail("claim.overclaim-not-border", "faction", colored(fp, current));
                }
                if (s.claimsMustBeConnected && !s.overclaimAllowUnconnected && owned > 0 && !isConnected(pos, target.getId())) {
                    return ClaimResult.fail("claim.not-connected");
                }
            } else {
                if (s.claimsMustBeConnected && owned > 0 && !isConnected(pos, target.getId())) {
                    return ClaimResult.fail("claim.not-connected");
                }
                if (s.bufferZone > 0) {
                    Faction near = findOtherFactionNear(pos, target.getId(), s.bufferZone);
                    if (near != null) {
                        return ClaimResult.fail("claim.buffer", "faction", colored(fp, near));
                    }
                }
                if (s.minDistanceSpawn > 0 && isNearWorldSpawn(pos, s.minDistanceSpawn)) {
                    return ClaimResult.fail("claim.near-spawn");
                }
                if (s.minDistanceSafezone > 0 && isNearOwner(pos, Faction.SAFEZONE, s.minDistanceSafezone)) {
                    return ClaimResult.fail("claim.near-safezone");
                }
                if (s.minDistanceWarzone > 0 && isNearOwner(pos, Faction.WARZONE, s.minDistanceWarzone)) {
                    return ClaimResult.fail("claim.near-warzone");
                }
            }
            if (s.claimEnemyRadius > 0 && player != null && plugin.teleports().isEnemyNearby(player, fp, s.claimEnemyRadius)) {
                return ClaimResult.fail("claim.enemy-nearby");
            }
        }

        LandClaimEvent event = new LandClaimEvent(target, pos, player, current);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return ClaimResult.fail("claim.cancelled");
        }
        setOwner(pos, target, fp.getUniqueId());
        if (player != null) {
            Sounds.play(player, s.sound("claim"));
        }
        if (overclaim) {
            cleanupAfterLoss(current, pos);
            fm.log(current, "OVERCLAIMED", "faction", target.getTag(), "x", blockX, "z", blockZ);
            fm.log(target, "OVERCLAIM", "player", fp.getName(), "faction", current.getTag());
            if (s.overclaimNotify) {
                fm.notifyMembers(current, "claim.overclaim-victim", "attacker", target.getTag(), "x", blockX, "z", blockZ);
            }
            if (s.overclaimBroadcast) {
                plugin.msg().broadcast("claim.overclaim-broadcast", "attacker", target.getTag(), "faction", current.getTag());
            }
            fm.checkRaidable(current);
            fm.checkRaidable(target);
            return ClaimResult.ok("claim.overclaim-success", "faction", colored(fp, current));
        }
        fm.checkRaidable(target);
        return ClaimResult.ok("claim.success", "x", blockX, "z", blockZ, "faction", target.getTag());
    }

    /** Tente d'abandonner un chunk (faction du joueur, ou n'importe lequel en bypass). */
    public ClaimResult unclaim(Player player, FPlayer fp, ChunkPos pos) {
        FactionManager fm = plugin.factions();
        Faction current = getOwner(pos);
        int blockX = (pos.getX() << 4) + 8;
        int blockZ = (pos.getZ() << 4) + 8;
        if (current.isWilderness()) {
            return ClaimResult.fail("unclaim.wilderness");
        }
        if (current.isSystem()) {
            if (!fp.isBypass()) {
                return ClaimResult.fail("claim.system-zone", "faction", colored(fp, current));
            }
        } else if (!current.getId().equals(fp.getFactionId()) && !fp.isBypass()) {
            return ClaimResult.fail("unclaim.not-own");
        } else if (!fm.hasPermission(fp, current, PermAction.UNCLAIM)) {
            return ClaimResult.fail("general.no-faction-permission", "action", plugin.roles().actionName(PermAction.UNCLAIM));
        }
        LandUnclaimEvent event = new LandUnclaimEvent(current, pos, player);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return ClaimResult.fail("unclaim.cancelled");
        }
        remove(pos);
        if (current.isNormal()) {
            cleanupAfterLoss(current, pos);
            fm.checkRaidable(current);
        }
        if (player != null) {
            Sounds.play(player, plugin.settings().sound("unclaim"));
        }
        return ClaimResult.ok("unclaim.success", "x", blockX, "z", blockZ);
    }

    // ============================================================= Zones admin

    public void claimZone(Faction zone, ChunkPos pos, UUID by) {
        Faction current = getOwner(pos);
        if (current == zone) {
            return;
        }
        setOwner(pos, zone, by);
        if (current.isNormal()) {
            cleanupAfterLoss(current, pos);
            plugin.factions().checkRaidable(current);
        }
    }

    public boolean unclaimZone(Faction zone, ChunkPos pos) {
        return zone.getId().equals(getOwnerId(pos)) && remove(pos);
    }
}
