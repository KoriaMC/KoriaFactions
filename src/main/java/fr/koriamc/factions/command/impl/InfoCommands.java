package fr.koriamc.factions.command.impl;

import fr.koriamc.factions.KoriaFactions;
import fr.koriamc.factions.command.CommandContext;
import fr.koriamc.factions.command.SubCommand;
import fr.koriamc.factions.config.Messages;
import fr.koriamc.factions.config.Settings;
import fr.koriamc.factions.manager.FactionManager;
import fr.koriamc.factions.model.FPlayer;
import fr.koriamc.factions.model.Faction;
import fr.koriamc.factions.model.LogEntry;
import fr.koriamc.factions.model.PermAction;
import fr.koriamc.factions.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Commandes d'information : show, list, top, power, logs.
 */
public final class InfoCommands {

    private InfoCommands() {
    }

    private static int page(String input) {
        Integer page = Text.parseInt(input);
        return page == null ? 1 : Math.max(1, page);
    }

    /** /f show [faction|joueur] */
    public static final class Show extends SubCommand {
        private static final String[][] LISTS = {
                {"{online}", "online_count"}, {"{offline}", "offline_count"}
        };

        public Show(KoriaFactions plugin) {
            super(plugin, "show", false, 0);
        }

        @Override
        public void execute(CommandContext ctx) {
            Faction target;
            if (ctx.size() == 0) {
                if (!requireFaction(ctx)) {
                    return;
                }
                target = ctx.faction;
            } else {
                target = findFaction(ctx, ctx.arg(0));
                if (target == null) {
                    return;
                }
            }
            if (!charge(ctx, "show")) {
                return;
            }
            Messages msg = plugin.msg();
            Object[] kv = plugin.placeholders().faction(target, ctx.fp);
            boolean hideEmpty = msg.config().getBoolean("show.hide-empty-lists");
            List<String> lines = msg.raw(target.isSystem() ? "show.system" : "show.lines");
            for (String line : lines) {
                if (hideEmpty && isEmptyList(line, kv)) {
                    continue;
                }
                msg.sendLine(ctx.sender, msg.format(line, kv));
            }
        }

        private static boolean isEmptyList(String line, Object[] kv) {
            for (String[] list : LISTS) {
                if (line.contains(list[0])) {
                    for (int i = 0; i + 1 < kv.length; i += 2) {
                        if (list[1].equals(kv[i]) && "0".equals(String.valueOf(kv[i + 1]))) {
                            return true;
                        }
                    }
                }
            }
            return false;
        }

        @Override
        public List<String> complete(CommandContext ctx) {
            if (ctx.size() != 1) {
                return Collections.emptyList();
            }
            List<String> options = factionTags(ctx.arg(0));
            options.addAll(onlineNames(ctx.arg(0)));
            return options;
        }
    }

    /** /f list [page] */
    public static final class ListFactions extends SubCommand {
        public ListFactions(KoriaFactions plugin) {
            super(plugin, "list", false, 0);
        }

