package me.devplugins.api.events;

import me.devplugins.model.Ticket;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class TicketCloseEvent extends Event {
    
    private static final HandlerList handlers = new HandlerList();
    
    private final Ticket ticket;
    private final String reason;
    
    public TicketCloseEvent(Ticket ticket, String reason) {
        this.ticket = ticket;
        this.reason = reason;
    }
    
    public Ticket getTicket() {
        return ticket;
    }
    
    public String getReason() {
        return reason;
    }
    
    @Override
    public HandlerList getHandlers() {
        return handlers;
    }
    
    public static HandlerList getHandlerList() {
        return handlers;
    }
}