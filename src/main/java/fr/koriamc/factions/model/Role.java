package fr.koriamc.factions.model;

import java.util.Locale;

/**
 * Rôles d'une faction, du plus bas au plus haut.
 */
public enum Role {
    RECRUIT,
    MEMBER,
    OFFICER,
    COLEADER,
    LEADER;

    public boolean isAtLeast(Role other) {
        return ordinal() >= other.ordinal();
    }

    public boolean isHigherThan(Role other) {
        return ordinal() > other.ordinal();
    }

    public static Role parse(String name) {
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
