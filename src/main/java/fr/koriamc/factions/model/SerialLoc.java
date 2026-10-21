package fr.koriamc.factions.model;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

/**
 * Position sérialisable (le monde est stocké par son nom).
 */
public final class SerialLoc {

    private String world;
    private double x;
    private double y;
    private double z;
    private float yaw;
    private float pitch;

    public SerialLoc() {
    }

    public static SerialLoc of(Location location) {
        SerialLoc loc = new SerialLoc();
        loc.world = location.getWorld().getName();
        loc.x = location.getX();
        loc.y = location.getY();
        loc.z = location.getZ();
        loc.yaw = location.getYaw();
        loc.pitch = location.getPitch();
        return loc;
    }

    /** @return la position, ou null si le monde n'est pas chargé. */
    public Location toLocation() {
        World w = Bukkit.getWorld(world);
        return w == null ? null : new Location(w, x, y, z, yaw, pitch);
    }

    public ChunkPos chunk() {
        return new ChunkPos(world, (int) Math.floor(x) >> 4, (int) Math.floor(z) >> 4);
    }

    public String getWorld() {
        return world;
    }

    public int getBlockX() {
        return (int) Math.floor(x);
    }

    public int getBlockY() {
        return (int) Math.floor(y);
    }

    public int getBlockZ() {
        return (int) Math.floor(z);
    }
}
