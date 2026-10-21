package fr.koriamc.factions.manager;

import fr.koriamc.factions.KoriaFactions;
import fr.koriamc.factions.model.Faction;
import fr.koriamc.factions.model.UpgradeType;
import fr.koriamc.factions.util.Items;
import fr.koriamc.factions.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

/**
 * Coffre partagé de faction (/f chest).
 */
public final class ChestManager implements Listener {

    /** Propriétaire d'inventaire permettant de reconnaître un coffre de faction. */
    public static final class ChestHolder implements InventoryHolder {
        private final String factionId;
        private Inventory inventory;

        ChestHolder(String factionId) {
            this.factionId = factionId;
        }

        public String getFactionId() {
            return factionId;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }

    private final KoriaFactions plugin;
    private final Map<String, Inventory> inventories = new HashMap<String, Inventory>();

    public ChestManager(KoriaFactions plugin) {
        this.plugin = plugin;
    }

    /** Lignes débloquées (chest.default-rows + amélioration CHEST), de 0 à 6. */
    public int getUnlockedRows(Faction f) {
        return Math.max(0, Math.min(6, plugin.settings().chestRows + (int) plugin.upgrades().getValue(f, UpgradeType.CHEST)));
    }

    /**
     * Le coffre peut-il être ouvert ? Il faut au moins une ligne débloquée, ou des
     * objets encore stockés (amélioration retirée par un admin).
     */
    public boolean canOpen(Faction f) {
        if (getUnlockedRows(f) > 0) {
            return true;
        }
        for (ItemStack item : stored(f)) {
            if (item != null) {
                return true;
            }
        }
        return false;
    }

    private int rows(Faction f, ItemStack[] contents) {
        int rows = getUnlockedRows(f);
        // on ne réduit jamais le coffre en dessous du dernier objet stocké
        for (int i = contents.length - 1; i >= 0; i--) {
            if (contents[i] != null) {
                rows = Math.max(rows, i / 9 + 1);
                break;
            }
        }
        return Math.max(1, Math.min(6, rows));
    }

    private ItemStack[] stored(Faction f) {
        Inventory open = inventories.get(f.getId());
        if (open != null) {
            return open.getContents();
        }
        try {
            return Items.fromBase64(f.getChestData());
        } catch (RuntimeException e) {
            plugin.getLogger().log(Level.SEVERE, "Coffre illisible pour la faction " + f.getTag(), e);
            return new ItemStack[0];
        }
    }

    public Inventory get(Faction f) {
        Inventory current = inventories.get(f.getId());
        ItemStack[] contents = stored(f);
        int size = rows(f, contents) * 9;
        if (current != null && current.getSize() == size) {
            return current;
        }
        if (current != null) {
            for (HumanEntity viewer : new ArrayList<HumanEntity>(current.getViewers())) {
                viewer.closeInventory();
            }
        }
        ChestHolder holder = new ChestHolder(f.getId());
        String title = Text.truncate(Text.color(Text.replace(plugin.settings().chestTitle, "faction", f.getTag())), 32);
        Inventory inventory = Bukkit.createInventory(holder, size, title);
        for (int i = 0; i < contents.length && i < size; i++) {
            inventory.setItem(i, contents[i]);
        }
        holder.inventory = inventory;
        inventories.put(f.getId(), inventory);
        return inventory;
    }

    public void open(Player player, Faction f) {
        player.openInventory(get(f));
    }

    /** Redimensionne le coffre après une amélioration. */
    public void refresh(Faction f) {
        if (inventories.containsKey(f.getId())) {
            save(f);
            get(f);
        }
    }

    /** Redimensionne les coffres ouverts (après un /f reload). */
    public void refreshAll() {
        for (String id : new ArrayList<String>(inventories.keySet())) {
            Faction f = plugin.factions().getById(id);
            if (f != null) {
                refresh(f);
            }
        }
    }

    public void save(Faction f) {
        Inventory inventory = inventories.get(f.getId());
        if (inventory != null) {
            f.setChestData(Items.toBase64(inventory.getContents()));
        }
    }

    public void saveAll() {
        for (String id : inventories.keySet()) {
            Faction f = plugin.factions().getById(id);
            if (f != null) {
                save(f);
            }
        }
    }

    public void closeAll() {
        saveAll();
        for (Inventory inventory : inventories.values()) {
            for (HumanEntity viewer : new ArrayList<HumanEntity>(inventory.getViewers())) {
                viewer.closeInventory();
            }
        }
        inventories.clear();
    }

    /** Vide le coffre. */
    public void clear(Faction f) {
        Inventory inventory = inventories.remove(f.getId());
        if (inventory != null) {
            for (HumanEntity viewer : new ArrayList<HumanEntity>(inventory.getViewers())) {
                viewer.closeInventory();
            }
        }
        f.setChestData(null);
    }

    /** Dissolution : le contenu tombe au sol aux pieds du joueur (si configuré). */
    public void handleDisband(Faction f, Player player) {
        ItemStack[] contents = stored(f);
        clear(f);
        if (!plugin.settings().disbandDropChest || player == null) {
            return;
        }
        Location location = player.getLocation();
        for (ItemStack item : contents) {
            if (item != null) {
                location.getWorld().dropItemNaturally(location, item);
            }
        }
    }

    // ============================================================= Écouteurs

    private static boolean isChest(Inventory inventory) {
        return inventory != null && inventory.getHolder() instanceof ChestHolder;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onClose(InventoryCloseEvent event) {
        if (isChest(event.getInventory())) {
            Faction f = plugin.factions().getById(((ChestHolder) event.getInventory().getHolder()).getFactionId());
            if (f != null) {
                save(f);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onClick(InventoryClickEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (!isChest(top) || plugin.settings().chestBlacklist.isEmpty()) {
            return;
        }
        boolean clickedTop = event.getRawSlot() < top.getSize();
        List<ItemStack> incoming = new ArrayList<ItemStack>();
        if (clickedTop) {
            incoming.add(event.getCursor());
            if (event.getClick() == ClickType.NUMBER_KEY) {
                incoming.add(event.getWhoClicked().getInventory().getItem(event.getHotbarButton()));
            }
        } else if (event.isShiftClick()) {
            incoming.add(event.getCurrentItem());
        }
        for (ItemStack item : incoming) {
            if (item != null && plugin.settings().isChestItemBlacklisted(item)) {
                event.setCancelled(true);
                plugin.msg().send((Player) event.getWhoClicked(), "chest.blacklisted");
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDrag(InventoryDragEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (!isChest(top) || !plugin.settings().isChestItemBlacklisted(event.getOldCursor())) {
            return;
        }
        for (int slot : event.getRawSlots()) {
            if (slot < top.getSize()) {
                event.setCancelled(true);
                plugin.msg().send((Player) event.getWhoClicked(), "chest.blacklisted");
                return;
            }
        }
    }
}
