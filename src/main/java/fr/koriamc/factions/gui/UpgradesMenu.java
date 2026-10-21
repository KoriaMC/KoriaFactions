package fr.koriamc.factions.gui;

import fr.koriamc.factions.KoriaFactions;
import fr.koriamc.factions.manager.UpgradeManager.Upgrade;
import fr.koriamc.factions.model.Faction;
import fr.koriamc.factions.model.PermAction;
import fr.koriamc.factions.util.Items;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

/**
 * /f upgrades. Chaque amélioration (factions.json) s'affiche à sa case avec le
 * modèle de menus.json (ameliorations.modele-amelioration). Action propre : ACHETER.
 */
final class UpgradesMenu extends Menu {

    UpgradesMenu(KoriaFactions plugin, Player player) {
        super(plugin, player);
    }

    @Override
    protected ConfigurationSection section() {
        return plugin.menus().section("ameliorations");
    }

    @Override
    protected void render() {
        final Faction f = faction();
        if (f == null) {
            return;
        }
        ConfigurationSection s = section();
        ConfigurationSection t = s.getConfigurationSection("modele-amelioration");
        boolean canBuy = plugin.factions().hasPermission(fp, f, PermAction.UPGRADE);
        String none = t == null ? "" : t.getString("aucun", "");
        for (final Upgrade upgrade : plugin.upgrades().getAll()) {
            int level = Math.min(f.getUpgradeLevel(upgrade.id), upgrade.getMaxLevel());
            boolean max = level >= upgrade.getMaxLevel();
            String status;
            if (t == null) {
                status = "";
            } else if (max) {
                status = t.getString("statut-max", "");
            } else if (!canBuy) {
                status = t.getString("statut-sans-permission", "");
            } else {
                status = t.getString("statut-disponible", "");
            }
            Object[] kv = {
                    "name", upgrade.name,
                    "upgrade", upgrade.id,
                    "icone", upgrade.icon,
                    "description", upgrade.description,
                    "level", level,
                    "max_level", upgrade.getMaxLevel(),
                    "current", level > 0 ? upgrade.formatValue(level) : none,
                    "next", max ? none : upgrade.formatValue(level + 1),
                    "cost", max ? none : plugin.economy().format(upgrade.levels.get(level).cost),
                    "status", status,
                    "faction", f.getTag(),
                    "label", plugin.commands().getLabel()
            };
            boolean glow = max && t != null && t.getBoolean("brillant-si-max");
            set(upgrade.slot, Items.fromSection(t, "materiau", glow, kv), clickHandler(t, new CustomAction() {
                @Override
                public boolean run(String type, String value) {
                    if (!type.equals("ACHETER")) {
                        return false;
                    }
                    Faction current = faction();
                    if (current != null && plugin.upgrades().purchase(player, fp, current, upgrade)) {
                        open();
                    }
                    return true;
                }
            }, kv));
        }
        Object[] vars = {"player", player.getName(), "faction", f.getTag(), "label", plugin.commands().getLabel(),
                "balance", plugin.economy().format(plugin.economy().getBalance(player))};
        setConfigured(s.getConfigurationSection("info-argent"), vars);
        setConfigured(s.getConfigurationSection("retour"), vars);
    }
}
