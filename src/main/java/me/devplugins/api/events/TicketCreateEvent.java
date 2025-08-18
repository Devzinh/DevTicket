package me.devplugins.api.events;

import me.devplugins.model.Ticket;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class TicketCreateEvent extends Event {
    
    private static final HandlerList handlers = new HandlerList();
    
    private final Ticket ticket;
    private final Player player;
    
    public TicketCreateEvent(Ticket ticket, Player player) {
        this.ticket = ticket;
        this.player = player;
    }
    
    public Ticket getTicket() {
        return ticket;
    }
    
    public Player getPlayer() {
        return player;
    }
    
    @Override
    public HandlerList getHandlers() {
        return handlers;
    }
    
    public static HandlerList getHandlerList() {
        return handlers;
    }
}