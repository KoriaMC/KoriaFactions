package fr.koriamc.factions.gui;

import fr.koriamc.factions.KoriaFactions;
import fr.koriamc.factions.config.Settings;
import fr.koriamc.factions.model.Faction;
import fr.koriamc.factions.model.FactionFlag;
import fr.koriamc.factions.util.Items;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * /f flags. Apparence et effet des clics : menus.json (flags). Action propre :
 * BASCULER (active / désactive le flag).
 */
final class FlagsMenu extends Menu {

    FlagsMenu(KoriaFactions plugin, Player player) {
        super(plugin, player);
    }

    @Override
    protected ConfigurationSection section() {
        return plugin.menus().section("flags");
    }

    @Override
    protected void render() {
        final Faction f = faction();
        if (f == null) {
            return;
        }
        ConfigurationSection s = section();
        ConfigurationSection template = s.getConfigurationSection("modele-flag");
        List<Integer> slots = s.getIntegerList("cases");
        int index = 0;
        for (final FactionFlag flag : FactionFlag.values()) {
            if (index >= slots.size()) {
                break;
            }
            Settings.FlagInfo info = plugin.settings().flags.get(flag);
            final boolean value = plugin.factions().getFlag(f, flag);
            boolean editable = info.editable || fp.isBypass();
            Object[] kv = {
                    "flag", info.name,
                    "flag_id", flag.name(),
                    "icone", info.icon,
                    "description", info.description,
                    "state", template == null ? "" : template.getString(value ? "etat-oui" : "etat-non", ""),
                    "state_color", value ? "&a" : "&c",
                    "editable", template == null ? "" : template.getString(editable ? "texte-modifiable" : "texte-verrouille", ""),
                    "faction", f.getTag(),
                    "label", plugin.commands().getLabel()
            };
            boolean glow = value && template != null && template.getBoolean("brillant-si-actif");
            set(slots.get(index++), Items.fromSection(template, "materiau", glow, kv), clickHandler(template, new CustomAction() {
                @Override
                public boolean run(String type, String arg) {
                    if (!type.equals("BASCULER")) {
                        return false;
                    }
                    Faction current = faction();
                    if (current != null && plugin.factions().changeFlag(player, fp, current, flag,
                            !plugin.factions().getFlag(current, flag), false)) {
                        open();
                    }
                    return true;
                }
            }, kv));
        }
        setConfigured(s.getConfigurationSection("retour"), vars());
    }
}
