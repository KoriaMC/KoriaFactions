package fr.koriamc.factions.gui;

import fr.koriamc.factions.KoriaFactions;
import fr.koriamc.factions.config.RoleSettings;
import fr.koriamc.factions.hook.GuiHook;
import fr.koriamc.factions.model.FPlayer;
import fr.koriamc.factions.model.Faction;
import fr.koriamc.factions.model.PermAction;
import fr.koriamc.factions.model.PermSubject;
import fr.koriamc.factions.model.Role;
import fr.koriamc.factions.util.Text;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Écran KoriaMC des permissions d'un rôle ou d'un membre (menus.json, permissions.ecran-koriamc.layout-liste) :
 * une case par action, dans une grille scrollable (liste list:factions_perms). Les clics passent par
 * /f permsgui ; l'écran est mis à jour sans être rouvert.
 */
public final class PermsScreen {

    private static final String LIST = "factions_perms";
    private static final String CHOICE_LIST = "factions_perms_choix";
    /** Au-delà, le nom de l'action est coupé sur deux lignes. */
    private static final int LINE_LENGTH = 18;

    private final KoriaFactions plugin;
    private final GuiHook gui;

    PermsScreen(KoriaFactions plugin, GuiHook gui) {
        this.plugin = plugin;
        this.gui = gui;
    }

    private ConfigurationSection section() {
        return plugin.menus().section("permissions");
    }

    String layout() {
        return option(section(), "layout-liste").trim().toLowerCase(Locale.ROOT);
    }

    /** Écran de choix d'un rôle ou d'un membre. */
    String choiceLayout() {
        return option(section(), "layout-choix").trim().toLowerCase(Locale.ROOT);
    }

    /** Grille des permissions d'un membre (avec le bouton Configuration par défaut). */
    String playerLayout() {
        return option(section(), "layout-joueur").trim().toLowerCase(Locale.ROOT);
    }

    /** Choix du rôle (carte "Configuration par défaut"). @return false sans le client KoriaMC */
    boolean openRoles(Player player) {
        String layout = choiceLayout();
        FPlayer fp = plugin.players().get(player);
        Faction f = plugin.factions().getFaction(fp);
        if (f == null || layout.isEmpty() || !gui.canDisplay(player)) {
            return false;
        }
        ConfigurationSection s = section();
        RoleSettings roles = plugin.roles();
        List<String> tooltip = s.getStringList("ecran-koriamc.infobulle-choix-role");
        List<Map<String, String>> items = new ArrayList<Map<String, String>>();
        for (PermSubject subject : PermSubject.values()) {
            if (!PermsSubjectMenu.isAvailable(plugin, subject)) {
                continue;
            }
            String name = roles.subjectName(subject, plugin.settings());
            items.add(choice(name, roles.color(subject.getRole()), "menu permissions:" + subject.name(), tooltip,
                    new Object[]{"subject", name}));
        }
        pushChoice(player, option(s, "sous-titre-roles"), "", "menu permissions", items);
        return open(player, layout);
    }

    /** Configuration par défaut d'un membre : choix du rôle dont les permissions sont copiées. */
    private boolean openCopy(Player player, FPlayer target) {
        String layout = choiceLayout();
        if (layout.isEmpty() || !gui.canDisplay(player)) {
            return false;
        }
        ConfigurationSection s = section();
        RoleSettings roles = plugin.roles();
        List<String> tooltip = s.getStringList("ecran-koriamc.infobulle-choix-copie");
        List<Map<String, String>> items = new ArrayList<Map<String, String>>();
        for (PermSubject subject : PermSubject.values()) {
            if (!PermsSubjectMenu.isAvailable(plugin, subject)) {
                continue;
            }
            String name = roles.subjectName(subject, plugin.settings());
            items.add(choice(name, roles.color(subject.getRole()),
                    "permsgui joueur " + target.getName() + " copier " + subject.name(), tooltip,
                    new Object[]{"subject", name, "member", target.getName()}));
        }
        pushChoice(player, Text.replace(option(s, "sous-titre-copie"), "member", target.getName()), "",
                "menu permissions-joueur:" + target.getName(), items);
        return open(player, layout);
    }

    /** Choix du membre (carte "Les permissions d'un joueur"). @return false sans le client KoriaMC */
    boolean openMembers(Player player) {
        String layout = choiceLayout();
        FPlayer fp = plugin.players().get(player);
        Faction f = plugin.factions().getFaction(fp);
        if (f == null || layout.isEmpty() || !gui.canDisplay(player)) {
            return false;
        }
        ConfigurationSection s = section();
        RoleSettings roles = plugin.roles();
        List<String> tooltip = s.getStringList("ecran-koriamc.infobulle-choix-joueur");
        List<Map<String, String>> items = new ArrayList<Map<String, String>>();
        for (FPlayer member : plugin.factions().getMembers(f)) {
            // le chef possède toujours toutes les permissions
            if (member.getRole() == Role.LEADER) {
                continue;
            }
            items.add(choice(member.getName(), roles.color(member.getRole()), "menu permissions-joueur:" + member.getName(),
                    tooltip, new Object[]{"member", member.getName(), "role", roles.name(member.getRole()),
                            "role_color", roles.color(member.getRole()),
                            "custom", f.countPlayerPermissions(member.getUniqueId())}));
        }
        pushChoice(player, option(s, "sous-titre-joueurs"), option(s, "aucun-membre"), "menu permissions", items);
        return open(player, layout);
    }

