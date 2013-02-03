/*
 * Copyright (C) 2012 Calenria <https://github.com/Calenria/> and contributors
 * 
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 3.0 of the License, or (at your option)
 * any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 */

package com.github.calenria.adventuremanager;

import java.io.File;
import java.util.Date;
import java.util.HashMap;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import com.github.calenria.adventuremanager.listener.AdventureManagerListener;
import com.github.calenria.adventuremanager.models.AMWorld;
import com.onarandombox.MultiverseCore.MultiverseCore;
import com.onarandombox.MultiverseCore.api.MVPlugin;
import com.sk89q.bukkit.util.CommandsManagerRegistration;
import com.sk89q.minecraft.util.commands.CommandException;
import com.sk89q.minecraft.util.commands.CommandPermissionsException;
import com.sk89q.minecraft.util.commands.CommandUsageException;
import com.sk89q.minecraft.util.commands.CommandsManager;
import com.sk89q.minecraft.util.commands.MissingNestedCommandException;
import com.sk89q.minecraft.util.commands.WrappedCommandException;

public class AdventureManager extends JavaPlugin implements MVPlugin {

    private static Logger                   log              = Logger.getLogger("Minecraft");
    private static final String             logPrefix        = "[AdventureManager] ";
    public CommandsManager<CommandSender>   commands;
    public static MultiverseCore            multiversePlugin;
    public static String                    pluginPath;
    private final static int                requiresProtocol = 14;

    private static HashMap<String, AMWorld> playersPerWorld  = new HashMap<String, AMWorld>();

    private static AdventureManager         plugin;

    public BukkitTask                       task;

    private static AdventureManagerListener listener;

    public AdventureManager() {
        plugin = this;
    }

    public static AdventureManager getInstance() {
        if (plugin != null) {
            return plugin;
        }
        plugin = new AdventureManager();
        return plugin;
    }

