package fr.koriamc.factions.model;

import org.bukkit.entity.Creeper;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.entity.Wither;
import org.bukkit.entity.WitherSkull;
import org.bukkit.entity.minecart.ExplosiveMinecart;

/**
 * Catégories d'explosions configurables.
 */
public enum ExplosionType {
    TNT,
    CREEPER,
    FIREBALL,
    WITHER,
    OTHER;

    public static ExplosionType of(Entity entity) {
        if (entity instanceof TNTPrimed || entity instanceof ExplosiveMinecart) {
            return TNT;
        }
        if (entity instanceof Creeper) {
            return CREEPER;
        }
        // WitherSkull hérite de Fireball : on le teste avant
        if (entity instanceof WitherSkull || entity instanceof Wither) {
            return WITHER;
        }
        if (entity instanceof Fireball) {
            return FIREBALL;
        }
        return OTHER;
    }
}
