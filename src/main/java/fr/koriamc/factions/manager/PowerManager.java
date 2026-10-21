package fr.koriamc.factions.manager;

import fr.koriamc.factions.KoriaFactions;
import fr.koriamc.factions.api.event.PowerLossEvent;
import fr.koriamc.factions.config.Settings;
import fr.koriamc.factions.model.FPlayer;
import fr.koriamc.factions.model.Faction;
import fr.koriamc.factions.model.FactionFlag;
import fr.koriamc.factions.model.TerritoryType;
import fr.koriamc.factions.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Régénération et perte de puissance.
 */
public final class PowerManager {

    private final KoriaFactions plugin;
    private BukkitTask task;

    public PowerManager(KoriaFactions plugin) {
        this.plugin = plugin;
    }

    public void start() {
        stop();
        final int interval = plugin.settings().regenIntervalSeconds;
        task = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            @Override
            public void run() {
                tick(interval);
            }
        }, interval * 20L, interval * 20L);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    private void tick(int intervalSeconds) {
        Settings s = plugin.settings();
        double gain = s.regenOnline * intervalSeconds / 60.0;
        Set<Faction> touched = new HashSet<Faction>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            FPlayer fp = plugin.players().get(player);
            if (gain != 0 && !s.regenDisabledWorlds.contains(player.getWorld().getName().toLowerCase(Locale.ROOT))) {
                plugin.players().addPower(fp, gain);
            } else {
                // recalcule tout de même la puissance max (permissions modifiées)
                plugin.players().setPower(fp, fp.getPower());
            }
            Faction f = plugin.factions().getFaction(fp);
            if (f != null) {
                touched.add(f);
            }
        }
        for (Faction f : touched) {
            plugin.factions().checkRaidable(f);
        }
    }

    /** Régénération accumulée pendant la déconnexion. */
    public void applyOfflineRegen(FPlayer fp) {
        double perMinute = plugin.settings().regenOffline;
        if (perMinute == 0 || fp.getLastSeen() <= 0) {
            return;
        }
        double minutes = (System.currentTimeMillis() - fp.getLastSeen()) / 60000.0;
        if (minutes > 0) {
            plugin.players().addPower(fp, minutes * perMinute);
        }
    }

    /** Applique la perte de puissance d'une mort et le gain éventuel du tueur. */
    public void handleDeath(Player victim, Player killer) {
        Settings s = plugin.settings();
        FPlayer fp = plugin.players().get(victim);
        Faction faction = plugin.factions().getFaction(fp);
        if (s.deathOnlyPvp && killer == null) {
            return;
        }
        String world = victim.getWorld().getName();
        double loss = s.deathLoss;
        if (s.noLossWorlds.contains(world.toLowerCase(Locale.ROOT)) || s.isWorldDisabled(world)) {
            loss = 0;
        } else if (faction != null && s.peacefulNoLoss && plugin.factions().getFlag(faction, FactionFlag.PEACEFUL)) {
            loss = 0;
        } else {
            Faction territory = plugin.claims().getOwner(victim.getLocation());
            TerritoryType type = plugin.factions().getTerritoryType(fp, territory);
            Double multiplier = s.lossMultipliers.get(type);
            loss *= multiplier == null ? 1.0 : multiplier;
        }
        if (loss > 0) {
            PowerLossEvent event = new PowerLossEvent(faction, fp, loss);
            Bukkit.getPluginManager().callEvent(event);
            loss = event.isCancelled() ? 0 : event.getLoss();
        }
        if (loss > 0) {
            plugin.players().addPower(fp, -loss);
            plugin.msg().send(victim, "power.death", "loss", Text.power(loss),
                    "power", Text.power(fp.getPower()), "max_power", Text.power(plugin.players().getMaxPower(fp)));
        } else {
            plugin.msg().send(victim, "power.death-no-loss");
        }
        if (faction != null) {
            plugin.factions().checkRaidable(faction);
        }
        if (killer != null && killer != victim && s.killGain > 0) {
            FPlayer kfp = plugin.players().get(killer);
            plugin.players().addPower(kfp, s.killGain);
            plugin.msg().send(killer, "power.kill", "gain", Text.power(s.killGain));
            Faction kf = plugin.factions().getFaction(kfp);
            if (kf != null) {
                plugin.factions().checkRaidable(kf);
            }
        }
    }
}
