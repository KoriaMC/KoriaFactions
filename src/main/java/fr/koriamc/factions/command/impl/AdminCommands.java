package fr.koriamc.factions.command.impl;

import fr.koriamc.factions.KoriaFactions;
import fr.koriamc.factions.api.event.FactionMembershipEvent;
import fr.koriamc.factions.command.CommandContext;
import fr.koriamc.factions.command.SubCommand;
import fr.koriamc.factions.config.JsonFile;
import fr.koriamc.factions.config.Settings;
import fr.koriamc.factions.manager.UpgradeManager;
import fr.koriamc.factions.model.FPlayer;
import fr.koriamc.factions.model.Faction;
import fr.koriamc.factions.model.FactionFlag;
import fr.koriamc.factions.model.Role;
import fr.koriamc.factions.util.Text;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Commandes d'administration.
 */
public final class AdminCommands {

    private AdminCommands() {
    }

    /** /f bypass */
    public static final class Bypass extends SubCommand {
        public Bypass(KoriaFactions plugin) {
            super(plugin, "bypass", true, 0);
            admin();
        }

        @Override
        public void execute(CommandContext ctx) {
            boolean on = !ctx.fp.isBypass();
            ctx.fp.setBypass(on);
            ctx.msg(on ? "admin.bypass-on" : "admin.bypass-off");
        }
    }

    /** /f spy */
    public static final class Spy extends SubCommand {
        public Spy(KoriaFactions plugin) {
            super(plugin, "spy", true, 0);
            admin();
        }

        @Override
        public void execute(CommandContext ctx) {
            boolean on = !ctx.fp.isSpy();
            ctx.fp.setSpy(on);
            ctx.msg(on ? "admin.spy-on" : "admin.spy-off");
        }
    }

    /** /f setpower et /f addpower &lt;joueur&gt; &lt;valeur&gt; */
    public static final class SetPower extends SubCommand {
        private final boolean add;

        public SetPower(KoriaFactions plugin, boolean add) {
            super(plugin, add ? "addpower" : "setpower", false, 2);
            this.add = add;
            admin();
        }

        @Override
        public void execute(CommandContext ctx) {
            FPlayer target = findPlayer(ctx, ctx.arg(0));
            Double value = target == null ? null : parseDouble(ctx, ctx.arg(1));
            if (value == null) {
                return;
            }
            if (add) {
                plugin.players().addPower(target, value);
            } else {
                plugin.players().setPower(target, value);
            }
            Faction f = plugin.factions().getFaction(target);
            if (f != null) {
                plugin.factions().checkRaidable(f);
            }
            ctx.msg("power.admin-set", "player", target.getName(), "power", Text.power(target.getPower()));
        }

        @Override
        public List<String> complete(CommandContext ctx) {
            return ctx.size() == 1 ? onlineNames(ctx.arg(0)) : Collections.<String>emptyList();
        }
    }

    /** /f setpoints et /f addpoints &lt;faction&gt; &lt;valeur&gt; (addpoints accepte une valeur négative) */
    public static final class SetPoints extends SubCommand {
        private final boolean add;

        public SetPoints(KoriaFactions plugin, boolean add) {
            super(plugin, add ? "addpoints" : "setpoints", false, 2);
            this.add = add;
            admin();
        }

        @Override
        public void execute(CommandContext ctx) {
            Faction faction = findFaction(ctx, ctx.arg(0));
            Integer value = faction == null ? null : parseInt(ctx, ctx.arg(1));
            if (value == null) {
                return;
            }
            if (add) {
                plugin.factions().addPoints(faction, value, "admin");
            } else {
                plugin.factions().setPoints(faction, value, "admin");
            }
            ctx.msg("points.admin-set", "faction", faction.getTag(), "points", faction.getPoints());
        }

        @Override
        public List<String> complete(CommandContext ctx) {
            return ctx.size() == 1 ? factionTags(ctx.arg(0)) : Collections.<String>emptyList();
        }
    }

    /** /f forcejoin &lt;joueur&gt; &lt;faction&gt; */
    public static final class ForceJoin extends SubCommand {
        public ForceJoin(KoriaFactions plugin) {
            super(plugin, "forcejoin", false, 2);
            admin();
        }

