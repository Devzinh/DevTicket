package me.devplugins.model;

import java.util.UUID;

public class TicketMessage {
    
    private final int id;
    private final UUID ticketId;
    private final UUID senderUUID;
    private final String senderName;
    private final String message;
    private final long timestamp;
    private final boolean isStaff;
    
    public TicketMessage(int id, UUID ticketId, UUID senderUUID, String senderName, 
                        String message, long timestamp, boolean isStaff) {
        this.id = id;
        this.ticketId = ticketId;
        this.senderUUID = senderUUID;
        this.senderName = senderName;
        this.message = message;
        this.timestamp = timestamp;
        this.isStaff = isStaff;
    }
    
    public TicketMessage(UUID ticketId, UUID senderUUID, String senderName, String message, boolean isStaff) {
        this.id = 0;
        this.ticketId = ticketId;
        this.senderUUID = senderUUID;
        this.senderName = senderName;
        this.message = message;
        this.timestamp = System.currentTimeMillis();
        this.isStaff = isStaff;
    }
    
    public int getId() { return id; }
    public UUID getTicketId() { return ticketId; }
    public UUID getSenderUUID() { return senderUUID; }
    public String getSenderName() { return senderName; }
    public String getMessage() { return message; }
    public long getTimestamp() { return timestamp; }
    public boolean isStaff() { return isStaff; }
}