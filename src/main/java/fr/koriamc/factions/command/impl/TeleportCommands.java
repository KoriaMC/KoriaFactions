package fr.koriamc.factions.command.impl;

import fr.koriamc.factions.KoriaFactions;
import fr.koriamc.factions.command.CommandContext;
import fr.koriamc.factions.command.SubCommand;
import fr.koriamc.factions.config.Settings;
import fr.koriamc.factions.manager.TeleportManager;
import fr.koriamc.factions.model.Faction;
import fr.koriamc.factions.model.PermAction;
import fr.koriamc.factions.model.SerialLoc;
import fr.koriamc.factions.model.TerritoryType;
import fr.koriamc.factions.model.Warp;
import fr.koriamc.factions.util.Text;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Home et warps.
 */
public final class TeleportCommands {

    private TeleportCommands() {
    }

    /**
     * Vérifications communes avant une téléportation de faction.
     *
     * @return false si refusée (message envoyé)
     */
    private static boolean canLeave(CommandContext ctx, boolean denyFromEnemy, int enemyRadius) {
        if (ctx.isBypass()) {
            return true;
        }
        KoriaFactions plugin = ctx.plugin;
        Player player = ctx.player;
        if (denyFromEnemy && plugin.factions().getTerritoryType(ctx.fp,
                plugin.claims().getOwner(player.getLocation())) == TerritoryType.ENEMY) {
            ctx.msg("teleport.enemy-territory");
            return false;
        }
        if (enemyRadius > 0 && plugin.teleports().isEnemyNearby(player, ctx.fp, enemyRadius)) {
            ctx.msg("teleport.enemy-nearby");
            return false;
        }
        return true;
    }

    /** /f home */
    public static final class Home extends SubCommand {
        public Home(KoriaFactions plugin) {
            super(plugin, "home", true, 0);
            worldSensitive();
        }

        @Override
        public void execute(final CommandContext ctx) {
            final Settings s = plugin.settings();
            if (!s.homeEnabled) {
                ctx.msg("general.feature-disabled");
                return;
            }
            if (!requireFaction(ctx) || !requirePerm(ctx, PermAction.HOME)) {
                return;
            }
            final Faction f = ctx.faction;
            SerialLoc home = f.getHome();
            Location target = home == null ? null : home.toLocation();
            if (target == null) {
                ctx.msg("home.not-set");
                return;
            }
            Player player = ctx.player;
            if (!ctx.isBypass()) {
                if (s.homeDeniedWorlds.contains(player.getWorld().getName().toLowerCase(Locale.ROOT))) {
                    ctx.msg("home.denied-world");
                    return;
                }
                if (s.homeDenyCrossWorld && !target.getWorld().equals(player.getWorld())) {
                    ctx.msg("home.cross-world");
                    return;
                }
            }
            if (!canLeave(ctx, s.homeDenyFromEnemy, s.homeEnemyRadius)) {
                return;
            }
            plugin.teleports().teleport(player, target, "home", new TeleportManager.Check() {
                @Override
                public boolean run() {
                    Faction current = plugin.factions().getFaction(ctx.fp);
                    if (current != f || f.getHome() == null) {
                        ctx.msg("home.not-set");
                        return false;
                    }
                    return plugin.economy().charge(ctx.player, "home");
                }
            }, "home.teleported");
        }
    }

    /** /f sethome */
    public static final class SetHome extends SubCommand {
        public SetHome(KoriaFactions plugin) {
            super(plugin, "sethome", true, 0);
            worldSensitive();
        }

