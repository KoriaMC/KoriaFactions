package fr.koriamc.factions.manager;

import fr.koriamc.factions.KoriaFactions;
import fr.koriamc.factions.api.event.FactionDisbandEvent;
import fr.koriamc.factions.api.event.FactionMembershipEvent;
import fr.koriamc.factions.api.event.FactionPointsChangeEvent;
import fr.koriamc.factions.config.RoleSettings;
import fr.koriamc.factions.config.Settings;
import fr.koriamc.factions.model.AutoClaimMode;
import fr.koriamc.factions.model.FPlayer;
import fr.koriamc.factions.model.Faction;
import fr.koriamc.factions.model.FactionFlag;
import fr.koriamc.factions.model.LogEntry;
import fr.koriamc.factions.model.PermAction;
import fr.koriamc.factions.model.PermSubject;
import fr.koriamc.factions.model.Relation;
import fr.koriamc.factions.model.Role;
import fr.koriamc.factions.model.TerritoryType;
import fr.koriamc.factions.model.UpgradeType;
import fr.koriamc.factions.util.Sounds;
import fr.koriamc.factions.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Registre des factions : création, dissolution, membres, puissance,
 * flags et permissions.
 */
public final class FactionManager {

    private final KoriaFactions plugin;
    private final Map<String, Faction> byId = new HashMap<String, Faction>();
    private final Map<String, Faction> byTag = new HashMap<String, Faction>();

    public FactionManager(KoriaFactions plugin) {
        this.plugin = plugin;
    }

    // ============================================================ Chargement

    public void load(Collection<Faction> factions) {
        byId.clear();
        byTag.clear();
        for (Faction f : factions) {
            if (f.getId() == null) {
                continue;
            }
            f.ensureInit();
            byId.put(f.getId(), f);
        }
        ensureSystemFactions();
        // l'appartenance est portée par les joueurs : on reconstruit l'index
        for (FPlayer fp : plugin.players().getAll()) {
            if (fp.getFactionId() == null) {
                continue;
            }
            Faction f = byId.get(fp.getFactionId());
            if (f == null || f.isSystem()) {
                plugin.getLogger().warning("Le joueur " + fp.getName() + " appartenait à une faction inexistante, il en est retiré.");
                fp.setFaction(null, null);
            } else {
                f.getMemberIds().add(fp.getUniqueId());
            }
        }
        // cohérence : une faction sans chef en reçoit un
        for (Faction f : byId.values()) {
            if (f.isNormal() && !f.getMemberIds().isEmpty() && getLeader(f) == null) {
                FPlayer heir = findHeir(f, null);
                if (heir != null) {
                    heir.setRole(Role.LEADER);
                    plugin.getLogger().warning("La faction " + f.getTag() + " n'avait pas de chef : " + heir.getName() + " a été nommé.");
                }
            }
            f.setWasRaidable(isRaidable(f));
        }
        reindexTags();
    }

    private void ensureSystemFactions() {
        for (String id : new String[]{Faction.WILDERNESS, Faction.SAFEZONE, Faction.WARZONE}) {
            if (!byId.containsKey(id)) {
                Faction f = new Faction(id, id);
                f.ensureInit();
                byId.put(id, f);
            }
        }
        applySystemSettings();
    }

    /** Applique les noms / descriptions des zones système depuis la configuration. */
    public void applySystemSettings() {
        Settings s = plugin.settings();
        getWilderness().setTag(s.wildernessTag);
        getWilderness().setDescription(s.wildernessDescription);
        getSafeZone().setTag(s.safezoneTag);
        getSafeZone().setDescription(s.safezoneDescription);
        getWarZone().setTag(s.warzoneTag);
        getWarZone().setDescription(s.warzoneDescription);
        reindexTags();
    }

    private void reindexTags() {
        byTag.clear();
        for (Faction f : byId.values()) {
            byTag.put(normalize(f.getTag()), f);
        }
    }

    private static String normalize(String tag) {
        return Text.strip(tag).toLowerCase(Locale.ROOT);
    }

