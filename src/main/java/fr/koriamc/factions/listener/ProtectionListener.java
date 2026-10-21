package fr.koriamc.factions.listener;

import fr.koriamc.factions.KoriaFactions;
import fr.koriamc.factions.config.ProtectionSettings;
import fr.koriamc.factions.manager.ProtectionService;
import fr.koriamc.factions.model.ExplosionType;
import fr.koriamc.factions.model.Faction;
import fr.koriamc.factions.model.PermAction;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Explosive;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Wither;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.vehicle.VehicleDestroyEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Protection des territoires contre les actions des joueurs et de l'environnement.
 */
public final class ProtectionListener implements Listener {

    private final KoriaFactions plugin;

    public ProtectionListener(KoriaFactions plugin) {
        this.plugin = plugin;
    }

    private ProtectionService service() {
        return plugin.protectionService();
    }

    private ProtectionSettings ps() {
        return plugin.protection();
    }

    /** Joueur responsable d'une entité (lui-même ou le tireur d'un projectile). */
    public static Player responsiblePlayer(Entity entity) {
        if (entity instanceof Player) {
            return (Player) entity;
        }
        if (entity instanceof Projectile && ((Projectile) entity).getShooter() instanceof Player) {
            return (Player) ((Projectile) entity).getShooter();
        }
        return null;
    }

