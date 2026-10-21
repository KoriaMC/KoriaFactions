package fr.koriamc.factions.manager;

import fr.koriamc.factions.KoriaFactions;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Confirmation des actions dangereuses : la commande doit être retapée dans le
 * délai configuré.
 */
public final class ConfirmationManager {

    private final KoriaFactions plugin;
    private final Map<UUID, String> keys = new HashMap<UUID, String>();
    private final Map<UUID, Long> expiries = new HashMap<UUID, Long>();

    public ConfirmationManager(KoriaFactions plugin) {
        this.plugin = plugin;
    }

    /**
     * @return true si l'action est confirmée ; sinon la demande est enregistrée
     * et le joueur est invité à retaper la commande.
     */
    public boolean confirm(Player player, String key) {
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        Long expiry = expiries.get(uuid);
        if (key.equals(keys.get(uuid)) && expiry != null && expiry >= now) {
            clear(uuid);
            return true;
        }
        int seconds = Math.max(1, plugin.settings().confirmSeconds);
        keys.put(uuid, key);
        expiries.put(uuid, now + seconds * 1000L);
        plugin.msg().send(player, "general.confirm", "seconds", seconds);
        return false;
    }

    public void clear(UUID uuid) {
        keys.remove(uuid);
        expiries.remove(uuid);
    }
}
