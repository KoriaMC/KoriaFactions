package fr.koriamc.factions.gui;

import fr.koriamc.factions.KoriaFactions;
import fr.koriamc.factions.command.SubCommand;
import fr.koriamc.factions.config.JsonFile;
import fr.koriamc.factions.hook.GuiHook;
import fr.koriamc.factions.model.FPlayer;
import fr.koriamc.factions.model.Faction;
import fr.koriamc.factions.model.PermSubject;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.Locale;
import java.util.logging.Level;

/**
 * Ouverture des menus et gestion des clics.
 */
public final class MenuManager implements Listener {

    private final KoriaFactions plugin;
    /** Écran KoriaMC des permissions par défaut, copié dans koriamc/guis/ s'il n'y est pas. */
    private static final String PERMISSIONS_RESOURCE = "gui/permissions_faction.json";
    /** Grille des actions d'un rôle ou d'un membre. */
    private static final String PERMISSIONS_LIST_RESOURCE = "gui/permissions_faction_liste.json";
    /** Choix du rôle ou du membre. */
    private static final String PERMISSIONS_CHOICE_RESOURCE = "gui/permissions_faction_choix.json";
    /** Grille des actions d'un membre. */
    private static final String PERMISSIONS_PLAYER_RESOURCE = "gui/permissions_faction_joueur.json";
    /** Textes propres aux anciennes versions des écrans des permissions. */
    private static final String[] OUTDATED_MARKERS = {"%var_factions_perms_type%\\\" ==", "%var_factions_perms_joueur%",
            "%var_factions_commande% menu permissions\""};

    private final ChatPrompt prompt;
    private final GuiHook gui;
    private final PermsScreen permsScreen;

    public MenuManager(KoriaFactions plugin) {
        this.plugin = plugin;
        this.prompt = new ChatPrompt(plugin);
        this.gui = new GuiHook(plugin.getLogger());
        this.permsScreen = new PermsScreen(plugin, gui);
    }

    /** Écran KoriaMC des actions d'un rôle ou d'un membre (/f permsgui). */
    public PermsScreen permsScreen() {
        return permsScreen;
    }

