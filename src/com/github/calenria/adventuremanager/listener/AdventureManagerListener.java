package com.github.calenria.adventuremanager.listener;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import com.github.calenria.adventuremanager.AdventureManager;

public class AdventureManagerListener implements Listener {
    private AdventureManager plugin = null;

    public AdventureManagerListener(AdventureManager plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, this.plugin);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onPlayerChangedWorldEvent(PlayerChangedWorldEvent event) {
        World toWorld = event.getPlayer().getWorld();
        World fromWorld = event.getFrom();

        if (plugin.getConfig().getList("enabledWorlds").contains(toWorld.getName())) {
            plugin.setAktivePlayer(toWorld, true);
        }

        if (plugin.getConfig().getList("enabledWorlds").contains(fromWorld.getName())) {
            plugin.setAktivePlayer(fromWorld, false);
        }

    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onPlayerJoinEvent(PlayerJoinEvent event) {
        World world = event.getPlayer().getWorld();
        if (plugin.getConfig().getList("enabledWorlds").contains(world.getName())) {
            plugin.setAktivePlayer(world, true);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onPlayerQuitEvent(PlayerQuitEvent event) {
        World world = event.getPlayer().getWorld();
        if (plugin.getConfig().getList("enabledWorlds").contains(world.getName())) {
            plugin.setAktivePlayer(world, false);
        }
    }
}
