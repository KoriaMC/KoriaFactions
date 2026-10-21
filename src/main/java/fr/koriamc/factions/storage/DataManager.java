package fr.koriamc.factions.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import fr.koriamc.factions.KoriaFactions;
import fr.koriamc.factions.config.Settings;
import fr.koriamc.factions.model.ChunkPos;
import fr.koriamc.factions.model.Claim;
import fr.koriamc.factions.model.FPlayer;
import fr.koriamc.factions.model.Faction;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * Chargement et sauvegarde des données. Les objets sont sérialisés en JSON sur
 * le thread principal, puis écrits par un thread dédié.
 */
public final class DataManager {

    private final KoriaFactions plugin;
    private Storage storage;
    private Gson gson;
    private ExecutorService executor;
    private BukkitTask autoSave;
    /** Tant que le chargement n'a pas réussi, rien n'est écrit (évite d'écraser les données). */
    private boolean loaded;

    public DataManager(KoriaFactions plugin) {
        this.plugin = plugin;
    }

    public void init() throws Exception {
        Settings s = plugin.settings();
        GsonBuilder builder = new GsonBuilder().disableHtmlEscaping();
        File folder = plugin.getDataFolder();
        if ("MYSQL".equals(s.storageType)) {
            storage = SqlStorage.mysql(s.mysqlHost, s.mysqlPort, s.mysqlDatabase, s.mysqlUser, s.mysqlPassword,
                    s.mysqlPrefix, s.mysqlParameters);
        } else if ("SQLITE".equals(s.storageType)) {
            storage = SqlStorage.sqlite(new File(folder, s.sqliteFile));
        } else {
            if (!"JSON".equals(s.storageType)) {
                plugin.getLogger().warning("Type de stockage inconnu '" + s.storageType + "', JSON utilisé.");
            }
            if (s.jsonPretty) {
                builder.setPrettyPrinting();
            }
            storage = new JsonStorage(new File(folder, "data"), s.jsonBackups, plugin.getLogger());
        }
        gson = builder.create();
        storage.init();
        executor = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "KoriaFactions-Save");
            thread.setDaemon(true);
            return thread;
        });
    }

    public boolean isLoaded() {
        return loaded;
    }

    public String getStorageName() {
        return storage == null ? "?" : storage.getName();
    }

    public void load() throws Exception {
        DataSnapshot data = storage.load();

        List<FPlayer> players = new ArrayList<FPlayer>();
        for (Map.Entry<String, String> e : data.players.entrySet()) {
            try {
                FPlayer fp = gson.fromJson(e.getValue(), FPlayer.class);
                if (fp != null) {
                    fp.getUniqueId();
                    players.add(fp);
                }
            } catch (RuntimeException ex) {
                plugin.getLogger().warning("Joueur illisible ignoré (" + e.getKey() + ") : " + ex.getMessage());
            }
        }
        List<Faction> factions = new ArrayList<Faction>();
        for (Map.Entry<String, String> e : data.factions.entrySet()) {
            try {
                Faction f = gson.fromJson(e.getValue(), Faction.class);
                if (f != null) {
                    factions.add(f);
                }
            } catch (RuntimeException ex) {
                plugin.getLogger().warning("Faction illisible ignorée (" + e.getKey() + ") : " + ex.getMessage());
            }
        }
        Map<ChunkPos, Claim> claims = new HashMap<ChunkPos, Claim>();
        for (Map.Entry<String, String> e : data.claims.entrySet()) {
            ChunkPos pos = ChunkPos.parse(e.getKey());
            if (pos == null) {
                continue;
            }
            try {
                Claim claim = gson.fromJson(e.getValue(), Claim.class);
                if (claim != null && claim.getFactionId() != null) {
                    claims.put(pos, claim);
                }
            } catch (RuntimeException ex) {
                plugin.getLogger().warning("Claim illisible ignoré (" + e.getKey() + ") : " + ex.getMessage());
            }
        }

        plugin.players().load(players);
        plugin.factions().load(factions);
        plugin.claims().load(claims);
        for (Faction f : plugin.factions().getNormalFactions()) {
            f.setWasRaidable(plugin.factions().isRaidable(f));
        }
        loaded = true;
        plugin.getLogger().info("Données chargées (" + getStorageName() + ") : " + plugin.factions().getNormalFactions().size()
                + " faction(s), " + players.size() + " joueur(s), " + plugin.claims().size() + " claim(s).");
    }

    /** Copie sérialisée de toutes les données (thread principal). */
    public DataSnapshot snapshot() {
        plugin.chests().saveAll();
        DataSnapshot snapshot = new DataSnapshot();
        for (Faction f : plugin.factions().getAll()) {
            snapshot.factions.put(f.getId(), gson.toJson(f));
        }
        for (FPlayer fp : plugin.players().getAll()) {
            snapshot.players.put(fp.getUniqueId().toString(), gson.toJson(fp));
        }
        for (Map.Entry<ChunkPos, Claim> e : plugin.claims().getAll().entrySet()) {
            snapshot.claims.put(e.getKey().key(), gson.toJson(e.getValue()));
        }
        return snapshot;
    }

    public void saveAsync() {
        if (!loaded || executor == null || executor.isShutdown()) {
            return;
        }
        final DataSnapshot snapshot = snapshot();
        executor.submit(() -> write(snapshot));
    }

    private void write(DataSnapshot snapshot) {
        try {
            storage.save(snapshot);
            if (plugin.settings().debug) {
                plugin.getLogger().info("Sauvegarde effectuée.");
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Échec de la sauvegarde des données", e);
        }
    }

    public void startAutoSave() {
        stopAutoSave();
        int minutes = plugin.settings().autoSaveMinutes;
        if (minutes <= 0) {
            return;
        }
        long period = minutes * 60L * 20L;
        autoSave = Bukkit.getScheduler().runTaskTimer(plugin, this::saveAsync, period, period);
    }

    public void stopAutoSave() {
        if (autoSave != null) {
            autoSave.cancel();
            autoSave = null;
        }
    }

    /** Sauvegarde synchrone finale et fermeture du stockage. */
    public void shutdown() {
        stopAutoSave();
        if (storage == null) {
            return;
        }
        if (!loaded) {
            storage.close();
            return;
        }
        DataSnapshot snapshot = snapshot();
        if (executor != null) {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                    plugin.getLogger().warning("Des sauvegardes asynchrones n'ont pas pu se terminer à temps.");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        write(snapshot);
        storage.close();
    }
}
