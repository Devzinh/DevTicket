package me.devplugins.api.events;

import me.devplugins.model.Ticket;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class TicketAssignEvent extends Event {
    
    private static final HandlerList handlers = new HandlerList();
    
    private final Ticket ticket;
    private final Player staff;
    
    public TicketAssignEvent(Ticket ticket, Player staff) {
        this.ticket = ticket;
        this.staff = staff;
    }
    
    public Ticket getTicket() {
        return ticket;
    }
    
    public Player getStaff() {
        return staff;
    }
    
    @Override
    public HandlerList getHandlers() {
        return handlers;
    }
    
    public static HandlerList getHandlerList() {
        return handlers;
    }
}