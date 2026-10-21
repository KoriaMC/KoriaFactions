package fr.koriamc.factions.manager;

import fr.koriamc.factions.KoriaFactions;
import fr.koriamc.factions.model.Faction;
import fr.koriamc.factions.util.Text;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Classements des factions, mis en cache quelques secondes.
 */
public final class TopManager {

    public static final String[] CATEGORIES = {"POWER", "CLAIMS", "MEMBERS", "POINTS"};

    private final KoriaFactions plugin;
    private final Map<String, List<Faction>> cache = new HashMap<String, List<Faction>>();
    private final Map<String, Long> cacheTimes = new HashMap<String, Long>();

    public TopManager(KoriaFactions plugin) {
        this.plugin = plugin;
    }

    public void invalidate() {
        cache.clear();
        cacheTimes.clear();
    }

    public boolean isCategory(String category) {
        return plugin.settings().topCategories.contains(category) && Arrays.asList(CATEGORIES).contains(category);
    }

    public List<Faction> get(final String category) {
        Long time = cacheTimes.get(category);
        long ttl = plugin.settings().topCacheSeconds * 1000L;
        List<Faction> cached = cache.get(category);
        if (cached != null && time != null && System.currentTimeMillis() - time < ttl) {
            return cached;
        }
        List<Faction> list = plugin.factions().getNormalFactions();
        final Map<String, Double> values = new HashMap<String, Double>();
        for (Faction f : list) {
            values.put(f.getId(), value(f, category));
        }
        Collections.sort(list, new Comparator<Faction>() {
            @Override
            public int compare(Faction a, Faction b) {
                int c = Double.compare(values.get(b.getId()), values.get(a.getId()));
                return c != 0 ? c : String.CASE_INSENSITIVE_ORDER.compare(a.getTag(), b.getTag());
            }
        });
        list = Collections.unmodifiableList(new ArrayList<Faction>(list));
        cache.put(category, list);
        cacheTimes.put(category, System.currentTimeMillis());
        return list;
    }

    public double value(Faction f, String category) {
        switch (category) {
            case "POWER":
                return plugin.factions().getPower(f);
            case "CLAIMS":
                return plugin.claims().count(f.getId());
            case "MEMBERS":
                return f.getSize();
            case "POINTS":
                return f.getPoints();
            default:
                return 0;
        }
    }

    public String format(Faction f, String category) {
        double v = value(f, category);
        switch (category) {
            case "POWER":
                return Text.power(v);
            default:
                return Text.number(v, 0);
        }
    }
}
