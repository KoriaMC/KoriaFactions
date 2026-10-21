package fr.koriamc.factions.listener;

import fr.koriamc.factions.KoriaFactions;
import fr.koriamc.factions.config.Settings;
import fr.koriamc.factions.model.FPlayer;
import fr.koriamc.factions.model.Faction;
import fr.koriamc.factions.model.TerritoryType;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.Locale;
import java.util.Set;

/**
 * Connexion, déconnexion, déplacements, réapparition et commandes bloquées.
 */
public final class PlayerListener implements Listener {

    public static final String BYPASS_COMMANDS = "koriafactions.bypass.commands";

    private final KoriaFactions plugin;

    public PlayerListener(KoriaFactions plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onJoin(PlayerJoinEvent event) {
        final Player player = event.getPlayer();
        FPlayer fp = plugin.players().get(player);
        plugin.players().updateName(fp, player.getName());
        plugin.power().applyOfflineRegen(fp);
        fp.setLastSeen(System.currentTimeMillis());
        fp.setLastChunk(null);
        fp.setLastTerritoryId(null);
        plugin.players().setPower(fp, fp.getPower());
        plugin.territory().handleMove(player, player.getLocation());

        final Faction f = plugin.factions().getFaction(fp);
        if (f == null) {
            return;
        }
        Settings s = plugin.settings();
        if (s.notifyLogin) {
            for (Player member : plugin.factions().getOnlinePlayers(f)) {
                if (member != player) {
                    plugin.msg().send(member, "member-login", "player", player.getName());
                }
            }
        }
        if (s.motdEnabled && s.motdOnJoin && f.getMotd() != null && !f.getMotd().isEmpty()) {
            Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
                @Override
                public void run() {
                    if (player.isOnline() && f.getMotd() != null) {
                        plugin.msg().send(player, "motd.display", "faction", f.getTag(), "motd", f.getMotd());
                    }
                }
            }, Math.max(0, s.motdDelayTicks));
        }
        plugin.factions().checkRaidable(f);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        FPlayer fp = plugin.players().get(player);
        fp.setLastSeen(System.currentTimeMillis());
        plugin.teleports().cancelPending(player.getUniqueId(), null);
        plugin.confirmations().clear(player.getUniqueId());
        final Faction f = plugin.factions().getFaction(fp);
        if (f == null) {
            return;
        }
        if (plugin.settings().notifyLogout) {
            for (Player member : plugin.factions().getOnlinePlayers(f)) {
                if (member != player) {
                    plugin.msg().send(member, "member-logout", "player", player.getName());
                }
            }
        }
        Bukkit.getScheduler().runTask(plugin, new Runnable() {
            @Override
            public void run() {
                if (plugin.factions().getById(f.getId()) != null) {
                    plugin.factions().checkRaidable(f);
                }
            }
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        Location from = event.getFrom();
        Location to = event.getTo();
        if (from.getBlockX() == to.getBlockX() && from.getBlockY() == to.getBlockY()
                && from.getBlockZ() == to.getBlockZ() && from.getWorld() == to.getWorld()) {
            return;
        }
        Player player = event.getPlayer();
        plugin.teleports().onMove(player, to);
        if (from.getWorld() != to.getWorld() || (from.getBlockX() >> 4) != (to.getBlockX() >> 4)
                || (from.getBlockZ() >> 4) != (to.getBlockZ() >> 4)) {
            plugin.territory().handleMove(player, to);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        plugin.teleports().onMove(event.getPlayer(), event.getTo());
        plugin.territory().handleMove(event.getPlayer(), event.getTo());
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onRespawn(PlayerRespawnEvent event) {
        final Player player = event.getPlayer();
        if (plugin.settings().respawnAtHome) {
            Faction f = plugin.factions().getFaction(player);
            Location home = f == null || f.getHome() == null ? null : f.getHome().toLocation();
            if (home != null) {
                event.setRespawnLocation(home);
            }
        }
        Bukkit.getScheduler().runTask(plugin, new Runnable() {
            @Override
            public void run() {
                if (player.isOnline()) {
                    plugin.territory().handleMove(player, player.getLocation());
                }
            }
        });
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission(BYPASS_COMMANDS) || plugin.settings().isWorldDisabled(player.getWorld().getName())) {
            return;
        }
        FPlayer fp = plugin.players().get(player);
        if (fp.isBypass()) {
            return;
        }
        String message = event.getMessage();
        if (message.length() < 2) {
            return;
        }
        String command = message.substring(1).split(" ", 2)[0].toLowerCase(Locale.ROOT);
        int colon = command.indexOf(':');
        if (colon >= 0) {
            command = command.substring(colon + 1);
        }
        TerritoryType type = plugin.factions().getTerritoryType(fp, plugin.claims().getOwner(player.getLocation()));
        Set<String> blocked = plugin.protection().blockedCommands.get(type);
        if (blocked != null && blocked.contains(command)) {
            event.setCancelled(true);
            plugin.msg().send(player, "protection.command-blocked", "command", command);
        }
    }
}
