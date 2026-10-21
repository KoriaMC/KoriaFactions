package fr.koriamc.factions.config;

import fr.koriamc.factions.model.PermAction;
import fr.koriamc.factions.model.PermSubject;
import fr.koriamc.factions.model.Role;
import org.bukkit.configuration.ConfigurationSection;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Valeurs typées des rôles et permissions (factions.json : roles, permissions).
 */
public final class RoleSettings {

    private final Map<Role, String> names = new EnumMap<Role, String>(Role.class);
    private final Map<Role, String> prefixes = new EnumMap<Role, String>(Role.class);
    private final Map<Role, String> colors = new EnumMap<Role, String>(Role.class);
    private final Map<Role, Boolean> enabled = new EnumMap<Role, Boolean>(Role.class);
    private final Map<PermSubject, Set<PermAction>> defaults = new EnumMap<PermSubject, Set<PermAction>>(PermSubject.class);
    private final Map<PermSubject, Set<PermAction>> locked = new EnumMap<PermSubject, Set<PermAction>>(PermSubject.class);
    private final Map<PermAction, String> actionNames = new EnumMap<PermAction, String>(PermAction.class);
    private final Map<PermAction, String> actionDescriptions = new EnumMap<PermAction, String>(PermAction.class);
    private final Map<PermAction, String> actionIcons = new EnumMap<PermAction, String>(PermAction.class);
    private boolean playerPermissionsEnabled;

    public void load(ConfigurationSection c, Logger logger) {
        playerPermissionsEnabled = c.getBoolean("permissions.par-joueur");
        for (Role role : Role.values()) {
            String p = "roles." + role.name() + ".";
            names.put(role, Settings.str(c, p + "nom", role.name()));
            prefixes.put(role, Settings.str(c, p + "prefixe", ""));
            colors.put(role, Settings.str(c, p + "couleur", "&f"));
            boolean canDisable = role == Role.COLEADER || role == Role.RECRUIT;
            enabled.put(role, !canDisable || !c.contains(p + "actif") || c.getBoolean(p + "actif"));
        }
        for (PermSubject subject : PermSubject.values()) {
            String p = "permissions.par-defaut." + subject.name();
            Set<PermAction> set = Settings.actionSet(c.getStringList(p), logger, p);
            defaults.put(subject, set);
            String l = "permissions.verrouillees." + subject.name();
            locked.put(subject, Settings.actionSet(c.getStringList(l), logger, l));
        }
        for (PermAction action : PermAction.values()) {
            String p = "permissions.actions." + action.name() + ".";
            actionNames.put(action, Settings.str(c, p + "nom", action.name()));
            actionDescriptions.put(action, Settings.str(c, p + "description", ""));
            actionIcons.put(action, Settings.str(c, p + "icone", "PAPER"));
        }
    }

    /** Permissions personnalisées par membre (/f perms <joueur>). */
    public boolean isPlayerPermissionsEnabled() {
        return playerPermissionsEnabled;
    }

    public static Set<PermAction> territoryActions() {
        Set<PermAction> set = EnumSet.noneOf(PermAction.class);
        for (PermAction action : PermAction.values()) {
            if (action.isTerritory()) {
                set.add(action);
            }
        }
        return set;
    }

    public String name(Role role) {
        return names.get(role);
    }

    public String prefix(Role role) {
        return prefixes.get(role);
    }

    public String color(Role role) {
        return colors.get(role);
    }

    public boolean isEnabled(Role role) {
        return enabled.get(role);
    }

    /** Rôle immédiatement supérieur (en ignorant les rôles désactivés), ou null. */
    public Role next(Role role) {
        for (int i = role.ordinal() + 1; i < Role.values().length; i++) {
            Role r = Role.values()[i];
            if (isEnabled(r)) {
                return r;
            }
        }
        return null;
    }

    /** Rôle immédiatement inférieur (en ignorant les rôles désactivés), ou null. */
    public Role previous(Role role) {
        for (int i = role.ordinal() - 1; i >= 0; i--) {
            Role r = Role.values()[i];
            if (isEnabled(r)) {
                return r;
            }
        }
        return null;
    }

    /** Rôle le plus bas actif. */
    public Role lowest() {
        for (Role r : Role.values()) {
            if (isEnabled(r)) {
                return r;
            }
        }
        return Role.MEMBER;
    }

    public boolean isDefault(PermSubject subject, PermAction action) {
        return defaults.get(subject).contains(action);
    }

    public boolean isLocked(PermSubject subject, PermAction action) {
        return locked.get(subject).contains(action);
    }

    public String actionName(PermAction action) {
        return actionNames.get(action);
    }

    public String actionDescription(PermAction action) {
        return actionDescriptions.get(action);
    }

    public String actionIcon(PermAction action) {
        return actionIcons.get(action);
    }

    /** Nom affiché d'un sujet de permission (rôle). */
    public String subjectName(PermSubject subject, Settings settings) {
        return name(subject.getRole());
    }
}