        @Override
        public void execute(CommandContext ctx) {
            Settings s = plugin.settings();
            if (!s.homeEnabled) {
                ctx.msg("general.feature-disabled");
                return;
            }
            if (!requireFaction(ctx) || !requirePerm(ctx, PermAction.SETHOME)) {
                return;
            }
            Location location = ctx.player.getLocation();
            if (!ctx.isBypass()) {
                if (s.homeMustBeInTerritory && plugin.claims().getOwner(location) != ctx.faction) {
                    ctx.msg("home.must-be-in-territory");
                    return;
                }
                if (s.homeDeniedWorlds.contains(location.getWorld().getName().toLowerCase(Locale.ROOT))) {
                    ctx.msg("home.denied-world");
                    return;
                }
            }
            if (!charge(ctx, "sethome")) {
                return;
            }
            ctx.faction.setHome(SerialLoc.of(location));
            plugin.factions().log(ctx.faction, "SETHOME", "player", ctx.player.getName());
            ctx.msg("home.set");
            notifyOthers(ctx, ctx.faction, "home.notify-set", "player", ctx.player.getName());
        }
    }

    /** /f delhome */
    public static final class DelHome extends SubCommand {
        public DelHome(KoriaFactions plugin) {
            super(plugin, "delhome", true, 0);
        }

        @Override
        public void execute(CommandContext ctx) {
            if (!requireFaction(ctx) || !requirePerm(ctx, PermAction.SETHOME)) {
                return;
            }
            if (ctx.faction.getHome() == null) {
                ctx.msg("home.not-set");
                return;
            }
            ctx.faction.setHome(null);
            ctx.msg("home.deleted");
        }
    }

    private static List<String> warpNames(CommandContext ctx, String input) {
        List<String> names = new ArrayList<String>();
        if (ctx.faction != null) {
            for (Warp warp : ctx.faction.getWarps().values()) {
                names.add(warp.getName());
            }
        }
        return Text.complete(input, names);
    }

    /** /f warp [nom] [mot de passe] */
    public static final class WarpTo extends SubCommand {
        public WarpTo(KoriaFactions plugin) {
            super(plugin, "warp", true, 0);
            worldSensitive();
        }

        @Override
        public void execute(final CommandContext ctx) {
            final Settings s = plugin.settings();
            if (!s.warpsEnabled) {
                ctx.msg("general.feature-disabled");
                return;
            }
            if (!requireFaction(ctx) || !requirePerm(ctx, PermAction.WARP)) {
                return;
            }
            if (ctx.size() == 0) {
                plugin.menus().open(ctx.player, "warps");
                return;
            }
            final Faction f = ctx.faction;
            final Warp warp = f.getWarp(ctx.arg(0));
            Location target = warp == null || warp.getLocation() == null ? null : warp.getLocation().toLocation();
            if (target == null) {
                ctx.msg("warp.not-found", "warp", ctx.arg(0));
                return;
            }
            if (warp.hasPassword() && !ctx.isBypass()) {
                if (ctx.size() < 2) {
                    ctx.msg("warp.password-required", "warp", warp.getName());
                    return;
                }
                if (!warp.getPassword().equals(ctx.arg(1))) {
                    ctx.msg("warp.wrong-password");
                    return;
                }
            }
            if (!canLeave(ctx, s.warpDenyFromEnemy, s.warpEnemyRadius)) {
                return;
            }
            plugin.teleports().teleport(ctx.player, target, "warp", new TeleportManager.Check() {
                @Override
                public boolean run() {
                    if (plugin.factions().getFaction(ctx.fp) != f || f.getWarp(warp.getName()) != warp) {
                        ctx.msg("warp.not-found", "warp", warp.getName());
                        return false;
                    }
                    return plugin.economy().charge(ctx.player, "warp");
                }
            }, "warp.teleported", "warp", warp.getName());
        }

        @Override
        public List<String> complete(CommandContext ctx) {
            return ctx.size() == 1 ? warpNames(ctx, ctx.arg(0)) : Collections.<String>emptyList();
        }
    }

    /** /f setwarp &lt;nom&gt; [mot de passe] */
    public static final class SetWarp extends SubCommand {
        public SetWarp(KoriaFactions plugin) {
            super(plugin, "setwarp", true, 1);
            worldSensitive();
        }

