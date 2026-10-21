package fr.koriamc.factions.manager;

import fr.koriamc.factions.KoriaFactions;
import fr.koriamc.factions.config.Settings;
import fr.koriamc.factions.model.FPlayer;
import fr.koriamc.factions.model.Faction;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Registre des joueurs connus du plugin.
 */
public final class PlayerManager {

    private final KoriaFactions plugin;
    private final Map<UUID, FPlayer> players = new HashMap<UUID, FPlayer>();
    private final Map<String, UUID> byName = new HashMap<String, UUID>();

    public PlayerManager(KoriaFactions plugin) {
        this.plugin = plugin;
    }

    public void load(Collection<FPlayer> list) {
        players.clear();
        byName.clear();
        for (FPlayer fp : list) {
            fp.ensureInit();
            players.put(fp.getUniqueId(), fp);
            if (fp.getName() != null) {
                byName.put(fp.getName().toLowerCase(Locale.ROOT), fp.getUniqueId());
            }
        }
    }

    public FPlayer get(UUID uuid) {
        return players.get(uuid);
    }

    public FPlayer get(Player player) {
        return getOrCreate(player.getUniqueId(), player.getName());
    }

    public FPlayer getOrCreate(UUID uuid, String name) {
        FPlayer fp = players.get(uuid);
        if (fp == null) {
            Settings s = plugin.settings();
            fp = new FPlayer(uuid, name, s.powerDefault, s.notificationsDefault);
            players.put(uuid, fp);
            if (name != null) {
                byName.put(name.toLowerCase(Locale.ROOT), uuid);
            }
        }
        return fp;
    }

    /**
     * Recherche un joueur par pseudo : joueurs connectés (nom exact puis début
     * de nom), puis joueurs enregistrés.
     */
    public FPlayer getByName(String name) {
        if (name == null || name.isEmpty()) {
            return null;
        }
        Player exact = Bukkit.getPlayerExact(name);
        if (exact != null) {
            return get(exact);
        }
        UUID uuid = byName.get(name.toLowerCase(Locale.ROOT));
        if (uuid != null) {
            return players.get(uuid);
        }
        Player partial = Bukkit.getPlayer(name);
        return partial == null ? null : get(partial);
    }

    public void updateName(FPlayer fp, String name) {
        if (name == null || name.equals(fp.getName())) {
            return;
        }
        if (fp.getName() != null) {
            byName.remove(fp.getName().toLowerCase(Locale.ROOT));
        }
        fp.setName(name);
        byName.put(name.toLowerCase(Locale.ROOT), fp.getUniqueId());
    }

    public void remove(FPlayer fp) {
        players.remove(fp.getUniqueId());
        if (fp.getName() != null) {
            byName.remove(fp.getName().toLowerCase(Locale.ROOT));
        }
    }

    public Collection<FPlayer> getAll() {
        return Collections.unmodifiableCollection(players.values());
    }

    public List<FPlayer> getOnline() {
        List<FPlayer> list = new ArrayList<FPlayer>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            list.add(get(p));
        }
        return list;
    }

    public Faction getFaction(FPlayer fp) {
        return fp == null || fp.getFactionId() == null ? null : plugin.factions().getById(fp.getFactionId());
    }

    // ----------------------------------------------------------------- Power

    /** Puissance maximale selon les permissions du joueur connecté. */
    public double computeBaseMaxPower(Player player) {
        Settings s = plugin.settings();
        double max = s.powerMax;
        for (Map.Entry<String, Double> e : s.powerMaxByPermission.entrySet()) {
            if (e.getValue() > max && player.hasPermission(e.getKey())) {
                max = e.getValue();
            }
        }
        return max;
    }

    /** Puissance maximale. Hors ligne, la dernière valeur connue est utilisée. */
    public double getMaxPower(FPlayer fp) {
        Player player = fp.getPlayer();
        if (player != null) {
            double max = computeBaseMaxPower(player);
            fp.setCachedMaxPower(max);
            return max;
        }
        return fp.getCachedMaxPower() > 0 ? fp.getCachedMaxPower() : plugin.settings().powerMax;
    }

    /** Ajoute (ou retire) de la puissance en respectant les bornes. */
    public void addPower(FPlayer fp, double delta) {
        setPower(fp, fp.getPower() + delta);
    }

    public void setPower(FPlayer fp, double value) {
        double max = getMaxPower(fp);
        double min = plugin.settings().powerMin;
        fp.setPower(Math.max(min, Math.min(max, value)));
    }
}
