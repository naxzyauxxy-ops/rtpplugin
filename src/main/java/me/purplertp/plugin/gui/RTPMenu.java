package me.purplertp.plugin.gui;

import java.util.ArrayList;
import me.purplertp.plugin.PurpleRTP;
import me.purplertp.plugin.gui.RTPMenuListener;
import me.purplertp.plugin.utils.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class RTPMenu {
    private final PurpleRTP plugin;

    public RTPMenu(PurpleRTP plugin) {
        this.plugin = plugin;
    }

    public void open(Player player, RTPMenuListener listener) {
        ConfigurationSection buttons;
        FileConfiguration cfg = this.plugin.getConfig();
        String title = MessageUtils.format(cfg.getString("RTP-MENU.TITLE", "&8Random Teleport"));
        int size = cfg.getInt("RTP-MENU.SIZE", 27);
        boolean ph = cfg.getBoolean("RTP-MENU.PLACEHOLDER", true);
        Inventory inv = Bukkit.createInventory(null, (int)size, (String)title);
        if (ph) {
            this.fillPlaceholder(inv, size);
        }
        if ((buttons = cfg.getConfigurationSection("RTP-MENU.BUTTONS")) != null) {
            for (String key : buttons.getKeys(false)) {
                String base = "RTP-MENU.BUTTONS." + key + ".";
                if (!cfg.getBoolean(base + "ENABLED", true)) continue;
                Material mat = this.getMaterial(cfg.getString(base + "MATERIAL", "GRASS_BLOCK"));
                String displayName = MessageUtils.format(cfg.getString(base + "DISPLAY-NAME", key));
                String worldName = cfg.getString(base + "WORLD", "world");
                int slot = cfg.getInt(base + "SLOT", 13);
                int poolReady = this.plugin.getLocationPoolManager().poolSize(worldName);
                int ping = player.getPing();
                long playersInWorld = Bukkit.getOnlinePlayers().stream().filter(p -> p.getWorld() != null && p.getWorld().getName().equals(worldName)).count();
                ArrayList<String> lore = new ArrayList<String>();
                for (String line : cfg.getStringList(base + "LORE")) {
                    lore.add(MessageUtils.format(line.replace("{players}", String.valueOf(playersInWorld)).replace("{ping}", String.valueOf(ping)).replace("{pool}", String.valueOf(poolReady))));
                }
                ItemStack item = new ItemStack(mat);
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName(displayName);
                    meta.setLore(lore);
                    item.setItemMeta(meta);
                }
                inv.setItem(slot, item);
            }
        }
        listener.trackMain(player);
        player.openInventory(inv);
    }

    public boolean openRegionMenu(Player player, String dimensionKey, RTPMenuListener listener) {
        ConfigurationSection buttons;
        FileConfiguration cfg = this.plugin.getConfig();
        if (!cfg.getBoolean("REGION-MENUS.ENABLED", false)) {
            return false;
        }
        String sectionPath = "REGION-MENUS." + dimensionKey;
        ConfigurationSection section = cfg.getConfigurationSection(sectionPath);
        if (section == null) {
            return false;
        }
        String title = MessageUtils.format(section.getString("TITLE", "&8Region Select"));
        int size = section.getInt("SIZE", 27);
        boolean ph = section.getBoolean("PLACEHOLDER", true);
        Inventory inv = Bukkit.createInventory(null, (int)size, (String)title);
        if (ph) {
            this.fillPlaceholder(inv, size);
        }
        if ((buttons = section.getConfigurationSection("BUTTONS")) != null) {
            for (String key : buttons.getKeys(false)) {
                String base = sectionPath + ".BUTTONS." + key + ".";
                Material mat = this.getMaterial(cfg.getString(base + "MATERIAL", "GRASS_BLOCK"));
                String dispName = MessageUtils.format(cfg.getString(base + "DISPLAY-NAME", key));
                int slot = cfg.getInt(base + "SLOT", 13);
                int ping = player.getPing();
                ArrayList<String> lore = new ArrayList<String>();
                for (String line : cfg.getStringList(base + "LORE")) {
                    lore.add(MessageUtils.format(line.replace("{ping}", String.valueOf(ping))));
                }
                ItemStack item = new ItemStack(mat);
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName(dispName);
                    meta.setLore(lore);
                    item.setItemMeta(meta);
                }
                inv.setItem(slot, item);
            }
        }
        listener.trackRegion(player, dimensionKey);
        player.openInventory(inv);
        return true;
    }

    private void fillPlaceholder(Inventory inv, int size) {
        ItemStack pane = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta paneMeta = pane.getItemMeta();
        if (paneMeta != null) {
            paneMeta.setDisplayName(" ");
            pane.setItemMeta(paneMeta);
        }
        for (int i = 0; i < size; ++i) {
            inv.setItem(i, pane);
        }
    }

    private Material getMaterial(String name) {
        try {
            return Material.valueOf((String)name.toUpperCase());
        }
        catch (IllegalArgumentException e) {
            return Material.GRASS_BLOCK;
        }
    }
}

