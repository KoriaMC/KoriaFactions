package fr.koriamc.factions.command;

import fr.koriamc.factions.KoriaFactions;
import fr.koriamc.factions.command.impl.AdminCommands;
import fr.koriamc.factions.command.impl.ClaimCommands;
import fr.koriamc.factions.command.impl.CoreCommands;
import fr.koriamc.factions.command.impl.HelpCommand;
import fr.koriamc.factions.command.impl.InfoCommands;
import fr.koriamc.factions.command.impl.MemberCommands;
import fr.koriamc.factions.command.impl.SettingsCommands;
import fr.koriamc.factions.command.impl.TeleportCommands;
import fr.koriamc.factions.manager.TeleportManager;
import fr.koriamc.factions.model.Faction;
import fr.koriamc.factions.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.PluginManager;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;

/**
 * Enregistre la commande principale (nom et alias configurables) et distribue
 * les sous-commandes.
 */
public final class CommandManager {

    private final KoriaFactions plugin;
    private final Map<String, SubCommand> byId = new LinkedHashMap<String, SubCommand>();
    private final Map<String, SubCommand> byAlias = new HashMap<String, SubCommand>();
    private MainCommand command;
    private CommandMap commandMap;
    private String label = "f";
    private boolean menuWithoutArgs;
    private int helpPerPage = 8;
    private boolean helpClickable = true;

    public CommandManager(KoriaFactions plugin) {
        this.plugin = plugin;
        register(new HelpCommand(plugin));
        register(new CoreCommands.Menu(plugin));
        register(new SettingsCommands.PermsGui(plugin));
        register(new CoreCommands.Create(plugin));
        register(new CoreCommands.Disband(plugin));
        register(new CoreCommands.Rename(plugin));
        register(new CoreCommands.Desc(plugin));
        register(new CoreCommands.Motd(plugin));
        register(new InfoCommands.Show(plugin));
        register(new InfoCommands.ListFactions(plugin));
        register(new ClaimCommands.Map(plugin));
        register(new ClaimCommands.Claim(plugin));
        register(new ClaimCommands.ClaimAt(plugin));
        register(new ClaimCommands.Unclaim(plugin));
        register(new ClaimCommands.UnclaimAll(plugin));
        register(new ClaimCommands.AutoClaim(plugin, true));
        register(new ClaimCommands.AutoClaim(plugin, false));
        register(new MemberCommands.Invite(plugin));
        register(new MemberCommands.Deinvite(plugin));
        register(new MemberCommands.Invites(plugin));
        register(new MemberCommands.Join(plugin));
        register(new MemberCommands.Leave(plugin));
        register(new MemberCommands.Kick(plugin));
        register(new MemberCommands.Ban(plugin));
        register(new MemberCommands.Unban(plugin));
        register(new MemberCommands.BanList(plugin));
        register(new MemberCommands.Promote(plugin));
        register(new MemberCommands.Demote(plugin));
        register(new MemberCommands.Leader(plugin));
        register(new MemberCommands.Title(plugin));
        register(new TeleportCommands.Home(plugin));
        register(new TeleportCommands.SetHome(plugin));
        register(new TeleportCommands.DelHome(plugin));
        register(new TeleportCommands.WarpTo(plugin));
        register(new TeleportCommands.SetWarp(plugin));
        register(new TeleportCommands.DelWarp(plugin));
        register(new TeleportCommands.Warps(plugin));
        register(new SettingsCommands.Chat(plugin));
        register(new SettingsCommands.Chest(plugin));
        register(new ClaimCommands.SeeChunk(plugin));
        register(new InfoCommands.Top(plugin));
        register(new CoreCommands.Quests(plugin));
        register(new InfoCommands.Power(plugin));
        register(new SettingsCommands.Perms(plugin));
        register(new SettingsCommands.Flags(plugin));
        register(new SettingsCommands.Open(plugin));
        register(new ClaimCommands.Access(plugin));
        register(new SettingsCommands.Upgrades(plugin));
        register(new InfoCommands.Logs(plugin));
        register(new SettingsCommands.Announce(plugin));
        register(new SettingsCommands.Coords(plugin));
        register(new SettingsCommands.Toggle(plugin));
        register(new CoreCommands.Version(plugin));
        register(new AdminCommands.Bypass(plugin));
        register(new AdminCommands.Spy(plugin));
        register(new ClaimCommands.Zone(plugin, Faction.SAFEZONE));
        register(new ClaimCommands.Zone(plugin, Faction.WARZONE));
        register(new AdminCommands.SetPower(plugin, false));
        register(new AdminCommands.SetPower(plugin, true));
        register(new AdminCommands.SetPoints(plugin, false));
        register(new AdminCommands.SetPoints(plugin, true));
        register(new AdminCommands.ForceJoin(plugin));
        register(new AdminCommands.ForceKick(plugin));
        register(new AdminCommands.SetLeader(plugin));
        register(new AdminCommands.SetFlag(plugin));
        register(new AdminCommands.SetUpgrade(plugin));
        register(new AdminCommands.Config(plugin));
        register(new AdminCommands.Save(plugin));
        register(new AdminCommands.Reload(plugin));
    }

