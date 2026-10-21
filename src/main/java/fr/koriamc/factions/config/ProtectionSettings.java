package fr.koriamc.factions.config;

import fr.koriamc.factions.model.ExplosionType;
import fr.koriamc.factions.model.Faction;
import fr.koriamc.factions.model.PermAction;
import fr.koriamc.factions.model.TerritoryType;
import fr.koriamc.factions.util.MaterialSet;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Valeurs typées de la protection (territoire.json : zones, protection,
 * explosions, blocs, entités).
 */
public final class ProtectionSettings {

    /** Règles d'une zone (wilderness, safezone, warzone). */
    public static final class Zone {
        public boolean build;
        public boolean destroy;
        public boolean use;
        public MaterialSet useWhitelist;
        public boolean pvp;
        public boolean friendlyFire;
        public boolean playerDamage;
        public boolean fallDamage;
        public boolean hunger;
        public boolean mobSpawning;
        public boolean fireSpread;
        public boolean protectFromLiquids;
        public boolean enderpearl;
        public boolean blockNegativePotions;
        public Set<String> denyBuildWorlds;
        public final Map<ExplosionType, Boolean> explosions = new EnumMap<ExplosionType, Boolean>(ExplosionType.class);

        void load(ConfigurationSection s, boolean def, Logger logger, String name) {
            build = flag(s, "construire", def);
            destroy = flag(s, "detruire", def);
            use = flag(s, "utiliser", def);
            useWhitelist = MaterialSet.of(s.getStringList("toujours-utilisables"), logger, name + ".toujours-utilisables");
            pvp = flag(s, "pvp", true);
            // ancien nom (fichiers d'avant la suppression des alliances) : tir-allie
            friendlyFire = !s.isSet("tir-entre-membres") && s.isSet("tir-allie") ? s.getBoolean("tir-allie")
                    : flag(s, "tir-entre-membres", false);
            playerDamage = flag(s, "degats-joueurs", true);
            fallDamage = flag(s, "degats-de-chute", true);
            hunger = flag(s, "faim", true);
            mobSpawning = flag(s, "monstres", true);
            fireSpread = flag(s, "propagation-feu", true);
            protectFromLiquids = flag(s, "bloquer-liquides", false);
            enderpearl = flag(s, "ender-pearls", true);
            blockNegativePotions = flag(s, "bloquer-potions-negatives", false);
            denyBuildWorlds = Settings.lowerSet(s.getStringList("mondes-construction-interdite"));
        }

        /** Valeur du fichier, sinon celle du jar, sinon {@code fallback}. */
        private static boolean flag(ConfigurationSection s, String key, boolean fallback) {
            return s.contains(key) ? s.getBoolean(key) : fallback;
        }
    }

    public final Zone wilderness = new Zone();
    public final Zone safezone = new Zone();
    public final Zone warzone = new Zone();

    public boolean painEnabled;
    public double painDamage;
    public Set<PermAction> allowWhenRaidable;
    public Set<TerritoryType> denyEnderpearlIn;
    public boolean protectPistons;
    public boolean protectFromLiquids;
    public Set<EntityType> denyEntityGrief;

    public boolean explosionsOnlyBlockDamage;
    public final Map<ExplosionType, Boolean> explosionsOnline = new EnumMap<ExplosionType, Boolean>(ExplosionType.class);
    public final Map<ExplosionType, Boolean> explosionsOffline = new EnumMap<ExplosionType, Boolean>(ExplosionType.class);
    public boolean explosionsProtectPeaceful;

    public Set<SpawnReason> controlledSpawnReasons;

    public MaterialSet containers;
    public MaterialSet doors;
    public MaterialSet buttons;
    public MaterialSet levers;
    public MaterialSet pressurePlates;
    public MaterialSet interactables;
    public MaterialSet alwaysAllowed;
    public MaterialSet spawners;
    public MaterialSet restrictedItems;

    public Set<EntityType> interactEntities;
    public Set<EntityType> damageEntities;
    public Set<EntityType> protectedAnimals;

    public final Map<TerritoryType, Set<String>> blockedCommands = new EnumMap<TerritoryType, Set<String>>(TerritoryType.class);

