package fr.koriamc.factions.gui;

import fr.koriamc.factions.KoriaFactions;
import fr.koriamc.factions.model.FPlayer;
import fr.koriamc.factions.model.Faction;
import fr.koriamc.factions.util.Items;
import fr.koriamc.factions.util.Text;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Liste paginée des membres. Apparence et effet des clics : menus.json (membres).
 */
final class MembersMenu extends Menu {

    private final Faction faction;

    MembersMenu(KoriaFactions plugin, Player player, Faction faction, int page) {
        super(plugin, player);
        this.faction = faction;
        this.page = page;
    }

    @Override
    protected ConfigurationSection section() {
        return plugin.menus().section("membres");
    }

    @Override
    protected void render() {
        ConfigurationSection s = section();
        List<Integer> slots = s.getIntegerList("cases");
        if (slots.isEmpty()) {
            return;
        }
        List<FPlayer> members = plugin.factions().getMembers(faction);
        pages = Math.max(1, (members.size() + slots.size() - 1) / slots.size());
        page = Math.max(0, Math.min(page, pages - 1));
        ConfigurationSection template = s.getConfigurationSection("modele-membre");
        int start = page * slots.size();
        for (int i = 0; i < slots.size() && start + i < members.size(); i++) {
            FPlayer member = members.get(start + i);
            Object[] kv = {
                    "member", member.getName(),
                    "role", plugin.roles().name(member.getRole()),
                    "role_color", plugin.roles().color(member.getRole()),
                    "role_prefix", plugin.roles().prefix(member.getRole()),
                    "title", member.getTitle() == null ? "" : member.getTitle(),
                    "power", Text.power(member.getPower()),
                    "max_power", Text.power(plugin.players().getMaxPower(member)),
                    "status", template == null ? "" : template.getString(member.isOnline() ? "statut-en-ligne" : "statut-hors-ligne", ""),
                    "last_seen", member.isOnline() ? plugin.msg().text("values.online") : plugin.msg().since(member.getLastSeen()),
                    "faction", faction.getTag(),
                    "label", plugin.commands().getLabel()
            };
            set(slots.get(i), Items.fromSection(template, kv), clickHandler(template, null, kv));
        }
        Object[] vars = vars();
        if (page > 0) {
            setConfigured(s.getConfigurationSection("precedent"), vars);
        }
        if (page < pages - 1) {
            setConfigured(s.getConfigurationSection("suivant"), vars);
        }
        setConfigured(s.getConfigurationSection("retour"), vars);
    }
}
