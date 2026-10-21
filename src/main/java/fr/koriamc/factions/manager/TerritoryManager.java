package fr.koriamc.factions.manager;

import fr.koriamc.factions.KoriaFactions;
import fr.koriamc.factions.config.Messages;
import fr.koriamc.factions.config.Settings;
import fr.koriamc.factions.model.AutoClaimMode;
import fr.koriamc.factions.model.ChunkPos;
import fr.koriamc.factions.model.FPlayer;
import fr.koriamc.factions.model.Faction;
import fr.koriamc.factions.model.TerritoryType;
import fr.koriamc.factions.hook.GuiHook;
import fr.koriamc.factions.util.Nms;
import fr.koriamc.factions.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Locale;
import java.util.logging.Level;

/**
 * Changements de chunk : auto-claim, notifications d'entrée / sortie de territoire et carte automatique.
 * La notification est un toast KoriaMC (layout koriamc/guis/territoire.json) ; les joueurs sans le client
 * KoriaMC ont la barre d'action.
 */
public final class TerritoryManager {

    /** Toast par défaut, copié dans koriamc/guis/ s'il n'y est pas. */
    private static final String TOAST_RESOURCE = "gui/territoire.json";

    private final KoriaFactions plugin;
    private final GuiHook gui;

    public TerritoryManager(KoriaFactions plugin) {
        this.plugin = plugin;
        this.gui = new GuiHook(plugin.getLogger());
    }

    /**
     * Copie le toast dans koriamc/guis/ s'il n'existe pas encore (jamais écrasé : les modifications faites
     * dans l'éditeur de GUI sont gardées).
     */
    public void installToast() {
        Settings s = plugin.settings();
        if (!s.installToast || !gui.isAvailable()) {
            return;
        }
        File target = new File(new File("koriamc", "guis"), s.toastLayout + ".json");
        if (target.exists()) {
            return;
        }
        InputStream in = plugin.getResource(TOAST_RESOURCE);
        if (in == null) {
            return;
        }
        try {
            File folder = target.getParentFile();
            if (!folder.isDirectory() && !folder.mkdirs()) {
                throw new IOException("dossier " + folder.getPath() + " impossible à créer");
            }
            Files.copy(in, target.toPath());
            plugin.getLogger().info("Toast de territoire installé : " + target.getPath());
            gui.reloadLayouts();
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Impossible d'installer le toast " + target.getPath(), e);
        } finally {
            try {
                in.close();
            } catch (IOException ignored) {
                // rien à faire
            }
        }
    }

    public void handleMove(Player player, Location to) {
        FPlayer fp = plugin.players().get(player);
        ChunkPos pos = ChunkPos.of(to);
        if (pos.equals(fp.getLastChunk())) {
            return;
        }
        fp.setLastChunk(pos);
        if (plugin.settings().isWorldDisabled(pos.getWorldName())) {
            return;
        }
        handleAutoClaim(player, fp, pos);

        Faction owner = plugin.claims().getOwner(pos);
        if (!owner.getId().equals(fp.getLastTerritoryId())) {
            boolean first = fp.getLastTerritoryId() == null;
            Faction previous = first ? null : plugin.factions().getById(fp.getLastTerritoryId());
            fp.setLastTerritoryId(owner.getId());
            if (!first && plugin.settings().notificationsEnabled && fp.isTerritoryNotifications()) {
                notify(player, fp, owner, previous);
            }
        }
        if (fp.isAutoMap()) {
            plugin.map().send(player, fp);
        }
    }

    private void handleAutoClaim(Player player, FPlayer fp, ChunkPos pos) {
        AutoClaimMode mode = fp.getAutoClaim();
        if (mode == AutoClaimMode.NONE) {
            return;
        }
        Faction zone = plugin.factions().getById(fp.getAutoClaimTarget());
        if (zone != null && zone.isSystem()) {
            int changed;
            if (mode == AutoClaimMode.CLAIM) {
                plugin.claims().claimZone(zone, pos, fp.getUniqueId());
                changed = 1;
            } else {
                changed = plugin.claims().unclaimZone(zone, pos) ? 1 : 0;
            }
            if (changed > 0) {
                plugin.msg().send(player, mode == AutoClaimMode.CLAIM ? "zone.claimed" : "zone.unclaimed",
                        "count", changed, "zone", plugin.settings().zoneColor(zone.getId()) + zone.getTag());
            }
            return;
        }
        Faction faction = plugin.factions().getFaction(fp);
        if (faction == null) {
            fp.setAutoClaim(AutoClaimMode.NONE, null);
            return;
        }
        ClaimResult result = mode == AutoClaimMode.CLAIM
                ? plugin.claims().claim(player, fp, faction, pos)
                : plugin.claims().unclaim(player, fp, pos);
        result.send(plugin.msg(), player);
    }

    public void notify(Player player, FPlayer fp, Faction owner) {
        notify(player, fp, owner, null);
    }

