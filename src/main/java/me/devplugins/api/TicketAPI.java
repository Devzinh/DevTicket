package me.devplugins.api;

import me.devplugins.DevTicket;
import me.devplugins.manager.TicketManager;
import me.devplugins.model.Ticket;
import me.devplugins.model.TicketStatus;
import me.devplugins.api.events.TicketCreateEvent;
import me.devplugins.api.events.TicketCloseEvent;
import me.devplugins.api.events.TicketAssignEvent;
import me.devplugins.api.events.TicketCommentEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;


public class TicketAPI {
    
    private final DevTicket plugin;
    private final TicketManager ticketManager;
    
    public TicketAPI(DevTicket plugin, TicketManager ticketManager) {
        this.plugin = plugin;
        this.ticketManager = ticketManager;
    }
    

    public CompletableFuture<Ticket> createTicket(Player player, String title, String content, String category, String priority) {
        return ticketManager.createTicket(player, title, content, category, priority)
                .thenApply(ticket -> {
                    if (ticket != null) {
                        TicketCreateEvent event = new TicketCreateEvent(ticket, player);
                        Bukkit.getPluginManager().callEvent(event);
                    }
                    return ticket;
                });
    }
    

    public CompletableFuture<Ticket> getTicket(UUID ticketId) {
        return ticketManager.getTicket(ticketId);
    }
    

    public CompletableFuture<Ticket> getTicketByShortId(String shortId) {
        return ticketManager.getAllTickets().thenApply(tickets -> 
            tickets.stream()
                    .filter(ticket -> ticket.getShortId().equals(shortId))
                    .findFirst()
                    .orElse(null)
        );
    }
    

    public CompletableFuture<List<Ticket>> getPlayerTickets(UUID playerUUID) {
        return ticketManager.getPlayerTickets(playerUUID);
    }
    
    public CompletableFuture<List<Ticket>> getAllTickets() {
        return ticketManager.getAllTickets();
    }
    
    public CompletableFuture<List<Ticket>> getTicketsByStatus(TicketStatus status) {
        return ticketManager.getTicketsByStatus(status.name());
    }
    
    public CompletableFuture<List<Ticket>> getTicketsByCategory(String category) {
        return ticketManager.getAllTickets().thenApply(tickets ->
            tickets.stream()
                    .filter(ticket -> ticket.getCategory().equals(category))
                    .collect(java.util.stream.Collectors.toList())
        );
    }
    
    public CompletableFuture<Boolean> updateTicketStatus(UUID ticketId, TicketStatus status) {
        return ticketManager.updateTicketStatus(ticketId, status.name());
    }
    
    public CompletableFuture<Boolean> assignTicket(UUID ticketId, UUID staffUUID, String staffName) {
        return ticketManager.assignTicket(ticketId, staffUUID, staffName)
                .thenApply(success -> {
                    if (success) {
                        ticketManager.getTicket(ticketId).thenAccept(ticket -> {
                            if (ticket != null) {
                                Player staff = Bukkit.getPlayer(staffUUID);
                                TicketAssignEvent event = new TicketAssignEvent(ticket, staff);
                                Bukkit.getPluginManager().callEvent(event);
                            }
                        });
                    }
                    return success;
                });
    }
    
    public CompletableFuture<Boolean> closeTicket(UUID ticketId, String reason) {
        return ticketManager.closeTicket(ticketId, reason)
                .thenApply(success -> {
                    if (success) {
                        ticketManager.getTicket(ticketId).thenAccept(ticket -> {
                            if (ticket != null) {
                                TicketCloseEvent event = new TicketCloseEvent(ticket, reason);
                                Bukkit.getPluginManager().callEvent(event);
                            }
                        });
                    }
                    return success;
                });
    }
    
    public CompletableFuture<Boolean> addMessage(UUID ticketId, UUID senderUUID, String senderName, String message, boolean isStaff) {
        return ticketManager.addMessage(ticketId, senderUUID, senderName, message, isStaff)
                .thenApply(success -> {
                    if (success) {
                        ticketManager.getTicket(ticketId).thenAccept(ticket -> {
                            if (ticket != null) {
                                Player sender = Bukkit.getPlayer(senderUUID);
                                TicketCommentEvent event = new TicketCommentEvent(ticket, sender, message, isStaff);
                                Bukkit.getPluginManager().callEvent(event);
                            }
                        });
                    }
                    return success;
                });
    }
    
    public boolean isPlayerOnCooldown(UUID playerUUID) {
        return ticketManager.getCooldownRemaining(playerUUID) > 0;
    }
    
    public long getCooldownRemaining(UUID playerUUID) {
        return ticketManager.getCooldownRemaining(playerUUID);
    }
    
    public CompletableFuture<TicketStats> getTicketStats() {
        return ticketManager.getAllTickets().thenApply(allTickets -> {
            long total = allTickets.size();
            long open = allTickets.stream().filter(Ticket::isOpen).count();
            long closed = allTickets.stream().filter(Ticket::isClosed).count();
            
            return new TicketStats(total, open, closed);
        });
    }
    
    public CompletableFuture<TicketStats> getPlayerStats(UUID playerUUID) {
        return ticketManager.getPlayerTickets(playerUUID).thenApply(tickets -> {
            long total = tickets.size();
            long open = tickets.stream().filter(Ticket::isOpen).count();
            long closed = tickets.stream().filter(Ticket::isClosed).count();
            
            return new TicketStats(total, open, closed);
        });
    }
    public static class TicketStats {
        private final long total;
        private final long open;
        private final long closed;
        
        public TicketStats(long total, long open, long closed) {
            this.total = total;
            this.open = open;
            this.closed = closed;
        }
        
        public long getTotal() { return total; }
        public long getOpen() { return open; }
        public long getClosed() { return closed; }
    }
}