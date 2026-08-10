package me.purplertp.plugin.managers;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import me.purplertp.plugin.PurpleRTP;
import me.purplertp.plugin.managers.CooldownManager;
import me.purplertp.plugin.utils.MessageUtils;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class RTPManager {
    private final PurpleRTP plugin;
    private final Set<UUID> inRtp = Collections.newSetFromMap(new ConcurrentHashMap());

    public RTPManager(PurpleRTP plugin) {
        this.plugin = plugin;
    }

    public boolean isInRtp(UUID uuid) {
        return this.inRtp.contains(uuid);
    }

    public void cancelRtp(UUID uuid) {
        this.inRtp.remove(uuid);
    }

    public Set<UUID> getPlayersInRtp() {
        return this.inRtp;
    }

    public void randomTeleport(Player player, String worldName) {
        CooldownManager cm;
        FileConfiguration cfg = this.plugin.getConfig();
        if (!cfg.getBoolean("ENABLED", true)) {
            this.actionbar(player, cfg.getString("MESSAGES.DISABLED"));
            return;
        }
        if (this.inRtp.size() >= cfg.getInt("SETTINGS.PLAYERS-IN-RTP", 100)) {
            this.actionbar(player, cfg.getString("MESSAGES.MAX-PLAYERS"));
            return;
        }
        World world = Bukkit.getWorld((String)worldName);
        if (world == null) {
            this.actionbar(player, cfg.getString("MESSAGES.WORLD-NOT-EXIST"));
            return;
        }
        if (!player.hasPermission("havocrtp.bypass.cooldown") && (cm = this.plugin.getCooldownManager()).isOnCooldown(player.getUniqueId(), worldName)) {
            long remaining = cm.getRemainingCooldown(player.getUniqueId(), worldName);
            this.actionbar(player, cfg.getString("MESSAGES.COOLDOWN").replace("{remaining}", String.valueOf(remaining)));
            return;
        }
        if (this.inRtp.contains(player.getUniqueId())) {
            this.actionbar(player, "&8(&#f40d0d!&8) &7You are already teleporting!");
            return;
        }
        this.inRtp.add(player.getUniqueId());
        String path = "WORLD-SETTINGS." + worldName + ".";
        int cooldown = cfg.getInt(path + "COOLDOWN", 0);
        int centerX = cfg.getInt(path + "CENTER-X", 0);
        int centerZ = cfg.getInt(path + "CENTER-Z", 0);
        int minRadius = cfg.getInt(path + "MIN-RADIUS", 500);
        int maxRadius = cfg.getInt(path + "MAX-RADIUS", 5000);
        int maxAttempts = cfg.getInt(path + "MAX-ATTEMPTS", cfg.getInt("SETTINGS.MAX-ATTEMPTS", 25));
        Location poolLoc = this.plugin.getLocationPoolManager().pollLocation(worldName);
        if (poolLoc != null) {
            this.doTeleport(player, poolLoc, cooldown, worldName);
            return;
        }
        this.actionbar(player, cfg.getString("MESSAGES.SEARCHING", "&8(&#f40d0d!&8) &7Searching for a safe location..."));
        Bukkit.getScheduler().runTaskAsynchronously((Plugin)this.plugin, () -> {
            Location dest = this.findSafeLocation(world, centerX, centerZ, minRadius, maxRadius, maxAttempts);
            Bukkit.getScheduler().runTask((Plugin)this.plugin, () -> {
                if (!player.isOnline() || !this.inRtp.contains(player.getUniqueId())) {
                    return;
                }
                if (dest == null) {
                    this.inRtp.remove(player.getUniqueId());
                    this.actionbar(player, cfg.getString("MESSAGES.MAX-ATTEMPTS", "&8(&#f40d0d!&8) &7No safe location found.").replace("{attempts}", String.valueOf(maxAttempts)));
                    return;
                }
                this.doTeleport(player, dest, cooldown, worldName);
            });
        });
    }

    private void doTeleport(Player player, Location dest, int cooldown, String worldName) {
        String found;
        this.inRtp.remove(player.getUniqueId());
        player.teleport(dest);
        player.playSound(dest, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 1.2f);
        if (cooldown > 0) {
            this.plugin.getCooldownManager().setCooldown(player.getUniqueId(), worldName, cooldown);
        }
        if ((found = this.plugin.getConfig().getString("MESSAGES.SAFE-LOCATION-FOUND", "")) != null && !found.isEmpty()) {
            this.actionbar(player, found);
        }
    }

    private Location findSafeLocation(World world, int centerX, int centerZ, int minRadius, int maxRadius, int maxAttempts) {
        boolean isNether = world.getEnvironment() == World.Environment.NETHER;
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        for (int attempt = 0; attempt < maxAttempts; ++attempt) {
            try {
                double angle = rng.nextDouble() * 2.0 * Math.PI;
                double radius = (double)minRadius + Math.sqrt(rng.nextDouble()) * (double)(maxRadius - minRadius);
                int x = centerX + (int)(Math.cos(angle) * radius);
                int z = centerZ + (int)(Math.sin(angle) * radius);
                world.getChunkAt(x >> 4, z >> 4);
                Location loc = this.getSafeY(world, x, z, isNether);
                if (loc == null) continue;
                loc.setYaw(rng.nextFloat() * 360.0f);
                return loc;
            }
            catch (Exception exception) {
                // empty catch block
            }
        }
        return null;
    }

    private Location getSafeY(World world, int x, int z, boolean isNether) {
        int topY = isNether ? 120 : world.getHighestBlockYAt(x, z);
        int minY = isNether ? world.getMinHeight() + 1 : world.getMinHeight();
        for (int y = topY; y > minY; --y) {
            Block ground = new Location(world, (double)x, (double)y, (double)z).getBlock();
            Block feet = new Location(world, (double)x, (double)(y + 1), (double)z).getBlock();
            Block head = new Location(world, (double)x, (double)(y + 2), (double)z).getBlock();
            Material type = ground.getType();
            if (type.isAir() || !type.isSolid() || type == Material.WATER || type == Material.LAVA || type == Material.FIRE || type == Material.CACTUS || !feet.getType().isAir() || !head.getType().isAir()) continue;
            return new Location(world, (double)x + 0.5, (double)(y + 1), (double)z + 0.5);
        }
        return null;
    }

    private void actionbar(Player player, String message) {
        if (message == null || message.isEmpty()) {
            return;
        }
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, (BaseComponent)new TextComponent(MessageUtils.format(message)));
    }
}