    /** @param previous territoire quitté (null si inconnu) */
    public void notify(Player player, FPlayer fp, Faction owner, Faction previous) {
        Settings s = plugin.settings();
        Messages msg = plugin.msg();
        TerritoryType type = plugin.factions().getTerritoryType(fp, owner);
        Object[] kv = {
                "color", plugin.factions().getColor(fp, owner),
                "faction", owner.getTag(),
                "description", plugin.factions().getDescription(owner),
                "previous_color", previous == null ? "" : plugin.factions().getColor(fp, previous),
                "previous", previous == null ? "" : previous.getTag()
        };
        for (String display : s.notificationDisplays) {
            if ("TOAST".equals(display)) {
                toast(player, type, kv);
            } else if ("TITLE".equals(display)) {
                Nms.title(player, msg.format(template(type, "title"), kv), msg.format(template(type, "subtitle"), kv),
                        s.titleFadeIn, s.titleStay, s.titleFadeOut);
            } else if ("ACTIONBAR".equals(display)) {
                Nms.actionBar(player, msg.format(template(type, "actionbar"), kv));
            } else if ("CHAT".equals(display)) {
                msg.sendLine(player, msg.format(template(type, "chat"), kv));
            }
        }
    }

    /**
     * Toast KoriaMC : variables %var_territoire_...% puis notification au tick suivant (le temps que les
     * variables arrivent). Sans le client KoriaMC : affichage de secours (barre d'action par défaut).
     */
    private void toast(final Player player, TerritoryType type, Object[] kv) {
        final Settings s = plugin.settings();
        Messages msg = plugin.msg();
        if (!gui.canDisplay(player)) {
            if ("ACTIONBAR".equals(s.toastFallback)) {
                Nms.actionBar(player, msg.format(template(type, "actionbar"), kv));
            } else if ("CHAT".equals(s.toastFallback)) {
                msg.sendLine(player, msg.format(template(type, "chat"), kv));
            } else if ("TITLE".equals(s.toastFallback)) {
                Nms.title(player, msg.format(template(type, "title"), kv), msg.format(template(type, "subtitle"), kv),
                        s.titleFadeIn, s.titleStay, s.titleFadeOut);
            }
            return;
        }
        String subtitle = toastText(type, "sous-titre");
        if (kv[kv.length - 1].toString().isEmpty() && subtitle.contains("{previous}")) {
            // territoire quitté inconnu : ligne d'information par défaut
            subtitle = toastText(null, "sous-titre");
        }
        String subtitleText = shorten(Text.color(msg.format(subtitle, kv)), 44);
        gui.setVariable(player, "territoire_categorie", Text.color(msg.format(toastText(type, "categorie"), kv)));
        gui.setVariable(player, "territoire_titre", shorten(Text.color(msg.format(toastText(type, "titre"), kv)), 30));
        gui.setVariable(player, "territoire_sous_titre", subtitleText);
        gui.setVariable(player, "territoire_a_sous_titre", String.valueOf(!org.bukkit.ChatColor.stripColor(subtitleText).trim().isEmpty()));
        String icon = s.toastIcons.get(type.name());
        gui.setVariable(player, "territoire_icone", icon == null || icon.isEmpty() ? "minecraft:map" : icon);
        String name = type.name().toLowerCase(Locale.ROOT);
        gui.setVariable(player, "territoire_type", name);
        for (TerritoryType t : TerritoryType.values()) {
            gui.setVariable(player, "territoire_" + t.name().toLowerCase(Locale.ROOT), String.valueOf(t == type));
        }
        Bukkit.getScheduler().runTask(plugin, new Runnable() {
            @Override
            public void run() {
                if (player.isOnline() && !gui.notify(player, s.toastLayout, s.toastDurationMs)) {
                    plugin.getLogger().warning("Le toast " + s.toastLayout + " n'a pas pu s'afficher : le fichier koriamc/guis/"
                            + s.toastLayout + ".json existe-t-il ?");
                }
            }
        });
    }

    /**
     * Texte du toast (messages.json : territory.toast.&lt;TYPE&gt;.&lt;champ&gt;, sinon territory.toast.defaut).
     *
     * @param type null pour le texte par défaut
     */
    private String toastText(TerritoryType type, String field) {
        Messages msg = plugin.msg();
        if (type != null) {
            String own = msg.config().getString("territory.toast." + type.name() + "." + field);
            if (own != null) {
                return own;
            }
        }
        String value = msg.config().getString("territory.toast.defaut." + field);
        return value == null ? "" : value;
    }

    /** Coupe un texte coloré à max caractères visibles (avec "..."). */
    private static String shorten(String text, int max) {
        StringBuilder out = new StringBuilder();
        int visible = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == org.bukkit.ChatColor.COLOR_CHAR && i + 1 < text.length()) {
                out.append(c).append(text.charAt(++i));
                continue;
            }
            if (visible >= max) {
                return out.toString().trim() + "...";
            }
            out.append(c);
            visible++;
        }
        return out.toString();
    }

    /** Texte surchargé pour un type de territoire, sinon le texte général. */
    private String template(TerritoryType type, String field) {
        Messages msg = plugin.msg();
        String override = "territory.overrides." + type.name() + "." + field;
        if (msg.config().isSet(override)) {
            return msg.config().getString(override);
        }
        String value = msg.config().getString("territory." + field);
        return value == null ? "" : value;
    }
}
