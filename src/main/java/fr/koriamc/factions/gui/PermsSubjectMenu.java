package fr.koriamc.factions.gui;

import fr.koriamc.factions.KoriaFactions;
import fr.koriamc.factions.model.PermSubject;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

/**
 * /f perms, premier écran : objets de menus.json (permissions.objets). Un objet
 * nommé comme un rôle est masqué si ce rôle est désactivé ; l'objet "joueurs" est masqué si les permissions par joueur le sont.
 */
final class PermsSubjectMenu extends Menu {

    PermsSubjectMenu(KoriaFactions plugin, Player player) {
        super(plugin, player);
    }

    @Override
    protected ConfigurationSection section() {
        return plugin.menus().section("permissions");
    }

    @Override
    protected String titleTemplate() {
        return section().getString("titre-choix", "");
    }

    @Override
    protected int rows() {
        return section().getInt("lignes-choix", 3);
    }

    static boolean isAvailable(KoriaFactions plugin, PermSubject subject) {
        return plugin.roles().isEnabled(subject.getRole());
    }

    @Override
    protected void render() {
        ConfigurationSection items = section().getConfigurationSection("objets");
        if (items == null) {
            return;
        }
        Object[] kv = vars();
        for (String key : items.getKeys(false)) {
            PermSubject subject = PermSubject.parse(key);
            if (subject != null && !isAvailable(plugin, subject)) {
                continue;
            }
            // objet des alliances (supprimées) resté dans un ancien menus.json
            if (key.equalsIgnoreCase("ALLY")) {
                continue;
            }
            if (key.equals("joueurs") && !plugin.roles().isPlayerPermissionsEnabled()) {
                continue;
            }
            setConfigured(items.getConfigurationSection(key), kv);
        }
    }
}
