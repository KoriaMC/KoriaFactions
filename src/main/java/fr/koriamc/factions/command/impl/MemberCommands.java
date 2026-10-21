package fr.koriamc.factions.command.impl;

import fr.koriamc.factions.KoriaFactions;
import fr.koriamc.factions.api.event.FactionMembershipEvent;
import fr.koriamc.factions.command.CommandContext;
import fr.koriamc.factions.command.SubCommand;
import fr.koriamc.factions.config.Settings;
import fr.koriamc.factions.manager.FactionManager;
import fr.koriamc.factions.model.FPlayer;
import fr.koriamc.factions.model.Faction;
import fr.koriamc.factions.model.FactionFlag;
import fr.koriamc.factions.model.PermAction;
import fr.koriamc.factions.model.Role;
import fr.koriamc.factions.model.TerritoryType;
import fr.koriamc.factions.util.Sounds;
import fr.koriamc.factions.util.Text;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Gestion des membres : invitations, adhésion, départ, expulsion, bannissement,
 * grades, commandement et titres.
 */
public final class MemberCommands {

    private MemberCommands() {
    }

    /** /f invite &lt;joueur&gt; */
    public static final class Invite extends SubCommand {
        public Invite(KoriaFactions plugin) {
            super(plugin, "invite", true, 1);
        }

