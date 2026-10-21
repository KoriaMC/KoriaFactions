package fr.koriamc.factions.config;

import org.bukkit.configuration.ConfigurationSection;

import java.util.HashMap;
import java.util.Map;

/**
 * Valeurs typées de l'économie (config.json, section "economie").
 */
public final class EconomySettings {

    public boolean enabled;
    public String format;
    public int decimals;

    public final Map<String, Double> costs = new HashMap<String, Double>();

    public double killReward;
    public double deathPenalty;

    public void load(ConfigurationSection c) {
        enabled = c.getBoolean("economie.actif");
        format = Settings.str(c, "economie.format", "{amount}");
        decimals = Math.max(0, c.getInt("economie.decimales"));

        costs.clear();
        ConfigurationSection section = c.getConfigurationSection("economie.couts");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                costs.put(key, section.getDouble(key));
            }
        }

        killReward = c.getDouble("economie.recompense-kill");
        deathPenalty = c.getDouble("economie.penalite-mort");
    }

    /** Coût d'une commande (0 si non configuré). */
    public double cost(String key) {
        Double value = costs.get(key);
        return value == null ? 0 : Math.max(0, value);
    }
}
