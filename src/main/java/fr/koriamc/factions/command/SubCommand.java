package fr.koriamc.factions.command;

import fr.koriamc.factions.KoriaFactions;
import fr.koriamc.factions.model.FPlayer;
import fr.koriamc.factions.model.Faction;
import fr.koriamc.factions.model.PermAction;
import fr.koriamc.factions.model.Role;
import fr.koriamc.factions.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Sous-commande de /f. Nom, alias, permission, délai, usage et description
 * proviennent de config.json (section "commandes") et de messages.json (aide).
 */
public abstract class SubCommand {

    protected final KoriaFactions plugin;
    private final String id;
    private final boolean playerOnly;
    private final int minArgs;
    private boolean worldSensitive;

    private boolean enabled = true;
    private String name;
    private List<String> aliases = Collections.emptyList();
    private String permission = "";
    private String permissionDefault = "TRUE";
    private String adminPermission = "";
    private int cooldown;
    private String usage = "";
    private String description = "";
    private boolean hidden;
    private boolean admin;

    protected SubCommand(KoriaFactions plugin, String id, boolean playerOnly, int minArgs) {
        this.plugin = plugin;
        this.id = id;
        this.name = id;
        this.playerOnly = playerOnly;
        this.minArgs = minArgs;
    }

    /** Commande liée au monde du joueur (refusée dans les mondes désactivés). */
    protected SubCommand worldSensitive() {
        this.worldSensitive = true;
        return this;
    }

    /** Commande d'administration : permission accordée aux OP par défaut, aide séparée. */
    protected SubCommand admin() {
        this.admin = true;
        return this;
    }

    /** Commande absente de l'aide et de l'autocomplétion. */
    protected SubCommand hidden() {
        this.hidden = true;
        return this;
    }

    /** La commande accepte une cible d'une autre faction avec koriafactions.admin.&lt;id&gt;. */
    protected SubCommand withAdminPermission() {
        this.adminPermission = "koriafactions.admin." + id;
        return this;
    }

    /**
     * @param s    section "commandes.&lt;id&gt;" de config.json (peut être nulle)
     * @param help section "help.commands.&lt;id&gt;" de messages.json (peut être nulle)
     */
    public void configure(ConfigurationSection s, ConfigurationSection help) {
        usage = help == null ? "" : orDefault(help.getString("usage"), "");
        description = help == null ? "" : orDefault(help.getString("description"), "");
        permissionDefault = admin ? "OP" : "TRUE";
        if (s == null) {
            return;
        }
        enabled = !s.contains("actif") || s.getBoolean("actif");
        aliases = new ArrayList<String>();
        for (String alias : s.getStringList("alias")) {
            aliases.add(alias.toLowerCase(Locale.ROOT));
        }
        permission = orDefault(s.getString("permission"), "");
        cooldown = Math.max(0, s.getInt("delai"));
    }

    private static String orDefault(String value, String def) {
        return value == null ? def : value;
    }

    public abstract void execute(CommandContext ctx);

    public List<String> complete(CommandContext ctx) {
        return Collections.emptyList();
    }

