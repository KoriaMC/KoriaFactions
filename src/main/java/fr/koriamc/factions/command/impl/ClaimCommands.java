package fr.koriamc.factions.command.impl;

import fr.koriamc.factions.KoriaFactions;
import fr.koriamc.factions.api.event.LandUnclaimEvent;
import fr.koriamc.factions.command.CommandContext;
import fr.koriamc.factions.command.SubCommand;
import fr.koriamc.factions.manager.ClaimManager;
import fr.koriamc.factions.manager.ClaimResult;
import fr.koriamc.factions.model.AutoClaimMode;
import fr.koriamc.factions.model.ChunkPos;
import fr.koriamc.factions.model.FPlayer;
import fr.koriamc.factions.model.Faction;
import fr.koriamc.factions.model.PermAction;
import fr.koriamc.factions.util.Text;
import org.bukkit.Bukkit;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Territoire : claim, unclaim, auto-claim, carte, bordures, accès et zones admin.
 */
public final class ClaimCommands {

    private ClaimCommands() {
    }

    /** Lit un rayon optionnel ; renvoie null si invalide (message envoyé). */
    private static Integer radius(CommandContext ctx, String input) {
        if (input == null) {
            return 0;
        }
        Integer value = Text.parseInt(input);
        if (value == null) {
            ctx.msg("general.not-a-number", "input", input);
            return null;
        }
        int max = ctx.plugin.settings().maxClaimRadius;
        if (value > max && !ctx.isBypass()) {
            ctx.msg("claim.radius-too-big", "max", max);
            return null;
        }
        return Math.max(0, value);
    }

    /** /f claim [rayon] */
    public static final class Claim extends SubCommand {
        public Claim(KoriaFactions plugin) {
            super(plugin, "claim", true, 0);
            worldSensitive();
        }

        @Override
        public void execute(CommandContext ctx) {
            if (!requireFaction(ctx)) {
                return;
            }
            Integer radius = radius(ctx, ctx.arg(0));
            if (radius == null) {
                return;
            }
            ChunkPos center = ChunkPos.of(ctx.player.getLocation());
            if (radius == 0) {
                ClaimResult result = plugin.claims().claim(ctx.player, ctx.fp, ctx.faction, center);
                result.send(plugin.msg(), ctx.player);
                if (result.isSuccess()) {
                    plugin.factions().log(ctx.faction, "CLAIM", "player", ctx.player.getName(), "count", 1);
                }
                return;
            }
            int count = 0;
            ClaimResult firstFailure = null;
            for (ChunkPos pos : ClaimManager.square(center, radius)) {
                if (plugin.claims().getOwner(pos) == ctx.faction) {
                    continue;
                }
                ClaimResult result = plugin.claims().claim(ctx.player, ctx.fp, ctx.faction, pos);
                if (result.isSuccess()) {
                    count++;
                } else if (firstFailure == null) {
                    firstFailure = result;
                }
            }
            if (count > 0) {
                ctx.msg("claim.success-many", "count", count);
                plugin.factions().log(ctx.faction, "CLAIM", "player", ctx.player.getName(), "count", count);
            } else if (firstFailure != null) {
                firstFailure.send(plugin.msg(), ctx.player);
            } else {
                ctx.msg("claim.already-own");
            }
        }
    }

    /** /f claimat &lt;monde&gt; &lt;x&gt; &lt;z&gt; (clic sur la carte) */
    public static final class ClaimAt extends SubCommand {
        public ClaimAt(KoriaFactions plugin) {
            super(plugin, "claimat", true, 3);
            hidden();
            worldSensitive();
        }

        @Override
        public void execute(CommandContext ctx) {
            if (!requireFaction(ctx)) {
                return;
            }
            Integer x = parseInt(ctx, ctx.arg(1));
            Integer z = x == null ? null : parseInt(ctx, ctx.arg(2));
            if (x == null || z == null) {
                return;
            }
            ChunkPos here = ChunkPos.of(ctx.player.getLocation());
            ChunkPos pos = new ChunkPos(ctx.arg(0), x, z);
            if (!here.getWorldName().equals(pos.getWorldName())
                    || Math.max(Math.abs(here.getX() - x), Math.abs(here.getZ() - z)) > plugin.settings().mapClickMaxDistance) {
                ctx.msg("claim.too-far");
                return;
            }
            ClaimResult result = plugin.claims().claim(ctx.player, ctx.fp, ctx.faction, pos);
            result.send(plugin.msg(), ctx.player);
            if (result.isSuccess()) {
                plugin.factions().log(ctx.faction, "CLAIM", "player", ctx.player.getName(), "count", 1);
                if (!ctx.fp.isAutoMap()) {
                    plugin.map().send(ctx.player, ctx.fp);
                }
            }
        }
    }

