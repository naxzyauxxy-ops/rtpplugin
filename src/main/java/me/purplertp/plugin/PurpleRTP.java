package me.purplertp.plugin;

import me.purplertp.plugin.commands.RTPAdminCommand;
import me.purplertp.plugin.commands.RTPCommand;
import me.purplertp.plugin.gui.RTPMenuListener;
import me.purplertp.plugin.managers.CooldownManager;
import me.purplertp.plugin.managers.LocationPoolManager;
import me.purplertp.plugin.managers.NetworkManager;
import me.purplertp.plugin.license.LicenseClient;
import me.purplertp.plugin.managers.RTPManager;
import org.bukkit.command.CommandExecutor;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public class PurpleRTP
extends JavaPlugin {
    private static PurpleRTP instance;
    private CooldownManager cooldownManager;
    private RTPManager rtpManager;
    private LocationPoolManager locationPoolManager;
    private NetworkManager networkManager;
    private LicenseClient license;

    public void onEnable() {
        instance = this;
        this.saveDefaultConfig();

        // Licensing runs first: the check itself is off-thread, and an invalid
        // key disables the plugin before the managers start doing work.
        this.license = new LicenseClient(this);
        this.license.start();

        this.cooldownManager = new CooldownManager(this);
        this.locationPoolManager = new LocationPoolManager(this);
        this.rtpManager = new RTPManager(this);
        this.networkManager = new NetworkManager(this);
        this.locationPoolManager.startPoolFilling();
        this.networkManager.startSocketServer();
        RTPMenuListener menuListener = new RTPMenuListener(this);
        this.getCommand("rtp").setExecutor((CommandExecutor)new RTPCommand(this, menuListener));
        this.getCommand("rtpadmin").setExecutor((CommandExecutor)new RTPAdminCommand(this));
        this.getServer().getPluginManager().registerEvents((Listener)menuListener, (Plugin)this);
        this.getLogger().info("PurpleRTP (EU) enabled. LOCAL-SERVER=eu");
    }

    public void onDisable() {
        if (this.license != null) {
            this.license.stop();
        }
        if (this.cooldownManager != null) {
            this.cooldownManager.saveCooldowns();
        }
        if (this.locationPoolManager != null) {
            this.locationPoolManager.shutdown();
        }
        if (this.networkManager != null) {
            this.networkManager.stopSocketServer();
        }
        this.getLogger().info("PurpleRTP (EU) disabled.");
    }

    public static PurpleRTP getInstance() {
        return instance;
    }

    public CooldownManager getCooldownManager() {
        return this.cooldownManager;
    }

    public RTPManager getRtpManager() {
        return this.rtpManager;
    }

    public LocationPoolManager getLocationPoolManager() {
        return this.locationPoolManager;
    }

    public NetworkManager getNetworkManager() {
        return this.networkManager;
    }

    /** Licensing client. Never null once onEnable has run. */
    public LicenseClient license() {
        return this.license;
    }
}

