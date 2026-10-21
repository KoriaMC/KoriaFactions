package fr.koriamc.factions.manager;

import fr.koriamc.factions.KoriaFactions;
import fr.koriamc.factions.config.Settings;
import fr.koriamc.factions.model.FPlayer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

/**
 * /f seechunk : affiche les bordures du chunk courant avec des particules
 * visibles uniquement par le joueur concerné.
 */
public final class SeeChunkManager {

    private final KoriaFactions plugin;
    private BukkitTask task;

    public SeeChunkManager(KoriaFactions plugin) {
        this.plugin = plugin;
    }

    public void start() {
        stop();
        Settings s = plugin.settings();
        if (!s.seeChunkEnabled) {
            return;
        }
        task = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    FPlayer fp = plugin.players().get(player);
                    if (fp.isSeeChunk()) {
                        draw(player);
                    }
                }
            }
        }, s.seeChunkInterval, s.seeChunkInterval);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    private void draw(Player player) {
        Settings s = plugin.settings();
        Location loc = player.getLocation();
        World world = loc.getWorld();
        int minX = (loc.getBlockX() >> 4) << 4;
        int minZ = (loc.getBlockZ() >> 4) << 4;
        int eyeY = (int) player.getEyeLocation().getY();
        int fromY = Math.max(0, eyeY - s.seeChunkBelow);
        int toY = Math.min(255, eyeY + s.seeChunkAbove);
        for (int y = fromY; y <= toY; y += s.seeChunkSpacing) {
            for (int i = 0; i <= 16; i += s.seeChunkSpacing) {
                particle(player, world, minX + i, y, minZ);
                particle(player, world, minX + i, y, minZ + 16);
                particle(player, world, minX, y, minZ + i);
                particle(player, world, minX + 16, y, minZ + i);
            }
        }
    }

    private void particle(Player player, World world, double x, double y, double z) {
        player.spigot().playEffect(new Location(world, x, y + 0.5, z), plugin.settings().seeChunkEffect,
                0, 0, 0f, 0f, 0f, 0f, 1, 64);
    }
}