        @Override
        public void execute(CommandContext ctx) {
            Settings s = plugin.settings();
            if (!s.warpsEnabled) {
                ctx.msg("general.feature-disabled");
                return;
            }
            if (!requireFaction(ctx) || !requirePerm(ctx, PermAction.SETWARP)) {
                return;
            }
            String name = ctx.arg(0);
            if (name.length() > s.warpNameMax || !s.warpNamePattern.matcher(name).matches()) {
                ctx.msg("warp.invalid-name");
                return;
            }
            String password = ctx.arg(1);
            if (password != null && !s.warpPasswords) {
                ctx.msg("warp.passwords-disabled");
                return;
            }
            Location location = ctx.player.getLocation();
            Faction f = ctx.faction;
            if (!ctx.isBypass() && s.warpMustBeInTerritory && plugin.claims().getOwner(location) != f) {
                ctx.msg("warp.must-be-in-territory");
                return;
            }
            Warp existing = f.getWarp(name);
            int max = plugin.factions().getMaxWarps(f);
            if (existing == null && f.getWarps().size() >= max && !ctx.isBypass()) {
                ctx.msg("warp.limit", "max", max);
                return;
            }
            if (!charge(ctx, "setwarp")) {
                return;
            }
            if (existing != null) {
                existing.setLocation(SerialLoc.of(location));
                existing.setPassword(password);
                ctx.msg("warp.updated", "warp", existing.getName());
            } else {
                f.putWarp(new Warp(name, SerialLoc.of(location), password, ctx.player.getName()));
                ctx.msg("warp.set", "warp", name);
            }
            plugin.factions().log(f, "SETWARP", "player", ctx.player.getName(), "warp", name);
        }

        @Override
        public List<String> complete(CommandContext ctx) {
            return ctx.size() == 1 ? warpNames(ctx, ctx.arg(0)) : Collections.<String>emptyList();
        }
    }

    /** /f delwarp &lt;nom&gt; */
    public static final class DelWarp extends SubCommand {
        public DelWarp(KoriaFactions plugin) {
            super(plugin, "delwarp", true, 1);
        }

        @Override
        public void execute(CommandContext ctx) {
            if (!requireFaction(ctx) || !requirePerm(ctx, PermAction.DELWARP)) {
                return;
            }
            Warp removed = ctx.faction.removeWarp(ctx.arg(0));
            if (removed == null) {
                ctx.msg("warp.not-found", "warp", ctx.arg(0));
                return;
            }
            plugin.factions().log(ctx.faction, "DELWARP", "player", ctx.player.getName(), "warp", removed.getName());
            ctx.msg("warp.deleted", "warp", removed.getName());
        }

        @Override
        public List<String> complete(CommandContext ctx) {
            return ctx.size() == 1 ? warpNames(ctx, ctx.arg(0)) : Collections.<String>emptyList();
        }
    }

    /** /f warps */
    public static final class Warps extends SubCommand {
        public Warps(KoriaFactions plugin) {
            super(plugin, "warps", true, 0);
        }

        @Override
        public void execute(CommandContext ctx) {
            if (!plugin.settings().warpsEnabled) {
                ctx.msg("general.feature-disabled");
                return;
            }
            if (!requireFaction(ctx)) {
                return;
            }
            Faction f = ctx.faction;
            if (f.getWarps().isEmpty()) {
                ctx.msg("warp.list-empty");
                return;
            }
            ctx.msg("warp.list-header", "faction", f.getTag(), "count", f.getWarps().size(),
                    "max", plugin.factions().getMaxWarps(f));
            String protectedText = plugin.msg().text("warp.list-protected");
            for (Warp warp : f.getWarps().values()) {
                ctx.msg("warp.list-line", "warp", warp.getName(), "protected", warp.hasPassword() ? protectedText : "");
            }
        }
    }

}
