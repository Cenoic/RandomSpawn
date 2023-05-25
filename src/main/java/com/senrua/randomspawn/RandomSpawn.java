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

        getLogger().info("player respawning");
        if (!randomSpawnOnRespawn) {
            getLogger().info("Respawn denied");
            return;
        }

        if (player.getBedSpawnLocation() != null) {
            getLogger().info("Bedspawn Denied");
            return;
        }
        getLogger().info("Respawn Cleared");
        new BukkitRunnable() {
            @Override
            public void run() {
                getLogger().info("Running RandomSpawn Process");
                teleportToRandomSpawn(player);
            }
        }.runTaskLater(this, respawnDelayTicks);
    }


    private void teleportToRandomSpawn(Player player) {
        World world = player.getWorld();
        Random random = new Random();

        int attempts = 0;
        int maxAttempts = 10;

        while (attempts < maxAttempts) {
            double x = spawnX + random.nextDouble() * spawnRadius * 2 - spawnRadius;
            double z = spawnZ + random.nextDouble() * spawnRadius * 2 - spawnRadius;
            int y = world.getHighestBlockYAt((int) x, (int) z) + 1;

            if (y < 63) {
                attempts++;
                getLogger().info("Invalid Y coordinate for spawn location: " + y);
                continue;
            }

            Location spawnLocation = new Location(world, x, y, z);
            if (!isSafeSpawnLocation(spawnLocation)) {
                attempts++;
                getLogger().info("Unsafe spawn location generated: " + spawnLocation);
                continue;
            }

            player.teleport(spawnLocation);
            getLogger().info("Player teleported to random spawn location: " + spawnLocation);
            return; // Stop executing the code after teleporting the player
        }

        player.teleport(world.getSpawnLocation());
        getLogger().info("Player teleported to the original spawn location.");
    }

    private boolean isSafeSpawnLocation(Location location) {
        Block centerBlock = location.getBlock();
        Block belowBlock = location.clone().add(0, -1, 0).getBlock();

        Material centerBlockType = centerBlock.getType();
        Material belowBlockType = belowBlock.getType();

        return !isUnsafeBlock(centerBlockType) &&
                !isUnsafeBlock(belowBlockType);
    }

    private boolean isUnsafeBlock(Material blockType) {
        return blockType == Material.WATER ||
                blockType == Material.LAVA ||
                blockType == Material.CACTUS ||
                blockType == Material.FIRE ||
                blockType == Material.MAGMA_BLOCK ||
                blockType == Material.SWEET_BERRY_BUSH ||
                blockType == Material.BAMBOO;
    }


    @Override
    public void onDisable() {
        getLogger().info("[RandomSpawn] Issues, crashing, or want something added? Contact me on Discord. Arc#5404");
    }
}