        @Override
        public void execute(CommandContext ctx) {
            FPlayer target = findPlayer(ctx, ctx.arg(0));
            if (target == null) {
                return;
            }
            Faction faction = findFaction(ctx, ctx.arg(1));
            if (faction == null) {
                return;
            }
            if (!faction.isNormal()) {
                ctx.msg("general.system-faction");
                return;
            }
            Faction current = plugin.factions().getFaction(target);
            if (current == faction) {
                ctx.msg("invite.already-member", "player", target.getName());
                return;
            }
            if (current != null) {
                plugin.factions().removeMember(current, target, FactionMembershipEvent.Action.FORCED_LEAVE);
            }
            Settings s = plugin.settings();
            Role role = faction.getSize() == 0 ? Role.LEADER
                    : plugin.roles().isEnabled(s.joinRole) ? s.joinRole : plugin.roles().lowest();
            plugin.factions().addMember(faction, target, role);
            plugin.factions().log(faction, "JOIN", "player", target.getName());
            plugin.factions().notifyMembers(faction, "join.notify", "player", target.getName());
            ctx.msg("admin.forcejoin", "player", target.getName(), "faction", faction.getTag());
        }

        @Override
        public List<String> complete(CommandContext ctx) {
            if (ctx.size() == 1) {
                return onlineNames(ctx.arg(0));
            }
            return ctx.size() == 2 ? factionTags(ctx.arg(1)) : Collections.<String>emptyList();
        }
    }

    /** /f forcekick &lt;joueur&gt; */
    public static final class ForceKick extends SubCommand {
        public ForceKick(KoriaFactions plugin) {
            super(plugin, "forcekick", false, 1);
            admin();
        }

        @Override
        public void execute(CommandContext ctx) {
            FPlayer target = findPlayer(ctx, ctx.arg(0));
            if (target == null) {
                return;
            }
            Faction faction = plugin.factions().getFaction(target);
            if (faction == null) {
                ctx.msg("general.target-not-member", "player", target.getName());
                return;
            }
            plugin.factions().log(faction, "LEAVE", "player", target.getName());
            plugin.factions().removeMember(faction, target, FactionMembershipEvent.Action.FORCED_LEAVE);
            plugin.factions().notifyMembers(faction, "leave.notify", "player", target.getName());
            ctx.msg("admin.forcekick", "player", target.getName(), "faction", faction.getTag());
        }

        @Override
        public List<String> complete(CommandContext ctx) {
            return ctx.size() == 1 ? onlineNames(ctx.arg(0)) : Collections.<String>emptyList();
        }
    }

    /** /f setleader &lt;joueur&gt; */
    public static final class SetLeader extends SubCommand {
        public SetLeader(KoriaFactions plugin) {
            super(plugin, "setleader", false, 1);
            admin();
        }

        @Override
        public void execute(CommandContext ctx) {
            FPlayer target = findPlayer(ctx, ctx.arg(0));
            if (target == null) {
                return;
            }
            Faction faction = plugin.factions().getFaction(target);
            if (faction == null) {
                ctx.msg("general.target-not-member", "player", target.getName());
                return;
            }
            plugin.factions().setLeader(faction, target);
            plugin.factions().notifyMembers(faction, "leader.notify", "player", target.getName());
            ctx.msg("leader.admin-success", "player", target.getName(), "faction", faction.getTag());
        }

        @Override
        public List<String> complete(CommandContext ctx) {
            return ctx.size() == 1 ? onlineNames(ctx.arg(0)) : Collections.<String>emptyList();
        }
    }

    /** /f setflag &lt;faction&gt; &lt;flag&gt; &lt;on|off&gt; */
    public static final class SetFlag extends SubCommand {
        public SetFlag(KoriaFactions plugin) {
            super(plugin, "setflag", false, 3);
            admin();
        }

        @Override
        public void execute(CommandContext ctx) {
            Faction faction = findFaction(ctx, ctx.arg(0));
            if (faction == null) {
                return;
            }
            if (!faction.isNormal()) {
                ctx.msg("general.system-faction");
                return;
            }
            FactionFlag flag = FactionFlag.parse(ctx.arg(1));
            if (flag == null) {
                ctx.msg("flags.unknown", "flag", ctx.arg(1));
                return;
            }
            boolean value = Text.parseBoolean(ctx.arg(2), !plugin.factions().getFlag(faction, flag));
            Settings.FlagInfo info = plugin.settings().flags.get(flag);
            faction.setFlag(flag, value == info.def ? null : value);
            plugin.factions().checkRaidable(faction);
            plugin.top().invalidate();
            ctx.msg("flags.admin-changed", "flag", info.name, "faction", faction.getTag(),
                    "state", plugin.msg().text(value ? "flags.state-on" : "flags.state-off"));
        }

