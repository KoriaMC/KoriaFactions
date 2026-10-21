package fr.koriamc.factions.command.impl;

import fr.koriamc.factions.KoriaFactions;
import fr.koriamc.factions.command.CommandContext;
import fr.koriamc.factions.command.SubCommand;
import fr.koriamc.factions.config.Settings;
import fr.koriamc.factions.manager.PlaceholderService;
import fr.koriamc.factions.model.ChatMode;
import fr.koriamc.factions.model.FPlayer;
import fr.koriamc.factions.model.FactionFlag;
import fr.koriamc.factions.model.PermAction;
import fr.koriamc.factions.model.PermSubject;
import fr.koriamc.factions.model.Role;
import fr.koriamc.factions.util.Nms;
import fr.koriamc.factions.util.Text;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Paramètres et outils de faction : perms, flags, améliorations, coffre, chat,
 * préférences, annonces, coordonnées.
 */
public final class SettingsCommands {

    private SettingsCommands() {
    }

    /** /f perms [joueur] */
    public static final class Perms extends SubCommand {
        public Perms(KoriaFactions plugin) {
            super(plugin, "perms", true, 0);
        }

        @Override
        public void execute(CommandContext ctx) {
            if (!requireFaction(ctx)) {
                return;
            }
            if (ctx.size() == 0) {
                plugin.menus().open(ctx.player, "permissions");
                return;
            }
            if (!plugin.roles().isPlayerPermissionsEnabled()) {
                ctx.msg("general.feature-disabled");
                return;
            }
            FPlayer target = findMember(ctx, ctx.arg(0));
            if (target == null) {
                return;
            }
            if (target.getRole() == Role.LEADER) {
                ctx.msg("perms.leader-has-all");
                return;
            }
            plugin.menus().openPlayerPerms(ctx.player, target);
        }

        @Override
        public List<String> complete(CommandContext ctx) {
            return ctx.size() == 1 ? memberNames(ctx, ctx.arg(0)) : Collections.<String>emptyList();
        }
    }

    /**
     * /f permsgui role &lt;ROLE&gt; &lt;ACTION&gt; | joueur &lt;pseudo&gt; &lt;ACTION&gt; [reset] | joueur &lt;pseudo&gt; defaut |
     * joueur &lt;pseudo&gt; copier &lt;ROLE&gt; :
     * clics de l'écran KoriaMC des permissions (absente de l'aide).
     */
    public static final class PermsGui extends SubCommand {
        public PermsGui(KoriaFactions plugin) {
            super(plugin, "permsgui", true, 3);
            hidden();
        }

        @Override
        public void execute(CommandContext ctx) {
            if (!requireFaction(ctx)) {
                return;
            }
            if (ctx.arg(0).equalsIgnoreCase("role")) {
                plugin.menus().permsScreen().clickRole(ctx.player, PermSubject.parse(ctx.arg(1)), PermAction.parse(ctx.arg(2)));
            } else if (ctx.arg(0).equalsIgnoreCase("joueur") && plugin.roles().isPlayerPermissionsEnabled()) {
                plugin.menus().permsScreen().clickPlayer(ctx.player, plugin.players().getByName(ctx.arg(1)), ctx.arg(2),
                        ctx.size() > 3 ? ctx.arg(3) : "");
            }
        }
    }

    /** /f flags [flag] [on|off] */
    public static final class Flags extends SubCommand {
        public Flags(KoriaFactions plugin) {
            super(plugin, "flags", true, 0);
        }

