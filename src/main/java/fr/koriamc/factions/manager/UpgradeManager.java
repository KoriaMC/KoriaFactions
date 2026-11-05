package fr.koriamc.factions.manager;

import fr.koriamc.factions.KoriaFactions;
import fr.koriamc.factions.listener.ProtectionListener;
import fr.koriamc.factions.model.FPlayer;
import fr.koriamc.factions.model.Faction;
import fr.koriamc.factions.model.PermAction;
import fr.koriamc.factions.model.UpgradeType;
import fr.koriamc.factions.util.MaterialSet;
import fr.koriamc.factions.util.Sounds;
import fr.koriamc.factions.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockGrowEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.SpawnerSpawnEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

/**
 * Améliorations de faction (factions.json, section "ameliorations") et leurs effets en jeu.
 * Les définitions sont lues telles qu'écrites dans le fichier du serveur, sans
 * compléter avec les valeurs du jar : ce qui est dans le fichier est ce qui s'applique.
 */
public final class UpgradeManager implements Listener {

    /** Métadonnée posée sur les mobs supplémentaires de l'amélioration SPAWNERS. */
    public static final String SPAWNER_METADATA = "koriafactions-spawner";

    /** Un palier : coût et valeur de l'effet. */
    public static final class Level {
        public final double cost;
        public final double value;

        Level(double cost, double value) {
            this.cost = cost;
            this.value = value;
        }
    }

    /** Définition d'une amélioration. */
    public static final class Upgrade {
        public final String id;
        /** Nom tapé dans les commandes (/f setupgrade), en anglais. */
        public final String argument;
        public final UpgradeType type;
        public final int slot;
        public final String icon;
        public final String name;
        public final List<String> description;
        public final String valueFormat;
        public final List<Level> levels;

        Upgrade(String id, String argument, UpgradeType type, int slot, String icon, String name, List<String> description,
                String valueFormat, List<Level> levels) {
            this.id = id;
            this.argument = argument;
            this.type = type;
            this.slot = slot;
            this.icon = icon;
            this.name = name;
            this.description = description;
            this.valueFormat = valueFormat;
            this.levels = levels;
        }

        public int getMaxLevel() {
            return levels.size();
        }

        /** Valeur de l'effet au niveau donné (0 au niveau 0). */
        public double valueAt(int level) {
            if (level <= 0 || levels.isEmpty()) {
                return 0;
            }
            return levels.get(Math.min(level, levels.size()) - 1).value;
        }

        public String formatValue(int level) {
            return Text.replace(valueFormat, "value", Text.number(valueAt(level), 2));
        }
    }

    private final KoriaFactions plugin;
    private final Random random = new Random();
    private final Map<String, Upgrade> upgrades = new LinkedHashMap<String, Upgrade>();
    private final List<String> problems = new ArrayList<String>();
    private boolean enabled;
    private boolean announce;
    private boolean damagePvpOnly;
    private double damageMaxPercent;
    private final java.util.Set<String> sethomeCommands = new java.util.HashSet<String>();
    private boolean obsidianEnabled;
    private int obsidianRadius;
    private long obsidianResetMillis;
    private boolean obsidianIgnoreWater;
    /** Coups d'explosion nécessaires par matériau (sans amélioration). */
    private final Map<Material, Integer> obsidianHits = new HashMap<Material, Integer>();
    /** Dégâts en cours par bloc : "monde:x:y:z" -> {coups, dernier coup en ms}. */
    private final Map<String, long[]> blockDamage = new HashMap<String, long[]>();

    public UpgradeManager(KoriaFactions plugin) {
        this.plugin = plugin;
    }

    // ============================================================ Chargement

