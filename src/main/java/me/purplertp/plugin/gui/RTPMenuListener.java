package me.purplertp.plugin.gui;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import me.purplertp.plugin.PurpleRTP;
import me.purplertp.plugin.gui.RTPMenu;
import me.purplertp.plugin.utils.MessageUtils;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;

public class RTPMenuListener
implements Listener {
    private final Map<UUID, String> openMenus = new HashMap<UUID, String>();
    private final PurpleRTP plugin;
    private final RTPMenu menu;

    public RTPMenuListener(PurpleRTP plugin) {
        this.plugin = plugin;
        this.menu = new RTPMenu(plugin);
    }

    public void trackMain(Player player) {
        this.openMenus.put(player.getUniqueId(), "MAIN");
    }

    public void trackRegion(Player player, String dimensionKey) {
        this.openMenus.put(player.getUniqueId(), dimensionKey);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        this.openMenus.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        HumanEntity humanEntity = event.getWhoClicked();
        if (!(humanEntity instanceof Player)) {
            return;
        }
        Player player = (Player)humanEntity;
        if (event.getView().getTopInventory().getType() != InventoryType.CHEST) {
            return;
        }
        String menuKey = this.openMenus.get(player.getUniqueId());
        if (menuKey == null) {
            return;
        }
        event.setCancelled(true);
        if (event.getClickedInventory() != event.getView().getTopInventory()) {
            return;
        }
        if (event.getCurrentItem() == null) {
            return;
        }
        FileConfiguration cfg = this.plugin.getConfig();
        if (menuKey.equals("MAIN")) {
            ConfigurationSection buttons = cfg.getConfigurationSection("RTP-MENU.BUTTONS");
            if (buttons == null) {
                return;
            }
            for (String key : buttons.getKeys(false)) {
                String base = "RTP-MENU.BUTTONS." + key + ".";
                if (!cfg.getBoolean(base + "ENABLED", true) || cfg.getInt(base + "SLOT") != event.getRawSlot()) continue;
                boolean regionEnabled = cfg.getBoolean(base + "ENABLED-REGION", false);
                String worldName = cfg.getString(base + "WORLD", "world");
                player.closeInventory();
                if (regionEnabled) {
                    this.menu.openRegionMenu(player, key, this);
                } else {
                    if (this.plugin.getRtpManager().isInRtp(player.getUniqueId())) {
                        this.sendActionBar(player, "&8(&#f40d0d!&8) &7You are already teleporting!");
                        return;
                    }
                    this.plugin.getRtpManager().randomTeleport(player, worldName);
                }
                return;
            }
            return;
        }
        String sectionPath = "REGION-MENUS." + menuKey + ".BUTTONS";
        ConfigurationSection buttons = cfg.getConfigurationSection(sectionPath);
        if (buttons == null) {
            return;
        }
        for (String regionKey : buttons.getKeys(false)) {
            String serverKey;
            String base = sectionPath + "." + regionKey + ".";
            if (cfg.getInt(base + "SLOT") != event.getRawSlot()) continue;
            player.closeInventory();
            if (this.plugin.getRtpManager().isInRtp(player.getUniqueId())) {
                this.sendActionBar(player, "&8(&#f40d0d!&8) &7You are already teleporting!");
                return;
            }
            List servers = cfg.getStringList(base + "SERVERS");
            String string = serverKey = servers.isEmpty() ? null : (String)servers.get(0);
            if (serverKey == null) {
                return;
            }
            String localServer = this.plugin.getNetworkManager().getLocalServer();
            if (this.plugin.getNetworkManager().isSameRegion(localServer, serverKey)) {
                String worldName = cfg.getString("SERVER-SETTINGS." + serverKey + ".TARGET-WORLD", "world");
                this.plugin.getRtpManager().randomTeleport(player, worldName);
            } else {
                this.sendActionBar(player, "&8(&#f40d0d!&8) &7Connecting to &#f40d0d" + regionKey.toUpperCase() + "&7...");
                this.plugin.getNetworkManager().sendRtpTrigger(player.getUniqueId(), serverKey);
                this.plugin.getNetworkManager().transferToProxy(player, serverKey);
            }
            return;
        }
    }

    private void sendActionBar(Player player, String message) {
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, (BaseComponent)new TextComponent(MessageUtils.format(message)));
    }
}

