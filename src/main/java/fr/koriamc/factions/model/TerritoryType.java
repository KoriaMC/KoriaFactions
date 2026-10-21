package fr.koriamc.factions.model;

import java.util.Locale;

/**
 * Type de territoire tel que perçu par un joueur.
 */
public enum TerritoryType {
    WILDERNESS,
    SAFEZONE,
    WARZONE,
    OWN,
    ENEMY;

    public static TerritoryType of(Relation relation) {
        switch (relation) {
            case MEMBER:
                return OWN;
            default:
                return ENEMY;
        }
    }

    public static TerritoryType parse(String name) {
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
