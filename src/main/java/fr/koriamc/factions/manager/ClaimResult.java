package fr.koriamc.factions.manager;

import fr.koriamc.factions.config.Messages;
import org.bukkit.command.CommandSender;

/**
 * Résultat d'une tentative de claim / unclaim : succès ou échec, avec le message
 * associé. Une clé nulle signifie que le message a déjà été envoyé.
 */
public final class ClaimResult {

    private final boolean success;
    private final String key;
    private final Object[] args;

    private ClaimResult(boolean success, String key, Object[] args) {
        this.success = success;
        this.key = key;
        this.args = args;
    }

    public static ClaimResult ok(String key, Object... args) {
        return new ClaimResult(true, key, args);
    }

    public static ClaimResult fail(String key, Object... args) {
        return new ClaimResult(false, key, args);
    }

    public boolean isSuccess() {
        return success;
    }

    public void send(Messages messages, CommandSender to) {
        if (key != null) {
            messages.send(to, key, args);
        }
    }
}
