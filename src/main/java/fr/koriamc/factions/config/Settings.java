package fr.koriamc.factions.config;

import fr.koriamc.factions.manager.TopManager;
import fr.koriamc.factions.model.ChatMode;
import fr.koriamc.factions.model.FactionFlag;
import fr.koriamc.factions.model.PermAction;
import fr.koriamc.factions.model.Relation;
import fr.koriamc.factions.model.Role;
import fr.koriamc.factions.model.TerritoryType;
import fr.koriamc.factions.model.Faction;
import fr.koriamc.factions.util.MaterialSet;
import org.bukkit.Effect;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.EventPriority;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Valeurs typées de config.json, factions.json et territoire.json, relues à chaque /f reload.
 */
public final class Settings {

    /** Informations d'affichage et valeur par défaut d'un flag. */
    public static final class FlagInfo {
        public final boolean def;
        public final boolean editable;
        public final String name;
        public final String description;
        public final String icon;

        FlagInfo(boolean def, boolean editable, String name, String description, String icon) {
            this.def = def;
            this.editable = editable;
            this.name = name;
            this.description = description;
            this.icon = icon;
        }
    }

    /** Grade du serveur (couleur et préfixe du pseudo dans le chat). */
    public static final class Rank {
        public final String permission;
        public final String name;
        public final String color;
        public final String prefix;

        Rank(String permission, String name, String color, String prefix) {
            this.permission = permission;
            this.name = name;
            this.color = color;
            this.prefix = prefix;
        }
    }

    public boolean debug;
    public Set<String> disabledWorlds;

    // Stockage
    public String storageType;
    public int autoSaveMinutes;
    public int jsonBackups;
    public boolean jsonPretty;
    public String sqliteFile;
    public String mysqlHost;
    public int mysqlPort;
    public String mysqlDatabase;
    public String mysqlUser;
    public String mysqlPassword;
    public String mysqlPrefix;
    public String mysqlParameters;

    // Factions
    public int tagMin;
    public int tagMax;
    public Pattern tagPattern;
    public List<String> tagBlacklist;
    public boolean tagBlacklistContains;
    public long renameCooldownMs;
    public int descriptionMax;
    public boolean motdEnabled;
    public int motdMax;
    public boolean motdOnJoin;
    public int motdDelayTicks;
    public int maxMembers;
    public Role joinRole;
    public int maxColeaders;
    public int maxOfficers;
    public long inviteExpireMs;
    public int maxInvites;
    public boolean inviteClickable;
    public long joinCooldownMs;
    public boolean disbandWhenEmpty;
    public boolean leaderCanLeave;
    public boolean denyLeaveRaidable;
    public boolean denyLeaveEnemyTerritory;
    public boolean denyDisbandRaidable;
    public boolean disbandDropChest;
    public boolean broadcastCreate;
    public boolean broadcastDisband;
    public boolean broadcastRename;
    public int titleMax;
    public boolean titleColors;
    public int confirmSeconds;
    public boolean confirmDisband;
    public boolean confirmUnclaimAll;
    public boolean confirmLeave;
    public boolean confirmKick;
    public boolean notifyLogin;
    public boolean notifyLogout;
    public String listSort;
    public int listPerPage;

    // Zones
    public String wildernessTag;
    public String wildernessDescription;
    public String wildernessColor;
    public String safezoneTag;
    public String safezoneDescription;
    public String safezoneColor;
    public String warzoneTag;
    public String warzoneDescription;
    public String warzoneColor;

    // Flags
    public final Map<FactionFlag, FlagInfo> flags = new EnumMap<FactionFlag, FlagInfo>(FactionFlag.class);

    // Puissance
    public double powerDefault;
    public double powerMin;
    public double powerMax;
    public Map<String, Double> powerMaxByPermission;
    public int regenIntervalSeconds;
    public double regenOnline;
    public double regenOffline;
    public Set<String> regenDisabledWorlds;
    public double deathLoss;
    public boolean deathOnlyPvp;
    public boolean peacefulNoLoss;
    public Set<String> noLossWorlds;
    public final Map<TerritoryType, Double> lossMultipliers = new EnumMap<TerritoryType, Double>(TerritoryType.class);
    public double killGain;
    public double factionMaxPower;
    public boolean countOfflinePower;
    public boolean announceRaidable;

