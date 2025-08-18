package me.devplugins.integrations;

import me.devplugins.DevTicket;
import me.devplugins.config.ConfigManager;
import me.devplugins.model.Ticket;
import org.bukkit.Bukkit;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.CompletableFuture;


public class DiscordNotifier {
    
    private final DevTicket plugin;
    private final ConfigManager configManager;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm");
    
    public DiscordNotifier(DevTicket plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }

    public void notifyNewTicket(Ticket ticket) {
        if (!configManager.isDiscordEnabled()) {
            return;
        }
        
        String webhookUrl = configManager.getDiscordWebhook();
        if (webhookUrl == null || webhookUrl.trim().isEmpty()) {
            return;
        }
        
        String title = "🎫 Novo Ticket Criado";
        String description = String.format(
            "**ID:** #%s\\n" +
            "**Título:** %s\\n" +
            "**Autor:** %s\\n" +
            "**Categoria:** %s\\n" +
            "**Prioridade:** %s\\n" +
            "**Criado em:** %s",
            ticket.getShortId(),
            ticket.getTitle(),
            ticket.getPlayerName(),
            ticket.getCategory(),
            ticket.getPriority(),
            dateFormat.format(new Date(ticket.getCreatedAt()))
        );
        
        int color = getPriorityColor(ticket.getPriority());
        sendWebhook(title, description, color);
    }
    
    public void notifyTicketClosed(Ticket ticket, String reason) {
        if (!configManager.isDiscordEnabled()) return;
        
        String title = "✅ Ticket Fechado";
        String description = String.format(
            "**ID:** #%s\\n" +
            "**Título:** %s\\n" +
            "**Autor:** %s\\n" +
            "**Motivo:** %s\\n" +
            "**Fechado em:** %s",
            ticket.getShortId(),
            ticket.getTitle(),
            ticket.getPlayerName(),
            reason,
            dateFormat.format(new Date())
        );
        
        sendWebhook(title, description, 0x00FF00);
    }
    
    public void notifyTicketAssigned(Ticket ticket, String staffName) {
        if (!configManager.isDiscordEnabled()) return;
        
        String title = "👤 Ticket Atribuído";
        String description = String.format(
            "**ID:** #%s\\n" +
            "**Título:** %s\\n" +
            "**Autor:** %s\\n" +
            "**Atribuído a:** %s\\n" +
            "**Data:** %s",
            ticket.getShortId(),
            ticket.getTitle(),
            ticket.getPlayerName(),
            staffName,
            dateFormat.format(new Date())
        );
        
        sendWebhook(title, description, 0x0099FF);
    }
    
    private void sendWebhook(String title, String description, int color) {
        CompletableFuture.runAsync(() -> {
            try {
                String webhookUrl = configManager.getDiscordWebhook();
                if (webhookUrl == null || webhookUrl.trim().isEmpty()) {
                    return;
                }
                
                URL url = new URL(webhookUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setRequestProperty("User-Agent", "DevTicket-Bot/1.0.1");
                connection.setDoOutput(true);
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(10000);
                
                String jsonPayload = createEmbedJson(title, description, color);
                
                try (OutputStream os = connection.getOutputStream()) {
                    byte[] input = jsonPayload.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }
                
                int responseCode = connection.getResponseCode();
                
                if (responseCode != 204) {
                    plugin.getLogger().warning("Erro ao enviar webhook Discord. Código: " + responseCode);
                }
                
            } catch (Exception e) {
                plugin.getLogger().warning("Erro ao enviar notificação Discord: " + e.getMessage());
            }
        });
    }
    
    private String createEmbedJson(String title, String description, int color) {
        String escapedTitle = title.replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "");
        String escapedDescription = description.replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "");
        
        String json = String.format(
            "{" +
                "\"embeds\": [" +
                    "{" +
                        "\"title\": \"%s\"," +
                        "\"description\": \"%s\"," +
                        "\"color\": %d," +
                        "\"footer\": {" +
                            "\"text\": \"DevTicket v1.0.1\"" +
                        "}," +
                        "\"timestamp\": \"%s\"" +
                    "}" +
                "]" +
            "}",
            escapedTitle,
            escapedDescription,
            color,
            java.time.Instant.now().toString()
        );
        
        return json;
    }
    
    private int getPriorityColor(String priority) {
        switch (priority.toLowerCase()) {
            case "baixa": return 0x00FF00;
            case "média": return 0xFFFF00;
            case "alta": return 0xFF9900;
            case "urgente": return 0xFF0000;
            default: return 0x808080;
        }
    }
}