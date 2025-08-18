package me.devplugins.manager;

import me.devplugins.DevTicket;
import me.devplugins.config.ConfigManager;
import me.devplugins.database.DatabaseManager;
import me.devplugins.model.Ticket;
import me.devplugins.model.TicketMessage;
import me.devplugins.model.TicketStatus;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class TicketManager {

    private final DevTicket plugin;
    private final DatabaseManager databaseManager;
    private final ConfigManager configManager;
    private final Map<UUID, Long> playerCooldowns;
    private final Map<UUID, Ticket> ticketCache;

    public TicketManager(DevTicket plugin, DatabaseManager databaseManager, ConfigManager configManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
        this.configManager = configManager;
        this.playerCooldowns = new ConcurrentHashMap<>();
        this.ticketCache = new ConcurrentHashMap<>();
        loadCooldowns();
    }

    public CompletableFuture<Ticket> createTicket(Player player, String title, String content, String category, String priority) {
        return CompletableFuture.supplyAsync(() -> {
            if (isOnCooldown(player.getUniqueId())) {
                return null;
            }

            if (getPlayerOpenTicketCount(player.getUniqueId()) >= configManager.getMaxTicketsPerPlayer()) {
                return null;
            }

            UUID ticketId = UUID.randomUUID();
            Ticket ticket = new Ticket(ticketId, player.getUniqueId(), player.getName(), title, content, category, priority);

            boolean saved = databaseManager.saveTicket(ticket).join();
            
            if (saved) {
                ticketCache.put(ticketId, ticket);

                setCooldown(player.getUniqueId());

                notifyStaffNewTicket(ticket);
                
                plugin.getDiscordNotifier().notifyNewTicket(ticket);

                return ticket;
            } else {
                plugin.getLogger().severe("Erro ao salvar ticket no sistema de arquivos");
                return null;
            }
        });
    }

    public CompletableFuture<Ticket> getTicket(UUID ticketId) {
        return CompletableFuture.supplyAsync(() -> {
            if (ticketCache.containsKey(ticketId)) {
                return ticketCache.get(ticketId);
            }

            Ticket ticket = databaseManager.loadTicket(ticketId).join();
            
            if (ticket != null) {
                ticketCache.put(ticketId, ticket);
            }
            
            return ticket;
        });
    }

    public CompletableFuture<List<Ticket>> getPlayerTickets(UUID playerUUID) {
        return CompletableFuture.supplyAsync(() -> {
            List<Ticket> allTickets = databaseManager.loadAllTickets().join();
            
            return allTickets.stream()
                    .filter(ticket -> ticket.getPlayerUUID().equals(playerUUID))
                    .collect(java.util.stream.Collectors.toList());
        });
    }

    public CompletableFuture<List<Ticket>> getAllTickets() {
        return databaseManager.loadAllTickets();
    }

    public CompletableFuture<List<Ticket>> getTicketsByStatus(String status) {
        return CompletableFuture.supplyAsync(() -> {
            List<Ticket> allTickets = databaseManager.loadAllTickets().join();
            
            return allTickets.stream()
                    .filter(ticket -> ticket.getStatus().equals(status))
                    .collect(java.util.stream.Collectors.toList());
        });
    }

    public CompletableFuture<Boolean> updateTicketStatus(UUID ticketId, String status) {
        return CompletableFuture.supplyAsync(() -> {
            Ticket ticket = getTicket(ticketId).join();
            if (ticket != null) {
                ticket.setStatus(status);
                boolean saved = databaseManager.saveTicket(ticket).join();
                
                if (saved) {
                    ticketCache.put(ticketId, ticket);
                    return true;
                }
            }
            return false;
        });
    }

    public CompletableFuture<Boolean> assignTicket(UUID ticketId, UUID staffUUID, String staffName) {
        return CompletableFuture.supplyAsync(() -> {
            Ticket ticket = getTicket(ticketId).join();
            if (ticket != null) {
                if (staffUUID == null) {
                    ticket.setAssignedTo(null, null);
                    ticket.setStatus(TicketStatus.ABERTO.name());
                } else {
                    ticket.setAssignedTo(staffUUID, staffName);
                    ticket.setStatus(TicketStatus.EM_ANDAMENTO.name());
                }
                
                boolean saved = databaseManager.saveTicket(ticket).join();
                
                if (saved) {
                    ticketCache.put(ticketId, ticket);
                    
                    if (staffUUID != null) {
                        plugin.getDiscordNotifier().notifyTicketAssigned(ticket, staffName);
                    }
                    
                    return true;
                }
            }
            return false;
        });
    }

    public CompletableFuture<Boolean> closeTicket(UUID ticketId, String reason) {
        return CompletableFuture.supplyAsync(() -> {
            Ticket ticket = getTicket(ticketId).join();
            if (ticket != null) {
                ticket.close(reason);
                
                boolean saved = databaseManager.saveTicket(ticket).join();
                
                if (saved) {
                    ticketCache.put(ticketId, ticket);
                    
                    plugin.getDiscordNotifier().notifyTicketClosed(ticket, reason);
                    
                    return true;
                }
            }
            return false;
        });
    }

    public CompletableFuture<Boolean> addMessage(UUID ticketId, UUID senderUUID, String senderName, String message, boolean isStaff) {
        return CompletableFuture.supplyAsync(() -> {
            TicketMessage ticketMessage = new TicketMessage(ticketId, senderUUID, senderName, message, isStaff);
            
            boolean saved = databaseManager.saveMessage(ticketMessage).join();
            
            if (saved) {
                if (ticketCache.containsKey(ticketId)) {
                    ticketCache.get(ticketId).addMessage(ticketMessage);
                }
                return true;
            }
            
            return false;
        });
    }


    private boolean isOnCooldown(UUID playerUUID) {
        if (!playerCooldowns.containsKey(playerUUID)) {
            return false;
        }

        long lastTicket = playerCooldowns.get(playerUUID);
        long cooldownTime = configManager.getCooldownSeconds() * 1000L;
        return (System.currentTimeMillis() - lastTicket) < cooldownTime;
    }

    private void setCooldown(UUID playerUUID) {
        long now = System.currentTimeMillis();
        playerCooldowns.put(playerUUID, now);
        
        databaseManager.setCooldown(playerUUID, now);
    }

    private void loadCooldowns() {
        Map<UUID, Long> cooldowns = databaseManager.getAllCooldowns();
        playerCooldowns.putAll(cooldowns);
    }

    private int getPlayerOpenTicketCount(UUID playerUUID) {
        List<Ticket> playerTickets = getPlayerTickets(playerUUID).join();
        
        return (int) playerTickets.stream()
                .filter(Ticket::isOpen)
                .count();
    }

    private void notifyStaffNewTicket(Ticket ticket) {
        if (!configManager.isNewTicketAlert()) {
            return;
        }

        String message = configManager.getPrefix() + 
                        "§eNovo ticket criado por §b" + ticket.getPlayerName() + 
                        "§e! ID: §a#" + ticket.getShortId() + 
                        "§e | Categoria: §f" + ticket.getCategory();

        Bukkit.getOnlinePlayers().stream()
                .filter(player -> player.hasPermission("devticket.manage"))
                .forEach(player -> player.sendMessage(message));
    }

    public long getCooldownRemaining(UUID playerUUID) {
        if (!isOnCooldown(playerUUID)) {
            return 0;
        }

        long lastTicket = playerCooldowns.get(playerUUID);
        long cooldownTime = configManager.getCooldownSeconds() * 1000L;
        long elapsed = System.currentTimeMillis() - lastTicket;
        return (cooldownTime - elapsed) / 1000;
    }

    public void clearCache() {
        ticketCache.clear();
    }
}