    public void load(ConfigurationSection c) {
        upgrades.clear();
        problems.clear();
        enabled = c.getBoolean("ameliorations.actif");
        announce = c.getBoolean("ameliorations.annoncer-achat");
        damagePvpOnly = c.getBoolean("ameliorations.reduction-degats-seulement-pvp");
        damageMaxPercent = Math.max(0, Math.min(100, c.getDouble("ameliorations.reduction-degats-max")));
        sethomeCommands.clear();
        for (String cmd : c.getStringList("ameliorations.anti-sethome-commandes")) {
            sethomeCommands.add(cmd.trim().toLowerCase(Locale.ROOT).replaceFirst("^/", ""));
        }
        obsidianEnabled = c.getBoolean("raid-obsidienne.actif");
        obsidianRadius = Math.max(1, c.getInt("raid-obsidienne.rayon", 3));
        obsidianResetMillis = Math.max(0, c.getLong("raid-obsidienne.reinitialisation-minutes", 10)) * 60000L;
        obsidianIgnoreWater = c.getBoolean("raid-obsidienne.ignorer-dans-eau", true);
        obsidianHits.clear();
        ConfigurationSection hits = c.getConfigurationSection("raid-obsidienne.coups");
        if (hits != null) {
            for (String key : hits.getKeys(false)) {
                Material m = Material.matchMaterial(key.trim().toUpperCase(Locale.ROOT));
                if (m == null) {
                    problems.add("factions.json, raid-obsidienne.coups : matériau inconnu '" + key + "'");
                } else {
                    obsidianHits.put(m, Math.max(1, hits.getInt(key)));
                }
            }
        }
        ConfigurationSection root = c.getConfigurationSection("ameliorations.liste");
        if (root == null) {
            return;
        }
        Map<Integer, String> usedSlots = new HashMap<Integer, String>();
        for (String id : root.getKeys(false)) {
            ConfigurationSection s = root.getConfigurationSection(id);
            if (s == null) {
                problem(id, "ce n'est pas une section (vérifiez l'indentation)");
                continue;
            }
            String typeName = s.getString("type", null);
            UpgradeType type = UpgradeType.parse(typeName);
            if (type == null) {
                problem(id, "type inconnu '" + typeName + "' (types : " + typeList() + ")");
                continue;
            }
            List<Level> levels = readLevels(id, s);
            if (levels.isEmpty()) {
                problem(id, "aucun niveau valide, amélioration ignorée");
                continue;
            }
            Object slotValue = s.get("case", null);
            int slot = slotValue instanceof Number ? ((Number) slotValue).intValue() : -1;
            if (slot < 0 || slot >= 54) {
                problem(id, "case manquante ou invalide (0 à 53), amélioration non affichée dans le menu");
            } else if (usedSlots.containsKey(slot)) {
                problem(id, "même emplacement (" + slot + ") que '" + usedSlots.get(slot) + "'");
            } else {
                usedSlots.put(slot, id);
            }
            String icon = s.getString("icone", "PAPER");
            if (MaterialSet.parse(icon) == null) {
                problem(id, "icône inconnue '" + icon + "'");
            }
            List<String> description = new ArrayList<String>();
            Object rawDescription = s.get("description", null);
            if (rawDescription instanceof List) {
                for (Object line : (List<?>) rawDescription) {
                    description.add(String.valueOf(line));
                }
            } else if (rawDescription != null) {
                description.add(rawDescription.toString());
            }
            upgrades.put(id, new Upgrade(id, s.getString("argument", id).trim().toLowerCase(Locale.ROOT), type, slot, icon,
                    s.getString("nom", id), description,
                    s.getString("format-valeur", "{value}"), levels));
        }
        if (upgrades.isEmpty() && enabled) {
            problems.add("factions.json (ameliorations.liste) : aucune amélioration valide.");
        }
        warnUselessTypes();
        for (String problem : problems) {
            plugin.getLogger().warning(problem);
        }
        plugin.getLogger().info(upgrades.size() + " amélioration(s) chargée(s).");
    }

    /**
     * Paliers au format "niveaux": { "1": { "cout": ..., "valeur": ... }, ... } ou
     * sous forme de liste "niveaux": [ { "cout": ..., "valeur": ... }, ... ].
     */
    private List<Level> readLevels(String id, ConfigurationSection s) {
        List<Level> levels = new ArrayList<Level>();
        Object raw = s.get("niveaux", null);
        if (raw instanceof List) {
            int index = 1;
            for (Object entry : (List<?>) raw) {
                Level level = entry instanceof Map ? readLevel(id, String.valueOf(index), (Map<?, ?>) entry) : null;
                if (level == null && !(entry instanceof Map)) {
                    problem(id, "niveau " + index + " : format attendu { cout, valeur }");
                }
                if (level != null) {
                    levels.add(level);
                }
                index++;
            }
            return levels;
        }
        if (!(raw instanceof ConfigurationSection)) {
            return levels;
        }
        ConfigurationSection section = (ConfigurationSection) raw;
        TreeMap<Integer, Level> sorted = new TreeMap<Integer, Level>();
        for (String key : section.getKeys(false)) {
            Integer number = Text.parseInt(key);
            if (number == null || number <= 0) {
                problem(id, "niveau '" + key + "' : le numéro doit être un entier positif");
                continue;
            }
            ConfigurationSection l = section.getConfigurationSection(key);
            if (l == null) {
                problem(id, "niveau " + key + " : format attendu { cout, valeur }");
                continue;
            }
            Level level = readLevel(id, key, l.getValues(false));
            if (level != null) {
                sorted.put(number, level);
            }
        }
        int expected = 1;
        for (Map.Entry<Integer, Level> e : sorted.entrySet()) {
            if (e.getKey() != expected) {
                problem(id, "niveau " + expected + " absent : les niveaux sont renumérotés dans l'ordre");
                expected = e.getKey();
            }
            levels.add(e.getValue());
            expected++;
        }
        return levels;
    }

