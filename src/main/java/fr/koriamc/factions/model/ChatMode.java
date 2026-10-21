package fr.koriamc.factions.model;

import java.util.Locale;

/**
 * Canaux de discussion. Les noms courts acceptés par /f chat sont définis dans
 * config.json (chat.noms-des-canaux).
 */
public enum ChatMode {
    PUBLIC,
    FACTION,
    MOD;

    public ChatMode next() {
        return values()[(ordinal() + 1) % values().length];
    }

    public static ChatMode parse(String input) {
        if (input == null) {
            return null;
        }
        try {
            return valueOf(input.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