    // ============================================================== Accesseurs

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public List<String> getAliases() {
        return aliases;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isPlayerOnly() {
        return playerOnly;
    }

    public boolean isWorldSensitive() {
        return worldSensitive;
    }

    public int getMinArgs() {
        return minArgs;
    }

    public String getPermission() {
        return permission;
    }

    public String getPermissionDefault() {
        return permissionDefault;
    }

    public String getAdminPermission() {
        return adminPermission;
    }

    public int getCooldown() {
        return cooldown;
    }

    public String getUsage() {
        return usage;
    }

    public String getDescription() {
        return description;
    }

    public boolean isHidden() {
        return hidden;
    }

    public boolean isAdmin() {
        return admin;
    }

    public boolean hasPermission(CommandSender sender) {
        return permission == null || permission.isEmpty() || sender.hasPermission(permission);
    }

    // ================================================================= Aides

    protected boolean hasAdmin(CommandContext ctx) {
        return adminPermission != null && !adminPermission.isEmpty() && ctx.sender.hasPermission(adminPermission);
    }

    protected void usage(CommandContext ctx) {
        ctx.msg("general.usage", "command", ctx.subLabel, "usage", usage);
    }

    protected boolean requirePlayer(CommandContext ctx) {
        if (ctx.player == null) {
            ctx.msg("general.players-only");
            return false;
        }
        return true;
    }

    protected boolean requireFaction(CommandContext ctx) {
        if (!requirePlayer(ctx)) {
            return false;
        }
        if (ctx.faction == null) {
            ctx.msg("general.must-have-faction");
            return false;
        }
        return true;
    }

    protected boolean requireNoFaction(CommandContext ctx) {
        if (ctx.faction != null) {
            ctx.msg("general.must-not-have-faction");
            return false;
        }
        return true;
    }

    /** Vérifie une permission de faction du joueur dans sa propre faction. */
    protected boolean requirePerm(CommandContext ctx, PermAction action) {
        if (plugin.factions().hasPermission(ctx.fp, ctx.faction, action)) {
            return true;
        }
        ctx.msg("general.no-faction-permission", "action", plugin.roles().actionName(action));
        return false;
    }

    protected FPlayer findPlayer(CommandContext ctx, String name) {
        FPlayer target = plugin.players().getByName(name);
        if (target == null) {
            ctx.msg("general.player-not-found", "player", name);
        }
        return target;
    }

    /** Joueur appartenant à la faction de l'exécutant (message d'erreur sinon). */
    protected FPlayer findMember(CommandContext ctx, String name) {
        FPlayer target = findPlayer(ctx, name);
        if (target == null) {
            return null;
        }
        if (!ctx.faction.getId().equals(target.getFactionId())) {
            ctx.msg("general.target-not-member", "player", target.getName());
            return null;
        }
        return target;
    }

    /** Faction par nom, ou faction d'un joueur portant ce pseudo. */
    protected Faction findFaction(CommandContext ctx, String input) {
        Faction f = plugin.factions().getByTag(input);
        if (f == null) {
            FPlayer owner = plugin.players().getByName(input);
            if (owner != null) {
                f = plugin.factions().getFaction(owner);
            }
        }
        if (f == null) {
            ctx.msg("general.faction-not-found", "faction", input);
        }
        return f;
    }

    /** L'exécutant a-t-il un rang strictement supérieur à la cible ? */
    protected boolean canActOn(CommandContext ctx, FPlayer target) {
        if (ctx.isBypass()) {
            return true;
        }
        Role own = ctx.fp.getRole();
        if (own == Role.LEADER || own.isHigherThan(target.getRole())) {
            return true;
        }
        ctx.msg("general.role-too-low");
        return false;
    }

    protected boolean confirm(CommandContext ctx, boolean required, String key) {
        return !required || ctx.player == null || plugin.confirmations().confirm(ctx.player, key);
    }

    protected boolean charge(CommandContext ctx, String costKey) {
        return ctx.player == null || plugin.economy().charge(ctx.player, costKey);
    }

    protected Integer parseInt(CommandContext ctx, String input) {
        Integer value = Text.parseInt(input);
        if (value == null) {
            ctx.msg("general.not-a-number", "input", input);
        }
        return value;
    }

    protected Double parseDouble(CommandContext ctx, String input) {
        Double value = Text.parseDouble(input);
        if (value == null) {
            ctx.msg("general.not-a-number", "input", input);
        }
        return value;
    }

    // ========================================================= Autocomplétion

    protected List<String> onlineNames(String input) {
        List<String> names = new ArrayList<String>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            names.add(p.getName());
        }
        return Text.complete(input, names);
    }

    protected List<String> factionTags(String input) {
        return Text.complete(input, plugin.factions().getNormalTags());
    }

    protected List<String> memberNames(CommandContext ctx, String input) {
        List<String> names = new ArrayList<String>();
        if (ctx.faction != null) {
            for (FPlayer member : plugin.factions().getMembers(ctx.faction)) {
                names.add(member.getName());
            }
        }
        return Text.complete(input, names);
    }

    /** Notifie les membres connectés sauf l'exécutant. */
    protected void notifyOthers(CommandContext ctx, Faction f, String key, Object... kv) {
        for (Player member : plugin.factions().getOnlinePlayers(f)) {
            if (member != ctx.player) {
                plugin.msg().send(member, key, kv);
            }
        }
    }
}
