package me.devplugins.database;

import me.devplugins.DevTicket;
import me.devplugins.model.Ticket;
import me.devplugins.model.TicketMessage;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;


public class DatabaseManager {
    
    private final DevTicket plugin;
    private final File ticketsFolder;
    private final File messagesFolder;
    private final File cooldownsFile;
    private FileConfiguration cooldownsConfig;
    
    public DatabaseManager(DevTicket plugin) {
        this.plugin = plugin;
        this.ticketsFolder = new File(plugin.getDataFolder(), "tickets");
        this.messagesFolder = new File(plugin.getDataFolder(), "messages");
        this.cooldownsFile = new File(plugin.getDataFolder(), "cooldowns.yml");
    }
    
    public void initialize() {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }
        
        if (!ticketsFolder.exists()) {
            ticketsFolder.mkdirs();
        }
        
        if (!messagesFolder.exists()) {
            messagesFolder.mkdirs();
        }
        
        if (!cooldownsFile.exists()) {
            try {
                cooldownsFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Erro ao criar arquivo de cooldowns: " + e.getMessage());
            }
        }
        
        cooldownsConfig = YamlConfiguration.loadConfiguration(cooldownsFile);
        
        plugin.getLogger().info("Sistema de armazenamento YAML inicializado com sucesso!");
        plugin.getLogger().info("Tickets: " + ticketsFolder.getAbsolutePath());
        plugin.getLogger().info("Mensagens: " + messagesFolder.getAbsolutePath());
    }
    
        public CompletableFuture<Boolean> saveTicket(Ticket ticket) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                File ticketFile = new File(ticketsFolder, ticket.getId().toString() + ".yml");
                FileConfiguration config = new YamlConfiguration();
                
                config.set("id", ticket.getId().toString());
                config.set("player_uuid", ticket.getPlayerUUID().toString());
                config.set("player_name", ticket.getPlayerName());
                config.set("title", ticket.getTitle());
                config.set("content", ticket.getContent());
                config.set("category", ticket.getCategory());
                config.set("priority", ticket.getPriority());
                config.set("status", ticket.getStatus());
                config.set("created_at", ticket.getCreatedAt());
                config.set("updated_at", ticket.getUpdatedAt());
                
                if (ticket.getAssignedTo() != null) {
                    config.set("assigned_to", ticket.getAssignedTo().toString());
                    config.set("assigned_name", ticket.getAssignedName());
                }
                
                if (ticket.getClosedAt() > 0) {
                    config.set("closed_at", ticket.getClosedAt());
                    config.set("close_reason", ticket.getCloseReason());
                }
                
                config.save(ticketFile);
                return true;
                
            } catch (IOException e) {
                plugin.getLogger().severe("Erro ao salvar ticket: " + e.getMessage());
                return false;
            }
        });
    }
    
    public CompletableFuture<Ticket> loadTicket(UUID ticketId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                File ticketFile = new File(ticketsFolder, ticketId.toString() + ".yml");
                if (!ticketFile.exists()) {
                    return null;
                }
                
                FileConfiguration config = YamlConfiguration.loadConfiguration(ticketFile);
                
                UUID id = UUID.fromString(config.getString("id"));
                UUID playerUUID = UUID.fromString(config.getString("player_uuid"));
                String playerName = config.getString("player_name");
                String title = config.getString("title");
                String content = config.getString("content");
                String category = config.getString("category", "Outros");
                String priority = config.getString("priority", "Média");
                String status = config.getString("status", "ABERTO");
                long createdAt = config.getLong("created_at");
                long updatedAt = config.getLong("updated_at");
                
                UUID assignedTo = null;
                String assignedName = null;
                if (config.contains("assigned_to")) {
                    assignedTo = UUID.fromString(config.getString("assigned_to"));
                    assignedName = config.getString("assigned_name");
                }
                
                long closedAt = config.getLong("closed_at", 0);
                String closeReason = config.getString("close_reason");
                
                Ticket ticket = new Ticket(id, playerUUID, playerName, title, content, category, priority, 
                                         status, assignedTo, assignedName, createdAt, updatedAt, closedAt, closeReason);
                
                loadTicketMessages(ticket);
                
                return ticket;
                
            } catch (Exception e) {
                plugin.getLogger().severe("Erro ao carregar ticket: " + e.getMessage());
                return null;
            }
        });
    }
    
    public CompletableFuture<List<Ticket>> loadAllTickets() {
        return CompletableFuture.supplyAsync(() -> {
            List<Ticket> tickets = new ArrayList<>();
            
            File[] ticketFiles = ticketsFolder.listFiles((dir, name) -> name.endsWith(".yml"));
            if (ticketFiles != null) {
                for (File file : ticketFiles) {
                    try {
                        String fileName = file.getName().replace(".yml", "");
                        UUID ticketId = UUID.fromString(fileName);
                        
                        Ticket ticket = loadTicket(ticketId).join();
                        if (ticket != null) {
                            tickets.add(ticket);
                        }
                    } catch (Exception e) {
                        plugin.getLogger().warning("Erro ao carregar ticket " + file.getName() + ": " + e.getMessage());
                    }
                }
            }
            
            tickets.sort((t1, t2) -> Long.compare(t2.getCreatedAt(), t1.getCreatedAt()));
            
            return tickets;
        });
    }
    
    public CompletableFuture<Boolean> saveMessage(TicketMessage message) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                File messageFile = new File(messagesFolder, message.getTicketId().toString() + ".yml");
                FileConfiguration config;
                
                if (messageFile.exists()) {
                    config = YamlConfiguration.loadConfiguration(messageFile);
                } else {
                    config = new YamlConfiguration();
                }
                
                List<Map<String, Object>> messages = (List<Map<String, Object>>) config.getList("messages", new ArrayList<>());
                
                Map<String, Object> messageData = new HashMap<>();
                messageData.put("sender_uuid", message.getSenderUUID().toString());
                messageData.put("sender_name", message.getSenderName());
                messageData.put("message", message.getMessage());
                messageData.put("timestamp", message.getTimestamp());
                messageData.put("is_staff", message.isStaff());
                
                messages.add(messageData);
                config.set("messages", messages);
                
                config.save(messageFile);
                return true;
                
            } catch (IOException e) {
                plugin.getLogger().severe("Erro ao salvar mensagem: " + e.getMessage());
                return false;
            }
        });
    }
    
    private void loadTicketMessages(Ticket ticket) {
        try {
            File messageFile = new File(messagesFolder, ticket.getId().toString() + ".yml");
            if (!messageFile.exists()) {
                return;
            }
            
            FileConfiguration config = YamlConfiguration.loadConfiguration(messageFile);
            List<Map<?, ?>> messagesData = config.getMapList("messages");
            
            List<TicketMessage> messages = new ArrayList<>();
            for (Map<?, ?> data : messagesData) {
                UUID senderUUID = UUID.fromString((String) data.get("sender_uuid"));
                String senderName = (String) data.get("sender_name");
                String message = (String) data.get("message");
                long timestamp = ((Number) data.get("timestamp")).longValue();
                boolean isStaff = (Boolean) data.get("is_staff");
                
                messages.add(new TicketMessage(ticket.getId(), senderUUID, senderName, message, isStaff));
            }
            
            ticket.setMessages(messages);
            
        } catch (Exception e) {
            plugin.getLogger().warning("Erro ao carregar mensagens do ticket " + ticket.getShortId() + ": " + e.getMessage());
        }
    }
    
    public void setCooldown(UUID playerUUID, long timestamp) {
        cooldownsConfig.set(playerUUID.toString(), timestamp);
        saveCooldowns();
    }
    
    public long getCooldown(UUID playerUUID) {
        return cooldownsConfig.getLong(playerUUID.toString(), 0);
    }
    
    public Map<UUID, Long> getAllCooldowns() {
        Map<UUID, Long> cooldowns = new HashMap<>();
        
        for (String key : cooldownsConfig.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                long timestamp = cooldownsConfig.getLong(key);
                cooldowns.put(uuid, timestamp);
            } catch (IllegalArgumentException e) {
            }
        }
        
        return cooldowns;
    }
    
    private void saveCooldowns() {
        try {
            cooldownsConfig.save(cooldownsFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Erro ao salvar cooldowns: " + e.getMessage());
        }
    }
    
    public CompletableFuture<Boolean> deleteTicket(UUID ticketId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                File ticketFile = new File(ticketsFolder, ticketId.toString() + ".yml");
                File messageFile = new File(messagesFolder, ticketId.toString() + ".yml");
                
                boolean ticketDeleted = !ticketFile.exists() || ticketFile.delete();
                boolean messagesDeleted = !messageFile.exists() || messageFile.delete();
                
                return ticketDeleted && messagesDeleted;
                
            } catch (Exception e) {
                plugin.getLogger().severe("Erro ao deletar ticket: " + e.getMessage());
                return false;
            }
        });
    }
    
    public void close() {
        saveCooldowns();
        plugin.getLogger().info("Sistema de armazenamento YAML finalizado.");
    }
}