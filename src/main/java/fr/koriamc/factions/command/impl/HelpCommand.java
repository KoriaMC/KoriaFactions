package fr.koriamc.factions.command.impl;

import fr.koriamc.factions.KoriaFactions;
import fr.koriamc.factions.command.CommandContext;
import fr.koriamc.factions.command.SubCommand;
import fr.koriamc.factions.config.Messages;
import fr.koriamc.factions.util.Text;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;

import java.util.Arrays;
import java.util.List;

/**
 * /f help [admin] [page]
 */
public final class HelpCommand extends SubCommand {

    public HelpCommand(KoriaFactions plugin) {
        super(plugin, "help", false, 0);
    }

    @Override
    public void execute(CommandContext ctx) {
        boolean admin = "admin".equalsIgnoreCase(ctx.arg(0));
        String pageArg = ctx.arg(admin ? 1 : 0);
        Integer requested = pageArg == null ? Integer.valueOf(1) : Text.parseInt(pageArg);
        int page = requested == null ? 1 : requested;
        List<SubCommand> commands = plugin.commands().visible(ctx.sender, admin);
        int perPage = plugin.commands().getHelpPerPage();
        int pages = Math.max(1, (commands.size() + perPage - 1) / perPage);
        page = Math.max(1, Math.min(page, pages));
        Messages msg = plugin.msg();

        ctx.msg(admin ? "help.admin-header" : "help.header", "page", page, "pages", pages);
        String hover = msg.get("help.hover");
        for (int i = (page - 1) * perPage; i < commands.size() && i < page * perPage; i++) {
            SubCommand sub = commands.get(i);
            String line = msg.get("help.line", "label", ctx.label, "command", sub.getName(),
                    "usage", sub.getUsage(), "description", sub.getDescription());
            if (ctx.player != null && plugin.commands().isHelpClickable()) {
                TextComponent component = new TextComponent(TextComponent.fromLegacyText(line));
                component.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND,
                        "/" + ctx.label + " " + sub.getName() + " "));
                component.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, TextComponent.fromLegacyText(hover)));
                ctx.player.spigot().sendMessage(component);
            } else {
                ctx.sender.sendMessage(line);
            }
        }
        if (page < pages) {
            String next = admin ? "admin " + (page + 1) : String.valueOf(page + 1);
            ctx.msg("help.footer", "next", next);
        }
        if (!admin && !plugin.commands().visible(ctx.sender, true).isEmpty()) {
            ctx.msg("help.admin-hint");
        }
    }

    @Override
    public List<String> complete(CommandContext ctx) {
        return ctx.size() == 1 ? Text.complete(ctx.arg(0), Arrays.asList("admin", "1", "2")) : super.complete(ctx);
    }
}