    public void load(ConfigurationSection c, Logger logger) {
        wilderness.load(section(c, "zones.wilderness"), true, logger, "zones.wilderness");
        safezone.load(section(c, "zones.safezone"), false, logger, "zones.safezone");
        warzone.load(section(c, "zones.warzone"), false, logger, "zones.warzone");
        loadExplosions(c.getConfigurationSection("explosions.wilderness"), wilderness.explosions);
        loadExplosions(c.getConfigurationSection("explosions.safezone"), safezone.explosions);
        loadExplosions(c.getConfigurationSection("explosions.warzone"), warzone.explosions);
        loadExplosions(c.getConfigurationSection("explosions.faction-en-ligne"), explosionsOnline);
        loadExplosions(c.getConfigurationSection("explosions.faction-hors-ligne"), explosionsOffline);
        explosionsOnlyBlockDamage = c.getBoolean("explosions.seulement-blocs");
        explosionsProtectPeaceful = c.getBoolean("explosions.proteger-pacifiques");

        painDamage = Math.max(0, c.getDouble("protection.degats-si-construction"));
        painEnabled = painDamage > 0;
        allowWhenRaidable = Settings.actionSet(c.getStringList("protection.actions-si-raidable"), logger,
                "protection.actions-si-raidable");
        denyEnderpearlIn = EnumSet.noneOf(TerritoryType.class);
        for (String s : c.getStringList("protection.ender-pearls-interdites")) {
            TerritoryType type = TerritoryType.parse(s);
            if (type == null) {
                logger.warning("Type de territoire inconnu dans protection.ender-pearls-interdites : " + s);
            } else {
                denyEnderpearlIn.add(type);
            }
        }
        protectPistons = c.getBoolean("protection.proteger-pistons");
        protectFromLiquids = c.getBoolean("protection.bloquer-liquides");
        denyEntityGrief = entities(c.getStringList("protection.entites-sans-degats-de-blocs"), logger);

        controlledSpawnReasons = EnumSet.noneOf(SpawnReason.class);
        for (String s : c.getStringList("protection.monstres-raisons-controlees")) {
            try {
                controlledSpawnReasons.add(SpawnReason.valueOf(s.toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException e) {
                logger.warning("Raison d'apparition inconnue : " + s);
            }
        }

        containers = blocks(c, "conteneurs", logger);
        doors = blocks(c, "portes", logger);
        buttons = blocks(c, "boutons", logger);
        levers = blocks(c, "leviers", logger);
        pressurePlates = blocks(c, "plaques", logger);
        interactables = blocks(c, "interactifs", logger);
        alwaysAllowed = blocks(c, "toujours-autorises", logger);
        spawners = blocks(c, "spawners", logger);
        restrictedItems = blocks(c, "objets-restreints", logger);

        interactEntities = entities(c.getStringList("entites.clic-droit-protege"), logger);
        damageEntities = entities(c.getStringList("entites.degats-proteges"), logger);
        protectedAnimals = entities(c.getStringList("entites.animaux-proteges"), logger);

        blockedCommands.clear();
        for (TerritoryType type : TerritoryType.values()) {
            Set<String> commands = new HashSet<String>();
            for (String cmd : c.getStringList("protection.commandes-bloquees." + type.name())) {
                String clean = cmd.trim().toLowerCase(Locale.ROOT);
                if (clean.startsWith("/")) {
                    clean = clean.substring(1);
                }
                if (!clean.isEmpty()) {
                    commands.add(clean);
                }
            }
            blockedCommands.put(type, commands);
        }
    }

    private static MaterialSet blocks(ConfigurationSection c, String key, Logger logger) {
        return MaterialSet.of(c.getStringList("blocs." + key), logger, "blocs." + key);
    }

    private static ConfigurationSection section(ConfigurationSection c, String path) {
        ConfigurationSection s = c.getConfigurationSection(path);
        return s != null ? s : c.createSection(path);
    }

    private static void loadExplosions(ConfigurationSection s, Map<ExplosionType, Boolean> target) {
        target.clear();
        for (ExplosionType type : ExplosionType.values()) {
            target.put(type, s != null && s.contains(type.name()) && s.getBoolean(type.name()));
        }
    }

    private static Set<EntityType> entities(List<String> names, Logger logger) {
        Set<EntityType> set = EnumSet.noneOf(EntityType.class);
        for (String name : names) {
            try {
                set.add(EntityType.valueOf(name.toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException e) {
                logger.warning("Entité inconnue : " + name);
            }
        }
        return set;
    }

    public Zone zone(String factionId) {
        if (Faction.SAFEZONE.equals(factionId)) {
            return safezone;
        }
        if (Faction.WARZONE.equals(factionId)) {
            return warzone;
        }
        return wilderness;
    }
}
