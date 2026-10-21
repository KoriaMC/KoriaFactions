package fr.koriamc.factions.hook;

import fr.koriamc.factions.KoriaFactions;
import fr.koriamc.factions.model.FPlayer;
import fr.koriamc.factions.model.Faction;
import fr.koriamc.factions.model.Relation;
import fr.koriamc.factions.util.Text;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.clip.placeholderapi.expansion.Relational;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;

/**
 * Variables PlaceholderAPI :
 * <ul>
 *   <li>%koriafactions_&lt;variable&gt;% : variables internes (faction, power, claims...)</li>
 *   <li>%koriafactions_top_&lt;categorie&gt;_&lt;rang&gt;_name% / _value : classements</li>
 *   <li>%rel_koriafactions_relation% / %rel_koriafactions_relation_color% : relation entre deux joueurs</li>
 * </ul>
 */
public final class FactionsExpansion extends PlaceholderExpansion implements Relational {

    private final KoriaFactions plugin;

    public FactionsExpansion(KoriaFactions plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getIdentifier() {
        return "koriafactions";
    }

    @Override
    public String getAuthor() {
        return "KoriaMC";
    }

    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public boolean canRegister() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player player, String params) {
        String key = params.toLowerCase(Locale.ROOT);
        if (key.startsWith("top_")) {
            return top(key);
        }
        if (player == null) {
            return "";
        }
        FPlayer fp = plugin.players().get(player);
        String value = plugin.placeholders().resolvePlayer(key, player, fp);
        return value == null ? null : Text.color(value);
    }

    /** top_&lt;categorie&gt;_&lt;rang&gt;_name ou top_&lt;categorie&gt;_&lt;rang&gt;_value */
    private String top(String key) {
        String[] parts = key.split("_");
        if (parts.length != 4) {
            return null;
        }
        String category = parts[1].toUpperCase(Locale.ROOT);
        Integer rank = Text.parseInt(parts[2]);
        if (rank == null || rank < 1) {
            return null;
        }
        List<Faction> list = plugin.top().get(category);
        if (rank > list.size()) {
            return "";
        }
        Faction f = list.get(rank - 1);
        if ("name".equals(parts[3])) {
            return f.getTag();
        }
        if ("value".equals(parts[3])) {
            return plugin.top().format(f, category);
        }
        return null;
    }

    @Override
    public String onPlaceholderRequest(Player one, Player two, String identifier) {
        if (one == null || two == null) {
            return "";
        }
        Relation relation = plugin.factions().getRelation(plugin.players().get(one), plugin.players().get(two));
        if ("relation".equalsIgnoreCase(identifier)) {
            return plugin.settings().relationName(relation);
        }
        if ("relation_color".equalsIgnoreCase(identifier)) {
            return Text.color(plugin.settings().relationColor(relation));
        }
        return null;
    }
}