    @Override
    public void onEnable() {
        if (!setupMultiverse()) {
            log.log(Level.SEVERE, String.format("[%s] - Disabled due to no Multiverse dependency found!", getDescription().getName()));
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        setupCommands();
        plugin = this;

        if (!new File(plugin.getDataFolder(), "config.yml").exists()) {
            plugin.getConfig().options().copyDefaults(true);
            saveConfig();
        } else {
            reloadConfig();
        }

        setupdListeners();
        setupSheduler();

        log.log(Level.INFO, String.format("[%s] Enabled Version %s", getDescription().getName(), getDescription().getVersion()));
    }

    public void setupdListeners() {
        setListener(new AdventureManagerListener(this));
    }

    public void setupSheduler() {
        if (plugin.getConfig().getBoolean("resetAfterLastPlayer")) {
            plugin.task = plugin.getServer().getScheduler().runTaskTimer(this, new Runnable() {
                public void run() {
                    Set<String> playersKeySet = playersPerWorld.keySet();
                    for (String world : playersKeySet) {
                        if (Bukkit.getServer().getWorld(world).getPlayers().size() == 0) {
                            synchronized (playersPerWorld) {
                                if (checkReset(playersPerWorld.get(world))) {
                                    log.log(Level.INFO, String.format("[%s] Resetting World %s", getDescription().getName(), world));
                                    playersPerWorld.remove(world);

                                    final File worldFolder = new File(plugin.getServer().getWorldContainer().getAbsoluteFile() + File.separator + world);
                                    final File worldTemplateFolder = new File(plugin.getServer().getWorldContainer().getAbsoluteFile() + File.separator + world + ".Template");

                                    try {
                                        Commands.resetWorld(null, world, worldFolder, worldTemplateFolder);
                                    } catch (CommandException e) {
                                        log.log(Level.SEVERE, e.getLocalizedMessage(), e);
                                    }
                                }
                            }
                        }
                    }
                }
            }, 20L, plugin.getConfig().getLong("pollTime") * 20);
        }
    }

    private boolean checkReset(AMWorld amWorld) {
        Long now = System.currentTimeMillis();
        Long lastLeave = amWorld.lastPlayerLeave.getTime();

        if (now - lastLeave > plugin.getConfig().getLong("resetTimeSeconds") * 1000) {
            return true;
        }

        return false;
    }

    private boolean setupMultiverse() {
        if (getServer().getPluginManager().getPlugin("Multiverse-Core") == null) {
            return false;
        }
        multiversePlugin = (MultiverseCore) getServer().getPluginManager().getPlugin("Multiverse-Core");
        if (multiversePlugin == null) {
            log.info(logPrefix + "Multiverse-Core not found, will keep looking.");
            getServer().getPluginManager().disablePlugin(this);
            return false;
        }
        if (multiversePlugin.getProtocolVersion() < requiresProtocol) {
            log.severe(logPrefix + "Your Multiverse-Core is OUT OF DATE");
            log.severe(logPrefix + "This version of AdventureWorlds requires Protocol Level: " + requiresProtocol);
            log.severe(logPrefix + "Your of Core Protocol Level is: " + multiversePlugin.getProtocolVersion());
            log.severe(logPrefix + "Grab an updated copy at: ");
            log.severe(logPrefix + "http://bukkit.onarandombox.com/?dir=multiverse-core");
            getServer().getPluginManager().disablePlugin(this);
            return false;
        }
        return multiversePlugin != null;
    }

    @Override
    public void onDisable() {
        log.log(Level.INFO, String.format("[%s] Disabled Version %s", getDescription().getName(), getDescription().getVersion()));
    }

    private void setupCommands() {
        this.commands = new CommandsManager<CommandSender>() {
            @Override
            public boolean hasPermission(CommandSender sender, String perm) {
                return sender.hasPermission(perm);
            }
        };

        CommandsManagerRegistration cmdRegister = new CommandsManagerRegistration(this, this.commands);
        // cmdRegister.register(MapCommands.class);
        cmdRegister.register(Commands.class);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {

        try {
            this.commands.execute(cmd.getName(), args, sender, sender);
        } catch (CommandPermissionsException e) {
            sender.sendMessage(ChatColor.RED + "You don't have permission.");
        } catch (MissingNestedCommandException e) {
            sender.sendMessage(ChatColor.RED + e.getUsage());
        } catch (CommandUsageException e) {
            sender.sendMessage(ChatColor.RED + e.getMessage());
            sender.sendMessage(ChatColor.RED + e.getUsage());
        } catch (WrappedCommandException e) {
            if (e.getCause() instanceof NumberFormatException) {
                sender.sendMessage(ChatColor.RED + "Number expected, string received instead.");
            } else {
                sender.sendMessage(ChatColor.RED + "An error has occurred. See console.");
                e.printStackTrace();
            }
        } catch (CommandException e) {
            sender.sendMessage(ChatColor.RED + e.getMessage());
        }

        return true;
    }

    @Override
    public void log(Level level, String msg) {
        log.log(level, logPrefix + " " + msg);
    }

    @Override
    public String dumpVersionInfo(String msg) {
        return this.getDescription().getVersion();
    }

    @Override
    public MultiverseCore getCore() {
        return multiversePlugin;
    }

    @Override
    public void setCore(MultiverseCore core) {
        multiversePlugin = core;
    }

    @Override
    public int getProtocolVersion() {
        return requiresProtocol;
    }

    public synchronized void setAktivePlayer(World world, Boolean join) {
        synchronized (playersPerWorld) {
            if (playersPerWorld.containsKey(world.getName()) && join) {
                AMWorld amworld = playersPerWorld.get(world.getName());
                amworld.lastPlayerJoin = new Date();
                playersPerWorld.put(world.getName(), amworld);
            } else if (join) {
                AMWorld amworld = new AMWorld();
                amworld.lastPlayerJoin = new Date();
                amworld.world = world;
                playersPerWorld.put(world.getName(), amworld);
            }

            if (playersPerWorld.containsKey(world.getName()) && !join) {
                AMWorld amworld = playersPerWorld.get(world.getName());
                amworld.lastPlayerLeave = new Date();
                playersPerWorld.put(world.getName(), amworld);
            } else if (!join) {
                AMWorld amworld = new AMWorld();
                amworld.world = world;
                amworld.lastPlayerLeave = new Date();
                playersPerWorld.put(world.getName(), amworld);
            }
        }
    }

    public synchronized static void removeWorldFromList(String world) {
        synchronized (playersPerWorld) {
            playersPerWorld.remove(world);
        }
    }

    /**
     * @return the listener
     */
    public static AdventureManagerListener getListener() {
        return listener;
    }

    /**
     * @param listener
     *            the listener to set
     */
    public static void setListener(AdventureManagerListener listener) {
        AdventureManager.listener = listener;
    }

}
