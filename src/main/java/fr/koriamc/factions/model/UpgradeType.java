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
    DAMAGE_REDUCTION,
    /** Empêche les non-membres de faire /sethome dans le territoire. */
    ANTI_SETHOME,
    /** Coups d'explosion supplémentaires pour casser l'obsidienne du territoire. */
    OBSIDIAN,
    /** Autorise la machine de renforcement dans le territoire. */
    MACHINE;

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
