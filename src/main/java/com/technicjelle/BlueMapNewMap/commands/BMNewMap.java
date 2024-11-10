package com.technicjelle.BlueMapNewMap.commands;

import com.technicjelle.BlueMapNewMap.BlueMapNewMap;
import de.bluecolored.bluemap.api.BlueMapAPI;
import de.bluecolored.bluemap.common.BlueMapService;
import de.bluecolored.bluemap.common.api.BlueMapAPIImpl;
import de.bluecolored.bluemap.common.config.BlueMapConfigManager;
import de.bluecolored.bluemap.common.config.ConfigTemplate;
import de.bluecolored.bluemap.core.resources.pack.datapack.DataPack;
import de.bluecolored.bluemap.core.util.Key;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import static de.bluecolored.bluemap.common.config.BlueMapConfigManager.MAPS_CONFIG_FOLDER_NAME;

public class BMNewMap implements CommandExecutor, TabCompleter {
	private final BlueMapNewMap plugin;

	public BMNewMap(BlueMapNewMap plugin) {
		this.plugin = plugin;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		// === Checks ===
		if (!(sender.isOp() || sender.hasPermission("bm-newmap"))) {
			sender.sendMessage(ChatColor.RED + "You are not allowed to use this command" + ChatColor.RESET);
			return true;
		}

		if (args.length != 2) {
			return false;
		}

		if (BlueMapAPI.getInstance().isEmpty()) {
			sender.sendMessage(ChatColor.RED + "BlueMap API is not online!" + ChatColor.RESET);
			return true;
		}

		// === Variable initialization ===
		BlueMapAPI blueMapAPI = BlueMapAPI.getInstance().get();

		World bukkitWorld;
		String mapName;
		try {
			// == Bukkit World ==
			String worldName = args[0];
			bukkitWorld = Bukkit.getWorld(worldName);
			if (bukkitWorld == null) {
				sender.sendMessage(ChatColor.RED + "World \"" + worldName + "\" not found!" + ChatColor.RESET);
				return true;
			}

			// == Map Name ==
			mapName = args[1];
			if (!mapName.matches("^[A-Za-z0-9_-]*$")) {
				sender.sendMessage(ChatColor.RED + "Map name may only contain letters, numbers, dashes and underscores!" + ChatColor.RESET);
				return true;
			}
		} catch (ArrayIndexOutOfBoundsException e) {
			return false;
		}

		// === Run ===
		try (BlueMapService blueMapService = ((BlueMapAPIImpl) blueMapAPI).blueMapService()) {
			BlueMapConfigManager configManager = (BlueMapConfigManager) blueMapService.getConfig();

			Path mapPath = configManager.getConfigManager().resolveConfigFile(MAPS_CONFIG_FOLDER_NAME + "/" + mapName);
			if (Files.exists(mapPath)) {
				sender.sendMessage(ChatColor.RED + "Map \"" + mapName + "\" already exists!" + ChatColor.RESET);
				return true;
			}

			Path worldFolder = bukkitWorld.getWorldFolder().toPath();
			String configContent = switch (bukkitWorld.getEnvironment()) {
				case NORMAL -> newOverworld(configManager, mapName, worldFolder);
				case NETHER -> newNether(configManager, mapName, worldFolder);
				case THE_END -> newEnd(configManager, mapName, worldFolder);
			};

			Files.writeString(mapPath, configContent, StandardOpenOption.CREATE_NEW);

			sender.sendMessage("Created new map! You should run " + ChatColor.UNDERLINE + "/bluemap reload" + ChatColor.RESET + " to apply the changes.");
			return true;
		} catch (Exception e) {
			sender.sendMessage(ChatColor.RED + "Failed to create new map! Check the server console for more details." + ChatColor.RESET);
			plugin.getLogger().log(Level.SEVERE, "Failed to create new map!", e);
			return true;
		}
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
		List<String> completions = new ArrayList<>();

		if (args.length == 1) {
			for (World world : Bukkit.getWorlds()) {
				completions.add(world.getName());
			}
		}

		return completions;
	}

	private static String newOverworld(BlueMapConfigManager configManager, String name, Path worldFolder)
			throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
		return newConfig(configManager, name, worldFolder, "createOverworldMapTemplate", DataPack.DIMENSION_OVERWORLD);
	}

	private static String newNether(BlueMapConfigManager configManager, String name, Path worldFolder)
			throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
		return newConfig(configManager, name, worldFolder, "createNetherMapTemplate", DataPack.DIMENSION_THE_NETHER);
	}

	private static String newEnd(BlueMapConfigManager configManager, String name, Path worldFolder)
			throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
		return newConfig(configManager, name, worldFolder, "createEndMapTemplate", DataPack.DIMENSION_THE_END);
	}

	private static String newConfig(BlueMapConfigManager configManager, String name, Path worldFolder, String methodName, Key dimensionKey)
			throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
		Method method = configManager.getClass().getDeclaredMethod(methodName, String.class, Path.class, Key.class, int.class);
		method.setAccessible(true);
		Object result = method.invoke(configManager, name, worldFolder, dimensionKey, 0);
		ConfigTemplate template = (ConfigTemplate) result;
		return template.build();
	}
}
