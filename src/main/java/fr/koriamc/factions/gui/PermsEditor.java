package fr.koriamc.factions.gui;

import fr.koriamc.factions.KoriaFactions;
import fr.koriamc.factions.model.FPlayer;
import fr.koriamc.factions.model.Faction;
import fr.koriamc.factions.model.PermAction;
import fr.koriamc.factions.model.PermSubject;
import org.bukkit.entity.Player;

/**
 * Modification des permissions de faction, commune aux menus coffre et à l'écran KoriaMC.
 * Chaque méthode renvoie null si la modification est faite, sinon le texte de l'erreur (sans préfixe) ;
 * avec chat = true, le résultat est aussi envoyé dans le chat (menus coffre).
 */
final class PermsEditor {

    private PermsEditor() {
    }

    /** Bascule une action d'un rôle. */
    static String toggleRole(KoriaFactions plugin, Player player, FPlayer fp, Faction f, PermSubject subject,
                             PermAction action, boolean chat) {
        String reason = PermsActionMenu.denyReason(plugin, f, fp, subject, action);
        if (reason != null) {
            return deny(plugin, player, chat, reason, "action", plugin.roles().actionName(PermAction.PERMS));
        }
        boolean newValue = !plugin.factions().hasPermission(f, subject, action);
        f.setPermission(subject, action, newValue == plugin.roles().isDefault(subject, action) ? null : newValue);
        String state = plugin.msg().text(newValue ? "perms.state-on" : "perms.state-off");
        String subjectName = plugin.roles().subjectName(subject, plugin.settings());
        if (chat) {
            plugin.msg().send(player, "perms.changed", "action", plugin.roles().actionName(action),
                    "subject", subjectName, "state", state);
        }
        plugin.factions().log(f, "PERM", "player", fp.getName(), "action", plugin.roles().actionName(action),
                "subject", subjectName, "state", state);
        return null;
    }

    /** Bascule une action d'un membre, ou la remet comme son rôle (reset). */
    static String changePlayer(KoriaFactions plugin, Player player, FPlayer fp, Faction f, FPlayer target,
                               PermAction action, boolean reset, boolean chat) {
        String reason = PermsPlayerActionMenu.denyReason(plugin, f, fp, target, action);
        if (reason != null) {
            return deny(plugin, player, chat, reason, "action", plugin.roles().actionName(PermAction.PERMS),
                    "player", target.getName());
        }
        Boolean value;
        if (reset) {
            value = null;
        } else {
            boolean newValue = !plugin.factions().hasMemberPermission(f, target, action);
            boolean fromRole = plugin.factions().hasPermission(f, PermSubject.of(target.getRole()), action);
            // identique au rôle : la valeur personnalisée devient inutile
            value = newValue == fromRole ? null : newValue;
        }
        f.setPlayerPermission(target.getUniqueId(), action, value);
        boolean effective = plugin.factions().hasMemberPermission(f, target, action);
        String state = plugin.msg().text(effective ? "perms.state-on" : "perms.state-off");
        if (chat) {
            plugin.msg().send(player, value == null ? "perms.player-reset" : "perms.player-changed",
                    "action", plugin.roles().actionName(action), "player", target.getName(), "state", state);
        }
        plugin.factions().log(f, "PERM", "player", fp.getName(), "action", plugin.roles().actionName(action),
                "subject", target.getName(), "state", state);
        return null;
    }

    /**
     * Configuration par défaut : le membre reçoit les permissions d'un rôle (personnalisées seulement là où elles
     * diffèrent de son propre rôle ; les permissions verrouillées ne changent pas).
     */
    static String copyRole(KoriaFactions plugin, Player player, FPlayer fp, Faction f, FPlayer target, PermSubject source,
                           boolean chat) {
        PermSubject own = PermSubject.of(target.getRole());
        String error = null;
        int allowed = 0;
        for (PermAction action : PermAction.values()) {
            String reason = PermsPlayerActionMenu.denyReason(plugin, f, fp, target, action);
            if (reason != null) {
                if (error == null || "perms.locked".equals(error)) {
                    error = reason;
                }
                continue;
            }
            allowed++;
            boolean wanted = plugin.factions().hasPermission(f, source, action);
            boolean fromRole = own != null && plugin.factions().hasPermission(f, own, action);
            f.setPlayerPermission(target.getUniqueId(), action, wanted == fromRole ? null : wanted);
        }
        if (allowed == 0 && error != null) {
            return deny(plugin, player, chat, error, "action", plugin.roles().actionName(PermAction.PERMS),
                    "player", target.getName());
        }
        String sourceName = plugin.roles().subjectName(source, plugin.settings());
        if (chat) {
            plugin.msg().send(player, "perms.player-copied", "player", target.getName(), "subject", sourceName);
        }
        plugin.factions().log(f, "PERM", "player", fp.getName(), "action", plugin.msg().text("perms.all-actions"),
                "subject", target.getName(), "state", plugin.msg().text("perms.state-copied", "subject", sourceName));
        return null;
    }

    /** Toutes les permissions personnelles du membre reviennent à celles de son rôle. */
    static String resetPlayer(KoriaFactions plugin, Player player, FPlayer fp, Faction f, FPlayer target, boolean chat) {
        String error = null;
        int changed = 0;
        for (PermAction action : PermAction.values()) {
            if (f.getPlayerPermission(target.getUniqueId(), action) == null) {
                continue;
            }
            String reason = PermsPlayerActionMenu.denyReason(plugin, f, fp, target, action);
            if (reason == null) {
                f.setPlayerPermission(target.getUniqueId(), action, null);
                changed++;
            } else if (error == null || "perms.locked".equals(error)) {
                error = reason;
            }
        }
        if (changed == 0 && error != null && !"perms.locked".equals(error)) {
            return deny(plugin, player, chat, error, "action", plugin.roles().actionName(PermAction.PERMS),
                    "player", target.getName());
        }
        if (changed > 0) {
            String state = plugin.msg().text("perms.state-role");
            if (chat) {
                plugin.msg().send(player, "perms.player-reset-all", "player", target.getName());
            }
            plugin.factions().log(f, "PERM", "player", fp.getName(), "action", plugin.msg().text("perms.all-actions"),
                    "subject", target.getName(), "state", state);
        }
        return null;
    }

    private static String deny(KoriaFactions plugin, Player player, boolean chat, String key, Object... kv) {
        if (chat) {
            plugin.msg().send(player, key, kv);
        }
        String raw = plugin.msg().config().getString(key, "");
        return plugin.msg().format(raw.replace("{prefix}", ""), kv);
    }
}
