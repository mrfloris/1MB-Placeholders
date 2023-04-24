package me.OMBPlaceholders;

import java.util.Map;

import org.bukkit.entity.Player;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;

public class CustomPlaceholders extends PlaceholderExpansion {

	@Override
	public String onPlaceholderRequest(Player player, String identifier) {
		
		// Grab the list of placeholders
		Map<String, String> placeholders = Main.getInstance().getPlaceholders();
		
		// Check if it exists, and if it does, return the value
		if(placeholders.containsKey(identifier)) return placeholders.get(identifier);
		
		return null;
	}
	
	@Override
	public String getAuthor() {
		return "mrfloris";
	}

	@Override
	public String getIdentifier() {
		return "onemb";
	}

	@Override
	public String getVersion() {
		return Main.getInstance().getDescription().getVersion();
	}

}
