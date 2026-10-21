package fr.koriamc.factions.listener;

import fr.koriamc.factions.KoriaFactions;
import fr.koriamc.factions.config.EconomySettings;
import fr.koriamc.factions.config.ProtectionSettings;
import fr.koriamc.factions.config.Settings;
import fr.koriamc.factions.manager.FactionManager;
import fr.koriamc.factions.model.FPlayer;
import fr.koriamc.factions.model.Faction;
import fr.koriamc.factions.model.FactionFlag;
import fr.koriamc.factions.model.Relation;
import fr.koriamc.factions.model.TerritoryType;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * PvP, dégâts, faim, ender pearls, morts et statistiques des joueurs.
 */
public final class CombatListener implements Listener {

    private static final Set<PotionEffectType> HARMFUL = new HashSet<PotionEffectType>(Arrays.asList(
            PotionEffectType.POISON, PotionEffectType.HARM, PotionEffectType.WEAKNESS, PotionEffectType.SLOW,
            PotionEffectType.SLOW_DIGGING, PotionEffectType.BLINDNESS, PotionEffectType.CONFUSION,
            PotionEffectType.HUNGER, PotionEffectType.WITHER));

    private final KoriaFactions plugin;

    public CombatListener(KoriaFactions plugin) {
        this.plugin = plugin;
    }