        @Override
        public void execute(CommandContext ctx) {
            if (!charge(ctx, "list")) {
                return;
            }
            final FactionManager fm = plugin.factions();
            Settings s = plugin.settings();
            List<Faction> factions = fm.getNormalFactions();
            if (factions.isEmpty()) {
                ctx.msg("list.empty");
                return;
            }
            final String sort = s.listSort;
            Collections.sort(factions, new Comparator<Faction>() {
                @Override
                public int compare(Faction a, Faction b) {
                    int c;
                    switch (sort) {
                        case "POWER":
                            c = Double.compare(fm.getPower(b), fm.getPower(a));
                            break;
                        case "MEMBERS":
                            c = Integer.compare(b.getSize(), a.getSize());
                            break;
                        case "CLAIMS":
                            c = Integer.compare(plugin.claims().count(b.getId()), plugin.claims().count(a.getId()));
                            break;
                        case "NAME":
                            c = 0;
                            break;
                        default:
                            c = Integer.compare(fm.countOnline(b), fm.countOnline(a));
                            if (c == 0) {
                                c = Integer.compare(b.getSize(), a.getSize());
                            }
                    }
                    return c != 0 ? c : String.CASE_INSENSITIVE_ORDER.compare(a.getTag(), b.getTag());
                }
            });
            int perPage = s.listPerPage;
            int pages = Math.max(1, (factions.size() + perPage - 1) / perPage);
            int page = Math.min(page(ctx.arg(0)), pages);
            ctx.msg("list.header", "page", page, "pages", pages);
            if (page == 1) {
                int factionless = 0;
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (!plugin.players().get(p).hasFaction()) {
                        factionless++;
                    }
                }
                ctx.msg("list.factionless", "count", factionless);
            }
            for (int i = (page - 1) * perPage; i < factions.size() && i < page * perPage; i++) {
                Faction f = factions.get(i);
                ctx.msg("list.line",
                        "relation_color", fm.getColor(ctx.fp, f),
                        "faction", f.getTag(),
                        "online", fm.countOnline(f),
                        "members", f.getSize(),
                        "claims", plugin.claims().count(f.getId()),
                        "power", Text.power(fm.getPower(f)),
                        "max_power", Text.power(fm.getMaxPower(f)));
            }
        }
    }

    /** /f top [catégorie] [page] */
    public static final class Top extends SubCommand {
        public Top(KoriaFactions plugin) {
            super(plugin, "top", false, 0);
        }

        private String categoryList() {
            List<String> names = new ArrayList<String>();
            for (String category : plugin.settings().topCategories) {
                names.add(category.toLowerCase(Locale.ROOT));
            }
            return Text.join(names, ", ");
        }

        @Override
        public void execute(CommandContext ctx) {
            Settings s = plugin.settings();
            String category = s.topDefault;
            int page = 1;
            if (ctx.size() > 0) {
                Integer asNumber = Text.parseInt(ctx.arg(0));
                if (asNumber != null) {
                    page = Math.max(1, asNumber);
                } else {
                    category = ctx.arg(0).toUpperCase(Locale.ROOT);
                    page = page(ctx.arg(1));
                }
            }
            if (!plugin.top().isCategory(category)) {
                ctx.msg("top.invalid-category", "categories", categoryList());
                return;
            }
            List<Faction> list = plugin.top().get(category);
            if (list.isEmpty()) {
                ctx.msg("top.empty");
                return;
            }
            int perPage = s.topPerPage;
            int pages = Math.max(1, (list.size() + perPage - 1) / perPage);
            page = Math.min(page, pages);
            String categoryName = plugin.msg().text("top.categories." + category);
            ctx.msg("top.header", "category", categoryName, "page", page, "pages", pages);
            for (int i = (page - 1) * perPage; i < list.size() && i < page * perPage; i++) {
                Faction f = list.get(i);
                ctx.msg("top.line", "rank", i + 1, "relation_color", plugin.factions().getColor(ctx.fp, f),
                        "faction", f.getTag(), "value", plugin.top().format(f, category));
            }
            ctx.msg("top.footer", "categories", categoryList());
        }

        @Override
        public List<String> complete(CommandContext ctx) {
            if (ctx.size() != 1) {
                return Collections.emptyList();
            }
            List<String> names = new ArrayList<String>();
            for (String category : plugin.settings().topCategories) {
                names.add(category.toLowerCase(Locale.ROOT));
            }
            return Text.complete(ctx.arg(0), names);
        }
    }

    /** /f power [joueur] */
    public static final class Power extends SubCommand {
        public Power(KoriaFactions plugin) {
            super(plugin, "power", false, 0);
        }

        @Override
        public void execute(CommandContext ctx) {
            FPlayer target;
            if (ctx.size() == 0) {
                if (!requirePlayer(ctx)) {
                    return;
                }
                target = ctx.fp;
            } else {
                target = findPlayer(ctx, ctx.arg(0));
                if (target == null) {
                    return;
                }
            }
            String power = Text.power(target.getPower());
            String max = Text.power(plugin.players().getMaxPower(target));
            if (target == ctx.fp) {
                ctx.msg("power.self", "power", power, "max_power", max);
            } else {
                ctx.msg("power.other", "player", target.getName(), "power", power, "max_power", max);
            }
        }

        @Override
        public List<String> complete(CommandContext ctx) {
            return ctx.size() == 1 ? onlineNames(ctx.arg(0)) : Collections.<String>emptyList();
        }
    }

    /** /f logs [page] */
    public static final class Logs extends SubCommand {
        public Logs(KoriaFactions plugin) {
            super(plugin, "logs", true, 0);
        }

        @Override
        public void execute(CommandContext ctx) {
            Settings s = plugin.settings();
            if (!s.logsEnabled) {
                ctx.msg("general.feature-disabled");
                return;
            }
            if (!requireFaction(ctx) || !requirePerm(ctx, PermAction.LOGS)) {
                return;
            }
            List<LogEntry> entries = new ArrayList<LogEntry>(ctx.faction.getLogs());
            if (entries.isEmpty()) {
                ctx.msg("logs.empty");
                return;
            }
            Collections.reverse(entries);
            int perPage = s.logsPerPage;
            int pages = Math.max(1, (entries.size() + perPage - 1) / perPage);
            int page = Math.min(page(ctx.arg(0)), pages);
            SimpleDateFormat format;
            try {
                format = new SimpleDateFormat(s.logsDateFormat, Locale.FRANCE);
            } catch (IllegalArgumentException e) {
                format = new SimpleDateFormat("dd/MM HH:mm", Locale.FRANCE);
            }
            Messages msg = plugin.msg();
            ctx.msg("logs.header", "faction", ctx.faction.getTag(), "page", page, "pages", pages);
            for (int i = (page - 1) * perPage; i < entries.size() && i < page * perPage; i++) {
                LogEntry entry = entries.get(i);
                String key = "logs.entries." + entry.getType();
                String text = msg.has(key) ? msg.text(key, entry.toReplacements()) : entry.getType();
                ctx.msg("logs.line", "date", format.format(new Date(entry.getTime())), "message", text);
            }
        }
    }
}
