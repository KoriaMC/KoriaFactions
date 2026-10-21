package fr.koriamc.factions.command.impl;

import fr.koriamc.factions.KoriaFactions;
import fr.koriamc.factions.api.event.FactionCreateEvent;
import fr.koriamc.factions.api.event.FactionDisbandEvent;
import fr.koriamc.factions.command.CommandContext;
import fr.koriamc.factions.command.SubCommand;
import fr.koriamc.factions.config.Settings;
import fr.koriamc.factions.model.Faction;
import fr.koriamc.factions.model.FactionFlag;
import fr.koriamc.factions.model.PermAction;
import fr.koriamc.factions.model.Role;
import fr.koriamc.factions.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;

import java.util.Collections;
import java.util.List;

/**
 * Création, dissolution, nom, description, message du jour, menu et version.
 */
public final class CoreCommands {

    private CoreCommands() {
    }

    /** /f menu [menu] (identifiants : voir MenuManager.open, ex. permissions-roles) */
    public static final class Menu extends SubCommand {
        public Menu(KoriaFactions plugin) {
            super(plugin, "menu", true, 0);
        }

        @Override
        public void execute(CommandContext ctx) {
            plugin.menus().open(ctx.player, ctx.size() == 0 ? "principal" : ctx.arg(0));
        }
    }

    /**
     * /f quests : raccourci vers les quêtes de faction d'un autre plugin (KoriaQuest). La commande
     * exécutée par le joueur est lue dans config.json (commandes.quests.commande).
     */
    public static final class Quests extends SubCommand {
        private String target = "";

        public Quests(KoriaFactions plugin) {
            super(plugin, "quests", true, 0);
        }

        @Override
        public void configure(ConfigurationSection s, ConfigurationSection help) {
            super.configure(s, help);
            String value = s == null ? null : s.getString("commande");
            target = value == null ? "" : value.trim().replaceFirst("^/", "");
        }

        @Override
        public void execute(CommandContext ctx) {
            String label = target.isEmpty() ? "" : target.split(" ")[0];
            if (label.isEmpty() || !plugin.commands().isKnownCommand(label)) {
                ctx.msg("quests.unavailable");
                return;
            }
            ctx.player.performCommand(target);
        }
    }

    /** /f create &lt;nom&gt; */
    public static final class Create extends SubCommand {
        public Create(KoriaFactions plugin) {
            super(plugin, "create", true, 1);
        }

        @Override
        public void execute(CommandContext ctx) {
            if (!requireNoFaction(ctx)) {
                return;
            }
            Settings s = plugin.settings();
            String tag = ctx.arg(0);
            String error = plugin.factions().validateTag(tag, null);
            if (error != null) {
                ctx.msg(error, "faction", tag, "min", s.tagMin, "max", s.tagMax);
                return;
            }
            FactionCreateEvent event = new FactionCreateEvent(ctx.player, tag);
            Bukkit.getPluginManager().callEvent(event);
            if (event.isCancelled()) {
                ctx.msg("general.cancelled");
                return;
            }
            if (!charge(ctx, "create")) {
                return;
            }
            plugin.factions().create(tag, ctx.fp);
            ctx.msg("create.success", "faction", tag);
            if (s.broadcastCreate) {
                plugin.msg().broadcast("create.broadcast", "player", ctx.player.getName(), "faction", tag);
            }
        }
    }

    /** /f disband [faction] */
    public static final class Disband extends SubCommand {
        public Disband(KoriaFactions plugin) {
            super(plugin, "disband", false, 0);
            withAdminPermission();
        }

        @Override
        public void execute(CommandContext ctx) {
            Faction target;
            boolean adminMode = ctx.size() > 0;
            if (adminMode) {
                if (!hasAdmin(ctx)) {
                    ctx.msg("general.no-permission");
                    return;
                }
                target = findFaction(ctx, ctx.arg(0));
                if (target == null) {
                    return;
                }
                if (!target.isNormal()) {
                    ctx.msg("general.system-faction");
                    return;
                }
            } else {
                if (!requireFaction(ctx)) {
                    return;
                }
                target = ctx.faction;
                if (!ctx.isBypass()) {
                    if (ctx.fp.getRole() != Role.LEADER) {
                        ctx.msg("disband.only-leader");
                        return;
                    }
                    if (plugin.factions().getFlag(target, FactionFlag.PERMANENT)) {
                        ctx.msg("disband.permanent");
                        return;
                    }
                    if (plugin.settings().denyDisbandRaidable && plugin.factions().isRaidable(target)) {
                        ctx.msg("disband.raidable");
                        return;
                    }
                }
            }
            if (!confirm(ctx, plugin.settings().confirmDisband, "disband:" + target.getId())) {
                return;
            }
            FactionDisbandEvent.Reason reason = adminMode ? FactionDisbandEvent.Reason.ADMIN : FactionDisbandEvent.Reason.COMMAND;
            if (!plugin.factions().disband(target, ctx.sender.getName(), ctx.player, reason, false)) {
                ctx.msg("general.cancelled");
                return;
            }
            ctx.msg("disband.success", "faction", target.getTag());
        }

        @Override
        public List<String> complete(CommandContext ctx) {
            return ctx.size() == 1 && hasAdmin(ctx) ? factionTags(ctx.arg(0)) : Collections.<String>emptyList();
        }
    }

