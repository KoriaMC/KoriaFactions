package fr.koriamc.factions.model;

import java.util.HashMap;
import java.util.Map;

/**
 * Entrée du journal de faction. Le texte est rendu à l'affichage depuis
 * messages.json (logs.entries.TYPE) avec les variables enregistrées.
 */
public final class LogEntry {

    private long time;
    private String type;
    private Map<String, String> args = new HashMap<String, String>();

    public LogEntry() {
    }

    public LogEntry(String type, Object... kv) {
        this.time = System.currentTimeMillis();
        this.type = type;
        for (int i = 0; i + 1 < kv.length; i += 2) {
            args.put(String.valueOf(kv[i]), kv[i + 1] == null ? "" : String.valueOf(kv[i + 1]));
        }
    }

    public long getTime() {
        return time;
    }

    public String getType() {
        return type;
    }

    public Object[] toReplacements() {
        Object[] kv = new Object[args == null ? 0 : args.size() * 2];
        int i = 0;
        if (args != null) {
            for (Map.Entry<String, String> e : args.entrySet()) {
                kv[i++] = e.getKey();
                kv[i++] = e.getValue();
            }
        }
        return kv;
    }
}
