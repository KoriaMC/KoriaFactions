package fr.koriamc.factions.gui;

import fr.koriamc.factions.KoriaFactions;
import fr.koriamc.factions.model.Faction;
import fr.koriamc.factions.model.SerialLoc;
import fr.koriamc.factions.model.Warp;
import fr.koriamc.factions.util.Items;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Liste des warps de faction. Apparence et effet des clics : menus.json (warps).
 */
final class WarpsMenu extends Menu {

    WarpsMenu(KoriaFactions plugin, Player player) {
        super(plugin, player);
    }

    @Override
    protected ConfigurationSection section() {
        return plugin.menus().section("warps");
    }

    @Override
    protected void render() {
        Faction f = faction();
        if (f == null) {
            return;
        }
        ConfigurationSection s = section();
        ConfigurationSection template = s.getConfigurationSection("modele-warp");
        List<Integer> slots = s.getIntegerList("cases");
        List<Warp> warps = new ArrayList<Warp>(f.getWarps().values());
        if (warps.isEmpty()) {
            setConfigured(s.getConfigurationSection("vide"), vars());
        }
        for (int i = 0; i < warps.size() && i < slots.size(); i++) {
            Warp warp = warps.get(i);
            SerialLoc loc = warp.getLocation();
            Object[] kv = {
                    "warp", warp.getName(),
                    "world", loc == null ? "?" : loc.getWorld(),
                    "x", loc == null ? 0 : loc.getBlockX(),
                    "y", loc == null ? 0 : loc.getBlockY(),
                    "z", loc == null ? 0 : loc.getBlockZ(),
                    "password", template == null ? ""
                            : template.getString(warp.hasPassword() ? "mot-de-passe-oui" : "mot-de-passe-non", ""),
                    "faction", f.getTag(),
                    "label", plugin.commands().getLabel()
            };
            set(slots.get(i), Items.fromSection(template, kv), clickHandler(template, null, kv));
        }
        setConfigured(s.getConfigurationSection("retour"), vars());
    }
}
