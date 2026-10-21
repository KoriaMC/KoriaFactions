package fr.koriamc.factions.listener;

import fr.koriamc.factions.KoriaFactions;
import fr.koriamc.factions.config.ProtectionSettings;
import fr.koriamc.factions.model.ExplosionType;
import fr.koriamc.factions.model.Faction;
import fr.koriamc.factions.model.FactionFlag;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Ghast;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Slime;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityExplodeEvent;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Explosions et apparition des monstres.
 */
public final class WorldListener implements Listener {

    private final KoriaFactions plugin;

    public WorldListener(KoriaFactions plugin) {
        this.plugin = plugin;
    }

    /**
     * Filtre les blocs d'une explosion.
     *
     * @return true si l'explosion entière doit être annulée
     */
    private boolean filter(List<Block> blocks, ExplosionType type) {
        boolean onlyBlocks = plugin.protection().explosionsOnlyBlockDamage;
        Map<Long, Boolean> cache = new HashMap<Long, Boolean>();
        Iterator<Block> it = blocks.iterator();
        while (it.hasNext()) {
            Block block = it.next();
            long key = ((long) (block.getX() >> 4) << 32) ^ ((block.getZ() >> 4) & 0xFFFFFFFFL);
            Boolean allowed = cache.get(key);
            if (allowed == null) {
                allowed = plugin.protectionService().isExplosionAllowed(block.getLocation(), type);
                cache.put(key, allowed);
            }
            if (!allowed) {
                if (!onlyBlocks) {
                    return true;
                }
                it.remove();
            }
        }
        return false;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        ExplosionType type = ExplosionType.of(event.getEntity());
        if (!plugin.protection().explosionsOnlyBlockDamage
                && !plugin.protectionService().isExplosionAllowed(event.getLocation(), type)) {
            event.setCancelled(true);
            return;
        }
        if (filter(event.blockList(), type)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        if (filter(event.blockList(), ExplosionType.OTHER)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onSpawn(CreatureSpawnEvent event) {
        ProtectionSettings ps = plugin.protection();
        if (!ps.controlledSpawnReasons.contains(event.getSpawnReason())
                || plugin.settings().isWorldDisabled(event.getLocation().getWorld().getName())) {
            return;
        }
        Faction owner = plugin.claims().getOwner(event.getLocation());
        if (!owner.isNormal()) {
            if (!ps.zone(owner.getId()).mobSpawning) {
                event.setCancelled(true);
            }
            return;
        }
        Entity entity = event.getEntity();
        boolean hostile = entity instanceof Monster || entity instanceof Slime || entity instanceof Ghast;
        if (hostile && !plugin.factions().getFlag(owner, FactionFlag.MONSTERS)) {
            event.setCancelled(true);
        }
    }
}