    private void register(SubCommand sub) {
        byId.put(sub.getId(), sub);
    }

    // ========================================================= Configuration

    /**
     * @param c        réglages (config.json : "commande" et "commandes")
     * @param messages textes (messages.json : aide des commandes)
     */
    public void load(ConfigurationSection c, ConfigurationSection messages) {
        menuWithoutArgs = "MENU".equalsIgnoreCase(c.getString("commande.sans-argument"));
        helpPerPage = Math.max(1, c.getInt("commande.aide-par-page"));
        helpClickable = c.getBoolean("commande.aide-cliquable");
        byAlias.clear();
        PluginManager pm = Bukkit.getPluginManager();
        for (SubCommand sub : byId.values()) {
            sub.configure(c.getConfigurationSection("commandes." + sub.getId()),
                    messages.getConfigurationSection("help.commands." + sub.getId()));
            if (!sub.isEnabled()) {
                continue;
            }
            byAlias.put(sub.getId(), sub);
            byAlias.put(sub.getName(), sub);
            for (String alias : sub.getAliases()) {
                if (!byAlias.containsKey(alias)) {
                    byAlias.put(alias, sub);
                }
            }
            registerPermission(pm, sub.getPermission(), sub.getPermissionDefault());
            registerPermission(pm, sub.getAdminPermission(), "OP");
        }
    }

    private void registerPermission(PluginManager pm, String node, String def) {
        if (node == null || node.isEmpty()) {
            return;
        }
        PermissionDefault value = PermissionDefault.getByName(def);
        if (value == null) {
            value = PermissionDefault.OP;
        }
        Permission existing = pm.getPermission(node);
        if (existing == null) {
            pm.addPermission(new Permission(node, value));
        } else if (existing.getDefault() != value) {
            existing.setDefault(value);
        }
    }

    /** Enregistre la commande principale dans le CommandMap du serveur. */
    public void registerBukkitCommand() {
        ConfigurationSection main = plugin.config().getConfigurationSection("commande");
        String name = main == null || main.getString("nom") == null ? "f" : main.getString("nom").toLowerCase(Locale.ROOT);
        List<String> aliases = main == null ? Collections.<String>emptyList() : main.getStringList("alias");
        String description = main == null || main.getString("description") == null ? "" : main.getString("description");
        label = name;
        try {
            commandMap = findCommandMap();
            command = new MainCommand(name, description, aliases, this);
            commandMap.register(plugin.getName().toLowerCase(Locale.ROOT), command);
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Impossible d'enregistrer la commande /" + name, e);
        }
    }

    private static CommandMap findCommandMap() throws Exception {
        Object server = Bukkit.getServer();
        try {
            Method getter = server.getClass().getMethod("getCommandMap");
            return (CommandMap) getter.invoke(server);
        } catch (NoSuchMethodException e) {
            Field field = server.getClass().getDeclaredField("commandMap");
            field.setAccessible(true);
            return (CommandMap) field.get(server);
        }
    }

    @SuppressWarnings("unchecked")
    public void unregisterBukkitCommand() {
        if (command == null || commandMap == null) {
            return;
        }
        try {
            Field field = findField(commandMap.getClass(), "knownCommands");
            field.setAccessible(true);
            Map<String, Command> known = (Map<String, Command>) field.get(commandMap);
            Iterator<Map.Entry<String, Command>> it = known.entrySet().iterator();
            while (it.hasNext()) {
                if (it.next().getValue() == command) {
                    it.remove();
                }
            }
            command.unregister(commandMap);
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Impossible de désenregistrer la commande", e);
        }
        command = null;
    }

    private static Field findField(Class<?> type, String name) throws NoSuchFieldException {
        Class<?> current = type;
        while (current != null) {
            try {
                return current.getDeclaredField(name);
            } catch (NoSuchFieldException e) {
                current = current.getSuperclass();
            }
        }
        throw new NoSuchFieldException(name);
    }