    /** /f unclaim [rayon] */
    public static final class Unclaim extends SubCommand {
        public Unclaim(KoriaFactions plugin) {
            super(plugin, "unclaim", true, 0);
            worldSensitive();
        }

        @Override
        public void execute(CommandContext ctx) {
            if (!ctx.isBypass() && !requireFaction(ctx)) {
                return;
            }
            Integer radius = radius(ctx, ctx.arg(0));
            if (radius == null) {
                return;
            }
            ChunkPos center = ChunkPos.of(ctx.player.getLocation());
            if (radius == 0) {
                Faction owner = plugin.claims().getOwner(center);
                ClaimResult result = plugin.claims().unclaim(ctx.player, ctx.fp, center);
                result.send(plugin.msg(), ctx.player);
                if (result.isSuccess()) {
                    plugin.factions().log(owner, "UNCLAIM", "player", ctx.player.getName(), "count", 1);
                }
                return;
            }
            int count = 0;
            ClaimResult firstFailure = null;
            for (ChunkPos pos : ClaimManager.square(center, radius)) {
                Faction owner = plugin.claims().getOwner(pos);
                if (owner.isWilderness() || (!ctx.isBypass() && owner != ctx.faction)) {
                    continue;
                }
                ClaimResult result = plugin.claims().unclaim(ctx.player, ctx.fp, pos);
                if (result.isSuccess()) {
                    count++;
                } else if (firstFailure == null) {
                    firstFailure = result;
                }
            }
            if (count > 0) {
                ctx.msg("unclaim.success-many", "count", count);
                if (ctx.faction != null) {
                    plugin.factions().log(ctx.faction, "UNCLAIM", "player", ctx.player.getName(), "count", count);
                }
            } else if (firstFailure != null) {
                firstFailure.send(plugin.msg(), ctx.player);
            } else {
                ctx.msg("unclaim.not-own");
            }
        }
    }

    /** /f unclaimall */
    public static final class UnclaimAll extends SubCommand {
        public UnclaimAll(KoriaFactions plugin) {
            super(plugin, "unclaimall", true, 0);
        }

        @Override
        public void execute(CommandContext ctx) {
            if (!requireFaction(ctx) || !requirePerm(ctx, PermAction.UNCLAIM_ALL)) {
                return;
            }
            Faction f = ctx.faction;
            if (plugin.claims().count(f.getId()) == 0) {
                ctx.msg("unclaim.none");
                return;
            }
            if (!confirm(ctx, plugin.settings().confirmUnclaimAll, "unclaimall:" + f.getId())) {
                return;
            }
            int count = 0;
            for (ChunkPos pos : new ArrayList<ChunkPos>(plugin.claims().getClaims(f.getId()))) {
                LandUnclaimEvent event = new LandUnclaimEvent(f, pos, ctx.player);
                Bukkit.getPluginManager().callEvent(event);
                if (event.isCancelled()) {
                    continue;
                }
                plugin.claims().remove(pos);
                plugin.claims().cleanupAfterLoss(f, pos);
                count++;
            }
            plugin.factions().log(f, "UNCLAIM", "player", ctx.player.getName(), "count", count);
            plugin.factions().checkRaidable(f);
            ctx.msg("unclaim.all-success", "count", count);
        }
    }

    /** /f autoclaim et /f autounclaim */
    public static final class AutoClaim extends SubCommand {
        private final boolean claim;

        public AutoClaim(KoriaFactions plugin, boolean claim) {
            super(plugin, claim ? "autoclaim" : "autounclaim", true, 0);
            this.claim = claim;
            worldSensitive();
        }