    // ================================================================= Blocs

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        Block block = event.getBlockPlaced();
        PermAction action = ps().spawners.contains(block) ? PermAction.SPAWNER : PermAction.BUILD;
        if (!service().canAct(event.getPlayer(), block.getLocation(), block, action, false, true)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        PermAction action = ps().spawners.contains(block) ? PermAction.SPAWNER : PermAction.DESTROY;
        if (!service().canAct(event.getPlayer(), block.getLocation(), block, action, true, true)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBucketEmpty(PlayerBucketEmptyEvent event) {
        Block target = event.getBlockClicked().getRelative(event.getBlockFace());
        if (!service().canAct(event.getPlayer(), target.getLocation(), target, PermAction.USE_ITEM, false, true)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBucketFill(PlayerBucketFillEvent event) {
        Block target = event.getBlockClicked();
        if (!service().canAct(event.getPlayer(), target.getLocation(), target, PermAction.USE_ITEM, false, true)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        Block block = event.getClickedBlock();
        if (block == null) {
            return;
        }
        Player player = event.getPlayer();
        ProtectionSettings ps = ps();
        if (event.getAction() == Action.PHYSICAL) {
            PermAction action = null;
            if (ps.pressurePlates.contains(block)) {
                action = PermAction.PRESSURE_PLATE;
            } else if (block.getType() == Material.SOIL) {
                action = PermAction.DESTROY;
            }
            if (action != null && !service().canAct(player, block.getLocation(), block, action, true, false)) {
                event.setCancelled(true);
            }
            return;
        }
        if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
            Block fire = block.getRelative(event.getBlockFace());
            if (fire.getType() == Material.FIRE
                    && !service().canAct(player, fire.getLocation(), fire, PermAction.DESTROY, true, true)) {
                event.setCancelled(true);
            }
            return;
        }
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || ps.alwaysAllowed.contains(block)) {
            return;
        }
        PermAction action = null;
        if (ps.containers.contains(block)) {
            action = PermAction.CONTAINER;
        } else if (ps.doors.contains(block)) {
            action = PermAction.DOOR;
        } else if (ps.buttons.contains(block)) {
            action = PermAction.BUTTON;
        } else if (ps.levers.contains(block)) {
            action = PermAction.LEVER;
        } else if (ps.interactables.contains(block)) {
            action = PermAction.INTERACT;
        }
        if (action != null && !service().canAct(player, block.getLocation(), block, action, false, true)) {
            event.setCancelled(true);
            return;
        }
        ItemStack item = event.getItem();
        if (item != null && ps.restrictedItems.contains(item)) {
            Block target = block.getRelative(event.getBlockFace());
            if (!service().canAct(player, target.getLocation(), target, PermAction.USE_ITEM, false, true)) {
                event.setCancelled(true);
            }
        }
    }

    // =============================================================== Entités

    private void checkEntityInteract(Player player, Entity entity, org.bukkit.event.Cancellable event) {
        ProtectionSettings ps = ps();
        if (ps.interactEntities.contains(entity.getType()) || ps.protectedAnimals.contains(entity.getType())) {
            if (!service().canAct(player, entity.getLocation(), null, PermAction.ENTITY_INTERACT, false, true)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityInteract(PlayerInteractEntityEvent event) {
        checkEntityInteract(event.getPlayer(), event.getRightClicked(), event);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityInteractAt(PlayerInteractAtEntityEvent event) {
        checkEntityInteract(event.getPlayer(), event.getRightClicked(), event);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onArmorStand(PlayerArmorStandManipulateEvent event) {
        if (!service().canAct(event.getPlayer(), event.getRightClicked().getLocation(), null,
                PermAction.ENTITY_INTERACT, false, true)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        Entity victim = event.getEntity();
        if (victim instanceof Player) {
            return;
        }
        ProtectionSettings ps = ps();
        PermAction action;
        if (ps.damageEntities.contains(victim.getType())) {
            action = PermAction.DESTROY;
        } else if (ps.protectedAnimals.contains(victim.getType())) {
            action = PermAction.ENTITY_INTERACT;
        } else {
            return;
        }
        Player attacker = responsiblePlayer(event.getDamager());
        if (attacker == null) {
            if (event.getDamager() instanceof Explosive || event.getDamager() instanceof Creeper) {
                if (!service().isExplosionAllowed(victim.getLocation(), ExplosionType.of(event.getDamager()))) {
                    event.setCancelled(true);
                }
            }
            return;
        }
        if (!service().canAct(attacker, victim.getLocation(), null, action, true, true)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onVehicleDestroy(VehicleDestroyEvent event) {
        Player attacker = responsiblePlayer(event.getAttacker());
        if (attacker != null && ps().damageEntities.contains(event.getVehicle().getType())
                && !service().canAct(attacker, event.getVehicle().getLocation(), null, PermAction.DESTROY, true, true)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onHangingBreak(HangingBreakEvent event) {
        Location location = event.getEntity().getLocation();
        if (event instanceof HangingBreakByEntityEvent) {
            Entity remover = ((HangingBreakByEntityEvent) event).getRemover();
            Player player = responsiblePlayer(remover);
            if (player != null) {
                if (!service().canAct(player, location, null, PermAction.DESTROY, true, true)) {
                    event.setCancelled(true);
                }
                return;
            }
            if (remover instanceof Explosive || remover instanceof Creeper || remover instanceof Wither) {
                if (!service().isExplosionAllowed(location, ExplosionType.of(remover))) {
                    event.setCancelled(true);
                }
                return;
            }
        }
        if (event.getCause() == HangingBreakEvent.RemoveCause.EXPLOSION
                && !service().isExplosionAllowed(location, ExplosionType.OTHER)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onHangingPlace(HangingPlaceEvent event) {
        Block target = event.getBlock().getRelative(event.getBlockFace());
        if (!service().canAct(event.getPlayer(), target.getLocation(), target, PermAction.BUILD, false, true)) {
            event.setCancelled(true);
        }
    }

    // ========================================================= Environnement

    /** Un bloc peut-il être déplacé depuis le territoire {@code source} vers {@code block} ? */
    private boolean canMoveInto(Faction source, Block block) {
        Faction target = plugin.claims().getOwner(block.getLocation());
        return target == source || target.isWilderness();
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        if (!ps().protectPistons || plugin.settings().isWorldDisabled(event.getBlock().getWorld().getName())) {
            return;
        }
        Faction source = plugin.claims().getOwner(event.getBlock().getLocation());
        if (!canMoveInto(source, event.getBlock().getRelative(event.getDirection()))) {
            event.setCancelled(true);
            return;
        }
        for (Block block : event.getBlocks()) {
            if (!canMoveInto(source, block) || !canMoveInto(source, block.getRelative(event.getDirection()))) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        if (!ps().protectPistons || !event.isSticky()
                || plugin.settings().isWorldDisabled(event.getBlock().getWorld().getName())) {
            return;
        }
        Faction source = plugin.claims().getOwner(event.getBlock().getLocation());
        for (Block block : event.getBlocks()) {
            if (!canMoveInto(source, block)) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onLiquidFlow(BlockFromToEvent event) {
        Block from = event.getBlock();
        Block to = event.getToBlock();
        if ((from.getX() >> 4) == (to.getX() >> 4) && (from.getZ() >> 4) == (to.getZ() >> 4)) {
            return;
        }
        Material type = from.getType();
        if (type != Material.WATER && type != Material.STATIONARY_WATER
                && type != Material.LAVA && type != Material.STATIONARY_LAVA) {
            return;
        }
        if (plugin.settings().isWorldDisabled(from.getWorld().getName())) {
            return;
        }
        Faction target = plugin.claims().getOwner(to.getLocation());
        if (target.isWilderness() || target == plugin.claims().getOwner(from.getLocation())) {
            return;
        }
        boolean protect = target.isNormal() ? ps().protectFromLiquids : ps().zone(target.getId()).protectFromLiquids;
        if (protect) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onSpread(BlockSpreadEvent event) {
        if (event.getSource().getType() == Material.FIRE && !service().isFireSpreadAllowed(event.getBlock().getLocation())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBurn(BlockBurnEvent event) {
        if (!service().isFireSpreadAllowed(event.getBlock().getLocation())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onIgnite(BlockIgniteEvent event) {
        switch (event.getCause()) {
            case SPREAD:
            case LAVA:
            case LIGHTNING:
                if (!service().isFireSpreadAllowed(event.getBlock().getLocation())) {
                    event.setCancelled(true);
                }
                break;
            case FLINT_AND_STEEL:
                if (event.getPlayer() != null && !service().canAct(event.getPlayer(), event.getBlock().getLocation(),
                        event.getBlock(), PermAction.USE_ITEM, false, false)) {
                    event.setCancelled(true);
                }
                break;
            default:
                break;
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityChangeBlock(EntityChangeBlockEvent event) {
        if (!ps().denyEntityGrief.contains(event.getEntityType())
                || plugin.settings().isWorldDisabled(event.getBlock().getWorld().getName())) {
            return;
        }
        if (!plugin.claims().getOwner(event.getBlock().getLocation()).isWilderness()) {
            event.setCancelled(true);
        }
    }
}
