package me.devplugins.config;

import me.devplugins.DevTicket;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;

public class ConfigManager {
    
    private final DevTicket plugin;
    private FileConfiguration config;
    
    public ConfigManager(DevTicket plugin) {
        this.plugin = plugin;
        loadConfig();
    }
    
    public void loadConfig() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        this.config = plugin.getConfig();
    }
    
    public void reloadConfig() {
        loadConfig();
    }
        
    public int getMaxTicketsPerPlayer() {
        return config.getInt("tickets.max-per-player", 5);
    }
    
    public int getCooldownSeconds() {
        return config.getInt("tickets.cooldown-seconds", 300);
    }
    
    public int getAutoCloseDays() {
        return config.getInt("tickets.auto-close-days", 7);
    }
    
    public List<String> getCategories() {
        return config.getStringList("tickets.categories");
    }
    
    public List<String> getPriorities() {
        return config.getStringList("tickets.priorities");
    }
    
    public boolean isDiscordEnabled() {
        return config.getBoolean("notifications.discord.enabled", false);
    }
    
    public String getDiscordWebhook() {
        return config.getString("notifications.discord.webhook-url", "");
    }
    
    public boolean isNewTicketAlert() {
        return config.getBoolean("notifications.staff-alerts.new-ticket", true);
    }
    
    public boolean isTicketUpdatedAlert() {
        return config.getBoolean("notifications.staff-alerts.ticket-updated", true);
    }
    
    public boolean isOldTicketsAlert() {
        return config.getBoolean("notifications.staff-alerts.old-tickets", true);
    }
    
    public int getOldTicketDays() {
        return config.getInt("notifications.staff-alerts.old-ticket-days", 3);
    }
    
    public String getMessage(String key) {
        String message = config.getString("messages." + key, "Mensagem não encontrada: " + key);
        return ChatColor.translateAlternateColorCodes('&', message);
    }
    
    public String getMessage(String key, String... replacements) {
        String message = getMessage(key);
        for (int i = 0; i < replacements.length; i += 2) {
            if (i + 1 < replacements.length) {
                message = message.replace("{" + replacements[i] + "}", replacements[i + 1]);
            }
        }
        return message;
    }
    
    public String getPrefix() {
        return getMessage("prefix");
    }
}