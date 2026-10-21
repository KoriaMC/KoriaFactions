package fr.koriamc.factions.api;

import fr.koriamc.factions.KoriaFactions;
import fr.koriamc.factions.model.FPlayer;
import fr.koriamc.factions.model.Faction;
import fr.koriamc.factions.model.Relation;
import fr.koriamc.factions.model.TerritoryType;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Collection;

/**
 * Accès simplifié pour les autres plugins KoriaMC (quêtes, métiers, événements...).
 * <pre>
 * Faction f = KoriaFactionsAPI.getFaction(player);
 * if (f != null &amp;&amp; KoriaFactionsAPI.isInOwnTerritory(player)) { ... }
 * </pre>
 */
public final class KoriaFactionsAPI {

    private KoriaFactionsAPI() {
    }

    private static KoriaFactions plugin() {
        KoriaFactions plugin = KoriaFactions.get();
        if (plugin == null) {
            throw new IllegalStateException("KoriaFactions n'est pas activé");
        }
        return plugin;
    }

    public static boolean isAvailable() {
        return KoriaFactions.get() != null;
    }

    public static FPlayer getPlayer(Player player) {
        return plugin().players().get(player);
    }

    /** @return la faction du joueur, ou null s'il n'en a pas. */
    public static Faction getFaction(Player player) {
        return plugin().factions().getFaction(player);
    }

    public static Faction getFactionByTag(String tag) {
        return plugin().factions().getByTag(tag);
    }

    public static Faction getFactionById(String id) {
        return plugin().factions().getById(id);
    }

    /** @return le propriétaire du territoire (jamais null : wilderness par défaut). */
    public static Faction getFactionAt(Location location) {
        return plugin().claims().getOwner(location);
    }

    public static Collection<Faction> getFactions() {
        return plugin().factions().getNormalFactions();
    }

    public static Relation getRelation(Player a, Player b) {
        return plugin().factions().getRelation(getPlayer(a), getPlayer(b));
    }

    /** @return le type de territoire d'une position vu par ce joueur (WILDERNESS, SAFEZONE, WARZONE, OWN, ENEMY). */
    public static TerritoryType getTerritoryType(Player player, Location location) {
        return plugin().factions().getTerritoryType(getPlayer(player), getFactionAt(location));
    }

    public static boolean isInOwnTerritory(Player player) {
        Faction own = getFaction(player);
        return own != null && getFactionAt(player.getLocation()) == own;
    }

    public static boolean isInSafeZone(Location location) {
        return getFactionAt(location).isSafeZone();
    }

    public static boolean isInWarZone(Location location) {
        return getFactionAt(location).isWarZone();
    }

    public static double getPower(Player player) {
        return getPlayer(player).getPower();
    }

    public static double getFactionPower(Faction faction) {
        return plugin().factions().getPower(faction);
    }

    public static boolean isRaidable(Faction faction) {
        return plugin().factions().isRaidable(faction);
    }

    public static long getPoints(Faction faction) {
        return faction == null ? 0 : faction.getPoints();
    }

    /**
     * Crédite (ou retire si négatif) des points de faction.
     *
     * @param reason origine affichée aux autres plugins via FactionPointsChangeEvent (ex : "koriaquest")
     * @return le gain réellement appliqué
     */
    public static long addPoints(Faction faction, long amount, String reason) {
        return plugin().factions().addPoints(faction, amount, reason);
    }

    public static void setPoints(Faction faction, long points, String reason) {
        plugin().factions().setPoints(faction, points, reason);
    }

    /** @return le rang de la faction au classement des points (1 = premier), 0 si elle n'y figure pas. */
    public static int getPointsRank(Faction faction) {
        return plugin().top().get("POINTS").indexOf(faction) + 1;
    }
}
