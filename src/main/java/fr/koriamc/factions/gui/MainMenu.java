package fr.koriamc.factions.gui;

import fr.koriamc.factions.KoriaFactions;
import fr.koriamc.factions.model.Faction;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Menu principal (/f menu), entièrement décrit dans menus.json.
 * Les joueurs sans faction voient la section "sans-faction".
 */
final class MainMenu extends Menu {

    MainMenu(KoriaFactions plugin, Player player) {
        super(plugin, player);
    }

    @Override
    protected ConfigurationSection section() {
        return plugin.menus().section(faction() == null ? "sans-faction" : "principal");
    }

    @Override
    protected Object[] vars() {
        Faction f = faction();
        List<Object> kv = new ArrayList<Object>(Arrays.asList(super.vars()));
        if (f != null) {
            kv.addAll(Arrays.asList(plugin.placeholders().faction(f, fp)));
        }
        return kv.toArray();
    }

    @Override
    protected void render() {
        ConfigurationSection items = section().getConfigurationSection("objets");
        if (items == null) {
            return;
        }
        Object[] kv = vars();
        for (String key : items.getKeys(false)) {
            setConfigured(items.getConfigurationSection(key), kv);
        }
    }
}
