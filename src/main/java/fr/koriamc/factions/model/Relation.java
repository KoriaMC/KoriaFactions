package fr.koriamc.factions.model;

import java.util.Locale;

/**
 * Relations entre factions : membres de la même faction, ou ennemis.
 */
public enum Relation {
    ENEMY,
    MEMBER;

    public boolean isAtLeast(Relation other) {
        return ordinal() >= other.ordinal();
    }

    /** Les membres d'une même faction ne peuvent pas se nuire. */
    public boolean isFriendly() {
        return this == MEMBER;
    }

    public static Relation parse(String name) {
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
