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
import java.io.IOException;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.World.Environment;
import org.bukkit.WorldType;
import org.bukkit.command.CommandSender;

import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandContext;
import com.sk89q.minecraft.util.commands.CommandException;
import com.sk89q.minecraft.util.commands.CommandPermissions;

public class Commands {
    private static Logger log = Logger.getLogger("Minecraft");

    @Command(aliases = { "amreload" }, desc = "Läd das Plugin neu", usage = "[world] - Welt die vom Adventuremanager verwaltet werden soll")
    @CommandPermissions("adventuremanager.reload")
    public static void amreload(CommandContext args, CommandSender sender) throws CommandException {

        AdventureManager plugin = AdventureManager.getInstance();
        plugin.reloadConfig();
        try {
            plugin.getServer().getScheduler().cancelTask(plugin.task.getTaskId());
        } catch (NullPointerException e) {
            log.info("Task not found.");
        }
        sender.sendMessage(String.format("%s Version %s reloaded", plugin.getName(), plugin.getDescription().getVersion()));
        plugin.setupdListeners();
        plugin.setupSheduler();

    }

    @Command(aliases = { "amcreate" }, desc = "Erstellt einen Snapshot der Welt", usage = "[world] - Welt die vom Adventuremanager verwaltet werden soll", min = 1, max = 1)
    @CommandPermissions("adventuremanager.create")
    public static void amcreate(CommandContext args, CommandSender sender) throws CommandException {
        if (args.argsLength() == 1) {
            String world = args.getString(0);
            File worldFolder = new File(sender.getServer().getWorldContainer().getAbsoluteFile() + File.separator + world);
            File worldTemplateFolder = new File(sender.getServer().getWorldContainer().getAbsoluteFile() + File.separator + world + ".Template");

            if (AdventureManager.multiversePlugin.getMVWorldManager().getFirstSpawnWorld().getName().equals(world)) {
                throw new CommandException("Du kannst deine Hauptwelt nicht als AdventureManager Welt benutzen");
            }

            if (worldTemplateFolder.exists()) {
                throw new CommandException("Der Templateordner für " + world + " existiert bereits, nutze \"/amupdate " + world + "\" um den Templateordner neu zu erstellen");
            }

            if (worldFolder.exists()) {
                sender.sendMessage(world + " existiert, setze Konfiguration fort");

                if (AdventureManager.multiversePlugin.getMVWorldManager().isMVWorld(world)) {
                    sender.sendMessage("Welt könnte geladen sein, entferne alle Spieler aus der Welt (" + world + ")");
                    AdventureManager.multiversePlugin.getMVWorldManager().removePlayersFromWorld(world);
                    AdventureManager.removeWorldFromList(world);
                    sender.sendMessage("Deaktiviere (" + world + ")");
                    AdventureManager.multiversePlugin.getMVWorldManager().unloadWorld(world);
                } else {
                    sender.sendMessage("Welt ist nicht geladen, erstelle Multiverse Config für die Welt (" + world + ")");
                    AdventureManager.multiversePlugin.getMVWorldManager().addWorld(world, Environment.NORMAL, null, WorldType.NORMAL, true, null);
                    AdventureManager.multiversePlugin.getMVWorldManager().removePlayersFromWorld(world);
                    AdventureManager.removeWorldFromList(world);
                    sender.sendMessage("Deaktiviere (" + world + ")");
                    AdventureManager.multiversePlugin.getMVWorldManager().unloadWorld(world);
                }
                sender.sendMessage("Erstelle ein Template für die Welt (" + world + ")");
                try {
                    Utils.copyDir(worldFolder, worldTemplateFolder);
                    sender.sendMessage("Template für die Welt (" + world + ") für die Welt erstellt!");
                    sender.sendMessage("Aktiviere (" + world + ")");
                    AdventureManager.multiversePlugin.getMVWorldManager().loadWorld(world);
                } catch (IOException e) {
                    throw new CommandException("Es ist ein Fehler beim erstellen der Welt (" + world + ") aufgetreten!");
                }
            } else {
                throw new CommandException("Welt (" + world + ") existiert nicht!");
            }
        }
    }

