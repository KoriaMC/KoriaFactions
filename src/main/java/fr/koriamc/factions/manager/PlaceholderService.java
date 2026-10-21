package fr.koriamc.factions.manager;

import fr.koriamc.factions.KoriaFactions;
import fr.koriamc.factions.config.Messages;
import fr.koriamc.factions.model.FPlayer;
import fr.koriamc.factions.model.Faction;
import fr.koriamc.factions.model.FactionFlag;
import fr.koriamc.factions.model.SerialLoc;
import fr.koriamc.factions.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Résolution des variables {cle} internes, pour un joueur ou pour une faction.
 */
public final class PlaceholderService {

    private final KoriaFactions plugin;

    public PlaceholderService(KoriaFactions plugin) {
        this.plugin = plugin;
    }

    // ================================================================ Joueur

    /** @return la valeur d'une variable de joueur, ou null si elle est inconnue. */
    public String resolvePlayer(String key, Player player, FPlayer fp) {
        Faction f = plugin.factions().getFaction(fp);
        Messages msg = plugin.msg();
        switch (key) {
            case "player":
                return fp.getName();
            case "faction":
                return f == null ? msg.text("values.no-faction") : f.getTag();
            case "role":
                return f == null ? "" : plugin.roles().name(fp.getRole());
            case "role_prefix":
                return f == null ? "" : plugin.roles().prefix(fp.getRole());
            case "role_color":
                return f == null ? "" : plugin.roles().color(fp.getRole());
            case "title":
                return fp.getTitle() == null ? "" : fp.getTitle();
            case "power":
                return Text.power(fp.getPower());
            case "max_power":
                return Text.power(plugin.players().getMaxPower(fp));
            case "faction_power":
                return f == null ? "0" : Text.power(plugin.factions().getPower(f));
            case "faction_max_power":
                return f == null ? "0" : Text.power(plugin.factions().getMaxPower(f));
            case "faction_points":
                return f == null ? "0" : String.valueOf(f.getPoints());
            case "claims":
                return f == null ? "0" : String.valueOf(plugin.claims().count(f.getId()));
            case "max_claims":
                return f == null ? "0" : maxClaims(f);
            case "members_online":
                return f == null ? "0" : String.valueOf(plugin.factions().countOnline(f));
            case "members":
                return f == null ? "0" : String.valueOf(f.getSize());
            case "max_members":
                return f == null ? "0" : maxMembers(f);
            case "kills":
                return String.valueOf(fp.getKills());
            case "deaths":
                return String.valueOf(fp.getDeaths());
            case "kdr":
                return Text.number(fp.getKills() / (double) Math.max(1, fp.getDeaths()), 2);
            case "territory":
                return player == null ? "" : plugin.claims().getOwner(player.getLocation()).getTag();
            case "territory_color":
                return player == null ? "" : plugin.factions().getColor(fp, plugin.claims().getOwner(player.getLocation()));
            case "balance":
                return player == null ? "" : plugin.economy().format(plugin.economy().getBalance(player));
            case "world":
                return player == null ? "" : player.getWorld().getName();
            case "x":
                return player == null ? "" : String.valueOf(player.getLocation().getBlockX());
            case "y":
                return player == null ? "" : String.valueOf(player.getLocation().getBlockY());
            case "z":
                return player == null ? "" : String.valueOf(player.getLocation().getBlockZ());
            case "online_players":
                return String.valueOf(Bukkit.getOnlinePlayers().size());
            case "raidable":
                return f == null ? "" : msg.yesNo(plugin.factions().isRaidable(f));
            case "chat_mode":
                return msg.text("chat.modes." + fp.getChatMode().name());
            case "label":
                return plugin.commands().getLabel();
            default:
                return null;
        }
    }