    // Claims
    public boolean claimWorldsWhitelist;
    public Set<String> claimWorlds;
    public double powerPerClaim;
    public int maxClaims;
    public int maxClaimRadius;
    public boolean claimsMustBeConnected;
    public int bufferZone;
    public int minDistanceSpawn;
    public int minDistanceSafezone;
    public int minDistanceWarzone;
    public int claimEnemyRadius;
    public boolean overclaimEnabled;
    public boolean overclaimRequireRaidable;
    public boolean overclaimRequireBorder;
    public boolean overclaimAllowUnconnected;
    public boolean overclaimNotify;
    public boolean overclaimBroadcast;
    public boolean removeHomeWhenUnclaimed;
    public boolean removeWarpsWhenUnclaimed;
    public boolean accessEnabled;
    public Set<PermAction> accessGrants;

    // Relations (affichage : membres et ennemis)
    public final Map<Relation, String> relationNames = new EnumMap<Relation, String>(Relation.class);
    public final Map<Relation, String> relationColors = new EnumMap<Relation, String>(Relation.class);

    // PvP
    public boolean friendlyFire;
    public boolean enemyDamage;
    public boolean factionlessCanAttack;
    public boolean factionlessCanBeAttacked;
    public boolean peacefulNoPvp;
    public Set<String> pvpDisabledWorlds;
    public long pvpDenyCooldownMs;
    public boolean statsOnlyPvp;

    // Home
    public boolean homeEnabled;
    public boolean homeMustBeInTerritory;
    public int homeEnemyRadius;
    public boolean homeDenyFromEnemy;
    public boolean homeDenyCrossWorld;
    public Set<String> homeDeniedWorlds;
    public boolean respawnAtHome;

    // Warps
    public boolean warpsEnabled;
    public int maxWarps;
    public boolean warpMustBeInTerritory;
    public int warpNameMax;
    public Pattern warpNamePattern;
    public int warpEnemyRadius;
    public boolean warpDenyFromEnemy;
    public boolean warpPasswords;

    // Téléportations
    public final Map<String, Integer> warmups = new HashMap<String, Integer>();
    public final Map<String, Integer> teleportCooldowns = new HashMap<String, Integer>();
    public boolean instantInOwnTerritory;
    public int instantEnemyRadius;
    public boolean teleportCancelOnMove;
    public boolean teleportCancelOnDamage;
    public String countdownDisplay;
    public String soundTick;
    public String soundSuccess;
    public String soundCancel;

    // Notifications de territoire
    public boolean notificationsEnabled;
    public Set<String> notificationDisplays;
    /** Layout KoriaMC du toast de territoire (koriamc/guis/<id>.json). */
    public String toastLayout;
    public int toastDurationMs;
    public boolean installToast;
    /** Affichage pour les joueurs sans le client KoriaMC (ACTIONBAR, CHAT, TITLE ou vide). */
    public String toastFallback;
    public final java.util.Map<String, String> toastIcons = new java.util.HashMap<String, String>();
    public int titleFadeIn;
    public int titleStay;
    public int titleFadeOut;
    public boolean notificationsDefault;

    // Carte
    public int mapWidth;
    public int mapHeight;
    public String mapSymbols;
    public String mapSymbolYou;
    public String mapSymbolWilderness;
    public String mapSymbolSafezone;
    public String mapSymbolWarzone;
    public String mapColorYou;
    public boolean mapCompass;
    public boolean mapHover;
    public boolean mapClickToClaim;
    public int mapLegendMax;
    public int mapClickMaxDistance;

    // Seechunk
    public boolean seeChunkEnabled;
    public Effect seeChunkEffect;
    public int seeChunkInterval;
    public int seeChunkBelow;
    public int seeChunkAbove;
    public int seeChunkSpacing;

