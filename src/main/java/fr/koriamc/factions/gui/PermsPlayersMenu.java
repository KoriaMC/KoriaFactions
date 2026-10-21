package fr.koriamc.factions.gui;

import fr.koriamc.factions.KoriaFactions;
import fr.koriamc.factions.model.FPlayer;
import fr.koriamc.factions.model.Faction;
import fr.koriamc.factions.model.Role;
import fr.koriamc.factions.util.Items;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * /f perms, choix du membre dont on modifie les permissions personnelles.
 * Apparence et effet des clics : menus.json (permissions.modele-joueur).
 */
final class PermsPlayersMenu extends Menu {

    PermsPlayersMenu(KoriaFactions plugin, Player player, int page) {
        super(plugin, player);
        this.page = page;
    }

    @Override
    protected ConfigurationSection section() {
        return plugin.menus().section("permissions");
    }

    @Override
    protected String titleTemplate() {
        return section().getString("titre-joueurs", "");
    }

    @Override
    protected int rows() {
        return section().getInt("lignes-joueurs", 5);
    }

    @Override
    protected void render() {
        Faction f = faction();
        if (f == null) {
            return;
        }
        ConfigurationSection s = section();
        List<Integer> slots = s.getIntegerList("cases-joueurs");
        if (slots.isEmpty()) {
            return;
        }
        List<FPlayer> members = new ArrayList<FPlayer>();
        for (FPlayer member : plugin.factions().getMembers(f)) {
            // le chef possède toujours toutes les permissions
            if (member.getRole() != Role.LEADER) {
                members.add(member);
            }
        }
        pages = Math.max(1, (members.size() + slots.size() - 1) / slots.size());
        page = Math.max(0, Math.min(page, pages - 1));
        ConfigurationSection template = s.getConfigurationSection("modele-joueur");
        int start = page * slots.size();
        for (int i = 0; i < slots.size() && start + i < members.size(); i++) {
            FPlayer member = members.get(start + i);
            Object[] kv = {
                    "member", member.getName(),
                    "role", plugin.roles().name(member.getRole()),
                    "role_color", plugin.roles().color(member.getRole()),
                    "role_prefix", plugin.roles().prefix(member.getRole()),
                    "custom", f.countPlayerPermissions(member.getUniqueId()),
                    "faction", f.getTag(),
                    "label", plugin.commands().getLabel()
            };
            set(slots.get(i), Items.fromSection(template, kv), clickHandler(template, null, kv));
        }
        Object[] vars = vars();
        if (page > 0) {
            setConfigured(s.getConfigurationSection("joueurs-precedent"), vars);
        }
        if (page < pages - 1) {
            setConfigured(s.getConfigurationSection("joueurs-suivant"), vars);
        }
        setConfigured(s.getConfigurationSection("joueurs-retour"), vars);
    }
}
