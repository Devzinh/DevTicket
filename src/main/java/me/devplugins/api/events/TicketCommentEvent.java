package me.devplugins.api.events;

import me.devplugins.model.Ticket;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class TicketCommentEvent extends Event {
    
    private static final HandlerList handlers = new HandlerList();
    
    private final Ticket ticket;
    private final Player sender;
    private final String message;
    private final boolean isStaff;
    
    public TicketCommentEvent(Ticket ticket, Player sender, String message, boolean isStaff) {
        this.ticket = ticket;
        this.sender = sender;
        this.message = message;
        this.isStaff = isStaff;
    }
    
    public Ticket getTicket() {
        return ticket;
    }
    
    public Player getSender() {
        return sender;
    }
    
    public String getMessage() {
        return message;
    }
    
    public boolean isStaff() {
        return isStaff;
    }
    
    @Override
    public HandlerList getHandlers() {
        return handlers;
    }
    
    public static HandlerList getHandlerList() {
        return handlers;
    }
}