package me.OMBPlaceholders;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {

	// Map to contain all of the custom placeholders
	private Map<String, String> placeholders = new HashMap<>();
	
	@Override
	public void onEnable() {
		// Creating the config.yml
		
		// Check if the data folder exists - it should, but just in-case. 
		if(!getDataFolder().exists()) getDataFolder().mkdirs();

		File file = new File(getDataFolder(), "config.yml");
		if(!file.exists()) {
			// Copying defaults from the config.yml
			
			getConfig().options().copyDefaults(true);
			getConfig().options().copyHeader(true);
			saveDefaultConfig();
		}
		
		// Registering the placeholder expansion
		new CustomPlaceholders().register();
		
		// Loading the placeholders from the config.yml
		
		// If the config is empty, ignore everything.
		if(!getConfig().contains("Placeholders")) return;
		
		// Looping over every value under the "Placeholders" tag.
		for(String placeholder : getConfig().getConfigurationSection("Placeholders").getKeys(false)) {
			
			// Skipping duplicate entries
			if(placeholder.contains(placeholder)) continue;
			
			// Adding the placeholder to the map
			placeholders.put(placeholder, getConfig().getString("Placeholders." + placeholder));
		}
		
		// Just some console logging.
		getLogger().info("- Successfully loaded (" + placeholders.size() + ") custom placeholders.");
	}
	
	@Override
	public void onDisable() {
		// Don't need to do anything since there is no data to save, or anything to close.
	}
	
	// Allows us to grab this class instance from any class statically.
	public static Main getInstance() {
		return Main.getPlugin(Main.class);
	}

	public Map<String, String> getPlaceholders() {
		return placeholders;
	}
	
}
