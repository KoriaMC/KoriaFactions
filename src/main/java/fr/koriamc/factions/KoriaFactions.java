package fr.koriamc.factions;

import fr.koriamc.factions.command.CommandManager;
import fr.koriamc.factions.config.ConfigDocs;
import fr.koriamc.factions.config.JsonFile;
import fr.koriamc.factions.config.EconomySettings;
import fr.koriamc.factions.config.Messages;
import fr.koriamc.factions.config.ProtectionSettings;
import fr.koriamc.factions.config.RoleSettings;
import fr.koriamc.factions.config.Settings;
import fr.koriamc.factions.gui.Menu;
import fr.koriamc.factions.gui.MenuManager;
import fr.koriamc.factions.hook.PapiHook;
import fr.koriamc.factions.listener.ChatListener;
import fr.koriamc.factions.listener.CombatListener;
import fr.koriamc.factions.listener.PlayerListener;
import fr.koriamc.factions.listener.ProtectionListener;
import fr.koriamc.factions.listener.WorldListener;
import fr.koriamc.factions.manager.ChestManager;
import fr.koriamc.factions.manager.ClaimManager;
import fr.koriamc.factions.manager.ConfirmationManager;
import fr.koriamc.factions.manager.CooldownManager;
import fr.koriamc.factions.manager.EconomyManager;
import fr.koriamc.factions.manager.FactionManager;
import fr.koriamc.factions.manager.InactivityManager;
import fr.koriamc.factions.manager.MapManager;
import fr.koriamc.factions.manager.PlaceholderService;
import fr.koriamc.factions.manager.PlayerManager;
import fr.koriamc.factions.manager.PowerManager;
import fr.koriamc.factions.manager.ProtectionService;
import fr.koriamc.factions.manager.SeeChunkManager;
import fr.koriamc.factions.manager.TeleportManager;
import fr.koriamc.factions.manager.TerritoryManager;
import fr.koriamc.factions.manager.TopManager;
import fr.koriamc.factions.manager.UpgradeManager;
import fr.koriamc.factions.model.Faction;
import fr.koriamc.factions.storage.DataManager;
import org.bukkit.Bukkit;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;

/**
 * KoriaFactions — plugin de factions pour KoriaMC (Spigot 1.8.8).
 */
public final class KoriaFactions extends JavaPlugin {

    private static KoriaFactions instance;

    // Fichiers
    private JsonFile configFile;
    private JsonFile factionsFile;
    private JsonFile territoryFile;
    private JsonFile messagesFile;
    private JsonFile menusFile;
    private Configuration config = new MemoryConfiguration();
    private Configuration messagesConfig = new MemoryConfiguration();
    private Configuration menusConfig = new MemoryConfiguration();

    // Paramètres typés
    private final Settings settings = new Settings();
    private final ProtectionSettings protection = new ProtectionSettings();
    private final EconomySettings economySettings = new EconomySettings();
    private final RoleSettings roles = new RoleSettings();
    private final Messages messages = new Messages();
    private final ConfigDocs configDocs = new ConfigDocs();

    // Services
    private DataManager data;
    private PlayerManager players;
    private FactionManager factions;
    private ClaimManager claims;
    private PowerManager power;
    private EconomyManager economy;
    private TeleportManager teleports;
    private CooldownManager cooldowns;
    private ConfirmationManager confirmations;
    private TerritoryManager territory;
    private MapManager map;
    private PlaceholderService placeholders;
    private ProtectionService protectionService;
    private UpgradeManager upgrades;
    private ChestManager chests;
    private TopManager top;
    private InactivityManager inactivity;
    private SeeChunkManager seeChunk;
    private MenuManager menus;
    private CommandManager commands;
    private ChatListener chat;
    private PapiHook papi;

    public static KoriaFactions get() {
        return instance;
    }

