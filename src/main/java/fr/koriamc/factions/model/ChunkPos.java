package fr.koriamc.factions.model;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;

/**
 * Coordonnées immuables d'un chunk.
 */
public final class ChunkPos {

    private final String world;
    private final int x;
    private final int z;

    public ChunkPos(String world, int x, int z) {
        this.world = world;
        this.x = x;
        this.z = z;
    }

    public static ChunkPos of(Location location) {
        return new ChunkPos(location.getWorld().getName(), location.getBlockX() >> 4, location.getBlockZ() >> 4);
    }

    public static ChunkPos of(Block block) {
        return new ChunkPos(block.getWorld().getName(), block.getX() >> 4, block.getZ() >> 4);
    }

    public static ChunkPos of(Chunk chunk) {
        return new ChunkPos(chunk.getWorld().getName(), chunk.getX(), chunk.getZ());
    }

    /** Format "monde;x;z". */
    public static ChunkPos parse(String key) {
        if (key == null) {
            return null;
        }
        int second = key.lastIndexOf(';');
        int first = key.lastIndexOf(';', second - 1);
        if (first <= 0 || second <= first) {
            return null;
        }
        try {
            return new ChunkPos(key.substring(0, first),
                    Integer.parseInt(key.substring(first + 1, second)),
                    Integer.parseInt(key.substring(second + 1)));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public String key() {
        return world + ";" + x + ";" + z;
    }

    public ChunkPos relative(int dx, int dz) {
        return new ChunkPos(world, x + dx, z + dz);
    }

    public String getWorldName() {
        return world;
    }

    public World getWorld() {
        return Bukkit.getWorld(world);
    }

    public int getX() {
        return x;
    }

    public int getZ() {
        return z;
    }

    public int distanceSquared(ChunkPos other) {
        int dx = x - other.x;
        int dz = z - other.z;
        return dx * dx + dz * dz;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ChunkPos)) {
            return false;
        }
        ChunkPos other = (ChunkPos) o;
        return x == other.x && z == other.z && world.equals(other.world);
    }

    @Override
    public int hashCode() {
        int result = world.hashCode();
        result = 31 * result + x;
        result = 31 * result + z;
        return result;
    }

    @Override
    public String toString() {
        return key();
    }
}