    // =============================================================== Accès

    public Faction getById(String id) {
        return id == null ? null : byId.get(id);
    }

    public Faction getByTag(String tag) {
        return tag == null ? null : byTag.get(normalize(tag));
    }

    public Faction getWilderness() {
        return byId.get(Faction.WILDERNESS);
    }

    public Faction getSafeZone() {
        return byId.get(Faction.SAFEZONE);
    }

    public Faction getWarZone() {
        return byId.get(Faction.WARZONE);
    }

    public Collection<Faction> getAll() {
        return Collections.unmodifiableCollection(byId.values());
    }

    public List<Faction> getNormalFactions() {
        List<Faction> list = new ArrayList<Faction>();
        for (Faction f : byId.values()) {
            if (f.isNormal()) {
                list.add(f);
            }
        }
        return list;
    }

    public List<String> getNormalTags() {
        List<String> tags = new ArrayList<String>();
        for (Faction f : byId.values()) {
            if (f.isNormal()) {
                tags.add(f.getTag());
            }
        }
        return tags;
    }

    public Faction getFaction(FPlayer fp) {
        return plugin.players().getFaction(fp);
    }

    public Faction getFaction(Player player) {
        return getFaction(plugin.players().get(player));
    }

    // =================================================== Création / nommage

    /**
     * Vérifie un nom de faction.
     *
     * @return la clé du message d'erreur, ou null si le nom est valide
     */
    public String validateTag(String tag, Faction ignore) {
        Settings s = plugin.settings();
        String plain = Text.strip(tag);
        if (plain.length() < s.tagMin) {
            return "create.name-too-short";
        }
        if (plain.length() > s.tagMax) {
            return "create.name-too-long";
        }
        if (!s.tagPattern.matcher(tag).matches()) {
            return "create.name-invalid";
        }
        String lower = plain.toLowerCase(Locale.ROOT);
        for (String word : s.tagBlacklist) {
            if (s.tagBlacklistContains ? lower.contains(word) : lower.equals(word)) {
                return "create.name-blacklisted";
            }
        }
        Faction existing = getByTag(plain);
        if (existing != null && existing != ignore) {
            return "create.name-taken";
        }
        return null;
    }

    public Faction create(String tag, FPlayer leader) {
        String id;
        do {
            id = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        } while (byId.containsKey(id));
        Faction f = new Faction(id, tag);
        f.ensureInit();
        byId.put(id, f);
        byTag.put(normalize(tag), f);
        addMember(f, leader, Role.LEADER);
        log(f, "CREATE", "player", leader.getName());
        plugin.top().invalidate();
        return f;
    }

    public void rename(Faction f, String tag) {
        byTag.remove(normalize(f.getTag()));
        f.setTag(tag);
        f.setLastRenamed(System.currentTimeMillis());
        byTag.put(normalize(tag), f);
    }

    /**
     * Dissout une faction.
     *
     * @param actor  pseudo de l'auteur (peut être null)
     * @param player joueur à l'origine de la dissolution (pour le coffre), ou null
     * @param force  ignore l'annulation de l'évènement
     * @return false si l'évènement a été annulé
     */
    public boolean disband(Faction f, String actor, Player player, FactionDisbandEvent.Reason reason, boolean force) {
        if (!f.isNormal()) {
            return false;
        }
        FactionDisbandEvent event = new FactionDisbandEvent(f, actor, reason);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled() && !force) {
            return false;
        }
        Settings s = plugin.settings();
        plugin.chests().handleDisband(f, player);
        plugin.claims().unclaimAll(f.getId());

        for (UUID uuid : new ArrayList<UUID>(f.getMemberIds())) {
            FPlayer fp = plugin.players().get(uuid);
            if (fp == null) {
                continue;
            }
            Player online = fp.getPlayer();
            if (online != null && reason != FactionDisbandEvent.Reason.EMPTY) {
                plugin.msg().send(online, "disband.notify-members", "player", actor == null ? "?" : actor);
            }
            detach(f, fp);
        }
        for (FPlayer fp : plugin.players().getAll()) {
            if (f.getId().equals(fp.getAutoClaimTarget())) {
                fp.setAutoClaim(AutoClaimMode.NONE, null);
            }
        }
        byId.remove(f.getId());
        byTag.remove(normalize(f.getTag()));
        plugin.top().invalidate();