    @Override
    public void onEnable() {
        long start = System.currentTimeMillis();
        instance = this;

        configFile = new JsonFile(this, "config.json", "chat.grades", "economie.couts");
        factionsFile = new JsonFile(this, "factions.json", "puissance.max-par-grade", "permissions.verrouillees",
                "ameliorations.liste", "ameliorations.liste.*.niveaux");
        territoryFile = new JsonFile(this, "territoire.json", "protection.commandes-bloquees");
        messagesFile = new JsonFile(this, "messages.json", "territory.overrides", "protection.actions", "logs.entries");
        menusFile = new JsonFile(this, "menus.json", "principal.objets", "sans-faction.objets", "permissions.objets",
                "editeur.fichiers");

        data = new DataManager(this);
        players = new PlayerManager(this);
        factions = new FactionManager(this);
        claims = new ClaimManager(this);
        power = new PowerManager(this);
        economy = new EconomyManager(this);
        teleports = new TeleportManager(this);
        cooldowns = new CooldownManager();
        confirmations = new ConfirmationManager(this);
        territory = new TerritoryManager(this);
        map = new MapManager(this);
        placeholders = new PlaceholderService(this);
        protectionService = new ProtectionService(this);
        upgrades = new UpgradeManager(this);
        chests = new ChestManager(this);
        top = new TopManager(this);
        inactivity = new InactivityManager(this);
        seeChunk = new SeeChunkManager(this);
        menus = new MenuManager(this);
        commands = new CommandManager(this);
        chat = new ChatListener(this);

        warnOldYamlFiles();
        loadConfigs();
        configDocs.load(this);
        configDocs.writeReadme(this, configFiles());
        territory.installToast();
        menus.installLayouts();

        try {
            data.init();
            data.load();
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Impossible de charger les données : le plugin est désactivé pour éviter toute perte.", e);
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        economy.hook();
        // certains plugins d'économie s'enregistrent après nous
        Bukkit.getScheduler().runTask(this, new Runnable() {
            @Override
            public void run() {
                if (!economy.isAvailable()) {
                    economy.hook();
                }
                economy.warnIfUnavailable();
            }
        });
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            try {
                papi = new PapiHook(this);
                getLogger().info("PlaceholderAPI détecté : variables %koriafactions_...% enregistrées.");
            } catch (Throwable t) {
                getLogger().log(Level.WARNING, "Impossible de s'accrocher à PlaceholderAPI", t);
                papi = null;
            }
        }

        PluginManager pm = Bukkit.getPluginManager();
        pm.registerEvents(new PlayerListener(this), this);
        pm.registerEvents(new ProtectionListener(this), this);
        pm.registerEvents(new CombatListener(this), this);
        pm.registerEvents(new WorldListener(this), this);
        pm.registerEvents(chests, this);
        pm.registerEvents(upgrades, this);
        pm.registerEvents(menus, this);
        pm.registerEvents(menus.prompt(), this);
        chat.register();

        commands.load(config, messagesConfig);
        commands.registerBukkitCommand();

        startTasks();
        for (Player player : Bukkit.getOnlinePlayers()) {
            territory.handleMove(player, player.getLocation());
        }
        getLogger().info("KoriaFactions activé en " + (System.currentTimeMillis() - start) + " ms (stockage : "
                + data.getStorageName() + ", économie : " + (economy.isAvailable() ? economy.getProviderName() : "aucune") + ").");
    }