    // Chat
    public boolean chatEnabled;
    public EventPriority chatPriority;
    public String chatPublicMode;
    public String chatPublicFormat;
    public String chatPlaceholder;
    public String chatTagFormat;
    public String chatTagNoFaction;
    public String chatTagDefaultColor;
    public boolean chatRelationColorsPerRecipient;
    public final Map<ChatMode, String> chatFormats = new EnumMap<ChatMode, String>(ChatMode.class);
    public Role chatModMinRole;
    public String chatSpyFormat;
    public boolean chatLogPrivate;
    public final Map<ChatMode, String> chatQuickPrefixes = new EnumMap<ChatMode, String>(ChatMode.class);
    public final Map<String, ChatMode> chatAliases = new HashMap<String, ChatMode>();
    public final List<Rank> ranks = new ArrayList<Rank>();
    public Rank defaultRank;

    // Journal
    public boolean logsEnabled;
    public int logsMax;
    public int logsPerPage;
    public String logsDateFormat;

    // Classements
    public int topPerPage;
    public int topCacheSeconds;
    public List<String> topCategories;
    public String topDefault;

    // Coffre
    public boolean chestEnabled;
    public int chestRows;
    public String chestTitle;
    public MaterialSet chestBlacklist;

    // Inactivité
    public boolean inactivityEnabled;
    public int inactivityIntervalMinutes;
    public int inactivityKickDays;
    public int inactivityDisbandDays;
    public boolean inactivityTransferLeader;
    public int inactivityPurgeDays;

    // Annonces
    public int announceMax;
    public boolean announceTitle;

    // Sons
    public final Map<String, String> sounds = new HashMap<String, String>();

    /** Chaîne de configuration (valeur du jar si absente). */
    static String str(ConfigurationSection c, String path, String def) {
        String value = c.getString(path);
        return value == null ? def : value;
    }