    // ============================================================ Accesseurs

    public String getLabel() {
        return label;
    }

    public int getHelpPerPage() {
        return helpPerPage;
    }

    public boolean isHelpClickable() {
        return helpClickable;
    }

    /** Une commande de ce nom (ou alias) est-elle enregistrée sur le serveur, tous plugins confondus ? */
    public boolean isKnownCommand(String label) {
        try {
            CommandMap map = commandMap != null ? commandMap : findCommandMap();
            return map.getCommand(label.toLowerCase(Locale.ROOT)) != null;
        } catch (Exception e) {
            return false;
        }
    }

    public SubCommand get(String id) {
        return byId.get(id);
    }

    /** Sous-commandes visibles dans l'aide pour cet expéditeur. */
    public List<SubCommand> visible(CommandSender sender, boolean admin) {
        List<SubCommand> list = new ArrayList<SubCommand>();
        for (SubCommand sub : byId.values()) {
            if (sub.isEnabled() && !sub.isHidden() && sub.isAdmin() == admin && sub.hasPermission(sender)) {
                list.add(sub);
            }
        }
        return list;
    }

    // ============================================================= Exécution

    void dispatch(CommandSender sender, String usedLabel, String[] args) {
        if (args.length == 0) {
            if (menuWithoutArgs && sender instanceof Player) {
                plugin.menus().open((Player) sender, "principal");
                return;
            }
            args = new String[]{"help"};
        }
        String subLabel = args[0].toLowerCase(Locale.ROOT);
        SubCommand sub = byAlias.get(subLabel);
        String[] subArgs = Arrays.copyOfRange(args, 1, args.length);
        CommandContext ctx = new CommandContext(plugin, sender, usedLabel, subLabel, subArgs);
        if (sub == null) {
            ctx.msg("general.unknown-command");
            return;
        }
        if (!sub.hasPermission(sender)) {
            ctx.msg("general.no-permission");
            return;
        }
        if (sub.isPlayerOnly() && ctx.player == null) {
            ctx.msg("general.players-only");
            return;
        }
        if (subArgs.length < sub.getMinArgs()) {
            ctx.msg("general.usage", "command", subLabel, "usage", sub.getUsage());
            return;
        }
        if (sub.isWorldSensitive() && ctx.player != null && !ctx.isBypass()
                && plugin.settings().isWorldDisabled(ctx.player.getWorld().getName())) {
            ctx.msg("general.disabled-world");
            return;
        }
        if (sub.getCooldown() > 0 && ctx.player != null && !ctx.player.hasPermission(TeleportManager.BYPASS_COOLDOWN)) {
            String key = "command:" + sub.getId();
            long remaining = plugin.cooldowns().getRemaining(ctx.player.getUniqueId(), key);
            if (remaining > 0) {
                ctx.msg("general.cooldown", "time", plugin.msg().duration(remaining));
                return;
            }
            plugin.cooldowns().set(ctx.player.getUniqueId(), key, sub.getCooldown() * 1000L);
        }
        try {
            sub.execute(ctx);
        } catch (RuntimeException e) {
            plugin.getLogger().log(Level.SEVERE, "Erreur pendant /" + usedLabel + " " + Text.join(args, 0, " "), e);
            ctx.msg("general.error");
        }
    }

    List<String> tabComplete(CommandSender sender, String usedLabel, String[] args) {
        if (args.length <= 1) {
            String input = args.length == 0 ? "" : args[0];
            List<String> names = new ArrayList<String>();
            for (SubCommand sub : byId.values()) {
                if (sub.isEnabled() && !sub.isHidden() && sub.hasPermission(sender)) {
                    names.add(sub.getName());
                }
            }
            return Text.complete(input, names);
        }
        SubCommand sub = byAlias.get(args[0].toLowerCase(Locale.ROOT));
        if (sub == null || !sub.hasPermission(sender) || (sub.isPlayerOnly() && !(sender instanceof Player))) {
            return Collections.emptyList();
        }
        CommandContext ctx = new CommandContext(plugin, sender, usedLabel, args[0], Arrays.copyOfRange(args, 1, args.length));
        try {
            List<String> result = sub.complete(ctx);
            return result == null ? Collections.<String>emptyList() : result;
        } catch (RuntimeException e) {
            return Collections.emptyList();
        }
    }
}