    @Override
    public void onDisable() {
        if (data == null) {
            return;
        }
        if (commands != null) {
            commands.unregisterBukkitCommand();
        }
        stopTasks();
        if (teleports != null) {
            teleports.cancelAll();
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getOpenInventory().getTopInventory().getHolder() instanceof Menu) {
                player.closeInventory();
            }
        }
        if (chests != null) {
            chests.closeAll();
        }
        if (papi != null) {
            papi.unregister();
            papi = null;
        }
        if (chat != null) {
            HandlerList.unregisterAll(chat);
        }
        data.shutdown();
        instance = null;
    }

    /** @return les problèmes à signaler (erreurs JSON, améliorations invalides) */
    private List<String> loadConfigs() {
        List<String> problems = new ArrayList<String>();
        for (JsonFile file : configFiles()) {
            String error = file.load();
            if (error != null) {
                problems.add(file.getName() + " : " + error);
            }
        }
        config = JsonFile.combine(Arrays.asList(configFile, factionsFile, territoryFile));
        messagesConfig = messagesFile.toConfiguration();
        menusConfig = menusFile.toConfiguration();

        settings.load(config, getLogger());
        protection.load(config, getLogger());
        economySettings.load(config);
        roles.load(config, getLogger());
        messages.load(messagesConfig, settings);
        upgrades.load(config);
        problems.addAll(upgrades.getProblems());
        registerGradePermissions();
        return problems;
    }

    /** Les anciens fichiers .yml ne sont plus lus : on le signale une fois au démarrage. */
    private void warnOldYamlFiles() {
        List<String> found = new ArrayList<String>();
        for (String name : new String[]{"config.yml", "messages.yml", "commands.yml", "permissions.yml", "protection.yml",
                "economy.yml", "upgrades.yml", "menus.yml", "scoreboard.yml"}) {
            if (new File(getDataFolder(), name).exists()) {
                found.add(name);
            }
        }
        if (!found.isEmpty()) {
            getLogger().warning("Anciens fichiers ignorés (la configuration est maintenant en JSON) : " + found
                    + ". Vous pouvez les supprimer.");
        }
    }

    /**
     * Les permissions de grade (chat, puissance) ne sont accordées à personne par
     * défaut, pas même aux opérateurs : sinon un OP aurait toujours le grade le plus haut.
     */
    private void registerGradePermissions() {
        PluginManager pm = Bukkit.getPluginManager();
        List<String> nodes = new ArrayList<String>(settings.powerMaxByPermission.keySet());
        for (Settings.Rank rank : settings.ranks) {
            nodes.add(rank.permission);
        }
        for (String node : nodes) {
            Permission existing = pm.getPermission(node);
            if (existing == null) {
                pm.addPermission(new Permission(node, PermissionDefault.FALSE));
            }
        }
    }

    private void startTasks() {
        data.startAutoSave();
        power.start();
        inactivity.start();
        seeChunk.start();
    }

    private void stopTasks() {
        if (power == null) {
            return;
        }
        data.stopAutoSave();
        power.stop();
        inactivity.stop();
        seeChunk.stop();
    }

    /**
     * Recharge toute la configuration (/f reload).
     *
     * @return les problèmes détectés, à afficher à l'auteur de la commande
     */
    public List<String> reload() {
        List<String> problems = loadConfigs();
        territory.installToast();
        menus.installLayouts();
        factions.applySystemSettings();
        commands.load(config, messagesConfig);
        chat.register();
        economy.hook();
        economy.warnIfUnavailable();
        stopTasks();
        startTasks();
        top.invalidate();
        chests.refreshAll();
        for (Player player : Bukkit.getOnlinePlayers()) {
            players.setPower(players.get(player), players.get(player).getPower());
        }
        for (Faction f : factions.getNormalFactions()) {
            factions.checkRaidable(f);
        }
        return problems;
    }

    // ============================================================ Accesseurs

    public Settings settings() {
        return settings;
    }

    public ProtectionSettings protection() {
        return protection;
    }

    public EconomySettings economySettings() {
        return economySettings;
    }

    public RoleSettings roles() {
        return roles;
    }

    public Messages msg() {
        return messages;
    }

    /** Explication des options (menu /f config et LISEZMOI.txt). */
    public ConfigDocs configDocs() {
        return configDocs;
    }

    /** Réglages de config.json, factions.json et territoire.json réunis. */
    public Configuration config() {
        return config;
    }

    public Configuration messagesConfig() {
        return messagesConfig;
    }

    /** Fichiers de configuration, dans l'ordre du menu /f config. */
    public List<JsonFile> configFiles() {
        return Arrays.asList(configFile, factionsFile, territoryFile, messagesFile, menusFile);
    }

    public Configuration menusConfig() {
        return menusConfig;
    }

    public DataManager data() {
        return data;
    }

    public PlayerManager players() {
        return players;
    }

    public FactionManager factions() {
        return factions;
    }

    public ClaimManager claims() {
        return claims;
    }

    public PowerManager power() {
        return power;
    }

    public EconomyManager economy() {
        return economy;
    }

    public TeleportManager teleports() {
        return teleports;
    }

    public CooldownManager cooldowns() {
        return cooldowns;
    }

    public ConfirmationManager confirmations() {
        return confirmations;
    }

    public TerritoryManager territory() {
        return territory;
    }

    public MapManager map() {
        return map;
    }

    public PlaceholderService placeholders() {
        return placeholders;
    }

    public ProtectionService protectionService() {
        return protectionService;
    }

    public UpgradeManager upgrades() {
        return upgrades;
    }

    public ChestManager chests() {
        return chests;
    }

    public TopManager top() {
        return top;
    }

    public MenuManager menus() {
        return menus;
    }

    public CommandManager commands() {
        return commands;
    }

    public ChatListener chat() {
        return chat;
    }

    /** @return le pont PlaceholderAPI, ou null si le plugin est absent. */
    public PapiHook papi() {
        return papi;
    }
}