        @Override
        public void execute(CommandContext ctx) {
            if (!requireFaction(ctx) || !requirePerm(ctx, claim ? PermAction.CLAIM : PermAction.UNCLAIM)) {
                return;
            }
            AutoClaimMode mode = claim ? AutoClaimMode.CLAIM : AutoClaimMode.UNCLAIM;
            String prefix = claim ? "claim.autoclaim-" : "claim.autounclaim-";
            if (ctx.fp.getAutoClaim() == mode && ctx.fp.getAutoClaimTarget() == null) {
                ctx.fp.setAutoClaim(AutoClaimMode.NONE, null);
                ctx.msg(prefix + "off");
                return;
            }
            ctx.fp.setAutoClaim(mode, null);
            ctx.msg(prefix + "on");
            ChunkPos here = ChunkPos.of(ctx.player.getLocation());
            ClaimResult result = claim
                    ? plugin.claims().claim(ctx.player, ctx.fp, ctx.faction, here)
                    : plugin.claims().unclaim(ctx.player, ctx.fp, here);
            if (result.isSuccess()) {
                result.send(plugin.msg(), ctx.player);
            }
        }
    }

    /** /f map [on|off] */
    public static final class Map extends SubCommand {
        public Map(KoriaFactions plugin) {
            super(plugin, "map", true, 0);
            worldSensitive();
        }

        @Override
        public void execute(CommandContext ctx) {
            if (ctx.size() > 0) {
                boolean on = Text.parseBoolean(ctx.arg(0), !ctx.fp.isAutoMap());
                ctx.fp.setAutoMap(on);
                ctx.msg(on ? "map.auto-on" : "map.auto-off");
                if (!on) {
                    return;
                }
            } else if (!charge(ctx, "map")) {
                return;
            }
            plugin.map().send(ctx.player, ctx.fp);
        }

        @Override
        public List<String> complete(CommandContext ctx) {
            return ctx.size() == 1 ? Text.complete(ctx.arg(0), Arrays.asList("on", "off")) : Collections.<String>emptyList();
        }
    }

    /** /f seechunk */
    public static final class SeeChunk extends SubCommand {
        public SeeChunk(KoriaFactions plugin) {
            super(plugin, "seechunk", true, 0);
            worldSensitive();
        }

        @Override
        public void execute(CommandContext ctx) {
            if (!plugin.settings().seeChunkEnabled) {
                ctx.msg("general.feature-disabled");
                return;
            }
            boolean on = !ctx.fp.isSeeChunk();
            ctx.fp.setSeeChunk(on);
            ctx.msg(on ? "seechunk.enabled" : "seechunk.disabled");
        }
    }

    /** /f access &lt;player|faction|list|clear&gt; [nom] */
    public static final class Access extends SubCommand {
        public Access(KoriaFactions plugin) {
            super(plugin, "access", true, 1);
            worldSensitive();
        }

        @Override
        public void execute(CommandContext ctx) {
            if (!plugin.settings().accessEnabled) {
                ctx.msg("general.feature-disabled");
                return;
            }
            if (!requireFaction(ctx) || !requirePerm(ctx, PermAction.ACCESS)) {
                return;
            }
            ChunkPos pos = ChunkPos.of(ctx.player.getLocation());
            fr.koriamc.factions.model.Claim claim = plugin.claims().getClaim(pos);
            if (claim == null || !claim.getFactionId().equals(ctx.faction.getId())) {
                ctx.msg("access.not-own");
                return;
            }
            String action = ctx.arg(0).toLowerCase(Locale.ROOT);
            Object[] coords = {"x", pos.getX(), "z", pos.getZ()};
            if (action.equals("list")) {
                List<String> players = new ArrayList<String>();
                for (String uuid : claim.getAccessPlayers()) {
                    FPlayer fp = plugin.players().get(UUID.fromString(uuid));
                    players.add(fp == null ? uuid : fp.getName());
                }
                List<String> factions = new ArrayList<String>();
                for (String id : claim.getAccessFactions()) {
                    Faction f = plugin.factions().getById(id);
                    if (f != null) {
                        factions.add(f.getTag());
                    }
                }
                String none = plugin.msg().text("values.none");
                String separator = plugin.msg().text("show.separator");
                ctx.msg("access.list", "x", pos.getX(), "z", pos.getZ(),
                        "players", players.isEmpty() ? none : Text.join(players, separator),
                        "factions", factions.isEmpty() ? none : Text.join(factions, separator));
                return;
            }
            if (action.equals("clear")) {
                claim.getAccessPlayers().clear();
                claim.getAccessFactions().clear();
                ctx.msg("access.cleared");
                return;
            }
            if (ctx.size() < 2) {
                usage(ctx);
                return;
            }
            if (action.equals("player") || action.equals("p")) {
                FPlayer target = findPlayer(ctx, ctx.arg(1));
                if (target == null) {
                    return;
                }
                String uuid = target.getUniqueId().toString();
                boolean granted = claim.getAccessPlayers().add(uuid);
                if (!granted) {
                    claim.getAccessPlayers().remove(uuid);
                }
                ctx.msg(granted ? "access.granted-player" : "access.revoked-player", "name", target.getName(), coords[0], coords[1], coords[2], coords[3]);
            } else if (action.equals("faction") || action.equals("f")) {
                Faction target = findFaction(ctx, ctx.arg(1));
                if (target == null) {
                    return;
                }
                if (target == ctx.faction) {
                    ctx.msg("access.own-faction");
                    return;
                }
                if (!target.isNormal()) {
                    ctx.msg("general.system-faction");
                    return;
                }
                boolean granted = claim.getAccessFactions().add(target.getId());
                if (!granted) {
                    claim.getAccessFactions().remove(target.getId());
                }
                ctx.msg(granted ? "access.granted-faction" : "access.revoked-faction", "name", target.getTag(), coords[0], coords[1], coords[2], coords[3]);
            } else {
                usage(ctx);
            }
        }

