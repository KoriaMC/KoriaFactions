package fr.koriamc.factions.gui;

import fr.koriamc.factions.KoriaFactions;
import fr.koriamc.factions.config.RoleSettings;
import fr.koriamc.factions.model.FPlayer;
import fr.koriamc.factions.model.Faction;
import fr.koriamc.factions.model.PermAction;
import fr.koriamc.factions.model.PermSubject;
import fr.koriamc.factions.model.Role;
import fr.koriamc.factions.util.Items;
import fr.koriamc.factions.util.Text;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * /f perms &lt;joueur&gt; : permissions personnelles d'un membre, qui remplacent
 * celles de son rôle. Apparence et clics : menus.json (permissions.modele-action-joueur).
 * Actions propres : BASCULER, REINITIALISER (revenir au rôle).
 */
final class PermsPlayerActionMenu extends Menu {

    private final FPlayer target;

    PermsPlayerActionMenu(KoriaFactions plugin, Player player, FPlayer target) {
        super(plugin, player);
        this.target = target;
    }

    @Override
    protected ConfigurationSection section() {
        return plugin.menus().section("permissions");
    }

    @Override
    protected String titleTemplate() {
        return section().getString("titre-actions-joueur", "");
    }

    @Override
    protected int rows() {
        return section().getInt("lignes-actions", 6);
    }

    @Override
    protected Object[] vars() {
        Faction f = faction();
        return new Object[]{"player", player.getName(), "faction", f == null ? "" : f.getTag(),
                "member", target.getName(), "label", plugin.commands().getLabel()};
    }

    /** @return la clé du message si le joueur ne peut pas modifier cette permission, sinon null */
    static String denyReason(KoriaFactions plugin, Faction f, FPlayer editor, FPlayer target, PermAction action) {
        if (!plugin.factions().hasPermission(editor, f, PermAction.PERMS)) {
            return "general.no-faction-permission";
        }
        if (!f.getId().equals(target.getFactionId())) {
            return "general.target-not-member";
        }
        if (target.getRole() == Role.LEADER) {
            return "perms.leader-has-all";
        }
        if (editor.isBypass()) {
            return null;
        }
        if (plugin.roles().isLocked(PermSubject.of(target.getRole()), action)) {
            return "perms.locked";
        }
        Role own = editor.getRole();
        if (own != Role.LEADER && !own.isHigherThan(target.getRole())) {
            return "perms.cannot-edit-player";
        }
        return null;
    }

    @Override
    protected void render() {
        final Faction f = faction();
        if (f == null || !f.getId().equals(target.getFactionId())) {
            return;
        }
        ConfigurationSection s = section();
        ConfigurationSection template = s.getConfigurationSection("modele-action-joueur");
        List<Integer> slots = s.getIntegerList("cases-actions");
        RoleSettings roles = plugin.roles();
        PermSubject roleSubject = PermSubject.of(target.getRole());
        PermAction[] actions = PermAction.values();
        for (int i = 0; i < actions.length && i < slots.size(); i++) {
            final PermAction action = actions[i];
            boolean fromRole = plugin.factions().hasPermission(f, roleSubject, action);
            Boolean custom = f.getPlayerPermission(target.getUniqueId(), action);
            boolean locked = roleSubject == null || roles.isLocked(roleSubject, action);
            boolean effective = plugin.factions().hasMemberPermission(f, target, action);
            String on = template == null ? "" : template.getString("etat-oui", "");
            String off = template == null ? "" : template.getString("etat-non", "");
            String state = custom == null || locked
                    ? Text.replace(template == null ? "" : template.getString("etat-herite", ""), "role_state", fromRole ? on : off)
                    : custom ? on : off;
            Object[] kv = {
                    "action", roles.actionName(action),
                    "action_id", action.name(),
                    "icone", roles.actionIcon(action),
                    "description", roles.actionDescription(action),
                    "role", roles.name(target.getRole()),
                    "role_state", fromRole ? on : off,
                    "state", state,
                    "state_color", effective ? "&a" : "&c",
                    "locked", locked && template != null ? template.getString("texte-verrouille", "") : "",
                    "member", target.getName(),
                    "label", plugin.commands().getLabel()
            };
            boolean glow = custom != null && !locked && template != null && template.getBoolean("brillant-si-personnalise");
            set(slots.get(i), Items.fromSection(template, effective ? "materiau-autorise" : "materiau-refuse", glow, kv),
                    clickHandler(template, new CustomAction() {
                        @Override
                        public boolean run(String type, String value) {
                            if (!type.equals("BASCULER") && !type.equals("REINITIALISER")) {
                                return false;
                            }
                            Faction current = faction();
                            if (current != null) {
                                change(current, action, type.equals("REINITIALISER"));
                            }
                            return true;
                        }
                    }, kv));
        }
        setConfigured(s.getConfigurationSection("retour-actions-joueur"), vars());
    }

    private void change(Faction f, PermAction action, boolean reset) {
        if (PermsEditor.changePlayer(plugin, player, fp, f, target, action, reset, true) == null) {
            open();
        }
    }
}
