package fr.koriamc.factions.model;

import java.util.Locale;

/**
 * Destinataire d'une permission de faction : un rôle (hors chef).
 * Les joueurs des autres factions n'ont aucune permission dans le territoire.
 */
public enum PermSubject {
    COLEADER(Role.COLEADER),
    OFFICER(Role.OFFICER),
    MEMBER(Role.MEMBER),
    RECRUIT(Role.RECRUIT);

    private final Role role;

    PermSubject(Role role) {
        this.role = role;
    }

    public Role getRole() {
        return role;
    }

    /** @return le sujet du rôle, ou null pour le chef (qui a tous les droits). */
    public static PermSubject of(Role role) {
        for (PermSubject s : values()) {
            if (s.role == role) {
                return s;
            }
        }
        return null;
    }

    public static PermSubject parse(String name) {
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
