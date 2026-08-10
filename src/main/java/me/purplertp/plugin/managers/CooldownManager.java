package me.purplertp.plugin.managers;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import me.purplertp.plugin.PurpleRTP;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

public class CooldownManager {
    private final PurpleRTP plugin;
    private final Map<UUID, Map<String, Long>> cooldowns = new ConcurrentHashMap<UUID, Map<String, Long>>();
    private final File dataFile;

    public CooldownManager(PurpleRTP plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "cooldowns.yml");
        this.loadCooldowns();
    }

    public boolean isOnCooldown(UUID uuid, String worldName) {
        Map<String, Long> map = this.cooldowns.get(uuid);
        if (map == null) {
            return false;
        }
        Long expiry = map.get(worldName);
        if (expiry == null) {
            return false;
        }
        if (System.currentTimeMillis() >= expiry) {
            map.remove(worldName);
            return false;
        }
        return true;
    }

    public long getRemainingCooldown(UUID uuid, String worldName) {
        Map<String, Long> map = this.cooldowns.get(uuid);
        if (map == null) {
            return 0L;
        }
        Long expiry = map.get(worldName);
        if (expiry == null) {
            return 0L;
        }
        return Math.max(0L, (expiry - System.currentTimeMillis()) / 1000L);
    }

    public void setCooldown(UUID uuid, String worldName, int seconds) {
        this.cooldowns.computeIfAbsent(uuid, k -> new ConcurrentHashMap()).put(worldName, System.currentTimeMillis() + (long)seconds * 1000L);
    }

    public void removeCooldown(UUID uuid) {
        this.cooldowns.remove(uuid);
    }

    public void saveCooldowns() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (Map.Entry<UUID, Map<String, Long>> entry : this.cooldowns.entrySet()) {
            for (Map.Entry<String, Long> worldEntry : entry.getValue().entrySet()) {
                yaml.set(entry.getKey().toString() + "." + worldEntry.getKey(), (Object)worldEntry.getValue());
            }
        }
        try {
            yaml.save(this.dataFile);
        }
        catch (IOException e) {
            this.plugin.getLogger().warning("Could not save cooldowns: " + e.getMessage());
        }
    }

    private void loadCooldowns() {
        if (!this.dataFile.exists()) {
            return;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration((File)this.dataFile);
        for (String uuidStr : yaml.getKeys(false)) {
            UUID uuid;
            try {
                uuid = UUID.fromString(uuidStr);
            }
            catch (IllegalArgumentException e) {
                continue;
            }
            ConfigurationSection section = yaml.getConfigurationSection(uuidStr);
            if (section == null) continue;
            Map map = this.cooldowns.computeIfAbsent(uuid, k -> new ConcurrentHashMap());
            for (String world : section.getKeys(false)) {
                map.put(world, yaml.getLong(uuidStr + "." + world));
            }
        }
    }
}

