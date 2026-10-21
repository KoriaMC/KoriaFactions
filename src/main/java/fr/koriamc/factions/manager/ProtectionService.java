package fr.koriamc.factions.manager;

import fr.koriamc.factions.KoriaFactions;
import fr.koriamc.factions.config.ProtectionSettings;
import fr.koriamc.factions.config.Settings;
import fr.koriamc.factions.model.ChunkPos;
import fr.koriamc.factions.model.Claim;
import fr.koriamc.factions.model.ExplosionType;
import fr.koriamc.factions.model.FPlayer;
import fr.koriamc.factions.model.Faction;
import fr.koriamc.factions.model.FactionFlag;
import fr.koriamc.factions.model.PermAction;
import fr.koriamc.factions.model.Relation;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.Locale;
import java.util.Map;

/**
 * Règles de protection partagées par les écouteurs : actions des joueurs,
 * explosions et propagation du feu.
 */
public final class ProtectionService {

    private final KoriaFactions plugin;

    public ProtectionService(KoriaFactions plugin) {
        this.plugin = plugin;
    }

    /**
     * Le joueur peut-il effectuer cette action à cet endroit ?
     *
     * @param block    bloc concerné (pour les listes blanches des zones), peut être null
     * @param breaking vrai pour une destruction (utile pour SPAWNER)
     * @param notify   envoyer le message de refus
     */
    public boolean canAct(Player player, Location location, Block block, PermAction action, boolean breaking, boolean notify) {
        Settings s = plugin.settings();
        if (s.isWorldDisabled(location.getWorld().getName())) {
            return true;
        }
        FPlayer fp = plugin.players().get(player);
        if (fp.isBypass()) {
            return true;
        }
        ProtectionSettings ps = plugin.protection();
        Faction owner = plugin.claims().getOwner(location);
        boolean buildLike = action == PermAction.BUILD || action == PermAction.DESTROY || action == PermAction.SPAWNER;
        boolean destroying = action == PermAction.DESTROY || (action == PermAction.SPAWNER && breaking);

        if (owner.isWilderness()) {
            ProtectionSettings.Zone w = ps.wilderness;
            boolean allowed;
            if (buildLike) {
                allowed = (destroying ? w.destroy : w.build)
                        && !w.denyBuildWorlds.contains(location.getWorld().getName().toLowerCase(Locale.ROOT));
            } else {
                allowed = w.use || (block != null && w.useWhitelist.contains(block));
            }
            if (!allowed && notify) {
                deny(player, fp, owner, action);
            }
            return allowed;
        }

        if (owner.isSystem()) {
            if (player.hasPermission("koriafactions.zone." + owner.getId() + ".build")) {
                return true;
            }
            ProtectionSettings.Zone zone = ps.zone(owner.getId());
            boolean allowed;
            if (buildLike) {
                allowed = destroying ? zone.destroy : zone.build;
            } else {
                allowed = zone.use || (block != null && zone.useWhitelist.contains(block));
            }
            if (!allowed && notify) {
                deny(player, fp, owner, action);
            }
            return allowed;
        }

        Claim claim = plugin.claims().getClaim(ChunkPos.of(location));
        if (claim != null && s.accessEnabled && s.accessGrants.contains(action)
                && claim.hasAccess(player.getUniqueId(), fp.getFactionId())) {
            return true;
        }
        FactionManager fm = plugin.factions();
        if (fm.hasPermission(fp, owner, action)) {
            return true;
        }
        if (ps.allowWhenRaidable.contains(action) && fm.getRelationTo(fp, owner) == Relation.ENEMY && fm.isRaidable(owner)) {
            return true;
        }
        if (notify) {
            deny(player, fp, owner, action);
            if (ps.painEnabled && buildLike && ps.painDamage > 0) {
                player.damage(ps.painDamage);
                plugin.msg().send(player, "protection.pain");
            }
        }
        return false;
    }

    private void deny(Player player, FPlayer fp, Faction owner, PermAction action) {
        long now = System.currentTimeMillis();
        if (now - fp.getLastDenyMessage() < plugin.settings().pvpDenyCooldownMs) {
            return;
        }
        fp.setLastDenyMessage(now);
        String key = "protection.actions." + action.name();
        if (!plugin.msg().config().isSet(key)) {
            key = "protection.denied";
        }
        plugin.msg().send(player, key, "faction", plugin.factions().getColor(fp, owner) + owner.getTag(),
                "action", plugin.roles().actionName(action));
    }

    /** Une explosion de ce type peut-elle détruire des blocs à cet endroit ? */
    public boolean isExplosionAllowed(Location location, ExplosionType type) {
        if (plugin.settings().isWorldDisabled(location.getWorld().getName())) {
            return true;
        }
        ProtectionSettings ps = plugin.protection();
        Faction owner = plugin.claims().getOwner(location);
        Map<ExplosionType, Boolean> rules;
        if (owner.isNormal()) {
            FactionManager fm = plugin.factions();
            if (!fm.getFlag(owner, FactionFlag.EXPLOSIONS)) {
                return false;
            }
            if (ps.explosionsProtectPeaceful && fm.getFlag(owner, FactionFlag.PEACEFUL)) {
                return false;
            }
            rules = fm.countOnline(owner) > 0 ? ps.explosionsOnline : ps.explosionsOffline;
        } else {
            rules = ps.zone(owner.getId()).explosions;
        }
        Boolean allowed = rules.get(type);
        return allowed != null && allowed;
    }

    /** Le feu peut-il se propager / brûler des blocs à cet endroit ? */
    public boolean isFireSpreadAllowed(Location location) {
        if (plugin.settings().isWorldDisabled(location.getWorld().getName())) {
            return true;
        }
        Faction owner = plugin.claims().getOwner(location);
        if (owner.isNormal()) {
            return plugin.factions().getFlag(owner, FactionFlag.FIRE_SPREAD);
        }
        return plugin.protection().zone(owner.getId()).fireSpread;
    }
}
