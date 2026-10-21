package fr.koriamc.factions.manager;

import fr.koriamc.factions.KoriaFactions;
import fr.koriamc.factions.config.EconomySettings;
import fr.koriamc.factions.hook.VaultHook;
import fr.koriamc.factions.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * Pont vers Vault : tout ce qui se paie (commandes, améliorations) et les
 * récompenses passent par le compte des joueurs. Il n'existe aucun argent de faction.
 */
public final class EconomyManager {

    public static final String BYPASS_PERMISSION = "koriafactions.bypass.cost";

    private final KoriaFactions plugin;
    private VaultHook vault;

    public EconomyManager(KoriaFactions plugin) {
        this.plugin = plugin;
    }

    /** Recherche le fournisseur d'économie de Vault. */
    public void hook() {
        vault = null;
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            return;
        }
        try {
            vault = VaultHook.create();
        } catch (Throwable t) {
            plugin.getLogger().warning("Impossible de se connecter à Vault : " + t);
            vault = null;
        }
    }

    public boolean isAvailable() {
        return vault != null && plugin.economySettings().enabled;
    }

    public String getProviderName() {
        return vault == null ? null : vault.getName();
    }

    public String format(double amount) {
        EconomySettings s = plugin.economySettings();
        return Text.replace(s.format, "amount", Text.number(amount, s.decimals));
    }

    public double getBalance(Player player) {
        return isAvailable() ? vault.getBalance(player) : 0;
    }

    public boolean has(Player player, double amount) {
        return !isAvailable() || vault.has(player, amount);
    }

    public boolean withdraw(Player player, double amount) {
        if (!isAvailable() || amount <= 0) {
            return true;
        }
        return vault.withdraw(player, amount);
    }

    public void deposit(Player player, double amount) {
        if (isAvailable() && amount > 0) {
            vault.deposit(player, amount);
        }
    }

    // ================================================================= Coûts

    /** Résultat d'un paiement. */
    public enum Payment {
        /** Rien à payer (coût nul, économie désactivée ou permission de gratuité). */
        FREE,
        PAID,
        /** Économie activée mais aucun fournisseur Vault : le paiement est refusé. */
        UNAVAILABLE,
        NOT_ENOUGH
    }

    /**
     * Prélève un montant sur le compte Vault du joueur. Avec economie.actif à false
     * tout est gratuit ; avec economie.actif à true mais sans Vault, rien n'est
     * offert : le paiement est refusé.
     */
    public Payment pay(Player player, double cost) {
        if (cost <= 0 || !plugin.economySettings().enabled || player.hasPermission(BYPASS_PERMISSION)) {
            return Payment.FREE;
        }
        if (vault == null) {
            return Payment.UNAVAILABLE;
        }
        if (!vault.has(player, cost) || !vault.withdraw(player, cost)) {
            return Payment.NOT_ENOUGH;
        }
        return Payment.PAID;
    }

    /** Fait payer une commande dont le coût est défini dans config.json (economie.couts.&lt;cle&gt;). */
    public boolean charge(Player player, String costKey) {
        return charge(player, plugin.economySettings().cost(costKey));
    }

    /**
     * Prélève un montant au joueur.
     *
     * @return false si le joueur ne peut pas payer (message envoyé)
     */
    public boolean charge(Player player, double cost) {
        switch (pay(player, cost)) {
            case UNAVAILABLE:
                plugin.msg().send(player, "economy.unavailable");
                return false;
            case NOT_ENOUGH:
                plugin.msg().send(player, "economy.not-enough", "cost", format(cost));
                return false;
            case PAID:
                plugin.msg().send(player, "economy.paid", "cost", format(cost));
                return true;
            default:
                return true;
        }
    }

    /** Avertit la console si l'économie est activée sans fournisseur Vault. */
    public void warnIfUnavailable() {
        if (plugin.economySettings().enabled && vault == null) {
            plugin.getLogger().warning("economie.actif vaut true mais aucune économie Vault n'est disponible :"
                    + " les actions payantes sont refusées tant que Vault et un plugin d'économie ne sont pas installés.");
        }
    }
}