    private void pushChoice(Player player, String subtitle, String empty, String back, List<Map<String, String>> items) {
        gui.setVariable(player, "factions_commande", plugin.commands().getLabel());
        gui.setVariable(player, "factions_perms_retour", back);
        gui.setVariable(player, "factions_perms_sous_titre", Text.color(subtitle));
        gui.setVariable(player, "factions_perms_vide", Text.color(empty));
        gui.setList(player, CHOICE_LIST, items);
    }

    /** Case de choix : l1 / l2, couleur, infobulle, commande (après /f). */
    private static Map<String, String> choice(String name, String color, String command, List<String> tooltip,
                                              Object[] kv) {
        Map<String, String> item = new HashMap<String, String>();
        String[] lines = split(Text.strip(name));
        item.put("l1", lines[0]);
        item.put("l2", lines[1]);
        item.put("couleur", color);
        item.put("commande", command);
        item.put("infobulle", Text.color(Text.join(Text.replace(tooltip, kv), "\n")));
        return item;
    }

    /** Écran des actions d'un rôle. @return false si le joueur n'a pas le client KoriaMC (menu coffre à la place) */
    boolean openRole(Player player, PermSubject subject) {
        String layout = layout();
        if (layout.isEmpty() || !gui.canDisplay(player)) {
            return false;
        }
        showRole(player, subject, "");
        return open(player, layout);
    }

    /** Écran des actions personnelles d'un membre. @return false sans le client KoriaMC */
    boolean openPlayer(Player player, FPlayer target) {
        String layout = playerLayout();
        if (layout.isEmpty() || !gui.canDisplay(player)) {
            return false;
        }
        showPlayer(player, target, "");
        return open(player, layout);
    }

    private boolean open(Player player, String layout) {
        if (gui.open(player, layout)) {
            return true;
        }
        plugin.getLogger().warning("L'écran " + layout + " n'a pas pu s'ouvrir : le fichier koriamc/guis/" + layout
                + ".json existe-t-il ?");
        return false;
    }

    /** /f permsgui role &lt;ROLE&gt; &lt;ACTION&gt; */
    public void clickRole(Player player, PermSubject subject, PermAction action) {
        FPlayer fp = plugin.players().get(player);
        Faction f = plugin.factions().getFaction(fp);
        if (f == null || subject == null || !PermsSubjectMenu.isAvailable(plugin, subject)) {
            return;
        }
        String error = action == null ? "" : PermsEditor.toggleRole(plugin, player, fp, f, subject, action, false);
        showRole(player, subject, error);
    }

    /** /f permsgui joueur &lt;pseudo&gt; &lt;ACTION&gt; [reset] | defaut */
    public void clickPlayer(Player player, FPlayer target, String what, String extra) {
        FPlayer fp = plugin.players().get(player);
        Faction f = plugin.factions().getFaction(fp);
        if (f == null || target == null || !f.getId().equals(target.getFactionId())) {
            return;
        }
        if ("defaut".equalsIgnoreCase(what)) {
            openCopy(player, target);
            return;
        }
        if ("copier".equalsIgnoreCase(what)) {
            PermSubject source = PermSubject.parse(extra);
            String error = source == null || !PermsSubjectMenu.isAvailable(plugin, source) ? ""
                    : PermsEditor.copyRole(plugin, player, fp, f, target, source, false);
            showPlayer(player, target, error);
            open(player, playerLayout());
            return;
        }
        PermAction action = PermAction.parse(what);
        String error = action == null ? ""
                : PermsEditor.changePlayer(plugin, player, fp, f, target, action, "reset".equalsIgnoreCase(extra), false);
        showPlayer(player, target, error);
    }

    private void showRole(Player player, PermSubject subject, String error) {
        FPlayer fp = plugin.players().get(player);
        Faction f = plugin.factions().getFaction(fp);
        if (f == null) {
            return;
        }
        ConfigurationSection s = section();
        ConfigurationSection template = s.getConfigurationSection("modele-action");
        RoleSettings roles = plugin.roles();
        String subjectName = roles.subjectName(subject, plugin.settings());
        List<String> tooltip = s.getStringList("ecran-koriamc.infobulle-role");
        List<Map<String, String>> items = new ArrayList<Map<String, String>>();
        for (PermAction action : PermAction.values()) {
            boolean allowed = plugin.factions().hasPermission(f, subject, action);
            boolean locked = roles.isLocked(subject, action);
            Object[] kv = {
                    "action", roles.actionName(action),
                    "state", string(template, allowed ? "etat-oui" : "etat-non"),
                    "locked", locked ? string(template, "texte-verrouille") : "",
                    "subject", subjectName
            };
            items.add(item(action, roles.actionName(action), allowed ? "&a" : "&c", tooltip, kv));
        }
        push(player, "role", subject.name(),
                Text.replace(option(s, "sous-titre-role"), "subject", subjectName,
                        "role_color", subject.getRole() == null ? "" : roles.color(subject.getRole())),
                "menu permissions-roles", items, error);
    }