    /** Remplace toutes les variables {cle} de joueur connues dans un texte. */
    public String applyPlayer(String text, Player player, FPlayer fp) {
        if (text == null || text.indexOf('{') < 0) {
            return text;
        }
        StringBuilder out = new StringBuilder(text.length() + 16);
        int i = 0;
        while (i < text.length()) {
            char c = text.charAt(i);
            if (c == '{') {
                int end = text.indexOf('}', i + 1);
                if (end > i) {
                    String value = resolvePlayer(text.substring(i + 1, end), player, fp);
                    if (value != null) {
                        out.append(value);
                        i = end + 1;
                        continue;
                    }
                }
            }
            out.append(c);
            i++;
        }
        return out.toString();
    }

    /** Variables PlaceholderAPI (si le plugin est présent). */
    public String applyExternal(Player player, String text) {
        return plugin.papi() == null || text == null ? text : plugin.papi().apply(player, text);
    }

    private String maxClaims(Faction f) {
        int max = plugin.factions().getMaxLand(f);
        return max >= Integer.MAX_VALUE / 4 ? plugin.msg().text("values.unlimited") : String.valueOf(max);
    }

    private String maxMembers(Faction f) {
        int max = plugin.factions().getMaxMembers(f);
        return max == Integer.MAX_VALUE ? plugin.msg().text("values.unlimited") : String.valueOf(max);
    }

    // =============================================================== Faction

    /**
     * Toutes les variables d'une faction vue par un joueur (peut être null),
     * sous forme de paires pour {@link Messages}.
     */
    public Object[] faction(Faction f, FPlayer viewer) {
        Messages msg = plugin.msg();
        FactionManager fm = plugin.factions();
        FPlayer leader = fm.getLeader(f);
        String home;
        SerialLoc h = f.getHome();
        if (h == null) {
            home = msg.text("show.home-none");
        } else {
            home = msg.text("show.home-format", "world", h.getWorld(), "x", h.getBlockX(), "y", h.getBlockY(), "z", h.getBlockZ());
        }
        List<String> online = new ArrayList<String>();
        List<String> offline = new ArrayList<String>();
        for (FPlayer member : fm.getMembers(f)) {
            String entry = msg.text("show.member-format",
                    "role_color", plugin.roles().color(member.getRole()),
                    "role_prefix", plugin.roles().prefix(member.getRole()),
                    "role", plugin.roles().name(member.getRole()),
                    "title", member.getTitle() == null ? "" : member.getTitle(),
                    "power", Text.power(member.getPower()),
                    "player", member.getName());
            (member.isOnline() ? online : offline).add(entry);
        }
        String separator = msg.text("show.separator");
        String empty = msg.text("show.empty-list");
        boolean raidable = fm.isRaidable(f);
        return new Object[]{
                "faction", f.getTag(),
                "relation_color", fm.getColor(viewer, f),
                "description", fm.getDescription(f),
                "motd", f.getMotd() == null ? "" : f.getMotd(),
                "age", msg.since(f.getCreatedAt()),
                "open", msg.yesNo(fm.getFlag(f, FactionFlag.OPEN)),
                "peaceful", msg.yesNo(fm.getFlag(f, FactionFlag.PEACEFUL)),
                "leader", leader == null ? msg.text("values.none") : leader.getName(),
                "power", Text.power(fm.getPower(f)),
                "max_power", Text.power(fm.getMaxPower(f)),
                "points", f.getPoints(),
                "claims", plugin.claims().count(f.getId()),
                "max_claims", f.isNormal() ? maxClaims(f) : "0",
                "members", f.getSize(),
                "max_members", f.isNormal() ? maxMembers(f) : "0",
                "members_online", fm.countOnline(f),
                "home", home,
                "warps", f.getWarps().size(),
                "raidable", raidable ? msg.text("show.raidable") : msg.text("show.not-raidable"),
                "online", online.isEmpty() ? empty : Text.join(online, separator),
                "online_count", online.size(),
                "offline", offline.isEmpty() ? empty : Text.join(offline, separator),
                "offline_count", offline.size()
        };
    }


    /** Coordonnées d'une position pour les messages. */
    public static Object[] location(Location l) {
        return new Object[]{"world", l.getWorld().getName(), "x", l.getBlockX(), "y", l.getBlockY(), "z", l.getBlockZ()};
    }
}