    /** Layout KoriaMC de /f perms (menus.json, permissions.ecran-koriamc.layout ; vide = menu coffre). */
    private String permissionsLayout() {
        // sans valeur par défaut explicite : celle du jar s'applique si l'option manque dans menus.json
        String layout = section("permissions").getString("ecran-koriamc.layout");
        return layout == null ? "" : layout.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * Copie les écrans des permissions dans koriamc/guis/ s'ils n'existent pas encore (jamais écrasés : les
     * modifications faites dans l'éditeur de GUI sont gardées).
     */
    public void installLayouts() {
        if (!section("permissions").getBoolean("ecran-koriamc.installer-layout", true) || !gui.isAvailable()) {
            return;
        }
        installLayout(permissionsLayout(), PERMISSIONS_RESOURCE);
        installLayout(permsScreen.layout(), PERMISSIONS_LIST_RESOURCE);
        installLayout(permsScreen.choiceLayout(), PERMISSIONS_CHOICE_RESOURCE);
        installLayout(permsScreen.playerLayout(), PERMISSIONS_PLAYER_RESOURCE);
    }

    private void installLayout(String layout, String resource) {
        if (layout.isEmpty()) {
            return;
        }
        File target = new File(new File("koriamc", "guis"), layout + ".json");
        if (target.exists() && !isBroken(target)) {
            return;
        }
        InputStream in = plugin.getResource(resource);
        if (in == null) {
            return;
        }
        try {
            File folder = target.getParentFile();
            if (!folder.isDirectory() && !folder.mkdirs()) {
                throw new IOException("dossier " + folder.getPath() + " impossible à créer");
            }
            Files.copy(in, target.toPath(), StandardCopyOption.REPLACE_EXISTING);
            plugin.getLogger().info("Écran des permissions installé : " + target.getPath());
            gui.reloadLayouts();
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Impossible d'installer l'écran " + target.getPath(), e);
        } finally {
            try {
                in.close();
            } catch (IOException ignored) {
                // rien à faire
            }
        }
    }

    /**
     * Écran installé par une version précédente du plugin, incompatible avec celle-ci (grille commune rôle / joueur,
     * placeholder entre guillemets dans une condition, retour codé en dur) : remplacé par la version du jar.
     */
    private boolean isBroken(File file) {
        try {
            String content = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
            for (String marker : OUTDATED_MARKERS) {
                if (content.contains(marker)) {
                    plugin.getLogger().info("Ancienne version de " + file.getPath() + " remplacée.");
                    return true;
                }
            }
            return false;
        } catch (IOException e) {
            return false;
        }
    }

    /** Ouvre l'écran KoriaMC des permissions. @return false si le joueur n'a pas le client KoriaMC (ou écran désactivé) */
    private boolean openPermissionsScreen(Player player) {
        String layout = permissionsLayout();
        if (layout.isEmpty() || !gui.canDisplay(player)) {
            return false;
        }
        gui.setVariable(player, "factions_commande", plugin.commands().getLabel());
        if (gui.open(player, layout)) {
            return true;
        }
        plugin.getLogger().warning("L'écran " + layout + " n'a pas pu s'ouvrir : le fichier koriamc/guis/" + layout
                + ".json existe-t-il ?");
        return false;
    }

    /** Saisie de valeurs au clavier (éditeur de configuration). */
    public ChatPrompt prompt() {
        return prompt;
    }

    /**
     * Éditeur de configuration : liste des fichiers, ou directement un fichier.
     *
     * @return false si le fichier demandé n'existe pas
     */
    public boolean openConfig(Player player, String fileName) {
        if (fileName == null) {
            new ConfigFilesMenu(plugin, player).open();
            return true;
        }
        for (JsonFile file : plugin.configFiles()) {
            String name = file.getName();
            if (name.equalsIgnoreCase(fileName) || name.equalsIgnoreCase(fileName + ".json")) {
                new ConfigSectionMenu(plugin, player, file, Collections.<String>emptyList(), 0).open();
                return true;
            }
        }
        return false;
    }

    /** Section de menus.json (jamais nulle). */
    public ConfigurationSection section(String path) {
        ConfigurationSection s = plugin.menusConfig().getConfigurationSection(path);
        return s != null ? s : new MemoryConfiguration().createSection(path);
    }

    /**
     * Ouvre un menu d'après son identifiant (action MENU:&lt;id&gt; de menus.json, /f menu &lt;id&gt;) :
     * principal, membres, permissions (écran KoriaMC, sinon permissions-roles), permissions-roles,
     * permissions-ecran (écran KoriaMC, sinon principal), permissions:&lt;ROLE&gt;, permissions-joueurs,
     * permissions-joueur:&lt;pseudo&gt;, flags, ameliorations, warps, config, config:&lt;fichier&gt;.
     */
    public void open(Player player, String id) {
        String raw = id == null ? "principal" : id.trim();
        int colon = raw.indexOf(':');
        String key = (colon < 0 ? raw : raw.substring(0, colon)).toLowerCase(Locale.ROOT);
        String arg = colon < 0 ? null : raw.substring(colon + 1).trim();
        if (key.equals("config")) {
            SubCommand command = plugin.commands().get("config");
            if (command == null || !command.hasPermission(player)) {
                plugin.msg().send(player, "general.no-permission");
            } else if (!openConfig(player, arg)) {
                plugin.msg().send(player, "config-editor.unknown-file", "file", arg);
            }
            return;
        }
        FPlayer fp = plugin.players().get(player);
        Faction faction = plugin.factions().getFaction(fp);
        if (!key.equals("principal") && faction == null) {
            plugin.msg().send(player, "general.must-have-faction");
            return;
        }
        switch (key) {
            case "principal":
                new MainMenu(plugin, player).open();
                break;
            case "membres":
                new MembersMenu(plugin, player, faction, 0).open();
                break;
            case "permissions":
                PermSubject subject = arg == null ? null : PermSubject.parse(arg);
                if (arg == null) {
                    if (!openPermissionsScreen(player)) {
                        new PermsSubjectMenu(plugin, player).open();
                    }
                } else if (subject == null || !PermsSubjectMenu.isAvailable(plugin, subject)) {
                    plugin.getLogger().warning("Rôle inconnu ou désactivé dans MENU:" + raw);
                } else {
                    if (!permsScreen.openRole(player, subject)) {
                        new PermsActionMenu(plugin, player, subject).open();
                    }
                }
                break;
            case "permissions-roles":
                if (!permsScreen.openRoles(player)) {
                    new PermsSubjectMenu(plugin, player).open();
                }
                break;
            case "permissions-ecran":
                if (!openPermissionsScreen(player)) {
                    new MainMenu(plugin, player).open();
                }
                break;
            case "permissions-joueurs":
                if (!plugin.roles().isPlayerPermissionsEnabled()) {
                    plugin.msg().send(player, "general.feature-disabled");
                    return;
                }
                if (!permsScreen.openMembers(player)) {
                    new PermsPlayersMenu(plugin, player, 0).open();
                }
                break;
            case "permissions-joueur":
                FPlayer target = arg == null ? null : plugin.players().getByName(arg);
                if (!plugin.roles().isPlayerPermissionsEnabled()) {
                    plugin.msg().send(player, "general.feature-disabled");
                } else if (target == null || !faction.getId().equals(target.getFactionId())) {
                    plugin.msg().send(player, "general.target-not-member", "player", arg == null ? "?" : arg);
                } else {
                    openPlayerPerms(player, target);
                }
                break;
            case "flags":
                new FlagsMenu(plugin, player).open();
                break;
            case "ameliorations":
                if (!plugin.upgrades().isEnabled()) {
                    plugin.msg().send(player, "general.feature-disabled");
                    return;
                }
                new UpgradesMenu(plugin, player).open();
                break;
            case "warps":
                if (!plugin.settings().warpsEnabled) {
                    plugin.msg().send(player, "general.feature-disabled");
                    return;
                }
                new WarpsMenu(plugin, player).open();
                break;
            default:
                plugin.getLogger().warning("Menu inconnu : " + id);
        }
    }

    /** Ouvre les permissions personnelles d'un membre de la faction du joueur. */
    public void openPlayerPerms(Player player, FPlayer target) {
        if (!permsScreen.openPlayer(player, target)) {
            new PermsPlayerActionMenu(plugin, player, target).open();
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onClick(InventoryClickEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (top == null || !(top.getHolder() instanceof Menu)) {
            return;
        }
        event.setCancelled(true);
        if (event.getRawSlot() >= 0 && event.getRawSlot() < top.getSize() && event.getWhoClicked() instanceof Player) {
            ((Menu) top.getHolder()).handleClick(event);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof Menu) {
            event.setCancelled(true);
        }
    }
}
