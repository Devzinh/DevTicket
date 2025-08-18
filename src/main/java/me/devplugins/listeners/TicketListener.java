package me.devplugins.listeners;

import me.devplugins.DevTicket;
import me.devplugins.config.ConfigManager;
import me.devplugins.gui.TicketGUI;
import me.devplugins.manager.TicketManager;
import me.devplugins.model.Ticket;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerEditBookEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class TicketListener implements Listener {

    private final DevTicket plugin;
    private final TicketManager ticketManager;
    private final ConfigManager configManager;
    
    private final Map<UUID, String> selectedCategories = new HashMap<>();
    private final Map<UUID, String> selectedPriorities = new HashMap<>();
    private final Map<UUID, TicketCreationState> creationStates = new HashMap<>();
    
    private final Map<String, Long> actionCooldowns = new HashMap<>();
    private final Map<UUID, Long> playerActionCooldowns = new HashMap<>();
    private static final long ACTION_COOLDOWN_MS = 3000;
    private static final long PLAYER_ACTION_COOLDOWN_MS = 1000;

    public TicketListener(DevTicket plugin, TicketManager ticketManager, ConfigManager configManager) {
        this.plugin = plugin;
        this.ticketManager = ticketManager;
        this.configManager = configManager;
    }
    
    private boolean isActionOnCooldown(UUID playerUUID, String action, String ticketId) {
        String key = playerUUID + ":" + action + ":" + ticketId;
        long currentTime = System.currentTimeMillis();
        
        actionCooldowns.entrySet().removeIf(entry -> currentTime - entry.getValue() > ACTION_COOLDOWN_MS);
        
        if (actionCooldowns.containsKey(key)) {
            return currentTime - actionCooldowns.get(key) < ACTION_COOLDOWN_MS;
        }
        
        actionCooldowns.put(key, currentTime);
        return false;
    }
    
    private boolean isPlayerOnActionCooldown(UUID playerUUID) {
        long currentTime = System.currentTimeMillis();
        
        playerActionCooldowns.entrySet().removeIf(entry -> currentTime - entry.getValue() > PLAYER_ACTION_COOLDOWN_MS);
        
        if (playerActionCooldowns.containsKey(playerUUID)) {
            return currentTime - playerActionCooldowns.get(playerUUID) < PLAYER_ACTION_COOLDOWN_MS;
        }
        
        playerActionCooldowns.put(playerUUID, currentTime);
        return false;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        if (player.hasPermission("devticket.manage")) {
            ticketManager.getTicketsByStatus("ABERTO").thenAccept(openTickets -> {
                long oldTickets = openTickets.stream()
                        .filter(ticket -> {
                            long daysSinceCreated = (System.currentTimeMillis() - ticket.getCreatedAt()) / (1000 * 60 * 60 * 24);
                            return daysSinceCreated >= configManager.getOldTicketDays();
                        })
                        .count();
                
                if (oldTickets > 0) {
                    player.sendMessage(configManager.getPrefix() + "§e⚠ Há " + oldTickets + " ticket(s) antigo(s) aguardando resposta!");
                }
            });
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        
        Player player = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();
        ItemStack clickedItem = event.getCurrentItem();
        
        if (isDevTicketMenu(title)) {
            event.setCancelled(true);
            
            if (clickedItem == null || !clickedItem.hasItemMeta()) return;
            
            ItemMeta meta = clickedItem.getItemMeta();
            if (meta == null || !meta.hasDisplayName()) return;
            
            String displayName = meta.getDisplayName();
            
            handleMenuClick(player, title, displayName, clickedItem);
        }
    }
    
    private void handleMenuClick(Player player, String title, String displayName, ItemStack clickedItem) {
        if (title.equals("§9§lDevTicket - Menu Principal")) {
            handleMainMenuClick(player, displayName);
        }
        else if (title.equals("§9§lSelecionar Categoria")) {
            handleCategoryMenuClick(player, displayName);
        }
        else if (title.startsWith("§9§lDefinir Prioridade")) {
            handlePriorityMenuClick(player, displayName);
        }
        else if (title.startsWith("§9§lMeus Tickets")) {
            handlePlayerTicketsMenuClick(player, displayName, clickedItem);
        }
        else if (title.startsWith("§9§lTicket #")) {
            handleTicketDetailsMenuClick(player, displayName, title);
        }
        else if (title.equals("§c§lPainel de Staff")) {
            handleStaffMenuClick(player, displayName);
        }
        else if (title.startsWith("§e§lMensagens - #")) {
            handleMessagesMenuClick(player, displayName, title);
        }
    }

    private void handleMainMenuClick(Player player, String displayName) {
        switch (displayName) {
            case "§a§lCriar Novo Ticket":
                if (!player.hasPermission("devticket.create")) {
                    player.sendMessage(configManager.getMessage("no-permission"));
                    return;
                }
                
                long cooldownRemaining = ticketManager.getCooldownRemaining(player.getUniqueId());
                if (cooldownRemaining > 0) {
                    player.sendMessage(configManager.getMessage("cooldown-active", "time", String.valueOf(cooldownRemaining)));
                    return;
                }
                
                player.openInventory(TicketGUI.createCategoryMenu(player));
                break;
                
            case "§e§lMeus Tickets":
                if (!player.hasPermission("devticket.view.own")) {
                    player.sendMessage(configManager.getMessage("no-permission"));
                    return;
                }
                
                ticketManager.getPlayerTickets(player.getUniqueId()).thenAccept(tickets -> {
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        player.openInventory(TicketGUI.createPlayerTicketsMenu(player, tickets));
                    });
                });
                break;
                
            case "§b§lMinhas Estatísticas":
                showPlayerStats(player);
                break;
                
            case "§d§lAjuda & Informações":
                showHelpInfo(player);
                break;
                
            case "§c§lPainel de Staff":
                if (!player.hasPermission("devticket.manage")) {
                    player.sendMessage(configManager.getMessage("no-permission"));
                    return;
                }
                player.openInventory(TicketGUI.createStaffMenu(player));
                break;
                
            case "§c§lFechar Menu":
                player.closeInventory();
                player.sendMessage(configManager.getPrefix() + "§7Menu fechado.");
                break;
        }
    }

    private void handleCategoryMenuClick(Player player, String displayName) {
        if (displayName.equals("§c§lFechar Menu")) {
            player.closeInventory();
            player.sendMessage(configManager.getPrefix() + "§7Criação de ticket cancelada.");
            return;
        }
        
        String category = ChatColor.stripColor(displayName);
        if (configManager.getCategories().contains(category)) {
            selectedCategories.put(player.getUniqueId(), category);
            player.openInventory(TicketGUI.createPriorityMenu(player, category));
        }
    }

    private void handlePlayerTicketsMenuClick(Player player, String displayName, ItemStack clickedItem) {
        switch (displayName) {
            case "§c§lVoltar ao Menu":
            case "§c§lVoltar":
                player.openInventory(TicketGUI.createMainMenu(player));
                break;
                
            case "§a§lCriar Novo Ticket":
                if (!player.hasPermission("devticket.create")) {
                    player.sendMessage(configManager.getMessage("no-permission"));
                    return;
                }
                
                long cooldownRemaining = ticketManager.getCooldownRemaining(player.getUniqueId());
                if (cooldownRemaining > 0) {
                    player.sendMessage(configManager.getMessage("cooldown-active", "time", String.valueOf(cooldownRemaining)));
                    return;
                }
                
                player.openInventory(TicketGUI.createCategoryMenu(player));
                break;
                
            default:
                if (clickedItem.hasItemMeta() && clickedItem.getItemMeta().hasLore()) {
                    List<String> lore = clickedItem.getItemMeta().getLore();
                    for (String line : lore) {
                        if (line.contains("ID: §f#")) {
                            String ticketId = line.replace("§7ID: §f#", "").replace("§8§m─────────────────────────────", "");
                            
                            ticketManager.getAllTickets().thenAccept(allTickets -> {
                                Ticket ticket = allTickets.stream()
                                        .filter(t -> t.getShortId().equals(ticketId))
                                        .findFirst()
                                        .orElse(null);
                                
                                if (ticket != null) {
                                    Bukkit.getScheduler().runTask(plugin, () -> {
                                        player.openInventory(TicketGUI.createTicketDetailsMenu(player, ticket));
                                    });
                                }
                            });
                            break;
                        }
                    }
                }
                break;
        }
    }

    private void handlePriorityMenuClick(Player player, String displayName) {
        if (displayName.equals("§c§lVoltar")) {
            player.openInventory(TicketGUI.createCategoryMenu(player));
            return;
        }
        
        if (displayName.equals("§c§lCancelar")) {
            player.closeInventory();
            player.sendMessage(configManager.getPrefix() + "§7Criação de ticket cancelada.");
            selectedCategories.remove(player.getUniqueId());
            selectedPriorities.remove(player.getUniqueId());
            return;
        }
        
        String priority = null;
        switch (displayName) {
            case "§a§lPrioridade Baixa":
                priority = "Baixa";
                break;
            case "§e§lPrioridade Média":
                priority = "Média";
                break;
            case "§6§lPrioridade Alta":
                priority = "Alta";
                break;
            case "§c§lPrioridade Urgente":
                priority = "Urgente";
                break;
        }
        
        if (priority != null) {
            selectedPriorities.put(player.getUniqueId(), priority);
            
            creationStates.put(player.getUniqueId(), TicketCreationState.WAITING_TITLE);
            player.closeInventory();
            
            String category = selectedCategories.get(player.getUniqueId());
            player.sendMessage("");
            player.sendMessage(configManager.getPrefix() + "§a§l✓ Configuração do Ticket:");
            player.sendMessage(configManager.getPrefix() + "§7Categoria: §f" + category);
            player.sendMessage(configManager.getPrefix() + "§7Prioridade: §f" + priority);
            player.sendMessage("");
            player.sendMessage(configManager.getPrefix() + "§b§lPasso 1/2: Digite o título do seu ticket");
            player.sendMessage(configManager.getPrefix() + "§7Seja claro e específico (ex: 'Não consigo entrar no servidor')");
            player.sendMessage(configManager.getPrefix() + "§7Digite 'cancelar' para cancelar a criação");
        }
    }



    private void handleTicketDetailsMenuClick(Player player, String displayName, String title) {
        String ticketId = title.replace("§9§lTicket #", "");
        
        switch (displayName) {
            case "§c§lVoltar":
                if (player.hasPermission("devticket.manage")) {
                    player.openInventory(TicketGUI.createStaffMenu(player));
                } else {
                    ticketManager.getPlayerTickets(player.getUniqueId()).thenAccept(tickets -> {
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            player.openInventory(TicketGUI.createPlayerTicketsMenu(player, tickets));
                        });
                    });
                }
                break;
                
            case "§e§lAdicionar Comentário":
                if (isActionOnCooldown(player.getUniqueId(), "comment", ticketId)) {
                    player.sendMessage(configManager.getPrefix() + "§cAguarde um momento antes de adicionar outro comentário!");
                    return;
                }
                
                if (isPlayerOnActionCooldown(player.getUniqueId())) {
                    player.sendMessage(configManager.getPrefix() + "§cVocê está fazendo ações muito rapidamente!");
                    return;
                }
                
                ticketManager.getAllTickets().thenAccept(allTickets -> {
                    Ticket ticket = allTickets.stream()
                            .filter(t -> t.getShortId().equals(ticketId))
                            .findFirst()
                            .orElse(null);
                    
                    if (ticket == null) {
                        player.sendMessage(configManager.getMessage("ticket-not-found"));
                        return;
                    }
                    
                    if (ticket.isClosed()) {
                        player.sendMessage(configManager.getPrefix() + "§cNão é possível comentar em um ticket fechado!");
                        return;
                    }
                    
                    boolean isOwner = ticket.getPlayerUUID().equals(player.getUniqueId());
                    boolean isStaff = player.hasPermission("devticket.manage");
                    
                    if (!isOwner && !isStaff) {
                        player.sendMessage(configManager.getPrefix() + "§cVocê não pode comentar neste ticket!");
                        return;
                    }
                    
                    if (isStaff && !isOwner && ticket.getAssignedTo() == null) {
                        player.sendMessage(configManager.getPrefix() + "§cEste ticket precisa estar atribuído antes de receber comentários!");
                        player.sendMessage(configManager.getPrefix() + "§7Use 'Atribuir a Mim' primeiro.");
                        return;
                    }
                    
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        creationStates.put(player.getUniqueId(), TicketCreationState.WAITING_COMMENT);
                        selectedCategories.put(player.getUniqueId(), ticketId);
                        player.closeInventory();
                        player.sendMessage("");
                        player.sendMessage(configManager.getPrefix() + "§b💬 Adicionar Comentário ao Ticket #" + ticketId);
                        player.sendMessage(configManager.getPrefix() + "§7Digite sua mensagem no chat:");
                        player.sendMessage(configManager.getPrefix() + "§7(Digite 'cancelar' para cancelar)");
                        player.sendMessage("");
                    });
                });
                break;
                
            case "§c§lFechar Ticket":
                if (!player.hasPermission("devticket.manage")) {
                    player.sendMessage(configManager.getMessage("no-permission"));
                    return;
                }
                
                if (isActionOnCooldown(player.getUniqueId(), "close", ticketId)) {
                    player.sendMessage(configManager.getPrefix() + "§cAguarde um momento antes de tentar novamente!");
                    return;
                }
                
                if (isPlayerOnActionCooldown(player.getUniqueId())) {
                    player.sendMessage(configManager.getPrefix() + "§cVocê está fazendo ações muito rapidamente!");
                    return;
                }
                
                ticketManager.getAllTickets().thenAccept(allTickets -> {
                    Ticket ticket = allTickets.stream()
                            .filter(t -> t.getShortId().equals(ticketId))
                            .findFirst()
                            .orElse(null);
                    
                    if (ticket == null) {
                        player.sendMessage(configManager.getMessage("ticket-not-found"));
                        return;
                    }
                    
                    if (ticket.isClosed()) {
                        player.sendMessage(configManager.getPrefix() + "§cEste ticket já está fechado!");
                        return;
                    }
                    
                    if (ticket.getAssignedTo() == null) {
                        player.sendMessage(configManager.getPrefix() + "§cEste ticket precisa estar atribuído antes de ser fechado!");
                        player.sendMessage(configManager.getPrefix() + "§7Use 'Atribuir a Mim' primeiro.");
                        return;
                    }
                    
                    ticketManager.closeTicket(ticket.getId(), "Fechado por " + player.getName()).thenAccept(success -> {
                        if (success) {
                            player.sendMessage(configManager.getMessage("ticket-closed", "id", ticketId));
                            player.closeInventory();
                        } else {
                            player.sendMessage(configManager.getPrefix() + "§cErro ao fechar o ticket!");
                        }
                    });
                });
                break;
                
            case "§b§lAtribuir a Mim":
                if (!player.hasPermission("devticket.manage")) {
                    player.sendMessage(configManager.getMessage("no-permission"));
                    return;
                }
                
                if (isActionOnCooldown(player.getUniqueId(), "assign", ticketId)) {
                    player.sendMessage(configManager.getPrefix() + "§cAguarde um momento antes de tentar novamente!");
                    return;
                }
                
                if (isPlayerOnActionCooldown(player.getUniqueId())) {
                    player.sendMessage(configManager.getPrefix() + "§cVocê está fazendo ações muito rapidamente!");
                    return;
                }
                
                ticketManager.getAllTickets().thenAccept(allTickets -> {
                    Ticket ticket = allTickets.stream()
                            .filter(t -> t.getShortId().equals(ticketId))
                            .findFirst()
                            .orElse(null);
                    
                    if (ticket == null) {
                        player.sendMessage(configManager.getMessage("ticket-not-found"));
                        return;
                    }
                    
                    if (ticket.isClosed()) {
                        player.sendMessage(configManager.getPrefix() + "§cNão é possível atribuir um ticket fechado!");
                        return;
                    }
                    
                    if (ticket.getAssignedTo() != null) {
                        if (ticket.getAssignedTo().equals(player.getUniqueId())) {
                            player.sendMessage(configManager.getPrefix() + "§eEste ticket já está atribuído a você!");
                            return;
                        } else {
                            player.sendMessage(configManager.getPrefix() + "§cEste ticket já está atribuído a §f" + ticket.getAssignedName() + "§c!");
                            player.sendMessage(configManager.getPrefix() + "§7Apenas o responsável atual ou um admin pode reatribuir.");
                            return;
                        }
                    }
                    
                    ticketManager.assignTicket(ticket.getId(), player.getUniqueId(), player.getName()).thenAccept(success -> {
                        if (success) {
                            player.sendMessage(configManager.getPrefix() + "§a✅ Ticket #" + ticketId + " atribuído a você!");
                            player.sendMessage(configManager.getPrefix() + "§7Você agora é responsável por este ticket.");
                            player.closeInventory();
                            
                            Player ticketOwner = Bukkit.getPlayer(ticket.getPlayerUUID());
                            if (ticketOwner != null) {
                                ticketOwner.sendMessage(configManager.getPrefix() + "§e📋 Seu ticket #" + ticketId + " foi atribuído!");
                                ticketOwner.sendMessage(configManager.getPrefix() + "§7Responsável: §f" + player.getName());
                            }
                        } else {
                            player.sendMessage(configManager.getPrefix() + "§cErro ao atribuir o ticket!");
                        }
                    });
                });
                break;
                
            case "§a§lReabrir Ticket":
                if (!player.hasPermission("devticket.manage")) {
                    player.sendMessage(configManager.getMessage("no-permission"));
                    return;
                }
                
                if (isActionOnCooldown(player.getUniqueId(), "reopen", ticketId)) {
                    player.sendMessage(configManager.getPrefix() + "§cAguarde um momento antes de tentar novamente!");
                    return;
                }
                
                if (isPlayerOnActionCooldown(player.getUniqueId())) {
                    player.sendMessage(configManager.getPrefix() + "§cVocê está fazendo ações muito rapidamente!");
                    return;
                }
                
                ticketManager.getAllTickets().thenAccept(allTickets -> {
                    Ticket ticket = allTickets.stream()
                            .filter(t -> t.getShortId().equals(ticketId))
                            .findFirst()
                            .orElse(null);
                    
                    if (ticket == null) {
                        player.sendMessage(configManager.getMessage("ticket-not-found"));
                        return;
                    }
                    
                    if (!ticket.isClosed()) {
                        player.sendMessage(configManager.getPrefix() + "§cEste ticket já está aberto!");
                        return;
                    }
                    
                    if (ticket.getAssignedTo() == null) {
                        player.sendMessage(configManager.getPrefix() + "§cEste ticket precisa estar atribuído antes de ser reaberto!");
                        player.sendMessage(configManager.getPrefix() + "§7Atribua o ticket a você primeiro.");
                        return;
                    }
                    
                    if (!ticket.getAssignedTo().equals(player.getUniqueId()) && !player.hasPermission("devticket.admin")) {
                        player.sendMessage(configManager.getPrefix() + "§cApenas o responsável (§f" + ticket.getAssignedName() + "§c) pode reabrir este ticket!");
                        return;
                    }
                    
                    ticketManager.updateTicketStatus(ticket.getId(), "ABERTO").thenAccept(success -> {
                        if (success) {
                            player.sendMessage(configManager.getPrefix() + "§a✅ Ticket #" + ticketId + " foi reaberto!");
                            player.sendMessage(configManager.getPrefix() + "§7O ticket voltou ao status 'Aberto'.");
                            player.closeInventory();
                            
                            Player ticketOwner = Bukkit.getPlayer(ticket.getPlayerUUID());
                            if (ticketOwner != null) {
                                ticketOwner.sendMessage(configManager.getPrefix() + "§e🔄 Seu ticket #" + ticketId + " foi reaberto!");
                                ticketOwner.sendMessage(configManager.getPrefix() + "§7Por: §f" + player.getName());
                            }
                        } else {
                            player.sendMessage(configManager.getPrefix() + "§cErro ao reabrir o ticket!");
                        }
                    });
                });
                break;
                
            case "§b§lVer Todas as Mensagens":
                if (isActionOnCooldown(player.getUniqueId(), "view_messages", ticketId)) {
                    player.sendMessage(configManager.getPrefix() + "§cAguarde um momento antes de tentar novamente!");
                    return;
                }
                
                if (isPlayerOnActionCooldown(player.getUniqueId())) {
                    player.sendMessage(configManager.getPrefix() + "§cVocê está fazendo ações muito rapidamente!");
                    return;
                }
                
                ticketManager.getAllTickets().thenAccept(allTickets -> {
                    Ticket ticket = allTickets.stream()
                            .filter(t -> t.getShortId().equals(ticketId))
                            .findFirst()
                            .orElse(null);
                    
                    if (ticket == null) {
                        player.sendMessage(configManager.getMessage("ticket-not-found"));
                        return;
                    }
                    
                    boolean isOwner = ticket.getPlayerUUID().equals(player.getUniqueId());
                    boolean isStaff = player.hasPermission("devticket.manage");
                    
                    if (!isOwner && !isStaff) {
                        player.sendMessage(configManager.getPrefix() + "§cVocê não pode ver as mensagens deste ticket!");
                        return;
                    }
                    
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        player.openInventory(TicketGUI.createTicketMessagesMenu(player, ticket));
                        });
                });
                break;
                
            case "§6§lDesatribuir Ticket":
                if (!player.hasPermission("devticket.manage")) {
                    player.sendMessage(configManager.getMessage("no-permission"));
                    return;
                }
                
                if (isActionOnCooldown(player.getUniqueId(), "unassign", ticketId)) {
                    player.sendMessage(configManager.getPrefix() + "§cAguarde um momento antes de tentar novamente!");
                    return;
                }
                
                if (isPlayerOnActionCooldown(player.getUniqueId())) {
                    player.sendMessage(configManager.getPrefix() + "§cVocê está fazendo ações muito rapidamente!");
                    return;
                }
                
                ticketManager.getAllTickets().thenAccept(allTickets -> {
                    Ticket ticket = allTickets.stream()
                            .filter(t -> t.getShortId().equals(ticketId))
                            .findFirst()
                            .orElse(null);
                    
                    if (ticket == null) {
                        player.sendMessage(configManager.getMessage("ticket-not-found"));
                        return;
                    }
                    
                    if (ticket.getAssignedTo() == null) {
                        player.sendMessage(configManager.getPrefix() + "§cEste ticket não está atribuído a ninguém!");
                        return;
                    }
                    
                    if (!ticket.getAssignedTo().equals(player.getUniqueId()) && !player.hasPermission("devticket.admin")) {
                        player.sendMessage(configManager.getPrefix() + "§cApenas o responsável (§f" + ticket.getAssignedName() + "§c) pode desatribuir este ticket!");
                        return;
                    }
                    
                    ticketManager.assignTicket(ticket.getId(), null, null).thenAccept(success -> {
                        if (success) {
                            player.sendMessage(configManager.getPrefix() + "§6✅ Ticket #" + ticketId + " foi desatribuído!");
                            player.sendMessage(configManager.getPrefix() + "§7O ticket voltou para a fila geral.");
                            player.closeInventory();
                            
                            Bukkit.getOnlinePlayers().stream()
                                    .filter(p -> p.hasPermission("devticket.manage") && !p.equals(player))
                                    .forEach(staff -> {
                                        staff.sendMessage(configManager.getPrefix() + "§6📋 Ticket #" + ticketId + " foi desatribuído!");
                                        staff.sendMessage(configManager.getPrefix() + "§7Por: §f" + player.getName());
                                    });
                        } else {
                            player.sendMessage(configManager.getPrefix() + "§cErro ao desatribuir o ticket!");
                        }
                    });
                });
                break;
        }
    }

    private void handleMessagesMenuClick(Player player, String displayName, String title) {
        String ticketId = title.replace("§e§lMensagens - #", "");
        
        switch (displayName) {
            case "§c§lVoltar":
                ticketManager.getAllTickets().thenAccept(allTickets -> {
                    Ticket ticket = allTickets.stream()
                            .filter(t -> t.getShortId().equals(ticketId))
                            .findFirst()
                            .orElse(null);
                    
                    if (ticket != null) {
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            player.openInventory(TicketGUI.createTicketDetailsMenu(player, ticket));
                        });
                    }
                });
                break;
                
            case "§a§lAdicionar Mensagem":
                creationStates.put(player.getUniqueId(), TicketCreationState.WAITING_COMMENT);
                selectedCategories.put(player.getUniqueId(), ticketId);
                player.closeInventory();
                player.sendMessage(configManager.getPrefix() + "§bDigite seu comentário no chat:");
                player.sendMessage(configManager.getPrefix() + "§7(Digite 'cancelar' para cancelar)");
                break;
        }
    }



    private void handleStaffMenuClick(Player player, String displayName) {
        switch (displayName) {
            case "§c§lVoltar":
                player.openInventory(TicketGUI.createMainMenu(player));
                break;
                
            case "§c§lTickets Abertos":
                ticketManager.getTicketsByStatus("ABERTO").thenAccept(tickets -> {
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        player.openInventory(TicketGUI.createPlayerTicketsMenu(player, tickets));
                    });
                });
                break;
                
            case "§e§lTickets em Andamento":
                ticketManager.getTicketsByStatus("EM_ANDAMENTO").thenAccept(tickets -> {
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        player.openInventory(TicketGUI.createPlayerTicketsMenu(player, tickets));
                    });
                });
                break;
                
            case "§b§lMeus Tickets":
                ticketManager.getAllTickets().thenAccept(allTickets -> {
                    var myTickets = allTickets.stream()
                            .filter(ticket -> player.getUniqueId().equals(ticket.getAssignedTo()))
                            .collect(Collectors.toList());
                    
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        player.openInventory(TicketGUI.createPlayerTicketsMenu(player, myTickets));
                    });
                });
                break;
                
            case "§d§lEstatísticas":
                showGlobalStats(player);
                break;
        }
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        
        if (!creationStates.containsKey(playerId)) return;
        
        event.setCancelled(true);
        String message = event.getMessage();
        
        if (message.equalsIgnoreCase("cancelar")) {
            creationStates.remove(playerId);
            selectedCategories.remove(playerId);
            selectedPriorities.remove(playerId);
            player.sendMessage(configManager.getPrefix() + "§cOperação cancelada!");
            return;
        }
        
        TicketCreationState state = creationStates.get(playerId);
        
        switch (state) {
            case WAITING_TITLE:
                selectedCategories.put(playerId, selectedCategories.get(playerId) + "|" + message);
                creationStates.put(playerId, TicketCreationState.WAITING_DESCRIPTION);
                player.sendMessage("");
                player.sendMessage(configManager.getPrefix() + "§a§l✓ Título definido: §f" + message);
                player.sendMessage("");
                player.sendMessage(configManager.getPrefix() + "§b§lPasso 2/2: Digite a descrição do problema");
                player.sendMessage(configManager.getPrefix() + "§7Seja detalhado e inclua:");
                player.sendMessage(configManager.getPrefix() + "§7• O que aconteceu?");
                player.sendMessage(configManager.getPrefix() + "§7• Quando aconteceu?");
                player.sendMessage(configManager.getPrefix() + "§7• Como reproduzir o problema?");
                player.sendMessage(configManager.getPrefix() + "§7Digite 'cancelar' para cancelar a criação");
                break;
                
            case WAITING_DESCRIPTION:
                String[] categoryAndTitle = selectedCategories.get(playerId).split("\\|", 2);
                String category = categoryAndTitle[0];
                String title = categoryAndTitle[1];
                String priority = selectedPriorities.get(playerId);
                
                ticketManager.createTicket(player, title, message, category, priority).thenAccept(ticket -> {
                    if (ticket != null) {
                        player.sendMessage("");
                        player.sendMessage(configManager.getPrefix() + "§a§l✅ Ticket Criado com Sucesso!");
                        player.sendMessage(configManager.getPrefix() + "§7ID do Ticket: §f#" + ticket.getShortId());
                        player.sendMessage(configManager.getPrefix() + "§7Categoria: §f" + category);
                        player.sendMessage(configManager.getPrefix() + "§7Prioridade: §f" + priority);
                        player.sendMessage("");
                        player.sendMessage(configManager.getPrefix() + "§b§lPróximos Passos:");
                        player.sendMessage(configManager.getPrefix() + "§7• Nossa equipe foi notificada");
                        player.sendMessage(configManager.getPrefix() + "§7• Você receberá uma resposta em breve");
                        player.sendMessage(configManager.getPrefix() + "§7• Use §f/ticket view " + ticket.getShortId() + " §7para acompanhar");
                        player.sendMessage("");
                    } else {
                        player.sendMessage("");
                        player.sendMessage(configManager.getPrefix() + "§c§l❌ Não foi possível criar o ticket!");
                        player.sendMessage(configManager.getPrefix() + "§7Você já atingiu o limite de §f" + configManager.getMaxTicketsPerPlayer() + " §7tickets abertos.");
                        player.sendMessage(configManager.getPrefix() + "§7Aguarde a resolução dos tickets existentes ou");
                        player.sendMessage(configManager.getPrefix() + "§7use §f/ticket list §7para ver seus tickets atuais.");
                        player.sendMessage("");
                    }
                });
                
                creationStates.remove(playerId);
                selectedCategories.remove(playerId);
                selectedPriorities.remove(playerId);
                break;
                
            case WAITING_COMMENT:
                String ticketId = selectedCategories.get(playerId);
                
                ticketManager.getAllTickets().thenAccept(allTickets -> {
                    Ticket ticket = allTickets.stream()
                            .filter(t -> t.getShortId().equals(ticketId))
                            .findFirst()
                            .orElse(null);
                    
                    if (ticket != null) {
                        boolean isStaff = player.hasPermission("devticket.manage");
                        ticketManager.addMessage(ticket.getId(), player.getUniqueId(), player.getName(), message, isStaff).thenAccept(success -> {
                            if (success) {
                                player.sendMessage(configManager.getPrefix() + "§aComentário adicionado ao ticket #" + ticketId + "!");
                                player.sendMessage(configManager.getPrefix() + "§7Use §f/ticket view " + ticketId + " §7para ver todas as mensagens.");
                                
                                if (isStaff) {
                                    Player ticketOwner = Bukkit.getPlayer(ticket.getPlayerUUID());
                                    if (ticketOwner != null && !ticketOwner.equals(player)) {
                                        ticketOwner.sendMessage(configManager.getPrefix() + "§e📩 Nova mensagem no seu ticket #" + ticketId + "!");
                                        ticketOwner.sendMessage(configManager.getPrefix() + "§7De: §c[Staff] §f" + player.getName());
                                        ticketOwner.sendMessage(configManager.getPrefix() + "§7Use §f/ticket view " + ticketId + " §7para ver.");
                                    }
                                } else {
                                    if (ticket.getAssignedTo() != null) {
                                        Player assignedStaff = Bukkit.getPlayer(ticket.getAssignedTo());
                                        if (assignedStaff != null && !assignedStaff.equals(player)) {
                                            assignedStaff.sendMessage(configManager.getPrefix() + "§e📩 Nova mensagem no ticket #" + ticketId + "!");
                                            assignedStaff.sendMessage(configManager.getPrefix() + "§7De: §a[Player] §f" + player.getName());
                                            assignedStaff.sendMessage(configManager.getPrefix() + "§7Use §f/ticket view " + ticketId + " §7para ver.");
                                        }
                                    } else {
                                        Bukkit.getOnlinePlayers().stream()
                                                .filter(p -> p.hasPermission("devticket.manage") && !p.equals(player))
                                                .forEach(staff -> {
                                                    staff.sendMessage(configManager.getPrefix() + "§e📩 Nova mensagem no ticket #" + ticketId + "!");
                                                    staff.sendMessage(configManager.getPrefix() + "§7De: §a[Player] §f" + player.getName());
                                                });
                                    }
                                }
                            } else {
                                player.sendMessage(configManager.getPrefix() + "§cErro ao adicionar mensagem!");
                            }
                        });
                    }
                });
                
                creationStates.remove(playerId);
                selectedCategories.remove(playerId);
                break;
        }
    }

    @EventHandler
    public void onPlayerEditBook(PlayerEditBookEvent event) {
        if (event.isSigning()) {
            Player player = event.getPlayer();
            String ticketContent = event.getNewBookMeta().getPages().stream()
                    .map(page -> ChatColor.stripColor(page))
                    .collect(Collectors.joining("\n"));
            
            if (ticketContent.trim().isEmpty()) {
                player.sendMessage(configManager.getPrefix() + "§cVocê não pode enviar um ticket vazio!");
                return;
            }
            
            String ticketTitle = event.getNewBookMeta().getTitle();
            
            ticketManager.createTicket(player, ticketTitle, ticketContent, "Outros", "Média").thenAccept(ticket -> {
                if (ticket != null) {
                    player.sendMessage(configManager.getMessage("ticket-created", "id", ticket.getShortId()));
                } else {
                    player.sendMessage(configManager.getMessage("max-tickets-reached", "max", String.valueOf(configManager.getMaxTicketsPerPlayer())));
                }
            });
        }
    }

    private void showPlayerStats(Player player) {
        ticketManager.getPlayerTickets(player.getUniqueId()).thenAccept(tickets -> {
            long totalTickets = tickets.size();
            long openTickets = tickets.stream().filter(Ticket::isOpen).count();
            long closedTickets = tickets.stream().filter(Ticket::isClosed).count();
            
            player.sendMessage("§8§m" + "=".repeat(30));
            player.sendMessage("§b§lSuas Estatísticas");
            player.sendMessage("§7Total de tickets: §f" + totalTickets);
            player.sendMessage("§7Tickets abertos: §a" + openTickets);
            player.sendMessage("§7Tickets fechados: §c" + closedTickets);
            player.sendMessage("§8§m" + "=".repeat(30));
        });
    }

    private void showGlobalStats(Player player) {
        ticketManager.getAllTickets().thenAccept(allTickets -> {
            long totalTickets = allTickets.size();
            long openTickets = allTickets.stream().filter(Ticket::isOpen).count();
            long closedTickets = allTickets.stream().filter(Ticket::isClosed).count();
            
            player.sendMessage("§8§m" + "=".repeat(30));
            player.sendMessage("§c§lEstatísticas Globais");
            player.sendMessage("§7Total de tickets: §f" + totalTickets);
            player.sendMessage("§7Tickets abertos: §a" + openTickets);
            player.sendMessage("§7Tickets fechados: §c" + closedTickets);
            player.sendMessage("§8§m" + "=".repeat(30));
        });
    }

    private void showHelpInfo(Player player) {
        player.closeInventory();
        player.sendMessage("");
        player.sendMessage(configManager.getPrefix() + "§d§l📋 Como Usar o Sistema de Tickets");
        player.sendMessage("");
        player.sendMessage("§e§l1. Criando um Ticket:");
        player.sendMessage("§7• Escolha a categoria correta");
        player.sendMessage("§7• Defina a prioridade adequada");
        player.sendMessage("§7• Use um título claro e específico");
        player.sendMessage("§7• Descreva o problema detalhadamente");
        player.sendMessage("");
        player.sendMessage("§e§l2. Acompanhando seu Ticket:");
        player.sendMessage("§7• Use §f/ticket list §7para ver seus tickets");
        player.sendMessage("§7• Use §f/ticket view <id> §7para ver detalhes");
        player.sendMessage("§7• Adicione comentários com §f/ticket comment <id> <mensagem>");
        player.sendMessage("");
        player.sendMessage("§e§l3. Dicas Importantes:");
        player.sendMessage("§7• Seja paciente - nossa equipe responderá em breve");
        player.sendMessage("§7• Não crie tickets duplicados");
        player.sendMessage("§7• Use a categoria correta para agilizar o atendimento");
        player.sendMessage("");
        player.sendMessage("§b§lPrecisa de ajuda? Digite §f/ticket §b§lpara começar!");
    }

    private boolean isDevTicketMenu(String title) {
        return title.contains("DevTicket") || 
               title.contains("Ticket") || 
               title.contains("Categoria") || 
               title.contains("Prioridade") ||
               title.contains("Mensagens") ||
               title.contains("Staff");
    }
    


    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        
        String title = event.getView().getTitle();
        if (isDevTicketMenu(title)) {
            event.setCancelled(true);
        }
    }

    private enum TicketCreationState {
        WAITING_TITLE,
        WAITING_DESCRIPTION,
        WAITING_COMMENT
    }
}