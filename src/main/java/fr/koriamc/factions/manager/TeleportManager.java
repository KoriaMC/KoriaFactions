package fr.koriamc.factions.manager;

import fr.koriamc.factions.KoriaFactions;
import fr.koriamc.factions.config.Settings;
import fr.koriamc.factions.model.FPlayer;
import fr.koriamc.factions.model.Faction;
import fr.koriamc.factions.model.Relation;
import fr.koriamc.factions.util.Nms;
import fr.koriamc.factions.util.Sounds;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Téléportations avec délai d'attente, annulation et délai de réutilisation.
 */
public final class TeleportManager {

    public static final String BYPASS_WARMUP = "koriafactions.bypass.warmup";
    public static final String BYPASS_COOLDOWN = "koriafactions.bypass.cooldown";

    private final KoriaFactions plugin;
    private final Map<UUID, Pending> pending = new HashMap<UUID, Pending>();

    public TeleportManager(KoriaFactions plugin) {
        this.plugin = plugin;
    }

    /** Callback exécuté juste avant la téléportation ; renvoie false pour l'annuler. */
    public interface Check {
        boolean run();
    }

    private final class Pending extends BukkitRunnable {
        private final Player player;
        private final Location origin;
        private final Location target;
        private final String type;
        private final Check check;
        private final String successKey;
        private final Object[] successArgs;
        private int remaining;

        Pending(Player player, Location target, String type, int seconds, Check check, String successKey, Object[] successArgs) {
            this.player = player;
            this.origin = player.getLocation();
            this.target = target;
            this.type = type;
            this.remaining = seconds;
            this.check = check;
            this.successKey = successKey;
            this.successArgs = successArgs;
        }

        @Override
        public void run() {
            if (!player.isOnline()) {
                cancelPending(player.getUniqueId(), null);
                return;
            }
            if (remaining <= 0) {
                pending.remove(player.getUniqueId());
                cancel();
                execute(player, target, type, check, successKey, successArgs);
                return;
            }
            showCountdown(player, remaining);
            remaining--;
        }
    }

    /**
     * Lance une téléportation.
     *
     * @param type  home ou warp (délais configurés dans factions.json)
     * @param check vérification finale (peut être null)
     */
    public void teleport(Player player, Location target, String type, Check check, String successKey, Object... successArgs) {
        if (target == null || target.getWorld() == null) {
            return;
        }
        Settings s = plugin.settings();
        UUID uuid = player.getUniqueId();
        long remainingCooldown = plugin.cooldowns().getRemaining(uuid, "teleport:" + type);
        if (remainingCooldown > 0 && !player.hasPermission(BYPASS_COOLDOWN)) {
            plugin.msg().send(player, "general.cooldown", "time", plugin.msg().duration(remainingCooldown));
            return;
        }
        if (pending.containsKey(uuid)) {
            plugin.msg().send(player, "teleport.already-pending");
            return;
        }
        Integer warmup = s.warmups.get(type);
        int seconds = warmup == null ? 0 : warmup;
        if (seconds > 0 && isInstant(player, type)) {
            seconds = 0;
        }
        if (seconds <= 0 || player.hasPermission(BYPASS_WARMUP)) {
            execute(player, target, type, check, successKey, successArgs);
            return;
        }
        plugin.msg().send(player, "teleport.warmup", "seconds", seconds);
        Pending task = new Pending(player, target, type, seconds, check, successKey, successArgs);
        pending.put(uuid, task);
        task.runTaskTimer(plugin, 0L, 20L);
    }

    /**
     * Téléportation sans délai depuis le territoire de sa faction, sans ennemi
     * dans le rayon configuré.
     */
    private boolean isInstant(Player player, String type) {
        Settings s = plugin.settings();
        if (!s.instantInOwnTerritory) {
            return false;
        }
        FPlayer fp = plugin.players().get(player);
        Faction here = plugin.claims().getOwner(player.getLocation());
        if (!here.isNormal() || !here.getId().equals(fp.getFactionId())) {
            return false;
        }
        return !isEnemyNearby(player, fp, s.instantEnemyRadius);
    }

    private void execute(Player player, Location target, String type, Check check, String successKey, Object[] successArgs) {
        if (check != null && !check.run()) {
            return;
        }
        if (!target.getChunk().isLoaded()) {
            target.getChunk().load();
        }
        player.teleport(target, TeleportCause.PLUGIN);
        Sounds.play(player, plugin.settings().soundSuccess);
        Integer cooldown = plugin.settings().teleportCooldowns.get(type);
        if (cooldown != null && cooldown > 0) {
            plugin.cooldowns().set(player.getUniqueId(), "teleport:" + type, cooldown * 1000L);
        }
        if (successKey != null) {
            plugin.msg().send(player, successKey, successArgs);
        }
    }

    private void showCountdown(Player player, int seconds) {
        Settings s = plugin.settings();
        String text = plugin.msg().get("teleport.countdown", "seconds", seconds);
        if ("ACTIONBAR".equals(s.countdownDisplay)) {
            Nms.actionBar(player, text);
        } else if ("TITLE".equals(s.countdownDisplay)) {
            Nms.title(player, "", text, 0, 25, 0);
        } else if ("CHAT".equals(s.countdownDisplay)) {
            player.sendMessage(text);
        }
        Sounds.play(player, s.soundTick);
    }

    public void cancelPending(UUID uuid, String messageKey) {
        Pending task = pending.remove(uuid);
        if (task == null) {
            return;
        }
        task.cancel();
        if (messageKey != null && task.player.isOnline()) {
            plugin.msg().send(task.player, messageKey);
            Sounds.play(task.player, plugin.settings().soundCancel);
        }
    }

    public void onMove(Player player, Location to) {
        if (!plugin.settings().teleportCancelOnMove) {
            return;
        }
        Pending task = pending.get(player.getUniqueId());
        if (task == null) {
            return;
        }
        Location o = task.origin;
        if (o.getWorld() != to.getWorld() || o.getBlockX() != to.getBlockX()
                || o.getBlockY() != to.getBlockY() || o.getBlockZ() != to.getBlockZ()) {
            cancelPending(player.getUniqueId(), "teleport.cancelled-move");
        }
    }

    public void onDamage(Player player) {
        if (plugin.settings().teleportCancelOnDamage && pending.containsKey(player.getUniqueId())) {
            cancelPending(player.getUniqueId(), "teleport.cancelled-damage");
        }
    }

    public void cancelAll() {
        for (UUID uuid : new HashMap<UUID, Pending>(pending).keySet()) {
            cancelPending(uuid, null);
        }
    }

    /** Un ennemi visible et non spectateur se trouve-t-il dans le rayon ? */
    public boolean isEnemyNearby(Player player, FPlayer fp, int radius) {
        if (radius <= 0) {
            return false;
        }
        for (Entity entity : player.getNearbyEntities(radius, radius, radius)) {
            if (!(entity instanceof Player) || entity == player) {
                continue;
            }
            Player other = (Player) entity;
            if (other.getGameMode() == GameMode.SPECTATOR || other.getGameMode() == GameMode.CREATIVE
                    || !player.canSee(other) || other.hasMetadata("NPC")) {
                continue;
            }
            FPlayer ofp = plugin.players().get(other);
            if (!ofp.isBypass() && plugin.factions().getRelation(fp, ofp) == Relation.ENEMY) {
                return true;
            }
        }
        return false;
    }
}
