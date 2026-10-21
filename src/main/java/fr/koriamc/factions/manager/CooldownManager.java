package fr.koriamc.factions.manager;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Délais de réutilisation par joueur et par clé.
 */
public final class CooldownManager {

    private final Map<UUID, Map<String, Long>> cooldowns = new HashMap<UUID, Map<String, Long>>();

    /** @return le temps restant en millisecondes (0 si disponible). */
    public long getRemaining(UUID uuid, String key) {
        Map<String, Long> map = cooldowns.get(uuid);
        if (map == null) {
            return 0;
        }
        Long end = map.get(key);
        if (end == null) {
            return 0;
        }
        long remaining = end - System.currentTimeMillis();
        if (remaining <= 0) {
            map.remove(key);
            return 0;
        }
        return remaining;
    }

    public void set(UUID uuid, String key, long durationMs) {
        if (durationMs <= 0) {
            return;
        }
        Map<String, Long> map = cooldowns.get(uuid);
        if (map == null) {
            map = new HashMap<String, Long>();
            cooldowns.put(uuid, map);
        }
        map.put(key, System.currentTimeMillis() + durationMs);
    }

    public void clear(UUID uuid) {
        cooldowns.remove(uuid);
    }
}
