package fr.koriamc.factions.model;

import java.util.Locale;

/**
 * Effets possibles d'une amélioration de faction (factions.json, ameliorations.liste).
 */
public enum UpgradeType {
    MEMBERS,
    WARPS,
    CHEST,
    POWER,
    CROPS,
    EXP,
    SPAWNERS,
    DAMAGE_REDUCTION;

    public static UpgradeType parse(String name) {
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