    /** /f rename &lt;nom&gt; [faction] */
    public static final class Rename extends SubCommand {
        public Rename(KoriaFactions plugin) {
            super(plugin, "rename", false, 1);
            withAdminPermission();
        }

        @Override
        public void execute(CommandContext ctx) {
            Settings s = plugin.settings();
            String tag = ctx.arg(0);
            Faction target;
            boolean own = ctx.size() < 2;
            if (!own) {
                if (!hasAdmin(ctx)) {
                    ctx.msg("general.no-permission");
                    return;
                }
                target = findFaction(ctx, ctx.arg(1));
                if (target == null) {
                    return;
                }
            } else {
                if (!requireFaction(ctx) || !requirePerm(ctx, PermAction.RENAME)) {
                    return;
                }
                target = ctx.faction;
                long remaining = s.renameCooldownMs - (System.currentTimeMillis() - target.getLastRenamed());
                if (remaining > 0 && target.getLastRenamed() > 0 && !ctx.isBypass()) {
                    ctx.msg("rename.cooldown", "time", plugin.msg().duration(remaining));
                    return;
                }
            }
            if (!target.isNormal()) {
                ctx.msg("general.system-faction");
                return;
            }
            String error = plugin.factions().validateTag(tag, target);
            if (error != null) {
                ctx.msg(error, "faction", tag, "min", s.tagMin, "max", s.tagMax);
                return;
            }
            if (own && !charge(ctx, "rename")) {
                return;
            }
            String old = target.getTag();
            plugin.factions().rename(target, tag);
            plugin.factions().log(target, "RENAME", "player", ctx.sender.getName(), "faction", tag);
            plugin.factions().notifyMembers(target, "rename.success", "faction", tag);
            if (ctx.player == null || !target.getId().equals(ctx.fp.getFactionId())) {
                ctx.msg("rename.success", "faction", tag);
            }
            if (s.broadcastRename) {
                plugin.msg().broadcast("rename.broadcast", "old", old, "faction", tag);
            }
        }

        @Override
        public List<String> complete(CommandContext ctx) {
            return ctx.size() == 2 && hasAdmin(ctx) ? factionTags(ctx.arg(1)) : Collections.<String>emptyList();
        }
    }

    /** /f desc &lt;texte&gt; */
    public static final class Desc extends SubCommand {
        public Desc(KoriaFactions plugin) {
            super(plugin, "desc", true, 1);
        }

        @Override
        public void execute(CommandContext ctx) {
            if (!requireFaction(ctx) || !requirePerm(ctx, PermAction.DESCRIPTION)) {
                return;
            }
            String text = ctx.joined(0);
            int max = plugin.settings().descriptionMax;
            if (Text.strip(text).length() > max) {
                ctx.msg("description.too-long", "max", max);
                return;
            }
            if (!charge(ctx, "description")) {
                return;
            }
            ctx.faction.setDescription(text);
            plugin.factions().log(ctx.faction, "DESCRIPTION", "player", ctx.player.getName());
            ctx.msg("description.success", "description", text);
        }
    }

    /** /f motd [texte|clear] */
    public static final class Motd extends SubCommand {
        public Motd(KoriaFactions plugin) {
            super(plugin, "motd", true, 0);
        }

        @Override
        public void execute(CommandContext ctx) {
            Settings s = plugin.settings();
            if (!s.motdEnabled) {
                ctx.msg("general.feature-disabled");
                return;
            }
            if (!requireFaction(ctx)) {
                return;
            }
            Faction f = ctx.faction;
            if (ctx.size() == 0) {
                if (f.getMotd() != null && !f.getMotd().isEmpty()) {
                    ctx.msg("motd.display", "faction", f.getTag(), "motd", f.getMotd());
                } else {
                    usage(ctx);
                }
                return;
            }
            if (!requirePerm(ctx, PermAction.MOTD)) {
                return;
            }
            if (ctx.size() == 1 && ("clear".equalsIgnoreCase(ctx.arg(0)) || "reset".equalsIgnoreCase(ctx.arg(0)))) {
                f.setMotd(null);
                ctx.msg("motd.cleared");
                return;
            }
            String text = ctx.joined(0);
            if (Text.strip(text).length() > s.motdMax) {
                ctx.msg("motd.too-long", "max", s.motdMax);
                return;
            }
            if (!charge(ctx, "motd")) {
                return;
            }
            f.setMotd(text);
            plugin.factions().log(f, "MOTD", "player", ctx.player.getName());
            ctx.msg("motd.success");
        }
    }

    /** /f version */
    public static final class Version extends SubCommand {
        public Version(KoriaFactions plugin) {
            super(plugin, "version", false, 0);
        }

        @Override
        public void execute(CommandContext ctx) {
            ctx.msg("admin.version",
                    "version", plugin.getDescription().getVersion(),
                    "storage", plugin.data().getStorageName(),
                    "factions", plugin.factions().getNormalFactions().size(),
                    "players", plugin.players().getAll().size(),
                    "claims", plugin.claims().size(),
                    "vault", plugin.msg().yesNo(plugin.economy().isAvailable()),
                    "papi", plugin.msg().yesNo(plugin.papi() != null));
        }
    }
}