        @Override
        public void execute(CommandContext ctx) {
            if (!requireFaction(ctx)) {
                return;
            }
            if (ctx.size() == 0) {
                plugin.menus().open(ctx.player, "flags");
                return;
            }
            if ("list".equalsIgnoreCase(ctx.arg(0))) {
                ctx.msg("flags.list-header", "faction", ctx.faction.getTag());
                for (FactionFlag flag : FactionFlag.values()) {
                    Settings.FlagInfo info = plugin.settings().flags.get(flag);
                    boolean value = plugin.factions().getFlag(ctx.faction, flag);
                    ctx.msg("flags.list-line", "flag", info.name, "description", info.description,
                            "state", plugin.msg().text(value ? "flags.state-on" : "flags.state-off"));
                }
                return;
            }
            FactionFlag flag = FactionFlag.parse(ctx.arg(0));
            if (flag == null) {
                ctx.msg("flags.unknown", "flag", ctx.arg(0));
                return;
            }
            boolean current = plugin.factions().getFlag(ctx.faction, flag);
            boolean value = ctx.size() > 1 ? Text.parseBoolean(ctx.arg(1), !current) : !current;
            plugin.factions().changeFlag(ctx.player, ctx.fp, ctx.faction, flag, value, false);
        }

        @Override
        public List<String> complete(CommandContext ctx) {
            if (ctx.size() == 1) {
                List<String> names = new ArrayList<String>();
                names.add("list");
                for (FactionFlag flag : FactionFlag.values()) {
                    names.add(flag.name().toLowerCase(Locale.ROOT));
                }
                return Text.complete(ctx.arg(0), names);
            }
            return ctx.size() == 2 ? Text.complete(ctx.arg(1), Arrays.asList("on", "off")) : Collections.<String>emptyList();
        }
    }

    /** /f open */
    public static final class Open extends SubCommand {
        public Open(KoriaFactions plugin) {
            super(plugin, "open", true, 0);
        }

        @Override
        public void execute(CommandContext ctx) {
            if (!requireFaction(ctx)) {
                return;
            }
            boolean value = !plugin.factions().getFlag(ctx.faction, FactionFlag.OPEN);
            if (plugin.factions().changeFlag(ctx.player, ctx.fp, ctx.faction, FactionFlag.OPEN, value, true)) {
                plugin.factions().notifyMembers(ctx.faction, "flags.open",
                        "state", plugin.msg().text(value ? "flags.open-on" : "flags.open-off"));
            }
        }
    }

    /** /f upgrades */
    public static final class Upgrades extends SubCommand {
        public Upgrades(KoriaFactions plugin) {
            super(plugin, "upgrades", true, 0);
        }

        @Override
        public void execute(CommandContext ctx) {
            if (requireFaction(ctx)) {
                plugin.menus().open(ctx.player, "ameliorations");
            }
        }
    }

    /** /f chest */
    public static final class Chest extends SubCommand {
        public Chest(KoriaFactions plugin) {
            super(plugin, "chest", true, 0);
        }

        @Override
        public void execute(CommandContext ctx) {
            if (!plugin.settings().chestEnabled) {
                ctx.msg("general.feature-disabled");
                return;
            }
            if (!requireFaction(ctx) || !requirePerm(ctx, PermAction.CHEST)) {
                return;
            }
            if (!plugin.chests().canOpen(ctx.faction)) {
                ctx.msg("chest.locked", "label", plugin.commands().getLabel());
                return;
            }
            plugin.chests().open(ctx.player, ctx.faction);
        }
    }

    /** /f chat [canal] [message] */
    public static final class Chat extends SubCommand {
        public Chat(KoriaFactions plugin) {
            super(plugin, "chat", true, 0);
        }

        private boolean available(CommandContext ctx, ChatMode mode) {
            Settings s = plugin.settings();
            if (mode == ChatMode.PUBLIC) {
                return true;
            }
            if (ctx.faction == null) {
                return false;
            }
            if (mode == ChatMode.MOD) {
                return ctx.fp.getRole().isAtLeast(s.chatModMinRole);
            }
            return true;
        }

