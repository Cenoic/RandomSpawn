package com.senrua.randomspawn;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Random;

public class RandomSpawn extends JavaPlugin implements Listener {

    private int spawnX;
    private int spawnZ;
    private int spawnRadius;
    private int respawnDelayTicks;
    private boolean randomSpawnOnFirstJoin;
    private boolean randomSpawnOnRespawn;
    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadConfig();
        loadConfig();

        getServer().getPluginManager().registerEvents(this, this);
    }

    private void loadConfig() {
        ConfigurationSection config = getConfig().getConfigurationSection("spawn");
        spawnX = config.getInt("x");
        spawnZ = config.getInt("z");
        spawnRadius = config.getInt("radius");
        respawnDelayTicks = getConfig().getInt("respawnDelayTicks");
        randomSpawnOnFirstJoin = getConfig().getBoolean("randomSpawnOnFirstJoin");
        randomSpawnOnRespawn = getConfig().getBoolean("randomSpawnOnRespawn");
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Check if random spawning on first join is enabled
        if (randomSpawnOnFirstJoin) {
            // Check if player has played before using Bukkit API
            if (!player.hasPlayedBefore()) {
                // Player is joining for the first time, teleport them to a random spawn location
                teleportToRandomSpawn(player);
            }
        }
    }


    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();

        if (!randomSpawnOnRespawn) {
            return;
        }
        if (player.getBedSpawnLocation() != null) {
            return;
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                teleportToRandomSpawn(player);
            }
        }.runTaskLater(this, respawnDelayTicks);
    }


    private void teleportToRandomSpawn(Player player) {
        World world = player.getWorld();
        Random random = new Random();

        int attempts = 0;
        int maxAttempts = 5;

        while (attempts < maxAttempts) {
            double x = spawnX + random.nextDouble() * spawnRadius * 2 - spawnRadius + 0.5;
            double z = spawnZ + random.nextDouble() * spawnRadius * 2 - spawnRadius + 0.5;
            int y = world.getHighestBlockYAt((int) x, (int) z) + 1;

            // Add a check for Y64 or higher to prevent players from spawning in ravines/caves/water
            if (y >= 64) {
                Location spawnLocation = new Location(world, x, y, z);

                // Check if the block at the spawn location, and the block 1 in all directions is water or lava
                if (!isUnsafeBlock(spawnLocation.getBlock()) ||
                        !isUnsafeBlock(spawnLocation.clone().add(0, -1, 0).getBlock()) ||
                        !isUnsafeBlock(spawnLocation.clone().add(1, -1, 0).getBlock()) ||
                        !isUnsafeBlock(spawnLocation.clone().add(0, -1, 1).getBlock()) ||
                        !isUnsafeBlock(spawnLocation.clone().add(-1, -1, 0).getBlock()) ||
                        !isUnsafeBlock(spawnLocation.clone().add(0, -2, 0).getBlock()) ||
                        !isUnsafeBlock(spawnLocation.clone().add(-1, 0, 0).getBlock()) ||
                        !isUnsafeBlock(spawnLocation.clone().add(0, 2, 0).getBlock()) ||
                        !isUnsafeBlock(spawnLocation.clone().add(0, -1, -1).getBlock())) {

                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            if (isSafeLocation(spawnLocation)) {
                                player.teleport(spawnLocation);
                            }
                        }
                    }.runTaskLater(this, 10L); // 20 ticks = 1-second delay

                    attempts++;
                } else {
                    attempts++; // Increment attempts without teleporting if location is unsafe
                }
            } else {
                attempts++; // Increment attempts without teleporting if Y coordinate is less than 64
            }
        }

        // If a suitable location is not found after the maximum attempts, teleport them to the original spawn location
        player.teleport(world.getSpawnLocation());
    }


    private boolean isUnsafeBlock(Block block) {
        Material blockType = block.getType();
        return blockType == Material.WATER || blockType == Material.LAVA || blockType == Material.CACTUS || blockType == Material.FIRE || blockType == Material.MAGMA_BLOCK || blockType == Material.COBWEB || blockType == Material.SWEET_BERRY_BUSH || blockType == Material.BAMBOO || blockType == Material.LILY_PAD;
    }
    private boolean isSafeLocation(Location location) {
        Block feetBlock = location.getBlock();
        Block headBlock = feetBlock.getRelative(0, 1, 0);

        return feetBlock.getType().isSolid() && headBlock.getType().isSolid();
    }


    @Override
    public void onDisable() {
        getLogger().info("[RandomSpawn] Issues, crashing, or want something added? Contact me on Discord. Arc#5404");
    }
}