    private Level readLevel(String id, String number, Map<?, ?> values) {
        Double cost = number(values.get("cout"));
        Double value = number(values.get("valeur"));
        if (cost == null || value == null) {
            problem(id, "niveau " + number + " : cout et valeur doivent être des nombres"
                    + " (ex. cout 50000, valeur 2)");
            return null;
        }
        return new Level(Math.max(0, cost), value);
    }

    /** Nombre écrit tel quel, ou texte comme "50 000", "50_000", "50k", "1,5M". */
    private static Double number(Object raw) {
        if (raw instanceof Number) {
            return ((Number) raw).doubleValue();
        }
        if (raw == null) {
            return null;
        }
        String s = raw.toString().trim().toLowerCase(Locale.ROOT).replace(" ", "").replace("_", "");
        double multiplier = 1;
        if (s.endsWith("k")) {
            multiplier = 1000;
            s = s.substring(0, s.length() - 1);
        } else if (s.endsWith("m")) {
            multiplier = 1000000;
            s = s.substring(0, s.length() - 1);
        }
        Double parsed = Text.parseDouble(s);
        return parsed == null ? null : parsed * multiplier;
    }

    /** Signale les types sans effet et les améliorations placées hors du menu. */
    private void warnUselessTypes() {
        int menuSize = Math.max(1, Math.min(6, plugin.menusConfig().getInt("ameliorations.lignes", 4))) * 9;
        for (Upgrade u : upgrades.values()) {
            if (u.slot >= menuSize) {
                problem(u.id, "case " + u.slot + " hors du menu (menus.json : ameliorations.lignes)");
            }
        }
        for (Upgrade u : upgrades.values()) {
            if (u.type == UpgradeType.MEMBERS && plugin.settings().maxMembers <= 0) {
                problem(u.id, "sans effet : factions.membres-max vaut 0 (membres illimités)");
            } else if (u.type == UpgradeType.CHEST && !plugin.settings().chestEnabled) {
                problem(u.id, "sans effet : coffre.actif vaut false");
            } else if (u.type == UpgradeType.WARPS && !plugin.settings().warpsEnabled) {
                problem(u.id, "sans effet : warps.actif vaut false");
            }
        }
    }

    private void problem(String id, String text) {
        problems.add("factions.json, amélioration '" + id + "' : " + text);
    }

    private static String typeList() {
        List<String> names = new ArrayList<String>();
        for (UpgradeType type : UpgradeType.values()) {
            names.add(type.name());
        }
        return Text.join(names, ", ");
    }

