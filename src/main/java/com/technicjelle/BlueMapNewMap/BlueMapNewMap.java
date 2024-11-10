package com.technicjelle.BlueMapNewMap;

import com.technicjelle.BlueMapNewMap.commands.BMNewMap;
import com.technicjelle.UpdateChecker;
import de.bluecolored.bluemap.api.BlueMapAPI;
import org.bstats.bukkit.Metrics;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;


public final class BlueMapNewMap extends JavaPlugin {

	@Override
	public void onEnable() {
		new Metrics(this, 23860);

		UpdateChecker updateChecker = new UpdateChecker("TechnicJelle", "BlueMapNewMap", getDescription().getVersion());
		updateChecker.checkAsync();
		BlueMapAPI.onEnable(api -> updateChecker.logUpdateMessage(getLogger()));

		PluginCommand bmNewMap = getCommand("bm-newmap");
		BMNewMap executor = new BMNewMap(this);
		if (bmNewMap != null) {
			bmNewMap.setExecutor(executor);
			bmNewMap.setTabCompleter(executor);
		} else {
			getLogger().warning("bm-newmap is null. This is not good");
		}
	}
}