    // ================================================================ Dégâts

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getEntity();
        if (plugin.settings().isWorldDisabled(player.getWorld().getName())) {
            return;
        }
        Faction owner = plugin.claims().getOwner(player.getLocation());
        ProtectionSettings ps = plugin.protection();
        if (owner.isSafeZone() && !ps.safezone.playerDamage && event.getCause() != EntityDamageEvent.DamageCause.VOID) {
            event.setCancelled(true);
        } else if (owner.isWarZone() && event.getCause() == EntityDamageEvent.DamageCause.FALL && !ps.warzone.fallDamage) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPvp(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        Player victim = (Player) event.getEntity();
        Player attacker = ProtectionListener.responsiblePlayer(event.getDamager());
        if (attacker == null || attacker == victim) {
            return;
        }
        String reason = denyReason(attacker, victim);
        if (reason != null) {
            event.setCancelled(true);
            FPlayer afp = plugin.players().get(attacker);
            long now = System.currentTimeMillis();
            if (now - afp.getLastDenyMessage() >= plugin.settings().pvpDenyCooldownMs) {
                afp.setLastDenyMessage(now);
                plugin.msg().send(attacker, reason);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamageMonitor(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player) {
            plugin.teleports().onDamage((Player) event.getEntity());
        }
    }

    /** @return la clé du message si l'attaque est interdite, sinon null. */
    public String denyReason(Player attacker, Player victim) {
        Settings s = plugin.settings();
        ProtectionSettings ps = plugin.protection();
        FactionManager fm = plugin.factions();
        FPlayer a = plugin.players().get(attacker);
        FPlayer v = plugin.players().get(victim);
        if (a.isBypass() || s.isWorldDisabled(victim.getWorld().getName())) {
            return null;
        }
        if (s.pvpDisabledWorlds.contains(victim.getWorld().getName().toLowerCase(Locale.ROOT))) {
            return "pvp.world";
        }
        Faction attackerZone = plugin.claims().getOwner(attacker.getLocation());
        Faction victimZone = plugin.claims().getOwner(victim.getLocation());
        if ((attackerZone.isSafeZone() || victimZone.isSafeZone()) && !ps.safezone.pvp) {
            return "pvp.safezone";
        }
        Relation relation = fm.getRelation(a, v);
        if (victimZone.isWarZone()) {
            if (!ps.warzone.pvp) {
                return "pvp.territory";
            }
            return relation.isFriendly() && !ps.warzone.friendlyFire ? relationReason(relation) : null;
        }
        Faction fa = fm.getFaction(a);
        Faction fv = fm.getFaction(v);
        if (fa == null && !s.factionlessCanAttack) {
            return "pvp.factionless-attacker";
        }
        if (fv == null && !s.factionlessCanBeAttacked) {
            return "pvp.factionless-victim";
        }
        if (s.peacefulNoPvp && ((fa != null && fm.getFlag(fa, FactionFlag.PEACEFUL))
                || (fv != null && fm.getFlag(fv, FactionFlag.PEACEFUL)))) {
            return "pvp.peaceful";
        }
        if (victimZone.isWilderness() && !ps.wilderness.pvp) {
            return "pvp.wilderness";
        }
        if ((victimZone.isNormal() && !fm.getFlag(victimZone, FactionFlag.PVP))
                || (attackerZone.isNormal() && !fm.getFlag(attackerZone, FactionFlag.PVP))) {
            return "pvp.territory";
        }
        switch (relation) {
            case MEMBER:
                return s.friendlyFire || (fa != null && fm.getFlag(fa, FactionFlag.FRIENDLY_FIRE)) ? null : "pvp.member";
            case ENEMY:
                return s.enemyDamage ? null : "pvp.enemy";
            default:
                return null;
        }
    }

    private static String relationReason(Relation relation) {
        return "pvp.member";
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPotion(PotionSplashEvent event) {
        boolean harmful = false;
        for (PotionEffect effect : event.getPotion().getEffects()) {
            if (HARMFUL.contains(effect.getType())) {
                harmful = true;
                break;
            }
        }
        if (!harmful) {
            return;
        }
        Player shooter = event.getPotion().getShooter() instanceof Player ? (Player) event.getPotion().getShooter() : null;
        for (LivingEntity entity : event.getAffectedEntities()) {
            if (!(entity instanceof Player) || entity == shooter) {
                continue;
            }
            Player victim = (Player) entity;
            if (shooter == null) {
                if (plugin.protection().safezone.blockNegativePotions
                        && plugin.claims().getOwner(victim.getLocation()).isSafeZone()) {
                    event.setIntensity(victim, 0);
                }
            } else if (denyReason(shooter, victim) != null) {
                event.setIntensity(victim, 0);
            }
        }
    }

    // ============================================================ Faim / pearls

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFood(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getEntity();
        if (event.getFoodLevel() >= player.getFoodLevel()) {
            return;
        }
        Faction owner = plugin.claims().getOwner(player.getLocation());
        if ((owner.isSafeZone() && !plugin.protection().safezone.hunger)
                || (owner.isWarZone() && !plugin.protection().warzone.hunger)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPearl(PlayerTeleportEvent event) {
        if (event.getCause() != PlayerTeleportEvent.TeleportCause.ENDER_PEARL) {
            return;
        }
        Player player = event.getPlayer();
        FPlayer fp = plugin.players().get(player);
        Location to = event.getTo();
        if (fp.isBypass() || plugin.settings().isWorldDisabled(to.getWorld().getName())) {
            return;
        }
        ProtectionSettings ps = plugin.protection();
        TerritoryType type = plugin.factions().getTerritoryType(fp, plugin.claims().getOwner(to));
        boolean denied = ps.denyEnderpearlIn.contains(type)
                || (type == TerritoryType.SAFEZONE && !ps.safezone.enderpearl)
                || (type == TerritoryType.WARZONE && !ps.warzone.enderpearl);
        if (denied) {
            event.setCancelled(true);
            if (player.getGameMode() != GameMode.CREATIVE) {
                player.getInventory().addItem(new ItemStack(Material.ENDER_PEARL, 1));
            }
            plugin.msg().send(player, "protection.enderpearl");
        }
    }

    // ================================================================= Morts

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        if (plugin.settings().isWorldDisabled(victim.getWorld().getName())) {
            return;
        }
        Player killer = victim.getKiller();
        plugin.power().handleDeath(victim, killer);

        Settings s = plugin.settings();
        boolean pvp = killer != null && killer != victim;
        if (pvp || !s.statsOnlyPvp) {
            FPlayer vfp = plugin.players().get(victim);
            vfp.setDeaths(vfp.getDeaths() + 1);
        }
        if (pvp) {
            FPlayer kfp = plugin.players().get(killer);
            kfp.setKills(kfp.getKills() + 1);
            EconomySettings eco = plugin.economySettings();
            if (eco.killReward > 0 && plugin.economy().isAvailable()) {
                plugin.economy().deposit(killer, eco.killReward);
                plugin.msg().send(killer, "economy.kill-reward", "amount", plugin.economy().format(eco.killReward),
                        "player", victim.getName());
            }
            if (eco.deathPenalty > 0 && plugin.economy().isAvailable()) {
                double penalty = Math.min(eco.deathPenalty, plugin.economy().getBalance(victim));
                if (penalty > 0 && plugin.economy().withdraw(victim, penalty)) {
                    plugin.msg().send(victim, "economy.death-penalty", "amount", plugin.economy().format(penalty));
                }
            }
        }
        plugin.top().invalidate();
    }
}
