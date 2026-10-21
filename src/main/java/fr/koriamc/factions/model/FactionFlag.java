package fr.koriamc.factions.model;

import java.util.Locale;

/**
 * Paramètres activables d'une faction (/f flags).
 */
public enum FactionFlag {
    OPEN,
    PEACEFUL,
    PERMANENT,
    EXPLOSIONS,
    FRIENDLY_FIRE,
    PVP,
    MONSTERS,
    FIRE_SPREAD,
    INFINITE_POWER;

    public static FactionFlag parse(String name) {
        if (name == null) {
            return null;
        }
        try {
            return valueOf(name.trim().toUpperCase(Locale.ROOT).replace('-', '_'));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