        @Override
        public List<String> complete(CommandContext ctx) {
            if (ctx.size() == 1) {
                return factionTags(ctx.arg(0));
            }
            if (ctx.size() == 2) {
                List<String> names = new ArrayList<String>();
                for (FactionFlag flag : FactionFlag.values()) {
                    names.add(flag.name().toLowerCase(Locale.ROOT));
                }
                return Text.complete(ctx.arg(1), names);
            }
            return ctx.size() == 3 ? Text.complete(ctx.arg(2), Arrays.asList("on", "off")) : Collections.<String>emptyList();
        }
    }

    /** /f setupgrade &lt;faction&gt; &lt;amélioration&gt; &lt;niveau&gt; */
    public static final class SetUpgrade extends SubCommand {
        public SetUpgrade(KoriaFactions plugin) {
            super(plugin, "setupgrade", false, 3);
            admin();
        }

        @Override
        public void execute(CommandContext ctx) {
            Faction faction = findFaction(ctx, ctx.arg(0));
            if (faction == null) {
                return;
            }
            UpgradeManager.Upgrade upgrade = plugin.upgrades().byArgument(ctx.arg(1));
            if (upgrade == null) {
                ctx.msg("upgrades.unknown", "upgrade", ctx.arg(1));
                return;
            }
            Integer level = parseInt(ctx, ctx.arg(2));
            if (level == null) {
                return;
            }
            level = Math.max(0, Math.min(upgrade.getMaxLevel(), level));
            faction.setUpgradeLevel(upgrade.id, level);
            plugin.upgrades().afterLevelChange(faction, upgrade);
            ctx.msg("upgrades.admin-set", "upgrade", upgrade.name, "faction", faction.getTag(), "level", level);
        }

        @Override
        public List<String> complete(CommandContext ctx) {
            if (ctx.size() == 1) {
                return factionTags(ctx.arg(0));
            }
            if (ctx.size() == 2) {
                List<String> ids = new ArrayList<String>();
                for (UpgradeManager.Upgrade upgrade : plugin.upgrades().getAll()) {
                    ids.add(upgrade.argument);
                }
                return Text.complete(ctx.arg(1), ids);
            }
            return Collections.emptyList();
        }
    }

    /** /f config [fichier] : éditeur de configuration en jeu. */
    public static final class Config extends SubCommand {
        public Config(KoriaFactions plugin) {
            super(plugin, "config", true, 0);
            admin();
        }

        @Override
        public void execute(CommandContext ctx) {
            if (!plugin.menus().openConfig(ctx.player, ctx.arg(0))) {
                ctx.msg("config-editor.unknown-file", "file", ctx.arg(0));
            }
        }

        @Override
        public List<String> complete(CommandContext ctx) {
            if (ctx.size() != 1) {
                return Collections.emptyList();
            }
            List<String> names = new ArrayList<String>();
            for (JsonFile file : plugin.configFiles()) {
                names.add(file.getName().replace(".json", ""));
            }
            return Text.complete(ctx.arg(0), names);
        }
    }

    /** /f save */
    public static final class Save extends SubCommand {
        public Save(KoriaFactions plugin) {
            super(plugin, "save", false, 0);
            admin();
        }

        @Override
        public void execute(CommandContext ctx) {
            plugin.data().saveAsync();
            ctx.msg("general.saved");
        }
    }

    /** /f reload */
    public static final class Reload extends SubCommand {
        public Reload(KoriaFactions plugin) {
            super(plugin, "reload", false, 0);
            admin();
        }

        @Override
        public void execute(CommandContext ctx) {
            long start = System.currentTimeMillis();
            List<String> problems = plugin.reload();
            ctx.msg("general.reloaded", "time", System.currentTimeMillis() - start);
            if (!problems.isEmpty()) {
                ctx.msg("general.reload-problems", "count", problems.size());
                for (String problem : problems) {
                    ctx.msg("general.reload-problem", "problem", problem);
                }
            }
        }
    }
}