        if (s.broadcastDisband) {
            plugin.msg().broadcast("disband.broadcast", "faction", f.getTag(), "player", actor == null ? "?" : actor);
        }
        return true;
    }

    // =============================================================== Membres

    public void addMember(Faction f, FPlayer fp, Role role) {
        fp.setFaction(f.getId(), role);
        f.getMemberIds().add(fp.getUniqueId());
        f.deinvite(fp.getUniqueId());
        // la puissance du nouveau membre peut rendre la faction à nouveau protégée
        checkRaidable(f);
        plugin.top().invalidate();
    }

    /**
     * Fait quitter la faction à un joueur. Si la faction se retrouve vide elle est
     * dissoute (selon la configuration).
     */
    public void removeMember(Faction f, FPlayer fp, FactionMembershipEvent.Action action) {
        boolean wasLeader = fp.getRole() == Role.LEADER;
        detach(f, fp);
        fp.setLastFactionLeave(System.currentTimeMillis());
        if (f.getMemberIds().isEmpty() && plugin.settings().disbandWhenEmpty && !getFlag(f, FactionFlag.PERMANENT)) {
            if (disband(f, fp.getName(), null, FactionDisbandEvent.Reason.EMPTY, true)) {
                Player online = fp.getPlayer();
                if (online != null) {
                    plugin.msg().send(online, "leave.disbanded-empty", "faction", f.getTag());
                }
            }
        } else if (wasLeader || getLeader(f) == null) {
            FPlayer heir = findHeir(f, null);
            if (heir != null) {
                heir.setRole(Role.LEADER);
                log(f, "LEADER", "player", heir.getName());
            }
        }
        if (getById(f.getId()) != null) {
            // un départ fait baisser la puissance : la faction peut devenir raidable
            checkRaidable(f);
        }
        plugin.top().invalidate();
    }

    private void detach(Faction f, FPlayer fp) {
        f.getMemberIds().remove(fp.getUniqueId());
        f.clearPlayerPermissions(fp.getUniqueId());
        fp.setAutoClaim(AutoClaimMode.NONE, null);
        fp.setFaction(null, null);
    }

    /** Membre actif le plus gradé (hors {@code exclude}), pour succéder au chef. */
    public FPlayer findHeir(Faction f, UUID exclude) {
        FPlayer best = null;
        for (FPlayer fp : getMembers(f)) {
            if (fp.getUniqueId().equals(exclude) || fp.getRole() == Role.LEADER) {
                continue;
            }
            if (best == null || fp.getRole().isHigherThan(best.getRole())
                    || (fp.getRole() == best.getRole() && fp.getLastSeen() > best.getLastSeen())) {
                best = fp;
            }
        }
        return best;
    }

    public void setLeader(Faction f, FPlayer newLeader) {
        FPlayer old = getLeader(f);
        if (old != null && old != newLeader) {
            old.setRole(plugin.roles().next(Role.OFFICER) == Role.COLEADER ? Role.COLEADER : Role.OFFICER);
        }
        newLeader.setRole(Role.LEADER);
        log(f, "LEADER", "player", newLeader.getName());
    }

    /** Membres triés par rôle décroissant puis par pseudo. */
    public List<FPlayer> getMembers(Faction f) {
        List<FPlayer> list = new ArrayList<FPlayer>();
        for (UUID uuid : f.getMemberIds()) {
            FPlayer fp = plugin.players().get(uuid);
            if (fp != null) {
                list.add(fp);
            }
        }
        Collections.sort(list, new Comparator<FPlayer>() {
            @Override
            public int compare(FPlayer a, FPlayer b) {
                int c = b.getRole().compareTo(a.getRole());
                return c != 0 ? c : String.CASE_INSENSITIVE_ORDER.compare(String.valueOf(a.getName()), String.valueOf(b.getName()));
            }
        });
        return list;
    }

    public List<Player> getOnlinePlayers(Faction f) {
        List<Player> list = new ArrayList<Player>();
        for (UUID uuid : f.getMemberIds()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                list.add(p);
            }
        }
        return list;
    }

    public int countOnline(Faction f) {
        int count = 0;
        for (UUID uuid : f.getMemberIds()) {
            if (Bukkit.getPlayer(uuid) != null) {
                count++;
            }
        }
        return count;
    }

    public int countRole(Faction f, Role role) {
        int count = 0;
        for (FPlayer fp : getMembers(f)) {
            if (fp.getRole() == role) {
                count++;
            }
        }
        return count;
    }

    public FPlayer getLeader(Faction f) {
        for (UUID uuid : f.getMemberIds()) {
            FPlayer fp = plugin.players().get(uuid);
            if (fp != null && fp.getRole() == Role.LEADER) {
                return fp;
            }
        }
        return null;
    }

    /** Envoie un message aux membres connectés. */
    public void notifyMembers(Faction f, String key, Object... kv) {
        plugin.msg().send(getOnlinePlayers(f), key, kv);
    }

    // ============================================================= Relations

    /**
     * Relation entre deux factions : MEMBER pour la même faction, ENEMY sinon
     * (les zones système n'ont pas de relation).
     */
    public Relation getRelation(Faction a, Faction b) {
        if (a == null || b == null) {
            return Relation.ENEMY;
        }
        if (a.getId().equals(b.getId())) {
            return a.isNormal() ? Relation.MEMBER : Relation.ENEMY;
        }
        return Relation.ENEMY;
    }

    /** Relation entre deux joueurs (un joueur sans faction est l'ennemi de tous). */
    public Relation getRelation(FPlayer a, FPlayer b) {
        Faction fa = getFaction(a);
        Faction fb = getFaction(b);
        if (fa == null || fb == null) {
            return Relation.ENEMY;
        }
        return getRelation(fa, fb);
    }

    /** Relation d'un joueur envers une faction (territoire, chat...). */
    public Relation getRelationTo(FPlayer fp, Faction target) {
        if (target == null || target.isSystem()) {
            return Relation.ENEMY;
        }
        return getRelation(getFaction(fp), target);
    }

    public TerritoryType getTerritoryType(FPlayer fp, Faction territory) {
        if (territory == null || territory.isWilderness()) {
            return TerritoryType.WILDERNESS;
        }
        if (territory.isSafeZone()) {
            return TerritoryType.SAFEZONE;
        }
        if (territory.isWarZone()) {
            return TerritoryType.WARZONE;
        }
        return TerritoryType.of(getRelationTo(fp, territory));
    }

    /** Couleur (non traduite) d'une faction vue par un joueur. */
    public String getColor(FPlayer viewer, Faction target) {
        if (target == null) {
            return plugin.settings().wildernessColor;
        }
        if (target.isSystem()) {
            return plugin.settings().zoneColor(target.getId());
        }
        if (viewer == null) {
            return plugin.settings().relationColor(Relation.ENEMY);
        }
        return plugin.settings().relationColor(getRelationTo(viewer, target));
    }

    public String getColor(Faction viewer, Faction target) {
        if (target.isSystem()) {
            return plugin.settings().zoneColor(target.getId());
        }
        return plugin.settings().relationColor(getRelation(viewer, target));
    }

    // =============================================================== Points

    /**
     * Ajoute (ou retire si négatif) des points à une faction, après {@link FactionPointsChangeEvent}.
     * Le total ne descend jamais sous 0.
     *
     * @return le gain réellement appliqué, 0 si l'évènement est annulé ou la faction non classable
     */
    public long addPoints(Faction f, long amount, String reason) {
        if (f == null || !f.isNormal() || amount == 0) {
            return 0;
        }
        FactionPointsChangeEvent event = new FactionPointsChangeEvent(f, amount, reason == null ? "" : reason);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled() || event.getAmount() == 0) {
            return 0;
        }
        long before = f.getPoints();
        f.setPoints(before + event.getAmount());
        plugin.top().invalidate();
        return f.getPoints() - before;
    }

    public void setPoints(Faction f, long points, String reason) {
        if (f != null) {
            addPoints(f, Math.max(0, points) - f.getPoints(), reason);
        }
    }

    // ============================================================ Puissance

    public double getPower(Faction f) {
        if (!f.isNormal()) {
            return 0;
        }
        if (getFlag(f, FactionFlag.INFINITE_POWER)) {
            return 999999;
        }
        boolean countOffline = plugin.settings().countOfflinePower;
        double sum = 0;
        for (UUID uuid : f.getMemberIds()) {
            FPlayer fp = plugin.players().get(uuid);
            if (fp != null && (countOffline || fp.isOnline())) {
                sum += fp.getPower();
            }
        }
        sum += plugin.upgrades().getValue(f, UpgradeType.POWER);
        return cap(sum);
    }

    public double getMaxPower(Faction f) {
        if (!f.isNormal()) {
            return 0;
        }
        if (getFlag(f, FactionFlag.INFINITE_POWER)) {
            return 999999;
        }
        double sum = 0;
        for (UUID uuid : f.getMemberIds()) {
            FPlayer fp = plugin.players().get(uuid);
            if (fp != null) {
                sum += plugin.players().getMaxPower(fp);
            }
        }
        sum += plugin.upgrades().getValue(f, UpgradeType.POWER);
        return cap(sum);
    }

    private double cap(double value) {
        double max = plugin.settings().factionMaxPower;
        return max > 0 ? Math.min(value, max) : value;
    }

    /** Nombre de chunks que la puissance actuelle permet de garder. */
    public int getLandFromPower(Faction f) {
        double perClaim = plugin.settings().powerPerClaim;
        if (perClaim <= 0) {
            return Integer.MAX_VALUE / 2;
        }
        return (int) Math.floor(getPower(f) / perClaim);
    }

    /** Nombre maximum de chunks revendicables (puissance + plafond). */
    public int getMaxLand(Faction f) {
        int land = Math.max(0, getLandFromPower(f));
        int hardCap = getClaimCap(f);
        return hardCap > 0 ? Math.min(land, hardCap) : land;
    }

    /** Plafond absolu de claims (0 = aucun). */
    public int getClaimCap(Faction f) {
        return Math.max(0, plugin.settings().maxClaims);
    }

    public boolean isRaidable(Faction f) {
        if (!f.isNormal() || getFlag(f, FactionFlag.INFINITE_POWER)) {
            return false;
        }
        int claims = plugin.claims().count(f.getId());
        return claims > 0 && claims > getLandFromPower(f);
    }

    /** Vérifie si l'état "raidable" a changé et prévient la faction. */
    public void checkRaidable(Faction f) {
        if (!f.isNormal()) {
            return;
        }
        boolean now = isRaidable(f);
        if (now != f.wasRaidable()) {
            f.setWasRaidable(now);
            if (plugin.settings().announceRaidable) {
                notifyMembers(f, now ? "power.raidable" : "power.no-longer-raidable");
                if (now) {
                    for (Player p : getOnlinePlayers(f)) {
                        Sounds.play(p, plugin.settings().sound("raidable"));
                    }
                }
            }
        }
    }

    // ================================================================ Limites

    public int getMaxMembers(Faction f) {
        int base = plugin.settings().maxMembers;
        if (base <= 0) {
            return Integer.MAX_VALUE;
        }
        return base + (int) plugin.upgrades().getValue(f, UpgradeType.MEMBERS);
    }

    public int getMaxWarps(Faction f) {
        return Math.max(0, plugin.settings().maxWarps + (int) plugin.upgrades().getValue(f, UpgradeType.WARPS));
    }

    // ========================================================== Flags / perms

    public boolean getFlag(Faction f, FactionFlag flag) {
        Boolean value = f.getFlagValue(flag);
        if (value != null) {
            return value;
        }
        Settings.FlagInfo info = plugin.settings().flags.get(flag);
        return info != null && info.def;
    }

    /**
     * Modifie un flag en vérifiant les droits du joueur.
     *
     * @return false si le changement est refusé (message envoyé)
     */
    public boolean changeFlag(Player player, FPlayer fp, Faction f, FactionFlag flag, boolean value, boolean silent) {
        Settings.FlagInfo info = plugin.settings().flags.get(flag);
        if (!fp.isBypass()) {
            if (!info.editable) {
                plugin.msg().send(player, "flags.not-editable");
                return false;
            }
            if (!hasPermission(fp, f, PermAction.FLAGS)) {
                plugin.msg().send(player, "general.no-faction-permission", "action", plugin.roles().actionName(PermAction.FLAGS));
                return false;
            }
        }
        f.setFlag(flag, value == info.def ? null : value);
        String state = plugin.msg().text(value ? "flags.state-on" : "flags.state-off");
        if (silent) {
            // le message est géré par l'appelant
        } else if (f.getId().equals(fp.getFactionId())) {
            plugin.msg().send(player, "flags.changed", "flag", info.name, "state", state);
        } else {
            plugin.msg().send(player, "flags.admin-changed", "flag", info.name, "state", state, "faction", f.getTag());
        }
        log(f, "FLAG", "player", fp.getName(), "flag", info.name, "state", state);
        if (flag == FactionFlag.PEACEFUL || flag == FactionFlag.INFINITE_POWER) {
            checkRaidable(f);
            plugin.top().invalidate();
        }
        return true;
    }

    public boolean hasPermission(Faction f, PermSubject subject, PermAction action) {
        if (subject == null) {
            return false;
        }
        RoleSettings roles = plugin.roles();
        if (roles.isLocked(subject, action)) {
            return roles.isDefault(subject, action);
        }
        Boolean value = f.getPermission(subject, action);
        return value != null ? value : roles.isDefault(subject, action);
    }

    /**
     * Permission d'un joueur pour une action dans une faction donnée (la sienne
     * ou une autre selon la relation). Le mode bypass autorise tout.
     */
    public boolean hasPermission(FPlayer fp, Faction f, PermAction action) {
        if (fp.isBypass()) {
            return true;
        }
        if (f == null || !f.isNormal()) {
            return false;
        }
        if (f.getId().equals(fp.getFactionId())) {
            return hasMemberPermission(f, fp, action);
        }
        // les joueurs des autres factions n'ont aucune permission
        return false;
    }

    /**
     * Permission d'un membre : le chef a tout, puis la valeur personnalisée du
     * joueur (/f perms <joueur>) si elle existe, sinon celle de son rôle.
     */
    public boolean hasMemberPermission(Faction f, FPlayer member, PermAction action) {
        Role role = member.getRole();
        if (role == Role.LEADER) {
            return true;
        }
        PermSubject subject = PermSubject.of(role);
        if (subject != null && plugin.roles().isPlayerPermissionsEnabled() && !plugin.roles().isLocked(subject, action)) {
            Boolean custom = f.getPlayerPermission(member.getUniqueId(), action);
            if (custom != null) {
                return custom;
            }
        }
        return hasPermission(f, subject, action);
    }

    /** Permission d'un joueur dans sa propre faction. */
    public boolean hasPermission(FPlayer fp, PermAction action) {
        return hasPermission(fp, getFaction(fp), action);
    }

    // ================================================================ Journal

    public void log(Faction f, String type, Object... kv) {
        if (plugin.settings().logsEnabled && f != null && f.isNormal()) {
            f.addLog(new LogEntry(type, kv), plugin.settings().logsMax);
        }
    }

    /** Description affichable (valeur par défaut si vide). */
    public String getDescription(Faction f) {
        String d = f.getDescription();
        return d == null || d.isEmpty() ? plugin.msg().text("values.no-description") : d;
    }
}
