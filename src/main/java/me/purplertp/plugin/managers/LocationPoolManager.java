package me.purplertp.plugin.managers;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import me.purplertp.plugin.PurpleRTP;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

public class LocationPoolManager {
    private final PurpleRTP plugin;
    private final Map<String, ConcurrentLinkedQueue<Location>> pools = new ConcurrentHashMap<String, ConcurrentLinkedQueue<Location>>();
    private final Map<String, AtomicBoolean> filling = new ConcurrentHashMap<String, AtomicBoolean>();
    private BukkitTask refillTask;

    public LocationPoolManager(PurpleRTP plugin) {
        this.plugin = plugin;
    }

    public void startPoolFilling() {
        int intervalTicks = this.plugin.getConfig().getInt("SETTINGS.POOL-REFILL-INTERVAL", 40);
        this.refillTask = Bukkit.getScheduler().runTaskTimerAsynchronously((Plugin)this.plugin, () -> {
            ConfigurationSection worldSection = this.plugin.getConfig().getConfigurationSection("WORLD-SETTINGS");
            if (worldSection == null) {
                return;
            }
            int poolSize = this.plugin.getConfig().getInt("SETTINGS.POOL-SIZE", 20);
            int batchSize = this.plugin.getConfig().getInt("SETTINGS.POOL-FILL-BATCH", 5);
            Set worldKeys = worldSection.getKeys(false);
            for (String worldName : worldKeys) {
                ConcurrentLinkedQueue pool = this.pools.computeIfAbsent(worldName, k -> new ConcurrentLinkedQueue());
                AtomicBoolean isFilling = this.filling.computeIfAbsent(worldName, k -> new AtomicBoolean(false));
                if (pool.size() >= poolSize || !isFilling.compareAndSet(false, true)) continue;
                String path = "WORLD-SETTINGS." + worldName + ".";
                int maxRadius = this.plugin.getConfig().getInt(path + "MAX-RADIUS", 5000);
                int minRadius = this.plugin.getConfig().getInt(path + "MIN-RADIUS", 500);
                int centerX = this.plugin.getConfig().getInt(path + "CENTER-X", 0);
                int centerZ = this.plugin.getConfig().getInt(path + "CENTER-Z", 0);
                int maxAttempts = this.plugin.getConfig().getInt("SETTINGS.MAX-ATTEMPTS", 25);
                World world = Bukkit.getWorld((String)worldName);
                if (world == null) {
                    isFilling.set(false);
                    continue;
                }
                int needed = Math.min(batchSize, poolSize - pool.size());
                int generated = 0;
                for (int attempt = 0; attempt < maxAttempts * needed && generated < needed; ++attempt) {
                    Location loc = this.tryFindSafeLocation(world, centerX, centerZ, minRadius, maxRadius, maxAttempts);
                    if (loc == null) continue;
                    pool.add(loc);
                    ++generated;
                }
                isFilling.set(false);
            }
        }, 0L, (long)intervalTicks);
    }

    public Location pollLocation(String worldName) {
        ConcurrentLinkedQueue<Location> pool = this.pools.get(worldName);
        if (pool == null || pool.isEmpty()) {
            return null;
        }
        return pool.poll();
    }

    public int poolSize(String worldName) {
        ConcurrentLinkedQueue<Location> pool = this.pools.get(worldName);
        return pool == null ? 0 : pool.size();
    }

    public void shutdown() {
        if (this.refillTask != null) {
            this.refillTask.cancel();
        }
    }

    private Location tryFindSafeLocation(World world, int centerX, int centerZ, int minRadius, int maxRadius, int maxAttempts) {
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        boolean isNether = world.getEnvironment() == World.Environment.NETHER;
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
            catch (Exception e) {
                this.plugin.getLogger().log(Level.FINE, "Pool: chunk failed at " + centerX + "," + centerZ, e);
            }
        }
        return null;
    }

    private Location getSafeY(World world, int x, int z, boolean isNether) {
        int topY;
        int minY = isNether ? world.getMinHeight() + 1 : world.getMinHeight();
        for (int y = topY = isNether ? 120 : world.getHighestBlockYAt(x, z); y > minY; --y) {
            Block ground = new Location(world, (double)x, (double)y, (double)z).getBlock();
            Block feet = new Location(world, (double)x, (double)(y + 1), (double)z).getBlock();
            Block head = new Location(world, (double)x, (double)(y + 2), (double)z).getBlock();
            if (ground.getType().isAir() || !ground.getType().isSolid() || ground.getType() == Material.WATER || ground.getType() == Material.LAVA || ground.getType() == Material.FIRE || ground.getType() == Material.CACTUS || !feet.getType().isAir() || !head.getType().isAir()) continue;
            return new Location(world, (double)x + 0.5, (double)(y + 1), (double)z + 0.5);
        }
        return null;
    }
}

