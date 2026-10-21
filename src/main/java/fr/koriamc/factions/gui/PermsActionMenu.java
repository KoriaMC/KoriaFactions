package fr.koriamc.factions.gui;

import fr.koriamc.factions.KoriaFactions;
import fr.koriamc.factions.config.RoleSettings;
import fr.koriamc.factions.model.FPlayer;
import fr.koriamc.factions.model.Faction;
import fr.koriamc.factions.model.PermAction;
import fr.koriamc.factions.model.PermSubject;
import fr.koriamc.factions.model.Role;
import fr.koriamc.factions.util.Items;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * /f perms, actions d'un rôle. Apparence et effet des clics :
 * menus.json (permissions.modele-action). Action propre : BASCULER.
 */
final class PermsActionMenu extends Menu {

    private final PermSubject subject;

    PermsActionMenu(KoriaFactions plugin, Player player, PermSubject subject) {
        super(plugin, player);
        this.subject = subject;
    }

    @Override
    protected ConfigurationSection section() {
        return plugin.menus().section("permissions");
    }

    @Override
    protected String titleTemplate() {
        return section().getString("titre-actions", "");
    }

    @Override
    protected int rows() {
        return section().getInt("lignes-actions", 6);
    }

    @Override
    protected Object[] vars() {
        Faction f = faction();
        return new Object[]{"player", player.getName(), "faction", f == null ? "" : f.getTag(),
                "subject", plugin.roles().subjectName(subject, plugin.settings()), "label", plugin.commands().getLabel()};
    }

    /** Le joueur peut-il modifier les permissions de ce sujet ? */
    static String denyReason(KoriaFactions plugin, Faction f, FPlayer fp, PermSubject subject, PermAction action) {
        if (!plugin.factions().hasPermission(fp, f, PermAction.PERMS)) {
            return "general.no-faction-permission";
        }
        if (plugin.roles().isLocked(subject, action) && !fp.isBypass()) {
            return "perms.locked";
        }
        Role own = fp.getRole();
        if (!fp.isBypass() && own != Role.LEADER && !own.isHigherThan(subject.getRole())) {
            return "perms.cannot-edit-subject";
        }
        return null;
    }

    @Override
    protected void render() {
        final Faction f = faction();
        if (f == null) {
            return;
        }
        ConfigurationSection s = section();
        ConfigurationSection template = s.getConfigurationSection("modele-action");
        List<Integer> slots = s.getIntegerList("cases-actions");
        RoleSettings roles = plugin.roles();
        List<PermAction> actions = new ArrayList<PermAction>();
        for (PermAction action : PermAction.values()) {
            actions.add(action);
        }
        for (int i = 0; i < actions.size() && i < slots.size(); i++) {
            final PermAction action = actions.get(i);
            boolean allowed = plugin.factions().hasPermission(f, subject, action);
            boolean locked = roles.isLocked(subject, action);
            Object[] kv = {
                    "action", roles.actionName(action),
                    "action_id", action.name(),
                    "icone", roles.actionIcon(action),
                    "description", roles.actionDescription(action),
                    "state", template == null ? "" : template.getString(allowed ? "etat-oui" : "etat-non", ""),
                    "state_color", allowed ? "&a" : "&c",
                    "locked", locked && template != null ? template.getString("texte-verrouille", "") : "",
                    "subject", roles.subjectName(subject, plugin.settings()),
                    "label", plugin.commands().getLabel()
            };
            boolean glow = allowed && template != null && template.getBoolean("brillant-si-autorise");
            set(slots.get(i), Items.fromSection(template, allowed ? "materiau-autorise" : "materiau-refuse", glow, kv),
                    clickHandler(template, new CustomAction() {
                        @Override
                        public boolean run(String type, String value) {
                            if (!type.equals("BASCULER")) {
                                return false;
                            }
                            Faction current = faction();
                            if (current != null) {
                                toggle(current, action);
                            }
                            return true;
                        }
                    }, kv));
        }
        setConfigured(s.getConfigurationSection("retour-actions"), vars());
    }

    private void toggle(Faction f, PermAction action) {
        if (PermsEditor.toggleRole(plugin, player, fp, f, subject, action, true) == null) {
            open();
        }
    }
}