    /** Problèmes détectés au dernier chargement (affichés par /f reload). */
    public List<String> getProblems() {
        return Collections.unmodifiableList(problems);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public Collection<Upgrade> getAll() {
        return Collections.unmodifiableCollection(upgrades.values());
    }

    public Upgrade get(String id) {
        return id == null ? null : upgrades.get(id);
    }

    /** Amélioration par son nom de commande (members), ou par son identifiant. */
    public Upgrade byArgument(String argument) {
        if (argument == null) {
            return null;
        }
        String key = argument.trim().toLowerCase(Locale.ROOT);
        for (Upgrade upgrade : upgrades.values()) {
            if (upgrade.argument.equals(key)) {
                return upgrade;
            }
        }
        return upgrades.get(key);
    }

    /** Somme des effets d'un type pour une faction. */
    public double getValue(Faction f, UpgradeType type) {
        if (!enabled || f == null || !f.isNormal()) {
            return 0;
        }
        double total = 0;
        for (Upgrade u : upgrades.values()) {
            if (u.type == type) {
                total += u.valueAt(f.getUpgradeLevel(u.id));
            }
        }
        return total;
    }

    // ================================================================= Achat

    /** Achète le niveau suivant, payé par le joueur. @return true en cas de succès. */
    public boolean purchase(Player player, FPlayer fp, Faction f, Upgrade upgrade) {
        if (!enabled) {
            plugin.msg().send(player, "general.feature-disabled");
            return false;
        }
        if (!plugin.factions().hasPermission(fp, f, PermAction.UPGRADE)) {
            plugin.msg().send(player, "general.no-faction-permission", "action", plugin.roles().actionName(PermAction.UPGRADE));
            return false;
        }
        int level = f.getUpgradeLevel(upgrade.id);
        if (level >= upgrade.getMaxLevel()) {
            plugin.msg().send(player, "upgrades.max-level");
            return false;
        }
        double cost = upgrade.levels.get(level).cost;
        switch (plugin.economy().pay(player, cost)) {
            case UNAVAILABLE:
                plugin.msg().send(player, "economy.unavailable");
                return false;
            case NOT_ENOUGH:
                plugin.msg().send(player, "upgrades.not-enough-money", "cost", plugin.economy().format(cost));
                return false;
            default:
                break;
        }
        int newLevel = level + 1;
        f.setUpgradeLevel(upgrade.id, newLevel);
        String name = upgrade.name;
        plugin.factions().log(f, "UPGRADE", "player", fp.getName(), "upgrade", name, "level", newLevel);
        plugin.msg().send(player, "upgrades.purchased", "upgrade", name, "level", newLevel);
        if (announce) {
            for (Player member : plugin.factions().getOnlinePlayers(f)) {
                if (member != player) {
                    plugin.msg().send(member, "upgrades.announce", "player", fp.getName(), "upgrade", name, "level", newLevel);
                }
            }
        }
        Sounds.play(player, plugin.settings().sound("amelioration"));
        afterLevelChange(f, upgrade);
        return true;
    }

    /** Applique les conséquences immédiates d'un changement de niveau. */
    public void afterLevelChange(Faction f, Upgrade upgrade) {
        if (upgrade.type == UpgradeType.CHEST) {
            plugin.chests().refresh(f);
        }
        if (upgrade.type == UpgradeType.POWER) {
            plugin.factions().checkRaidable(f);
        }
        plugin.top().invalidate();
    }

    // ================================================================ Effets

    private Faction territory(Location location) {
        Faction f = plugin.claims().getOwner(location);
        return f.isNormal() ? f : null;
    }

    private boolean roll(double percent) {
        return percent > 0 && random.nextDouble() * 100.0 < percent;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onGrow(BlockGrowEvent event) {
        final Block block = event.getBlock();
        final Material type = event.getNewState().getType();
        final int max;
        switch (type) {
            case CROPS:
            case CARROT:
            case POTATO:
                max = 7;
                break;
            case NETHER_WARTS:
                max = 3;
                break;
            default:
                return;
        }
        Faction f = territory(block.getLocation());
        if (f == null || !roll(getValue(f, UpgradeType.CROPS))) {
            return;
        }
        Bukkit.getScheduler().runTask(plugin, new Runnable() {
            @Override
            @SuppressWarnings("deprecation")
            public void run() {
                if (block.getType() == type && block.getData() < max) {
                    block.setData((byte) (block.getData() + 1));
                }
            }
        });
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDeath(EntityDeathEvent event) {
        if (event.getEntity() instanceof Player || event.getEntity().getKiller() == null || event.getDroppedExp() <= 0) {
            return;
        }
        Faction f = territory(event.getEntity().getLocation());
        if (f == null) {
            return;
        }
        double bonus = getValue(f, UpgradeType.EXP);
        if (bonus > 0) {
            event.setDroppedExp((int) Math.round(event.getDroppedExp() * (1 + bonus / 100.0)));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSpawnerSpawn(SpawnerSpawnEvent event) {
        Faction f = territory(event.getLocation());
        if (f != null && roll(getValue(f, UpgradeType.SPAWNERS))) {
            Entity extra = event.getLocation().getWorld().spawnEntity(event.getLocation(), event.getEntityType());
            // apparu hors spawner (raison CUSTOM) : marqué pour que les autres plugins le traitent comme un mob de spawner
            extra.setMetadata(SPAWNER_METADATA, new FixedMetadataValue(plugin, true));
        }
    }

    /** Réduction des dégâts subis par un membre dans le territoire de sa faction. */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player) || !enabled) {
            return;
        }
        if (damagePvpOnly && !(event instanceof EntityDamageByEntityEvent
                && ProtectionListener.responsiblePlayer(((EntityDamageByEntityEvent) event).getDamager()) != null)) {
            return;
        }
        Player victim = (Player) event.getEntity();
        FPlayer fp = plugin.players().get(victim);
        Faction here = territory(victim.getLocation());
        if (here == null || !here.getId().equals(fp.getFactionId())) {
            return;
        }
        double percent = Math.min(damageMaxPercent, getValue(here, UpgradeType.DAMAGE_REDUCTION));
        if (percent > 0) {
            event.setDamage(event.getDamage() * (1 - percent / 100.0));
        }
    }

    // ======================================================== Anti-sethome

    /** Un non-membre ne peut pas faire /sethome dans le territoire d'une faction qui a l'amélioration. */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        if (!enabled || sethomeCommands.isEmpty()) {
            return;
        }
        String label = event.getMessage().substring(1).split(" ", 2)[0].toLowerCase(Locale.ROOT);
        int colon = label.indexOf(':');
        if (colon >= 0) {
            label = label.substring(colon + 1);
        }
        if (!sethomeCommands.contains(label)) {
            return;
        }
        Player player = event.getPlayer();
        Faction here = territory(player.getLocation());
        if (here == null || getValue(here, UpgradeType.ANTI_SETHOME) <= 0) {
            return;
        }
        FPlayer fp = plugin.players().get(player);
        if (fp != null && here.getId().equals(fp.getFactionId())) {
            return;
        }
        event.setCancelled(true);
        plugin.msg().send(player, "upgrades.anti-sethome", "faction", here.getTag());
    }

    // ===================================================== Raid : obsidienne

    /**
     * Les explosions autorisées abîment l'obsidienne autour d'elles : chaque bloc casse
     * au bout de N coups (raid-obsidienne.coups), plus le bonus OBSIDIAN de la faction
     * propriétaire du terrain.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onExplode(EntityExplodeEvent event) {
        if (!obsidianEnabled || obsidianHits.isEmpty()) {
            return;
        }
        Location center = event.getLocation();
        if (obsidianIgnoreWater && center.getBlock().isLiquid()) {
            return;
        }
        long now = System.currentTimeMillis();
        int r = obsidianRadius;
        for (int x = -r; x <= r; x++) {
            for (int y = -r; y <= r; y++) {
                for (int z = -r; z <= r; z++) {
                    if (x * x + y * y + z * z > r * r) {
                        continue;
                    }
                    Block block = center.getBlock().getRelative(x, y, z);
                    Integer base = obsidianHits.get(block.getType());
                    if (base == null) {
                        continue;
                    }
                    Faction owner = territory(block.getLocation());
                    if (owner != null && !plugin.protectionService().isExplosionAllowed(block.getLocation(),
                            fr.koriamc.factions.model.ExplosionType.of(event.getEntity()))) {
                        continue;
                    }
                    int needed = base + (int) Math.round(getValue(owner, UpgradeType.OBSIDIAN));
                    String key = block.getWorld().getName() + ":" + block.getX() + ":" + block.getY() + ":" + block.getZ();
                    long[] state = blockDamage.get(key);
                    if (state == null || (obsidianResetMillis > 0 && now - state[1] > obsidianResetMillis)) {
                        state = new long[]{0, now};
                        blockDamage.put(key, state);
                    }
                    state[0]++;
                    state[1] = now;
                    if (state[0] >= needed) {
                        blockDamage.remove(key);
                        Material type = block.getType();
                        block.setType(Material.AIR);
                        block.getWorld().dropItemNaturally(block.getLocation().add(0.5, 0.5, 0.5), new ItemStack(type));
                    }
                }
            }
        }
        if (blockDamage.size() > 20000) {
            blockDamage.clear();
        }
    }

    /** Coups restants avant de casser ce bloc (0 si ce n'est pas un bloc renforcé). */
    public int getRemainingHits(Block block) {
        Integer base = obsidianHits.get(block.getType());
        if (base == null) {
            return 0;
        }
        int needed = base + (int) Math.round(getValue(territory(block.getLocation()), UpgradeType.OBSIDIAN));
        long[] state = blockDamage.get(block.getWorld().getName() + ":" + block.getX() + ":" + block.getY() + ":" + block.getZ());
        return (int) Math.max(1, needed - (state == null ? 0 : state[0]));
    }

    // ================================================ Machine de renforcement

    /** La faction propriétaire de ce terrain a-t-elle débloqué la machine de renforcement ? */
    public boolean isMachineAllowed(Location location) {
        return getValue(territory(location), UpgradeType.MACHINE) > 0;
    }
}
