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
    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadConfig();
        loadConfig();

        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("[RandomSpawn] Successfully Loaded version 1.1 by Rainie");
    }

    private void loadConfig() {
        ConfigurationSection config = getConfig().getConfigurationSection("spawn");
        spawnX = config.getInt("x");
        spawnZ = config.getInt("z");
        spawnRadius = config.getInt("radius");
        respawnDelayTicks = getConfig().getInt("respawnDelayTicks");
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Check if player has played before using Bukkit API
        if (player.hasPlayedBefore()) {
            // Player has played before, do not teleport them
            return;
        }

        // Player is joining for the first time, teleport them to a random spawn location
        teleportToRandomSpawn(player);
    }


    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();

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
            int x = spawnX + random.nextInt(spawnRadius * 2) - spawnRadius;
            int z = spawnZ + random.nextInt(spawnRadius * 2) - spawnRadius;
            int y = world.getHighestBlockYAt(x, z) + 1;

            // Add a check for Y64 or higher to prevent players from spawning in ravines/caves/water
            if (y >= 64) {
                Location spawnLocation = new Location(world, x, y, z);

                // Check if the block at the spawn location, and the block 1 in all directions is water or lava
                if (!isUnsafeBlock(spawnLocation.getBlock()) ||
                        !isUnsafeBlock(spawnLocation.clone().add(0, -1, 0).getBlock()) ||
                        !isUnsafeBlock(spawnLocation.clone().add(1, -1, 0).getBlock()) ||
                        !isUnsafeBlock(spawnLocation.clone().add(0, -1, 1).getBlock()) ||
                        !isUnsafeBlock(spawnLocation.clone().add(-1, -1, 0).getBlock()) ||
                        !isUnsafeBlock(spawnLocation.clone().add(0, -1, -1).getBlock())) {

                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            if (isSafeLocation(spawnLocation)) {
                                player.teleport(spawnLocation);
                                getLogger().info("Teleported player " + player.getName() + " to location: " +
                                        "X: " + x + ", Y: " + y + ", Z: " + z);
                            }
                        }
                    }.runTaskLater(this, 10L); // 20 ticks = 1 second delay

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
        getLogger().warning("Unable to find a safe spawn location (Y64 or higher) for player " + player.getName() +
                ". Teleported them to the world's default spawn location.");
    }


    private boolean isUnsafeBlock(Block block) {
        Material blockType = block.getType();
        return blockType == Material.WATER || blockType == Material.LAVA;
    }
    private boolean isSafeLocation(Location location) {
        Block feetBlock = location.getBlock();
        Block headBlock = feetBlock.getRelative(0, 1, 0);

        return feetBlock.getType().isSolid() && headBlock.getType().isSolid();
    }


    @Override
    public void onDisable() {
        getLogger().info("[RandomSpawn] Successfully disabled version 1.1 by Rainie");
    }
}