    @Command(aliases = { "amupdate" }, desc = "Updatet den Snapshot mit aktuellen Änderungen", usage = "[world] - Welt die vom Adventuremanager geupdatet werden soll", min = 1, max = 1)
    @CommandPermissions("adventuremanager.update")
    public static void amupdate(CommandContext args, CommandSender sender) throws CommandException {
        if (args.argsLength() == 1) {
            String world = args.getString(0);
            File worldFolder = new File(sender.getServer().getWorldContainer().getAbsoluteFile() + File.separator + world);
            File worldTemplateFolder = new File(sender.getServer().getWorldContainer().getAbsoluteFile() + File.separator + world + ".Template");
            try {
                sender.sendMessage("Welt könnte geladen sein, entferne alle Spieler aus der Welt (" + world + ")");
                AdventureManager.multiversePlugin.getMVWorldManager().removePlayersFromWorld(world);
                AdventureManager.removeWorldFromList(world);
                sender.sendMessage("Deaktiviere (" + world + ")");
                AdventureManager.multiversePlugin.getMVWorldManager().unloadWorld(world);
                Bukkit.getServer().unloadWorld(world, true);
                Bukkit.getServer().savePlayers();
                Utils.cleanDirectory(worldTemplateFolder);
                worldTemplateFolder.mkdir();
                Utils.copyDir(worldFolder, worldTemplateFolder);
                sender.sendMessage("Template der Welt (" + world + ") wurde Aktualisiert");
                sender.sendMessage("Aktiviere (" + world + ")");
                AdventureManager.multiversePlugin.getMVWorldManager().loadWorld(world);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Command(aliases = { "amremove" }, desc = "Löscht den Snapshot", usage = "[world] - Welt die nicht mehr vom Adventuremanager verwaltet werden soll", min = 1, max = 1)
    @CommandPermissions("adventuremanager.remove")
    public static void amremove(CommandContext args, CommandSender sender) throws CommandException {
        if (args.argsLength() == 1) {
            final String world = args.getString(0);
            final File worldTemplateFolder = new File(sender.getServer().getWorldContainer().getAbsoluteFile() + File.separator + world + ".Template");
            if (worldTemplateFolder.exists()) {
                Utils.deleteDirectory(worldTemplateFolder);
                sender.sendMessage("Template der Welt (" + world + ") gelöscht");
            } else {
                throw new CommandException("Das Template der Welt (" + world + ") existiert nicht!");
            }
        }
    }

    @Command(aliases = { "amreset" }, desc = "Versetzt die Welt wieder in den Ursprungszustand", usage = "[world] - Welt die vom Adventuremanager resettet werden soll", min = 1, max = 1)
    @CommandPermissions("adventuremanager.reset")
    public static void amreset(CommandContext args, CommandSender sender) throws CommandException {
        if (args.argsLength() == 1) {
            final String world = args.getString(0);
            final File worldFolder = new File(sender.getServer().getWorldContainer().getAbsoluteFile() + File.separator + world);
            final File worldTemplateFolder = new File(sender.getServer().getWorldContainer().getAbsoluteFile() + File.separator + world + ".Template");

            resetWorld(sender, world, worldFolder, worldTemplateFolder);
        }
    }

    public static void resetWorld(CommandSender sender, final String world, final File worldFolder, final File worldTemplateFolder) throws CommandException {
        if (worldTemplateFolder.exists()) {
            if (sender == null) {
                log.info(world + " und der Templateordner existiert, setze Reset fort");
                log.info("Entferne alle Spieler aus der Welt (" + world + ")");
            } else {
                sender.sendMessage(world + " und der Templateordner existiert, setze Reset fort");
                sender.sendMessage("Entferne alle Spieler aus der Welt (" + world + ")");
            }

            AdventureManager.multiversePlugin.getMVWorldManager().removePlayersFromWorld(world);

            AdventureManager.removeWorldFromList(world);

            AdventureManager.multiversePlugin.getMVWorldManager().unloadWorld(world);
            Bukkit.getServer().unloadWorld(world, true);
            Bukkit.getServer().savePlayers();
            try {
                try {
                    Utils.cleanDirectory(worldFolder);
                    // worldFolder.mkdir();
                } catch (Exception e) {
                    if (sender == null) {
                        log.severe("Fehler beim Löschen der Welt.");
                    } else {
                        sender.sendMessage("Fehler beim Löschen der Welt.");
                    }

                }

                Utils.copyDir(worldTemplateFolder, worldFolder);
                if (sender == null) {
                    log.info("Welt (" + world + ") wurde resettet!");
                    log.info("Aktiviere (" + world + ")");
                } else {
                    sender.sendMessage("Welt (" + world + ") wurde resettet!");
                    sender.sendMessage("Aktiviere (" + world + ")");
                }
                AdventureManager.multiversePlugin.getMVWorldManager().loadWorld(world);

            } catch (IOException e) {
                if (sender == null) {
                    log.info("Eine Datei ist noch in benutzung, du wirst Informiert sowie die Welt resettet wurde!");
                } else {
                    sender.sendMessage("Eine Datei ist noch in benutzung, du wirst Informiert sowie die Welt resettet wurde!");
                }

                AdventureManager.getInstance().getServer().getScheduler().scheduleSyncRepeatingTask(AdventureManager.getInstance(), new Runnable() {
                    public void run() {
                        try {
                            Utils.copyDir(worldTemplateFolder, worldFolder);
                            AdventureManager.getInstance().getServer().broadcastMessage("Welt (" + world + ") wurde resettet!");
                            AdventureManager.getInstance().getServer().broadcastMessage("Aktiviere (" + world + ")");
                            AdventureManager.multiversePlugin.getMVWorldManager().loadWorld(world);
                            AdventureManager.getInstance().getServer().getScheduler().cancelTasks(AdventureManager.getInstance());
                        } catch (IOException e) {

                        }
                    }
                }, 60L, 10L);
            }
        } else {
            throw new CommandException("Die Welt (" + world + ") oder das zugehörige Template existiert nicht!");
        }
    }

}
