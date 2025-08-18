package me.devplugins.model;

import org.bukkit.ChatColor;
import java.util.UUID;
import java.util.ArrayList;
import java.util.List;

public class Ticket {

    private final UUID id;
    private final UUID playerUUID;
    private final String playerName;
    private final String title;
    private final String content;
    private final String category;
    private final String priority;
    private final long createdAt;
    private String status;
    private UUID assignedTo;
    private String assignedName;
    private long updatedAt;
    private long closedAt;
    private String closeReason;
    private List<TicketMessage> messages;

    public Ticket(UUID id, UUID playerUUID, String playerName, String title, String content, String category, String priority) {
        this.id = id;
        this.playerUUID = playerUUID;
        this.playerName = playerName;
        this.title = title;
        this.content = content;
        this.category = category;
        this.priority = priority;
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = this.createdAt;
        this.status = TicketStatus.ABERTO.name();
        this.messages = new ArrayList<>();
    }

    public Ticket(UUID id, UUID playerUUID, String playerName, String title, String content, 
                  String category, String priority, String status, UUID assignedTo, String assignedName,
                  long createdAt, long updatedAt, long closedAt, String closeReason) {
        this.id = id;
        this.playerUUID = playerUUID;
        this.playerName = playerName;
        this.title = title;
        this.content = content;
        this.category = category;
        this.priority = priority;
        this.status = status;
        this.assignedTo = assignedTo;
        this.assignedName = assignedName;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.closedAt = closedAt;
        this.closeReason = closeReason;
        this.messages = new ArrayList<>();
    }

    public UUID getId() { return id; }
    public UUID getPlayerUUID() { return playerUUID; }
    public String getPlayerName() { return playerName; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public String getCategory() { return category; }
    public String getPriority() { return priority; }
    public String getStatus() { return status; }
    public UUID getAssignedTo() { return assignedTo; }
    public String getAssignedName() { return assignedName; }
    public long getCreatedAt() { return createdAt; }
    public long getUpdatedAt() { return updatedAt; }
    public long getClosedAt() { return closedAt; }
    public String getCloseReason() { return closeReason; }
    public List<TicketMessage> getMessages() { return messages; }

    public void setStatus(String status) {
        this.status = status;
        this.updatedAt = System.currentTimeMillis();
    }

    public void setAssignedTo(UUID assignedTo, String assignedName) {
        this.assignedTo = assignedTo;
        this.assignedName = assignedName;
        this.updatedAt = System.currentTimeMillis();
    }

    public void close(String reason) {
        this.status = TicketStatus.FECHADO.name();
        this.closedAt = System.currentTimeMillis();
        this.updatedAt = this.closedAt;
        this.closeReason = reason;
    }

    public void addMessage(TicketMessage message) {
        this.messages.add(message);
        this.updatedAt = System.currentTimeMillis();
    }

    public void setMessages(List<TicketMessage> messages) {
        this.messages = messages;
    }

    public String getShortId() {
        return id.toString().substring(0, 8);
    }

    public boolean isOpen() {
        return status.equals(TicketStatus.ABERTO.name()) || status.equals(TicketStatus.EM_ANDAMENTO.name());
    }

    public boolean isClosed() {
        return status.equals(TicketStatus.FECHADO.name());
    }

    public TicketPriority getPriorityEnum() {
        try {
            return TicketPriority.valueOf(priority.toUpperCase().replace(" ", "_"));
        } catch (IllegalArgumentException e) {
            return TicketPriority.MEDIA;
        }
    }

    public TicketStatus getStatusEnum() {
        try {
            return TicketStatus.valueOf(status.toUpperCase().replace(" ", "_"));
        } catch (IllegalArgumentException e) {
            return TicketStatus.ABERTO;
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(ChatColor.AQUA).append("--- Ticket #").append(getShortId()).append(" ---\n");
        sb.append(ChatColor.YELLOW).append("Título: ").append(ChatColor.WHITE).append(title).append("\n");
        sb.append(ChatColor.YELLOW).append("Autor: ").append(ChatColor.WHITE).append(playerName).append("\n");
        sb.append(ChatColor.YELLOW).append("Categoria: ").append(ChatColor.WHITE).append(category).append("\n");
        sb.append(ChatColor.YELLOW).append("Prioridade: ").append(getPriorityColor()).append(priority).append("\n");
        sb.append(ChatColor.YELLOW).append("Status: ").append(getStatusColor()).append(status).append("\n");
        
        if (assignedName != null) {
            sb.append(ChatColor.YELLOW).append("Atribuído a: ").append(ChatColor.WHITE).append(assignedName).append("\n");
        }
        
        sb.append(ChatColor.YELLOW).append("Conteúdo: ").append(ChatColor.WHITE).append(content);
        
        return sb.toString();
    }

    private ChatColor getStatusColor() {
        switch (getStatusEnum()) {
            case ABERTO: return ChatColor.RED;
            case EM_ANDAMENTO: return ChatColor.YELLOW;
            case FECHADO: return ChatColor.GREEN;
            default: return ChatColor.WHITE;
        }
    }

    private ChatColor getPriorityColor() {
        switch (getPriorityEnum()) {
            case BAIXA: return ChatColor.GREEN;
            case MEDIA: return ChatColor.YELLOW;
            case ALTA: return ChatColor.GOLD;
            case URGENTE: return ChatColor.RED;
            default: return ChatColor.WHITE;
        }
    }
}