        @Override
        public List<String> complete(CommandContext ctx) {
            if (ctx.size() == 1) {
                return Text.complete(ctx.arg(0), Arrays.asList("player", "faction", "list", "clear"));
            }
            if (ctx.size() == 2) {
                String action = ctx.arg(0).toLowerCase(Locale.ROOT);
                if (action.startsWith("f")) {
                    return factionTags(ctx.arg(1));
                }
                return onlineNames(ctx.arg(1));
            }
            return Collections.emptyList();
        }
    }

    /** /f safezone et /f warzone [rayon|auto|unclaim [rayon]] */
    public static final class Zone extends SubCommand {
        private final String zoneId;

        public Zone(KoriaFactions plugin, String zoneId) {
            super(plugin, zoneId, true, 0);
            admin();
            this.zoneId = zoneId;
            worldSensitive();
        }

        @Override
        public void execute(CommandContext ctx) {
            Faction zone = plugin.factions().getById(zoneId);
            String zoneName = plugin.settings().zoneColor(zoneId) + zone.getTag();
            String action = ctx.arg(0) == null ? "" : ctx.arg(0).toLowerCase(Locale.ROOT);
            ChunkPos center = ChunkPos.of(ctx.player.getLocation());
            if (action.equals("auto")) {
                if (ctx.fp.getAutoClaim() == AutoClaimMode.CLAIM && zoneId.equals(ctx.fp.getAutoClaimTarget())) {
                    ctx.fp.setAutoClaim(AutoClaimMode.NONE, null);
                    ctx.msg("zone.auto-off");
                } else {
                    ctx.fp.setAutoClaim(AutoClaimMode.CLAIM, zoneId);
                    plugin.claims().claimZone(zone, center, ctx.player.getUniqueId());
                    ctx.msg("zone.auto-on", "zone", zoneName);
                }
                return;
            }
            boolean unclaim = action.equals("unclaim");
            String radiusArg = unclaim ? ctx.arg(1) : ctx.arg(0);
            Integer radius = radiusArg == null ? Integer.valueOf(0) : parseInt(ctx, radiusArg);
            if (radius == null) {
                return;
            }
            int count = 0;
            for (ChunkPos pos : ClaimManager.square(center, Math.max(0, radius))) {
                if (unclaim) {
                    if (plugin.claims().unclaimZone(zone, pos)) {
                        count++;
                    }
                } else if (!zoneId.equals(plugin.claims().getOwnerId(pos))) {
                    plugin.claims().claimZone(zone, pos, ctx.player.getUniqueId());
                    count++;
                }
            }
            ctx.msg(unclaim ? "zone.unclaimed" : "zone.claimed", "count", count, "zone", zoneName);
        }

        @Override
        public List<String> complete(CommandContext ctx) {
            return ctx.size() == 1 ? Text.complete(ctx.arg(0), Arrays.asList("auto", "unclaim", "1", "2", "3"))
                    : Collections.<String>emptyList();
        }
    }
}
