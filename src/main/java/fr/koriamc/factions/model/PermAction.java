package fr.koriamc.factions.model;

import java.util.Locale;

/**
 * Actions soumises aux permissions de faction (/f perms).
 * Les actions "de territoire" concernent ce qui se fait dans le territoire de la faction.
 */
public enum PermAction {
    BUILD(true),
    DESTROY(true),
    CONTAINER(true),
    DOOR(true),
    BUTTON(true),
    LEVER(true),
    PRESSURE_PLATE(true),
    USE_ITEM(true),
    INTERACT(true),
    ENTITY_INTERACT(true),
    SPAWNER(true),
    HOME(false),
    WARP(false),
    INVITE(false),
    KICK(false),
    BAN(false),
    CLAIM(false),
    UNCLAIM(false),
    UNCLAIM_ALL(false),
    PROMOTE(false),
    DEMOTE(false),
    SETHOME(false),
    SETWARP(false),
    DELWARP(false),
    CHEST(false),
    DESCRIPTION(false),
    MOTD(false),
    TITLE(false),
    FLAGS(false),
    UPGRADE(false),
    ACCESS(false),
    ANNOUNCE(false),
    LOGS(false),
    RENAME(false),
    PERMS(false);

    private final boolean territory;

    PermAction(boolean territory) {
        this.territory = territory;
    }

    public boolean isTerritory() {
        return territory;
    }

    public static PermAction parse(String name) {
        if (name == null) {
            return null;
        }
        try {
            return valueOf(name.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
