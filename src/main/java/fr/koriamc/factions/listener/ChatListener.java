package fr.koriamc.factions.listener;

import fr.koriamc.factions.KoriaFactions;
import fr.koriamc.factions.config.Settings;
import fr.koriamc.factions.model.ChatMode;
import fr.koriamc.factions.model.FPlayer;
import fr.koriamc.factions.model.Faction;
import fr.koriamc.factions.model.Relation;
import fr.koriamc.factions.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.EventExecutor;

import java.util.HashSet;
import java.util.IllegalFormatException;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Chat : tag de faction et couleur du grade dans le chat public, canaux privés
 * (faction, modération). La priorité de l'écouteur est configurable.
 */
public final class ChatListener implements Listener, EventExecutor {

    public static final String COLOR_PERMISSION = "koriafactions.chat.color";

    private final KoriaFactions plugin;

    public ChatListener(KoriaFactions plugin) {
        this.plugin = plugin;
    }

    /** (Ré)enregistre l'écouteur avec la priorité configurée. */
    public void register() {
        HandlerList.unregisterAll(this);
        Settings s = plugin.settings();
        if (s.chatEnabled) {
            Bukkit.getPluginManager().registerEvent(AsyncPlayerChatEvent.class, this, s.chatPriority, this, plugin, true);
        }
    }

    @Override
    public void execute(Listener listener, Event event) {
        if (event instanceof AsyncPlayerChatEvent) {
            onChat((AsyncPlayerChatEvent) event);
        }
    }

    private void onChat(AsyncPlayerChatEvent event) {
        final Player player = event.getPlayer();
        final FPlayer fp = plugin.players().get(player.getUniqueId());
        if (fp == null) {
            return;
        }
        Settings s = plugin.settings();
        ChatMode mode = fp.getChatMode();
        String message = event.getMessage();
        for (Map.Entry<ChatMode, String> quick : s.chatQuickPrefixes.entrySet()) {
            String prefix = quick.getValue();
            if (message.startsWith(prefix) && message.length() > prefix.length()) {
                mode = quick.getKey();
                message = message.substring(prefix.length()).trim();
                break;
            }
        }
        if (mode != ChatMode.PUBLIC && fp.hasFaction()) {
            event.setCancelled(true);
            final ChatMode channel = mode;
            final String text = message;
            if (event.isAsynchronous()) {
                Bukkit.getScheduler().runTask(plugin, new Runnable() {
                    @Override
                    public void run() {
                        if (player.isOnline()) {
                            sendToChannel(player, fp, channel, text);
                        }
                    }
                });
            } else {
                sendToChannel(player, fp, channel, text);
            }
            return;
        }
        formatPublic(event, fp);
    }

    // =========================================================== Chat public

    private String tag(FPlayer sender, FPlayer viewer) {
        Settings s = plugin.settings();
        Faction f = plugin.factions().getFaction(sender);
        if (f == null) {
            return Text.color(s.chatTagNoFaction);
        }
        String color = viewer == null ? s.chatTagDefaultColor : s.relationColor(plugin.factions().getRelation(viewer, sender));
        return Text.color(Text.replace(s.chatTagFormat,
                "relation_color", color,
                "role_prefix", plugin.roles().prefix(sender.getRole()),
                "faction", f.getTag(),
                "title", sender.getTitle() == null ? "" : sender.getTitle()));
    }

    private static String escape(String s) {
        return s.replace("%", "%%");
    }

    private String buildFormat(String base, String tag, Player sender) {
        Settings s = plugin.settings();
        if ("FORMAT".equals(s.chatPublicMode)) {
            Settings.Rank rank = s.rankOf(sender);
            String format = Text.replace(s.chatPublicFormat, "tag", escape(tag),
                    "rank", escape(rank.name), "rank_color", rank.color, "rank_prefix", escape(rank.prefix));
            format = format.replace("{player}", "%1$s").replace("{message}", "%2$s");
            return Text.color(format);
        }
        return base.replace(s.chatPlaceholder, escape(tag));
    }