        @Override
        public void execute(CommandContext ctx) {
            if (!requireFaction(ctx) || !requirePerm(ctx, PermAction.INVITE)) {
                return;
            }
            Settings s = plugin.settings();
            Faction f = ctx.faction;
            FPlayer target = findPlayer(ctx, ctx.arg(0));
            if (target == null) {
                return;
            }
            UUID uuid = target.getUniqueId();
            if (target == ctx.fp) {
                ctx.msg("general.target-self");
                return;
            }
            if (f.getId().equals(target.getFactionId())) {
                ctx.msg("invite.already-member", "player", target.getName());
                return;
            }
            if (f.isBanned(uuid)) {
                ctx.msg("invite.banned", "player", target.getName());
                return;
            }
            if (f.isInvited(uuid)) {
                ctx.msg("invite.already-invited", "player", target.getName());
                return;
            }
            f.purgeInvites();
            if (s.maxInvites > 0 && f.getInvites().size() >= s.maxInvites) {
                ctx.msg("invite.max-pending", "max", s.maxInvites);
                return;
            }
            if (!charge(ctx, "invite")) {
                return;
            }
            f.invite(uuid, s.inviteExpireMs > 0 ? System.currentTimeMillis() + s.inviteExpireMs : 0L);
            plugin.factions().log(f, "INVITE", "actor", ctx.player.getName(), "player", target.getName());
            ctx.msg("invite.sent", "player", target.getName());
            notifyOthers(ctx, f, "invite.notify", "inviter", ctx.player.getName(), "player", target.getName());

            Player online = target.getPlayer();
            if (online == null) {
                return;
            }
            Object[] kv = {"inviter", ctx.player.getName(), "faction", f.getTag(), "label", plugin.commands().getLabel()};
            if (s.inviteClickable) {
                String text = plugin.msg().get("invite.received", kv);
                TextComponent button = new TextComponent(TextComponent.fromLegacyText(plugin.msg().get("invite.received-button", kv)));
                button.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                        "/" + plugin.commands().getLabel() + " join " + f.getTag()));
                button.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                        TextComponent.fromLegacyText(plugin.msg().get("invite.received-hover", kv))));
                List<BaseComponent> parts = new ArrayList<BaseComponent>();
                Collections.addAll(parts, TextComponent.fromLegacyText(text));
                parts.add(button);
                online.spigot().sendMessage(parts.toArray(new BaseComponent[parts.size()]));
            } else {
                plugin.msg().send(online, "invite.received", kv);
            }
            Sounds.play(online, s.sound("invitation-recue"));
        }

        @Override
        public List<String> complete(CommandContext ctx) {
            return ctx.size() == 1 ? onlineNames(ctx.arg(0)) : Collections.<String>emptyList();
        }
    }

    /** /f deinvite &lt;joueur&gt; */
    public static final class Deinvite extends SubCommand {
        public Deinvite(KoriaFactions plugin) {
            super(plugin, "deinvite", true, 1);
        }

        @Override
        public void execute(CommandContext ctx) {
            if (!requireFaction(ctx) || !requirePerm(ctx, PermAction.INVITE)) {
                return;
            }
            FPlayer target = findPlayer(ctx, ctx.arg(0));
            if (target == null) {
                return;
            }
            if (!ctx.faction.deinvite(target.getUniqueId())) {
                ctx.msg("invite.not-invited", "player", target.getName());
                return;
            }
            ctx.msg("invite.revoked", "player", target.getName());
            Player online = target.getPlayer();
            if (online != null) {
                plugin.msg().send(online, "invite.revoked-target", "faction", ctx.faction.getTag());
            }
        }

        @Override
        public List<String> complete(CommandContext ctx) {
            if (ctx.size() != 1 || ctx.faction == null) {
                return Collections.emptyList();
            }
            List<String> names = new ArrayList<String>();
            for (String uuid : ctx.faction.getInvites().keySet()) {
                FPlayer fp = plugin.players().get(UUID.fromString(uuid));
                if (fp != null) {
                    names.add(fp.getName());
                }
            }
            return Text.complete(ctx.arg(0), names);
        }
    }

    /** /f invites */
    public static final class Invites extends SubCommand {
        public Invites(KoriaFactions plugin) {
            super(plugin, "invites", true, 0);
        }

        private String expiry(long time) {
            if (time <= 0) {
                return plugin.msg().text("values.unlimited");
            }
            return plugin.msg().duration(time - System.currentTimeMillis());
        }

        @Override
        public void execute(CommandContext ctx) {
            List<String[]> lines = new ArrayList<String[]>();
            if (ctx.faction != null) {
                ctx.faction.purgeInvites();
                for (Map.Entry<String, Long> e : ctx.faction.getInvites().entrySet()) {
                    FPlayer fp = plugin.players().get(UUID.fromString(e.getKey()));
                    lines.add(new String[]{fp == null ? e.getKey() : fp.getName(), expiry(e.getValue())});
                }
            } else {
                for (Faction f : plugin.factions().getNormalFactions()) {
                    if (f.isInvited(ctx.fp.getUniqueId())) {
                        lines.add(new String[]{f.getTag(), expiry(f.getInvites().get(ctx.fp.getUniqueId().toString()))});
                    }
                }
            }
            if (lines.isEmpty()) {
                ctx.msg("invite.list-empty");
                return;
            }
            ctx.msg(ctx.faction != null ? "invite.list-header-faction" : "invite.list-header-player");
            for (String[] line : lines) {
                ctx.msg("invite.list-line", "name", line[0], "time", line[1]);
            }
        }
    }

    /** /f join &lt;faction&gt; */
    public static final class Join extends SubCommand {
        public Join(KoriaFactions plugin) {
            super(plugin, "join", true, 1);
        }

        @Override
        public void execute(CommandContext ctx) {
            if (!requireNoFaction(ctx)) {
                return;
            }
            Faction target = findFaction(ctx, ctx.arg(0));
            if (target == null) {
                return;
            }
            if (!target.isNormal()) {
                ctx.msg("general.system-faction");
                return;
            }
            Settings s = plugin.settings();
            FactionManager fm = plugin.factions();
            UUID uuid = ctx.fp.getUniqueId();
            if (!ctx.isBypass()) {
                if (target.isBanned(uuid)) {
                    ctx.msg("join.banned", "faction", target.getTag());
                    return;
                }
                if (!fm.getFlag(target, FactionFlag.OPEN) && !target.isInvited(uuid)) {
                    ctx.msg("join.not-invited", "faction", target.getTag());
                    return;
                }
                int max = fm.getMaxMembers(target);
                if (target.getSize() >= max) {
                    ctx.msg("join.full", "faction", target.getTag(), "max", max);
                    return;
                }
                long remaining = s.joinCooldownMs - (System.currentTimeMillis() - ctx.fp.getLastFactionLeave());
                if (remaining > 0 && ctx.fp.getLastFactionLeave() > 0) {
                    ctx.msg("join.cooldown", "time", plugin.msg().duration(remaining));
                    return;
                }
            }
            FactionMembershipEvent event = new FactionMembershipEvent(target, ctx.fp, FactionMembershipEvent.Action.JOIN);
            Bukkit.getPluginManager().callEvent(event);
            if (event.isCancelled()) {
                ctx.msg("general.cancelled");
                return;
            }
            if (!charge(ctx, "join")) {
                return;
            }
            Role role = plugin.roles().isEnabled(s.joinRole) ? s.joinRole : plugin.roles().lowest();
            fm.addMember(target, ctx.fp, role);
            fm.log(target, "JOIN", "player", ctx.player.getName());
            ctx.msg("join.success", "faction", target.getTag());
            notifyOthers(ctx, target, "join.notify", "player", ctx.player.getName());
        }

        @Override
        public List<String> complete(CommandContext ctx) {
            if (ctx.size() != 1) {
                return Collections.emptyList();
            }
            List<String> tags = new ArrayList<String>();
            for (Faction f : plugin.factions().getNormalFactions()) {
                if (f.isInvited(ctx.fp.getUniqueId()) || plugin.factions().getFlag(f, FactionFlag.OPEN)) {
                    tags.add(f.getTag());
                }
            }
            return Text.complete(ctx.arg(0), tags);
        }
    }

    /** /f leave */
    public static final class Leave extends SubCommand {
        public Leave(KoriaFactions plugin) {
            super(plugin, "leave", true, 0);
        }

        @Override
        public void execute(CommandContext ctx) {
            if (!requireFaction(ctx)) {
                return;
            }
            Settings s = plugin.settings();
            FactionManager fm = plugin.factions();
            Faction f = ctx.faction;
            if (!ctx.isBypass()) {
                if (ctx.fp.getRole() == Role.LEADER && !s.leaderCanLeave && f.getSize() > 1) {
                    ctx.msg("leave.leader");
                    return;
                }
                if (s.denyLeaveRaidable && fm.isRaidable(f)) {
                    ctx.msg("leave.raidable");
                    return;
                }
                if (s.denyLeaveEnemyTerritory && fm.getTerritoryType(ctx.fp,
                        plugin.claims().getOwner(ctx.player.getLocation())) == TerritoryType.ENEMY) {
                    ctx.msg("leave.enemy-territory");
                    return;
                }
            }
            if (!confirm(ctx, s.confirmLeave, "leave:" + f.getId())) {
                return;
            }
            FactionMembershipEvent event = new FactionMembershipEvent(f, ctx.fp, FactionMembershipEvent.Action.LEAVE);
            Bukkit.getPluginManager().callEvent(event);
            if (event.isCancelled()) {
                ctx.msg("general.cancelled");
                return;
            }
            if (!charge(ctx, "leave")) {
                return;
            }
            fm.log(f, "LEAVE", "player", ctx.player.getName());
            fm.removeMember(f, ctx.fp, FactionMembershipEvent.Action.LEAVE);
            ctx.msg("leave.success", "faction", f.getTag());
            fm.notifyMembers(f, "leave.notify", "player", ctx.player.getName());
        }
    }

    /** /f kick &lt;joueur&gt; */
    public static final class Kick extends SubCommand {
        public Kick(KoriaFactions plugin) {
            super(plugin, "kick", true, 1);
        }

        @Override
        public void execute(CommandContext ctx) {
            if (!requireFaction(ctx) || !requirePerm(ctx, PermAction.KICK)) {
                return;
            }
            FPlayer target = findMember(ctx, ctx.arg(0));
            if (target == null) {
                return;
            }
            if (target == ctx.fp) {
                ctx.msg("general.target-self");
                return;
            }
            if (target.getRole() == Role.LEADER) {
                ctx.msg("general.role-too-low");
                return;
            }
            if (!canActOn(ctx, target)) {
                return;
            }
            Faction f = ctx.faction;
            if (!confirm(ctx, plugin.settings().confirmKick, "kick:" + target.getUniqueId())) {
                return;
            }
            FactionMembershipEvent event = new FactionMembershipEvent(f, target, FactionMembershipEvent.Action.KICK);
            Bukkit.getPluginManager().callEvent(event);
            if (event.isCancelled()) {
                ctx.msg("general.cancelled");
                return;
            }
            plugin.factions().removeMember(f, target, FactionMembershipEvent.Action.KICK);
            plugin.factions().log(f, "KICK", "player", target.getName(), "actor", ctx.player.getName());
            ctx.msg("kick.success", "player", target.getName());
            notifyOthers(ctx, f, "kick.notify", "player", target.getName(), "kicker", ctx.player.getName());
            Player online = target.getPlayer();
            if (online != null) {
                plugin.msg().send(online, "kick.target", "faction", f.getTag(), "kicker", ctx.player.getName());
            }
        }

        @Override
        public List<String> complete(CommandContext ctx) {
            return ctx.size() == 1 ? memberNames(ctx, ctx.arg(0)) : Collections.<String>emptyList();
        }
    }

    /** /f ban &lt;joueur&gt; */
    public static final class Ban extends SubCommand {
        public Ban(KoriaFactions plugin) {
            super(plugin, "ban", true, 1);
        }

        @Override
        public void execute(CommandContext ctx) {
            if (!requireFaction(ctx) || !requirePerm(ctx, PermAction.BAN)) {
                return;
            }
            Faction f = ctx.faction;
            FPlayer target = findPlayer(ctx, ctx.arg(0));
            if (target == null) {
                return;
            }
            if (target == ctx.fp) {
                ctx.msg("general.target-self");
                return;
            }
            if (f.isBanned(target.getUniqueId())) {
                ctx.msg("ban.already", "player", target.getName());
                return;
            }
            Player online = target.getPlayer();
            if (f.getId().equals(target.getFactionId())) {
                if (target.getRole() == Role.LEADER) {
                    ctx.msg("general.role-too-low");
                    return;
                }
                if (!canActOn(ctx, target)) {
                    return;
                }
                plugin.factions().removeMember(f, target, FactionMembershipEvent.Action.KICK);
                plugin.factions().log(f, "KICK", "player", target.getName(), "actor", ctx.player.getName());
                if (online != null) {
                    plugin.msg().send(online, "kick.target", "faction", f.getTag(), "kicker", ctx.player.getName());
                }
            }
            f.getBans().add(target.getUniqueId().toString());
            f.deinvite(target.getUniqueId());
            plugin.factions().log(f, "BAN", "player", target.getName(), "actor", ctx.player.getName());
            ctx.msg("ban.success", "player", target.getName());
            if (online != null) {
                plugin.msg().send(online, "ban.target", "faction", f.getTag());
            }
        }

        @Override
        public List<String> complete(CommandContext ctx) {
            return ctx.size() == 1 ? onlineNames(ctx.arg(0)) : Collections.<String>emptyList();
        }
    }

    /** /f unban &lt;joueur&gt; */
    public static final class Unban extends SubCommand {
        public Unban(KoriaFactions plugin) {
            super(plugin, "unban", true, 1);
        }

        @Override
        public void execute(CommandContext ctx) {
            if (!requireFaction(ctx) || !requirePerm(ctx, PermAction.BAN)) {
                return;
            }
            FPlayer target = findPlayer(ctx, ctx.arg(0));
            if (target == null) {
                return;
            }
            if (!ctx.faction.getBans().remove(target.getUniqueId().toString())) {
                ctx.msg("ban.not-banned", "player", target.getName());
                return;
            }
            plugin.factions().log(ctx.faction, "UNBAN", "player", target.getName(), "actor", ctx.player.getName());
            ctx.msg("ban.unban-success", "player", target.getName());
        }

        @Override
        public List<String> complete(CommandContext ctx) {
            if (ctx.size() != 1 || ctx.faction == null) {
                return Collections.emptyList();
            }
            List<String> names = new ArrayList<String>();
            for (String uuid : ctx.faction.getBans()) {
                FPlayer fp = plugin.players().get(UUID.fromString(uuid));
                if (fp != null) {
                    names.add(fp.getName());
                }
            }
            return Text.complete(ctx.arg(0), names);
        }
    }

    /** /f banlist */
    public static final class BanList extends SubCommand {
        public BanList(KoriaFactions plugin) {
            super(plugin, "banlist", true, 0);
        }

        @Override
        public void execute(CommandContext ctx) {
            if (!requireFaction(ctx)) {
                return;
            }
            if (ctx.faction.getBans().isEmpty()) {
                ctx.msg("ban.list-empty");
                return;
            }
            ctx.msg("ban.list-header");
            for (String uuid : ctx.faction.getBans()) {
                FPlayer fp = plugin.players().get(UUID.fromString(uuid));
                ctx.msg("ban.list-line", "player", fp == null ? uuid : fp.getName());
            }
        }
    }

    /** /f promote et /f demote */
    private abstract static class RankChange extends SubCommand {
        private final boolean promote;

        RankChange(KoriaFactions plugin, boolean promote) {
            super(plugin, promote ? "promote" : "demote", true, 1);
            this.promote = promote;
        }

        @Override
        public void execute(CommandContext ctx) {
            if (!requireFaction(ctx) || !requirePerm(ctx, promote ? PermAction.PROMOTE : PermAction.DEMOTE)) {
                return;
            }
            FPlayer target = findMember(ctx, ctx.arg(0));
            if (target == null) {
                return;
            }
            if (target == ctx.fp) {
                ctx.msg("general.target-self");
                return;
            }
            if (target.getRole() == Role.LEADER || !canActOn(ctx, target)) {
                if (target.getRole() == Role.LEADER) {
                    ctx.msg("general.role-too-low");
                }
                return;
            }
            Settings s = plugin.settings();
            Role newRole;
            if (promote) {
                newRole = plugin.roles().next(target.getRole());
                if (newRole == null || newRole == Role.LEADER) {
                    ctx.msg("promote.max", "player", target.getName());
                    return;
                }
                Role own = ctx.fp.getRole();
                if (!ctx.isBypass() && own != Role.LEADER && !own.isHigherThan(newRole)) {
                    ctx.msg("promote.above-self");
                    return;
                }
                int limit = newRole == Role.COLEADER ? s.maxColeaders : newRole == Role.OFFICER ? s.maxOfficers : 0;
                if (limit > 0 && plugin.factions().countRole(ctx.faction, newRole) >= limit) {
                    ctx.msg("promote.limit", "role", plugin.roles().name(newRole), "max", limit);
                    return;
                }
            } else {
                newRole = plugin.roles().previous(target.getRole());
                if (newRole == null) {
                    ctx.msg("demote.min", "player", target.getName());
                    return;
                }
            }
            target.setRole(newRole);
            String roleName = plugin.roles().name(newRole);
            String prefix = promote ? "promote." : "demote.";
            plugin.factions().log(ctx.faction, promote ? "PROMOTE" : "DEMOTE",
                    "actor", ctx.player.getName(), "player", target.getName(), "role", roleName);
            ctx.msg(prefix + "success", "player", target.getName(), "role", roleName);
            Player online = target.getPlayer();
            if (online != null) {
                plugin.msg().send(online, prefix + "target", "role", roleName);
            }
            for (Player member : plugin.factions().getOnlinePlayers(ctx.faction)) {
                if (member != ctx.player && member != online) {
                    plugin.msg().send(member, prefix + "notify", "player", target.getName(), "role", roleName);
                }
            }
        }

        @Override
        public List<String> complete(CommandContext ctx) {
            return ctx.size() == 1 ? memberNames(ctx, ctx.arg(0)) : Collections.<String>emptyList();
        }
    }

    /** /f promote &lt;joueur&gt; */
    public static final class Promote extends RankChange {
        public Promote(KoriaFactions plugin) {
            super(plugin, true);
        }
    }

    /** /f demote &lt;joueur&gt; */
    public static final class Demote extends RankChange {
        public Demote(KoriaFactions plugin) {
            super(plugin, false);
        }
    }

    /** /f leader &lt;joueur&gt; */
    public static final class Leader extends SubCommand {
        public Leader(KoriaFactions plugin) {
            super(plugin, "leader", true, 1);
        }

        @Override
        public void execute(CommandContext ctx) {
            if (!requireFaction(ctx)) {
                return;
            }
            if (ctx.fp.getRole() != Role.LEADER && !ctx.isBypass()) {
                ctx.msg("leader.only-leader");
                return;
            }
            FPlayer target = findMember(ctx, ctx.arg(0));
            if (target == null) {
                return;
            }
            if (target == ctx.fp) {
                ctx.msg("general.target-self");
                return;
            }
            plugin.factions().setLeader(ctx.faction, target);
            ctx.msg("leader.success", "player", target.getName());
            Player online = target.getPlayer();
            if (online != null) {
                plugin.msg().send(online, "leader.target", "faction", ctx.faction.getTag());
            }
            for (Player member : plugin.factions().getOnlinePlayers(ctx.faction)) {
                if (member != ctx.player && member != online) {
                    plugin.msg().send(member, "leader.notify", "player", target.getName());
                }
            }
        }

        @Override
        public List<String> complete(CommandContext ctx) {
            return ctx.size() == 1 ? memberNames(ctx, ctx.arg(0)) : Collections.<String>emptyList();
        }
    }

    /** /f title &lt;joueur&gt; [titre] */
    public static final class Title extends SubCommand {
        public Title(KoriaFactions plugin) {
            super(plugin, "title", true, 1);
        }

        @Override
        public void execute(CommandContext ctx) {
            if (!requireFaction(ctx) || !requirePerm(ctx, PermAction.TITLE)) {
                return;
            }
            FPlayer target = findMember(ctx, ctx.arg(0));
            if (target == null) {
                return;
            }
            if (target != ctx.fp && !canActOn(ctx, target)) {
                return;
            }
            if (ctx.size() == 1) {
                target.setTitle(null);
                ctx.msg("title.cleared", "player", target.getName());
                return;
            }
            Settings s = plugin.settings();
            String title = ctx.joined(1);
            if (!s.titleColors) {
                title = Text.strip(title);
            }
            if (Text.strip(title).length() > s.titleMax) {
                ctx.msg("title.too-long", "max", s.titleMax);
                return;
            }
            if (!charge(ctx, "title")) {
                return;
            }
            target.setTitle(title);
            ctx.msg("title.success", "player", target.getName(), "title", title);
        }

        @Override
        public List<String> complete(CommandContext ctx) {
            return ctx.size() == 1 ? memberNames(ctx, ctx.arg(0)) : Collections.<String>emptyList();
        }
    }
}