        @Override
        public void execute(CommandContext ctx) {
            if (!plugin.settings().chatEnabled) {
                ctx.msg("general.feature-disabled");
                return;
            }
            ChatMode mode;
            if (ctx.size() == 0) {
                if (!requireFaction(ctx)) {
                    return;
                }
                mode = ctx.fp.getChatMode().next();
                while (!available(ctx, mode)) {
                    mode = mode.next();
                }
            } else {
                mode = plugin.settings().chatAliases.get(ctx.arg(0).toLowerCase(Locale.ROOT));
                if (mode == null) {
                    ctx.msg("chat.invalid-mode", "mode", ctx.arg(0));
                    return;
                }
                if (mode != ChatMode.PUBLIC && !requireFaction(ctx)) {
                    return;
                }
                if (mode == ChatMode.MOD && !available(ctx, mode)) {
                    ctx.msg("chat.mod-role");
                    return;
                }
                if (!available(ctx, mode)) {
                    ctx.msg("general.feature-disabled");
                    return;
                }
                if (ctx.size() > 1) {
                    plugin.chat().sendToChannel(ctx.player, ctx.fp, mode, ctx.joined(1));
                    return;
                }
            }
            ctx.fp.setChatMode(mode);
            ctx.msg("chat.mode-changed", "mode", plugin.msg().text("chat.modes." + mode.name()));
        }

        @Override
        public List<String> complete(CommandContext ctx) {
            if (ctx.size() != 1) {
                return Collections.emptyList();
            }
            return Text.complete(ctx.arg(0), new ArrayList<String>(plugin.settings().chatAliases.keySet()));
        }
    }

    /** /f toggle &lt;notifications|automap&gt; */
    public static final class Toggle extends SubCommand {
        public Toggle(KoriaFactions plugin) {
            super(plugin, "toggle", true, 1);
        }

        @Override
        public void execute(CommandContext ctx) {
            String option = ctx.arg(0).toLowerCase(Locale.ROOT);
            if (option.startsWith("notif") || option.startsWith("territ")) {
                boolean on = !ctx.fp.isTerritoryNotifications();
                ctx.fp.setTerritoryNotifications(on);
                ctx.msg(on ? "toggle.notifications-on" : "toggle.notifications-off");
            } else if (option.startsWith("automap") || option.equals("map")) {
                boolean on = !ctx.fp.isAutoMap();
                ctx.fp.setAutoMap(on);
                ctx.msg(on ? "toggle.automap-on" : "toggle.automap-off");
            } else {
                ctx.msg("toggle.unknown");
            }
        }

        @Override
        public List<String> complete(CommandContext ctx) {
            return ctx.size() == 1 ? Text.complete(ctx.arg(0), Arrays.asList("notifications", "automap"))
                    : Collections.<String>emptyList();
        }
    }

    /** /f announce &lt;message&gt; */
    public static final class Announce extends SubCommand {
        public Announce(KoriaFactions plugin) {
            super(plugin, "announce", true, 1);
        }

        @Override
        public void execute(CommandContext ctx) {
            if (!requireFaction(ctx) || !requirePerm(ctx, PermAction.ANNOUNCE)) {
                return;
            }
            Settings s = plugin.settings();
            String message = ctx.joined(0);
            if (Text.strip(message).length() > s.announceMax) {
                ctx.msg("announce.too-long", "max", s.announceMax);
                return;
            }
            if (!charge(ctx, "announce")) {
                return;
            }
            Object[] kv = {"player", ctx.player.getName(), "message", message};
            for (Player member : plugin.factions().getOnlinePlayers(ctx.faction)) {
                plugin.msg().send(member, "announce.message", kv);
                if (s.announceTitle) {
                    Nms.title(member, plugin.msg().get("announce.title", kv), plugin.msg().get("announce.subtitle", kv), 10, 70, 20);
                }
            }
            plugin.factions().log(ctx.faction, "ANNOUNCE", kv);
        }
    }

    /** /f coords */
    public static final class Coords extends SubCommand {
        public Coords(KoriaFactions plugin) {
            super(plugin, "coords", true, 0);
        }

        @Override
        public void execute(CommandContext ctx) {
            if (!requireFaction(ctx)) {
                return;
            }
            Object[] location = PlaceholderService.location(ctx.player.getLocation());
            Object[] kv = Arrays.copyOf(location, location.length + 2);
            kv[location.length] = "player";
            kv[location.length + 1] = ctx.player.getName();
            plugin.factions().notifyMembers(ctx.faction, "coords.message", kv);
        }
    }
}