    private void formatPublic(AsyncPlayerChatEvent event, FPlayer fp) {
        Settings s = plugin.settings();
        if ("NONE".equals(s.chatPublicMode)) {
            return;
        }
        String base = event.getFormat();
        if ("PLACEHOLDER".equals(s.chatPublicMode) && !base.contains(s.chatPlaceholder)) {
            return;
        }
        if (!s.chatRelationColorsPerRecipient) {
            event.setFormat(buildFormat(base, tag(fp, null), event.getPlayer()));
            return;
        }
        // un message par destinataire, coloré selon sa relation avec l'expéditeur
        Player sender = event.getPlayer();
        try {
            for (Player recipient : event.getRecipients()) {
                FPlayer viewer = plugin.players().get(recipient.getUniqueId());
                String format = buildFormat(base, tag(fp, viewer), sender);
                recipient.sendMessage(String.format(format, sender.getDisplayName(), event.getMessage()));
            }
        } catch (IllegalFormatException e) {
            plugin.getLogger().warning("Format de chat invalide : " + e.getMessage());
            event.setFormat(buildFormat(base, tag(fp, null), event.getPlayer()));
            return;
        }
        event.getRecipients().clear();
        event.setFormat(buildFormat(base, tag(fp, null), event.getPlayer()));
    }

    // ========================================================== Canaux privés

    /** Envoie un message dans un canal privé (thread principal). */
    public void sendToChannel(Player sender, FPlayer fp, ChatMode mode, String message) {
        Faction f = plugin.factions().getFaction(fp);
        if (f == null || mode == ChatMode.PUBLIC) {
            plugin.msg().send(sender, "general.must-have-faction");
            return;
        }
        Settings s = plugin.settings();
        Set<Player> recipients = new LinkedHashSet<Player>();
        switch (mode) {
            case MOD:
                for (Player member : plugin.factions().getOnlinePlayers(f)) {
                    if (plugin.players().get(member).getRole().isAtLeast(s.chatModMinRole)) {
                        recipients.add(member);
                    }
                }
                recipients.add(sender);
                break;
            default:
                recipients.addAll(plugin.factions().getOnlinePlayers(f));
        }
        String body = sender.hasPermission(COLOR_PERMISSION) ? Text.color(message) : message;
        String template = s.chatFormats.get(mode);
        String title = fp.getTitle() == null ? "" : fp.getTitle() + " ";
        String line = null;
        Set<UUID> received = new HashSet<UUID>();
        for (Player recipient : recipients) {
            FPlayer viewer = plugin.players().get(recipient);
            line = render(template, sender, fp, f, viewer, title, body);
            recipient.sendMessage(line);
            received.add(recipient.getUniqueId());
        }
        if (line == null) {
            line = render(template, sender, fp, f, fp, title, body);
        }
        String spyLine = Text.color(s.chatSpyFormat).replace("{message}", line);
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (!received.contains(online.getUniqueId()) && plugin.players().get(online).isSpy()) {
                online.sendMessage(spyLine);
            }
        }
        if (s.chatLogPrivate) {
            Bukkit.getConsoleSender().sendMessage(line);
        }
    }

    private String render(String template, Player sender, FPlayer fp, Faction f, FPlayer viewer, String title, String body) {
        Settings s = plugin.settings();
        String[] parts = template.split("\\{message\\}", 2);
        Settings.Rank rank = s.rankOf(sender);
        Object[] kv = {
                "player", sender.getName(),
                "rank", rank.name,
                "rank_color", rank.color,
                "rank_prefix", rank.prefix,
                "role_prefix", plugin.roles().prefix(fp.getRole()),
                "title", title,
                "faction", f.getTag(),
                "relation_color", s.relationColor(plugin.factions().getRelation(viewer, fp))
        };
        String before = Text.color(Text.replace(parts[0], kv));
        String after = parts.length > 1 ? Text.color(Text.replace(parts[1], kv)) : "";
        return before + body + after;
    }
}
