package fr.koriamc.factions.hook;

import fr.koriamc.factions.KoriaFactions;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.entity.Player;

/**
 * Pont vers PlaceholderAPI, chargé uniquement si le plugin est installé.
 */
public final class PapiHook {

    private final FactionsExpansion expansion;

    public PapiHook(KoriaFactions plugin) {
        this.expansion = new FactionsExpansion(plugin);
        this.expansion.register();
    }

    public String apply(Player player, String text) {
        return text.indexOf('%') < 0 ? text : PlaceholderAPI.setPlaceholders(player, text);
    }

    public void unregister() {
        try {
            expansion.unregister();
        } catch (Throwable ignored) {
            // PlaceholderAPI déjà arrêté
        }
    }
}
