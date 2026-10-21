package fr.koriamc.factions.manager;

import fr.koriamc.factions.KoriaFactions;
import fr.koriamc.factions.api.event.FactionDisbandEvent;
import fr.koriamc.factions.api.event.FactionMembershipEvent;
import fr.koriamc.factions.config.Settings;
import fr.koriamc.factions.model.FPlayer;
import fr.koriamc.factions.model.Faction;
import fr.koriamc.factions.model.FactionFlag;
import fr.koriamc.factions.model.Role;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;

/**
 * Expulsion des joueurs inactifs et dissolution des factions abandonnées.
 */
public final class InactivityManager {

    private static final long DAY = 24L * 60L * 60L * 1000L;

    private final KoriaFactions plugin;
    private BukkitTask task;

    public InactivityManager(KoriaFactions plugin) {
        this.plugin = plugin;
    }

    public void start() {
        stop();
        Settings s = plugin.settings();
        if (!s.inactivityEnabled) {
            return;
        }
        long period = s.inactivityIntervalMinutes * 60L * 20L;
        task = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            @Override
            public void run() {
                check();
            }
        }, 20L * 60L, period);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    private static boolean inactive(FPlayer fp, long now, long threshold) {
        return !fp.isOnline() && fp.getLastSeen() > 0 && now - fp.getLastSeen() > threshold;
    }

    public void check() {
        Settings s = plugin.settings();
        FactionManager fm = plugin.factions();
        long now = System.currentTimeMillis();
        long kickAfter = s.inactivityKickDays * DAY;
        long disbandAfter = s.inactivityDisbandDays * DAY;

        for (Faction f : fm.getNormalFactions()) {
            List<FPlayer> members = fm.getMembers(f);
            if (members.isEmpty()) {
                continue;
            }
            if (s.inactivityDisbandDays > 0 && !fm.getFlag(f, FactionFlag.PERMANENT)) {
                boolean allInactive = true;
                for (FPlayer fp : members) {
                    if (!inactive(fp, now, disbandAfter)) {
                        allInactive = false;
                        break;
                    }
                }
                if (allInactive) {
                    if (fm.disband(f, null, null, FactionDisbandEvent.Reason.INACTIVITY, true)) {
                        plugin.msg().broadcast("inactivity.disbanded", "faction", f.getTag());
                    }
                    continue;
                }
            }
            if (s.inactivityKickDays <= 0) {
                continue;
            }
            for (FPlayer fp : new ArrayList<FPlayer>(members)) {
                if (!inactive(fp, now, kickAfter) || !f.getMemberIds().contains(fp.getUniqueId())) {
                    continue;
                }
                if (fp.getRole() == Role.LEADER) {
                    if (!s.inactivityTransferLeader) {
                        continue;
                    }
                    FPlayer heir = activeHeir(f, fp, now, kickAfter);
                    if (heir == null) {
                        continue;
                    }
                    fm.setLeader(f, heir);
                    fm.notifyMembers(f, "inactivity.leader-transfer", "player", heir.getName());
                }
                fm.removeMember(f, fp, FactionMembershipEvent.Action.FORCED_LEAVE);
                fm.notifyMembers(f, "inactivity.kicked", "player", fp.getName());
                if (!fm.getAll().contains(f)) {
                    break;
                }
            }
        }

        if (s.inactivityPurgeDays > 0) {
            long purgeAfter = s.inactivityPurgeDays * DAY;
            for (FPlayer fp : new ArrayList<FPlayer>(plugin.players().getAll())) {
                if (!fp.hasFaction() && inactive(fp, now, purgeAfter)) {
                    plugin.players().remove(fp);
                }
            }
        }
    }

    private FPlayer activeHeir(Faction f, FPlayer leader, long now, long threshold) {
        FPlayer best = null;
        for (FPlayer fp : plugin.factions().getMembers(f)) {
            if (fp == leader || inactive(fp, now, threshold)) {
                continue;
            }
            if (best == null || fp.getRole().isHigherThan(best.getRole())) {
                best = fp;
            }
        }
        return best;
    }
}
