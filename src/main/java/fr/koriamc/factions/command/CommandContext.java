package fr.koriamc.factions.command;

import fr.koriamc.factions.KoriaFactions;
import fr.koriamc.factions.model.FPlayer;
import fr.koriamc.factions.model.Faction;
import fr.koriamc.factions.util.Text;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;

/**
 * Contexte d'exécution d'une sous-commande.
 */
public final class CommandContext {

    public final KoriaFactions plugin;
    public final CommandSender sender;
    /** Libellé de la commande principale (f, faction...). */
    public final String label;
    /** Nom utilisé pour la sous-commande. */
    public final String subLabel;
    public final String[] args;
    /** Joueur ayant tapé la commande, ou null (console). */
    public final Player player;
    /** Données du joueur, ou null (console). */
    public final FPlayer fp;
    /** Faction du joueur, ou null. */
    public final Faction faction;

    CommandContext(KoriaFactions plugin, CommandSender sender, String label, String subLabel, String[] args) {
        this.plugin = plugin;
        this.sender = sender;
        this.label = label;
        this.subLabel = subLabel;
        this.args = args;
        this.player = sender instanceof Player ? (Player) sender : null;
        this.fp = player == null ? null : plugin.players().get(player);
        this.faction = fp == null ? null : plugin.factions().getFaction(fp);
    }

    public int size() {
        return args.length;
    }

    public String arg(int index) {
        return index >= 0 && index < args.length ? args[index] : null;
    }

    public String joined(int from) {
        return Text.join(args, from, " ");
    }

    public boolean isBypass() {
        return fp != null && fp.isBypass();
    }

    /** Envoie un message en ajoutant automatiquement la variable {label}. */
    public void msg(String key, Object... kv) {
        Object[] all = Arrays.copyOf(kv, kv.length + 2);
        all[kv.length] = "label";
        all[kv.length + 1] = label;
        plugin.msg().send(sender, key, all);
    }
}