    private void showPlayer(Player player, FPlayer target, String error) {
        FPlayer fp = plugin.players().get(player);
        Faction f = plugin.factions().getFaction(fp);
        if (f == null || !f.getId().equals(target.getFactionId())) {
            return;
        }
        ConfigurationSection s = section();
        ConfigurationSection template = s.getConfigurationSection("modele-action-joueur");
        RoleSettings roles = plugin.roles();
        PermSubject roleSubject = PermSubject.of(target.getRole());
        String on = string(template, "etat-oui");
        String off = string(template, "etat-non");
        List<String> tooltip = s.getStringList("ecran-koriamc.infobulle-joueur");
        List<Map<String, String>> items = new ArrayList<Map<String, String>>();
        for (PermAction action : PermAction.values()) {
            boolean fromRole = plugin.factions().hasPermission(f, roleSubject, action);
            Boolean custom = f.getPlayerPermission(target.getUniqueId(), action);
            boolean locked = roleSubject == null || roles.isLocked(roleSubject, action);
            boolean effective = plugin.factions().hasMemberPermission(f, target, action);
            String state = custom == null || locked
                    ? Text.replace(string(template, "etat-herite"), "role_state", fromRole ? on : off)
                    : custom ? on : off;
            Object[] kv = {
                    "action", roles.actionName(action),
                    "role", roles.name(target.getRole()),
                    "role_state", fromRole ? on : off,
                    "state", state,
                    "locked", locked ? string(template, "texte-verrouille") : "",
                    "member", target.getName()
            };
            items.add(item(action, roles.actionName(action), effective ? "&a" : "&c", tooltip, kv));
        }
        push(player, "joueur", target.getName(),
                Text.replace(option(s, "sous-titre-joueur"), "member", target.getName(),
                        "role", roles.name(target.getRole()), "role_color", roles.color(target.getRole())),
                "menu permissions-joueurs", items, error);
    }

    private void push(Player player, String type, String target, String subtitle, String back,
                      List<Map<String, String>> items, String error) {
        String err = error == null ? "" : error;
        gui.setVariable(player, "factions_commande", plugin.commands().getLabel());
        gui.setVariable(player, "factions_perms_type", type);
        gui.setVariable(player, "factions_perms_joueur", String.valueOf("joueur".equals(type)));
        gui.setVariable(player, "factions_perms_cible", target);
        gui.setVariable(player, "factions_perms_sous_titre", Text.color(subtitle));
        gui.setVariable(player, "factions_perms_retour", back);
        gui.setVariable(player, "factions_perms_erreur", err);
        gui.setVariable(player, "factions_perms_erreur_oui", String.valueOf(!err.isEmpty()));
        gui.setList(player, LIST, items);
    }

    /** Case de la grille : id (action), l1 / l2 (nom sur une ou deux lignes), couleur, infobulle. */
    private static Map<String, String> item(PermAction action, String name, String color, List<String> tooltip,
                                            Object[] kv) {
        Map<String, String> item = new HashMap<String, String>();
        String[] lines = split(Text.strip(Text.color(name)));
        item.put("id", action.name());
        item.put("l1", lines[0]);
        item.put("l2", lines[1]);
        item.put("couleur", color);
        List<String> out = new ArrayList<String>();
        for (String line : tooltip) {
            String value = Text.replace(line, kv);
            // ligne qui ne contenait qu'une variable vide (ex. {locked}) : retirée
            if (value.trim().isEmpty() && !line.trim().isEmpty()) {
                continue;
            }
            out.add(value);
        }
        item.put("infobulle", Text.color(Text.join(out, "\n")));
        return item;
    }

    /** Coupe un nom trop long au premier espace proche du milieu. */
    static String[] split(String name) {
        if (name.length() <= LINE_LENGTH) {
            return new String[]{name, ""};
        }
        int middle = name.length() / 2;
        int best = -1;
        for (int i = 0; i < name.length(); i++) {
            if (name.charAt(i) == ' ' && (best < 0 || Math.abs(i - middle) < Math.abs(best - middle))) {
                best = i;
            }
        }
        if (best < 0) {
            return new String[]{name, ""};
        }
        return new String[]{name.substring(0, best), name.substring(best + 1)};
    }

    /** Option de permissions.ecran-koriamc (valeur du jar si elle manque dans menus.json). */
    private static String option(ConfigurationSection s, String key) {
        String value = s.getString("ecran-koriamc." + key);
        return value == null ? "" : value;
    }

    private static String string(ConfigurationSection template, String key) {
        return template == null ? "" : template.getString(key, "");
    }
}