    /**
     * Lit les réglages de config.json, factions.json et territoire.json, réunis
     * dans une seule vue (leurs sections racines sont distinctes).
     */
    public void load(ConfigurationSection c, Logger logger) {
        // ------------------------------------------------------ config.json
        debug = c.getBoolean("debug");
        disabledWorlds = lowerSet(c.getStringList("mondes-desactives"));

        storageType = str(c, "stockage.type", "JSON").toUpperCase(Locale.ROOT);
        autoSaveMinutes = c.getInt("stockage.sauvegarde-auto-minutes");
        jsonBackups = c.getInt("stockage.copies-de-securite");
        jsonPretty = c.getBoolean("stockage.json-indente");
        sqliteFile = str(c, "stockage.sqlite-fichier", "database.db");
        mysqlHost = c.getString("stockage.mysql.hote");
        mysqlPort = c.getInt("stockage.mysql.port");
        mysqlDatabase = c.getString("stockage.mysql.base");
        mysqlUser = c.getString("stockage.mysql.utilisateur");
        mysqlPassword = str(c, "stockage.mysql.mot-de-passe", "");
        mysqlPrefix = str(c, "stockage.mysql.prefixe-tables", "");
        mysqlParameters = str(c, "stockage.mysql.parametres", "");

        chatEnabled = c.getBoolean("chat.actif");
        try {
            chatPriority = EventPriority.valueOf(str(c, "chat.priorite", "HIGHEST").toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            chatPriority = EventPriority.HIGHEST;
        }
        chatPublicMode = str(c, "chat.mode-public", "FORMAT").toUpperCase(Locale.ROOT);
        chatPublicFormat = str(c, "chat.format-public", "{tag}{player}: {message}");
        chatPlaceholder = str(c, "chat.placeholder", "{FACTION}");
        chatTagFormat = str(c, "chat.tag", "");
        chatTagNoFaction = str(c, "chat.tag-sans-faction", "");
        chatTagDefaultColor = str(c, "chat.couleur-tag-par-defaut", "");
        chatRelationColorsPerRecipient = c.getBoolean("chat.couleur-par-destinataire");
        chatFormats.clear();
        chatQuickPrefixes.clear();
        chatAliases.clear();
        for (ChatMode mode : ChatMode.values()) {
            chatAliases.put(mode.name().toLowerCase(Locale.ROOT), mode);
            for (String alias : c.getStringList("chat.noms-des-canaux." + mode.name())) {
                chatAliases.put(alias.trim().toLowerCase(Locale.ROOT), mode);
            }
            if (mode != ChatMode.PUBLIC) {
                chatFormats.put(mode, str(c, "chat.canaux." + mode.name(), "{player}: {message}"));
                String prefix = str(c, "chat.prefixes-rapides." + mode.name(), "");
                if (!prefix.isEmpty()) {
                    chatQuickPrefixes.put(mode, prefix);
                }
            }
        }
        ranks.clear();
        ConfigurationSection rankSection = c.getConfigurationSection("chat.grades");
        if (rankSection != null) {
            for (String id : rankSection.getKeys(false)) {
                ConfigurationSection r = rankSection.getConfigurationSection(id);
                if (r == null || str(r, "permission", "").isEmpty()) {
                    logger.warning("Grade de chat '" + id + "' ignoré : permission manquante.");
                    continue;
                }
                ranks.add(new Rank(r.getString("permission"), str(r, "nom", id), str(r, "couleur", ""), str(r, "prefixe", "")));
            }
        }
        defaultRank = new Rank("", str(c, "chat.sans-grade.nom", ""), str(c, "chat.sans-grade.couleur", ""),
                str(c, "chat.sans-grade.prefixe", ""));
        chatModMinRole = parseRole(c.getString("chat.role-min-canal-mod"), Role.OFFICER);
        chatSpyFormat = str(c, "chat.format-espion", "{message}");
        chatLogPrivate = c.getBoolean("chat.canaux-dans-la-console");

        topPerPage = Math.max(1, c.getInt("classement.par-page"));
        topCacheSeconds = Math.max(0, c.getInt("classement.cache-secondes"));
        topCategories = new ArrayList<String>();
        for (String s : c.getStringList("classement.categories")) {
            String category = s.toUpperCase(Locale.ROOT);
            if (Arrays.asList(TopManager.CATEGORIES).contains(category)) {
                topCategories.add(category);
            } else {
                logger.warning("Catégorie inconnue dans classement.categories : " + s);
            }
        }
        topDefault = str(c, "classement.par-defaut", "POWER").toUpperCase(Locale.ROOT);
        if (!topCategories.contains(topDefault) && !topCategories.isEmpty()) {
            topDefault = topCategories.get(0);
        }

        logsEnabled = c.getBoolean("journal.actif");
        logsMax = c.getInt("journal.entrees-max");
        logsPerPage = Math.max(1, c.getInt("journal.par-page"));
        logsDateFormat = str(c, "journal.format-date", "dd/MM HH:mm");

        announceMax = c.getInt("annonces.longueur-max");
        announceTitle = c.getBoolean("annonces.en-titre");

        sounds.clear();
        ConfigurationSection soundSection = c.getConfigurationSection("sons");
        if (soundSection != null) {
            for (String key : soundSection.getKeys(false)) {
                sounds.put(key, str(soundSection, key, ""));
            }
        }

        // ---------------------------------------------------- factions.json
        tagMin = c.getInt("factions.nom-longueur-min");
        tagMax = c.getInt("factions.nom-longueur-max");
        tagPattern = pattern(c.getString("factions.nom-caracteres"), "^[a-zA-Z0-9_]+$", logger);
        tagBlacklist = new ArrayList<String>();
        for (String s : c.getStringList("factions.noms-interdits")) {
            tagBlacklist.add(s.toLowerCase(Locale.ROOT));
        }
        tagBlacklistContains = c.getBoolean("factions.noms-interdits-si-contenus");
        renameCooldownMs = c.getLong("factions.delai-renommage-minutes") * 60000L;
        descriptionMax = c.getInt("factions.description-longueur-max");
        motdEnabled = c.getBoolean("factions.message-du-jour.actif");
        motdMax = c.getInt("factions.message-du-jour.longueur-max");
        motdOnJoin = c.getBoolean("factions.message-du-jour.a-la-connexion");
        motdDelayTicks = c.getInt("factions.message-du-jour.delai-ticks");
        maxMembers = c.getInt("factions.membres-max");
        joinRole = parseRole(c.getString("factions.role-a-l-arrivee"), Role.RECRUIT);
        if (joinRole.isAtLeast(Role.COLEADER)) {
            joinRole = Role.MEMBER;
        }
        maxColeaders = c.getInt("factions.co-chefs-max");
        maxOfficers = c.getInt("factions.officiers-max");
        inviteExpireMs = c.getLong("factions.invitations.expiration-minutes") * 60000L;
        maxInvites = c.getInt("factions.invitations.max-en-attente");
        inviteClickable = c.getBoolean("factions.invitations.cliquables");
        joinCooldownMs = c.getLong("factions.delai-avant-rejoindre-minutes") * 60000L;
        disbandWhenEmpty = c.getBoolean("factions.dissoudre-si-vide");
        leaderCanLeave = c.getBoolean("factions.le-chef-peut-quitter");
        denyLeaveRaidable = c.getBoolean("factions.quitter-interdit-si-raidable");
        denyLeaveEnemyTerritory = c.getBoolean("factions.quitter-interdit-en-territoire-ennemi");
        denyDisbandRaidable = c.getBoolean("factions.dissolution-interdite-si-raidable");
        disbandDropChest = c.getBoolean("factions.dissolution-lache-le-coffre");
        broadcastCreate = c.getBoolean("factions.annonces-globales.creation");
        broadcastDisband = c.getBoolean("factions.annonces-globales.dissolution");
        broadcastRename = c.getBoolean("factions.annonces-globales.renommage");
        titleMax = c.getInt("factions.titres.longueur-max");
        titleColors = c.getBoolean("factions.titres.couleurs");
        confirmSeconds = c.getInt("factions.confirmations.secondes");
        confirmDisband = c.getBoolean("factions.confirmations.dissolution");
        confirmUnclaimAll = c.getBoolean("factions.confirmations.unclaim-tout");
        confirmLeave = c.getBoolean("factions.confirmations.quitter");
        confirmKick = c.getBoolean("factions.confirmations.expulsion");
        notifyLogin = c.getBoolean("factions.notifier-connexions");
        notifyLogout = c.getBoolean("factions.notifier-deconnexions");
        listSort = str(c, "factions.liste.tri", "ONLINE").toUpperCase(Locale.ROOT);
        listPerPage = Math.max(1, c.getInt("factions.liste.par-page"));

        flags.clear();
        for (FactionFlag flag : FactionFlag.values()) {
            String p = "flags." + flag.name() + ".";
            flags.put(flag, new FlagInfo(c.getBoolean(p + "defaut"), c.getBoolean(p + "modifiable"),
                    str(c, p + "nom", flag.name()), str(c, p + "description", ""), str(c, p + "icone", "PAPER")));
        }

        relationNames.clear();
        relationColors.clear();
        for (Relation relation : Relation.values()) {
            relationNames.put(relation, str(c, "relations.affichage." + relation.name() + ".nom", relation.name()));
            relationColors.put(relation, str(c, "relations.affichage." + relation.name() + ".couleur", "&f"));
        }

        powerDefault = c.getDouble("puissance.depart");
        powerMin = c.getDouble("puissance.min");
        powerMax = c.getDouble("puissance.max");
        powerMaxByPermission = new LinkedHashMap<String, Double>();
        ConfigurationSection grades = c.getConfigurationSection("puissance.max-par-grade");
        if (grades != null) {
            for (String id : grades.getKeys(false)) {
                String permission = grades.getString(id + ".permission");
                if (permission == null || permission.isEmpty()) {
                    logger.warning("Grade de puissance '" + id + "' ignoré : permission manquante.");
                } else {
                    powerMaxByPermission.put(permission, grades.getDouble(id + ".max"));
                }
            }
        }
        regenIntervalSeconds = Math.max(1, c.getInt("puissance.regeneration.intervalle-secondes"));
        regenOnline = c.getDouble("puissance.regeneration.par-minute-connecte");
        regenOffline = c.getDouble("puissance.regeneration.par-minute-deconnecte");
        regenDisabledWorlds = lowerSet(c.getStringList("puissance.regeneration.mondes-exclus"));
        deathLoss = c.getDouble("puissance.perte-a-la-mort");
        deathOnlyPvp = c.getBoolean("puissance.perte-seulement-pvp");
        peacefulNoLoss = c.getBoolean("puissance.pacifiques-sans-perte");
        noLossWorlds = lowerSet(c.getStringList("puissance.mondes-sans-perte"));
        lossMultipliers.clear();
        for (TerritoryType type : TerritoryType.values()) {
            String path = "puissance.multiplicateurs-de-perte." + type.name();
            lossMultipliers.put(type, c.contains(path) ? c.getDouble(path) : 1.0);
        }
        killGain = c.getDouble("puissance.gain-par-kill");
        factionMaxPower = c.getDouble("puissance.faction-max");
        countOfflinePower = c.getBoolean("puissance.compter-hors-ligne");
        announceRaidable = c.getBoolean("puissance.alerte-raidable");

        friendlyFire = c.getBoolean("pvp.entre-membres");
        enemyDamage = c.getBoolean("pvp.entre-ennemis");
        factionlessCanAttack = c.getBoolean("pvp.sans-faction-peut-attaquer");
        factionlessCanBeAttacked = c.getBoolean("pvp.sans-faction-peut-etre-attaque");
        peacefulNoPvp = c.getBoolean("pvp.pacifiques-sans-pvp");
        pvpDisabledWorlds = lowerSet(c.getStringList("pvp.mondes-sans-pvp"));
        pvpDenyCooldownMs = c.getLong("pvp.anti-spam-message-ms");
        statsOnlyPvp = c.getBoolean("pvp.stats-seulement-pvp");

        homeEnabled = c.getBoolean("home.actif");
        homeMustBeInTerritory = c.getBoolean("home.dans-son-territoire");
        homeEnemyRadius = Math.max(0, c.getInt("home.refus-si-ennemi-a-moins-de"));
        homeDenyFromEnemy = c.getBoolean("home.refus-depuis-territoire-ennemi");
        homeDenyCrossWorld = c.getBoolean("home.refus-autre-monde");
        homeDeniedWorlds = lowerSet(c.getStringList("home.mondes-interdits"));
        respawnAtHome = c.getBoolean("home.reapparaitre-au-home");

        warpsEnabled = c.getBoolean("warps.actif");
        maxWarps = c.getInt("warps.max");
        warpMustBeInTerritory = c.getBoolean("warps.dans-son-territoire");
        warpNameMax = c.getInt("warps.nom-longueur-max");
        warpNamePattern = pattern(c.getString("warps.nom-caracteres"), "^[a-zA-Z0-9_-]+$", logger);
        warpEnemyRadius = Math.max(0, c.getInt("warps.refus-si-ennemi-a-moins-de"));
        warpDenyFromEnemy = c.getBoolean("warps.refus-depuis-territoire-ennemi");
        warpPasswords = c.getBoolean("warps.mots-de-passe");

        warmups.clear();
        teleportCooldowns.clear();
        for (String type : new String[]{"home", "warp"}) {
            warmups.put(type, c.getInt("teleportation.attente-secondes." + type));
            teleportCooldowns.put(type, c.getInt("teleportation.delai-secondes." + type));
        }
        instantInOwnTerritory = c.getBoolean("teleportation.instantanee-dans-son-territoire");
        instantEnemyRadius = Math.max(0, c.getInt("teleportation.rayon-ennemi-instantanee"));
        teleportCancelOnMove = c.getBoolean("teleportation.annuler-si-mouvement");
        teleportCancelOnDamage = c.getBoolean("teleportation.annuler-si-degats");
        countdownDisplay = str(c, "teleportation.compte-a-rebours", "ACTIONBAR").toUpperCase(Locale.ROOT);
        soundTick = str(c, "teleportation.sons.tic", "");
        soundSuccess = str(c, "teleportation.sons.reussite", "");
        soundCancel = str(c, "teleportation.sons.annulation", "");

        chestEnabled = c.getBoolean("coffre.actif");
        chestRows = Math.max(0, Math.min(6, c.getInt("coffre.lignes-de-base")));
        chestTitle = str(c, "coffre.titre", "Coffre");
        chestBlacklist = MaterialSet.of(c.getStringList("coffre.objets-interdits"), logger, "coffre.objets-interdits");

        inactivityEnabled = c.getBoolean("inactivite.actif");
        inactivityIntervalMinutes = Math.max(1, c.getInt("inactivite.verification-minutes"));
        inactivityKickDays = c.getInt("inactivite.expulsion-apres-jours");
        inactivityDisbandDays = c.getInt("inactivite.dissolution-apres-jours");
        inactivityTransferLeader = c.getBoolean("inactivite.transfert-du-chef");
        inactivityPurgeDays = c.getInt("inactivite.purge-joueurs-apres-jours");

        // -------------------------------------------------- territoire.json
        claimWorldsWhitelist = "WHITELIST".equalsIgnoreCase(c.getString("claims.mondes-mode"));
        claimWorlds = lowerSet(c.getStringList("claims.mondes"));
        powerPerClaim = c.getDouble("claims.puissance-par-claim");
        maxClaims = c.getInt("claims.max-par-faction");
        maxClaimRadius = Math.max(0, c.getInt("claims.rayon-max"));
        claimsMustBeConnected = c.getBoolean("claims.connectes");
        bufferZone = Math.max(0, c.getInt("claims.zone-tampon"));
        minDistanceSpawn = Math.max(0, c.getInt("claims.distance-min-spawn"));
        minDistanceSafezone = Math.max(0, c.getInt("claims.distance-min-safezone"));
        minDistanceWarzone = Math.max(0, c.getInt("claims.distance-min-warzone"));
        claimEnemyRadius = Math.max(0, c.getInt("claims.refus-si-ennemi-a-moins-de"));
        overclaimEnabled = c.getBoolean("claims.overclaim.actif");
        overclaimRequireRaidable = c.getBoolean("claims.overclaim.cible-raidable");
        overclaimRequireBorder = c.getBoolean("claims.overclaim.bordure-seulement");
        overclaimAllowUnconnected = c.getBoolean("claims.overclaim.sans-connexion");
        overclaimNotify = c.getBoolean("claims.overclaim.prevenir-la-victime");
        overclaimBroadcast = c.getBoolean("claims.overclaim.annonce-globale");
        removeHomeWhenUnclaimed = c.getBoolean("claims.supprimer-home-hors-territoire");
        removeWarpsWhenUnclaimed = c.getBoolean("claims.supprimer-warps-hors-territoire");
        accessEnabled = c.getBoolean("claims.acces-par-chunk");
        accessGrants = actionSet(c.getStringList("claims.acces-actions"), logger, "claims.acces-actions");

        wildernessTag = str(c, "zones.wilderness.nom", "Wilderness");
        wildernessDescription = str(c, "zones.wilderness.description", "");
        wildernessColor = str(c, "zones.wilderness.couleur", "&2");
        safezoneTag = str(c, "zones.safezone.nom", "SafeZone");
        safezoneDescription = str(c, "zones.safezone.description", "");
        safezoneColor = str(c, "zones.safezone.couleur", "&6");
        warzoneTag = str(c, "zones.warzone.nom", "WarZone");
        warzoneDescription = str(c, "zones.warzone.description", "");
        warzoneColor = str(c, "zones.warzone.couleur", "&4");

        notificationsEnabled = c.getBoolean("notifications.actif");
        notificationDisplays = new HashSet<String>();
        // "types" remplace "affichage" (TITLE par défaut) : les gros titres ne reviennent pas avec un ancien fichier
        for (String s : c.getStringList("notifications.types")) {
            notificationDisplays.add(s.toUpperCase(Locale.ROOT));
        }
        toastLayout = str(c, "notifications.toast.layout", "territoire").trim().toLowerCase(Locale.ROOT);
        toastDurationMs = Math.max(500, c.getInt("notifications.toast.duree-ms", 3000));
        installToast = c.getBoolean("notifications.toast.installer-layout", true);
        toastFallback = str(c, "notifications.toast.sans-client-koriamc", "ACTIONBAR").trim().toUpperCase(Locale.ROOT);
        toastIcons.clear();
        org.bukkit.configuration.ConfigurationSection icons = c.getConfigurationSection("notifications.toast.icones");
        if (icons != null) {
            for (String key : icons.getKeys(false)) {
                toastIcons.put(key.toUpperCase(Locale.ROOT), icons.getString(key, ""));
            }
        }
        titleFadeIn = c.getInt("notifications.titre-apparition");
        titleStay = c.getInt("notifications.titre-duree");
        titleFadeOut = c.getInt("notifications.titre-disparition");
        notificationsDefault = c.getBoolean("notifications.actives-par-defaut");

        mapWidth = Math.max(9, c.getInt("carte.largeur"));
        mapHeight = Math.max(3, c.getInt("carte.hauteur"));
        mapSymbols = str(c, "carte.symboles", "#");
        if (mapSymbols.isEmpty()) {
            mapSymbols = "#";
        }
        mapSymbolYou = str(c, "carte.symbole-joueur", "+");
        mapSymbolWilderness = str(c, "carte.symbole-wilderness", "-");
        mapSymbolSafezone = str(c, "carte.symbole-safezone", "+");
        mapSymbolWarzone = str(c, "carte.symbole-warzone", "+");
        mapColorYou = str(c, "carte.couleur-joueur", "&e");
        mapCompass = c.getBoolean("carte.boussole");
        mapHover = c.getBoolean("carte.infos-au-survol");
        mapClickToClaim = c.getBoolean("carte.claim-au-clic");
        mapLegendMax = c.getInt("carte.legende-max");
        mapClickMaxDistance = c.getInt("carte.distance-max-claim-au-clic");

        seeChunkEnabled = c.getBoolean("seechunk.actif");
        try {
            seeChunkEffect = Effect.valueOf(str(c, "seechunk.particule", "FLAME").toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            logger.warning("Effet inconnu pour seechunk.particule, FLAME utilisé.");
            seeChunkEffect = Effect.FLAME;
        }
        seeChunkInterval = Math.max(1, c.getInt("seechunk.intervalle-ticks"));
        seeChunkBelow = Math.max(0, c.getInt("seechunk.hauteur-dessous"));
        seeChunkAbove = Math.max(0, c.getInt("seechunk.hauteur-dessus"));
        seeChunkSpacing = Math.max(1, c.getInt("seechunk.espacement"));
    }

    // --------------------------------------------------------------- Helpers

    public boolean isWorldDisabled(String world) {
        return world != null && disabledWorlds.contains(world.toLowerCase(Locale.ROOT));
    }

    public boolean isClaimWorld(String world) {
        boolean listed = claimWorlds.contains(world.toLowerCase(Locale.ROOT));
        return claimWorldsWhitelist == listed && !isWorldDisabled(world);
    }

    public String relationName(Relation relation) {
        return relationNames.get(relation);
    }

    public String relationColor(Relation relation) {
        return relationColors.get(relation);
    }

    /** Nom de relation coloré, prêt à être inséré dans un message. */
    public String coloredRelation(Relation relation) {
        return relationColor(relation) + relationName(relation);
    }

    /** Premier grade de la liste dont le joueur possède la permission. */
    public Rank rankOf(org.bukkit.permissions.Permissible player) {
        for (Rank rank : ranks) {
            if (player.hasPermission(rank.permission)) {
                return rank;
            }
        }
        return defaultRank;
    }

    public String sound(String key) {
        String s = sounds.get(key);
        return s == null ? "" : s;
    }

    public static Set<String> lowerSet(List<String> list) {
        Set<String> set = new HashSet<String>();
        for (String s : list) {
            set.add(s.toLowerCase(Locale.ROOT));
        }
        return set;
    }

    private static Role parseRole(String value, Role def) {
        Role role = Role.parse(value);
        return role == null ? def : role;
    }

    private static Pattern pattern(String value, String def, Logger logger) {
        try {
            return Pattern.compile(value == null ? def : value);
        } catch (PatternSyntaxException e) {
            logger.warning("Expression régulière invalide '" + value + "', valeur par défaut utilisée.");
            return Pattern.compile(def);
        }
    }

    static Set<PermAction> actionSet(List<String> names, Logger logger, String context) {
        Set<PermAction> set = EnumSet.noneOf(PermAction.class);
        for (String name : names) {
            if ("*".equals(name.trim())) {
                set.addAll(EnumSet.allOf(PermAction.class));
                continue;
            }
            PermAction action = PermAction.parse(name);
            if (action == null) {
                logger.warning("Action inconnue '" + name + "' dans " + context);
            } else {
                set.add(action);
            }
        }
        return set;
    }

    /** Couleur d'une zone système. */
    public String zoneColor(String factionId) {
        if (Faction.SAFEZONE.equals(factionId)) {
            return safezoneColor;
        }
        if (Faction.WARZONE.equals(factionId)) {
            return warzoneColor;
        }
        return wildernessColor;
    }

    public boolean isChestItemBlacklisted(org.bukkit.inventory.ItemStack item) {
        return chestBlacklist != null && chestBlacklist.contains(item);
    }
}
