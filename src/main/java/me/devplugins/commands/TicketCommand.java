package me.devplugins.commands;

import me.devplugins.DevTicket;
import me.devplugins.config.ConfigManager;
import me.devplugins.gui.TicketGUI;
import me.devplugins.manager.TicketManager;
import me.devplugins.model.Ticket;
import me.devplugins.model.TicketStatus;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class TicketCommand implements CommandExecutor, TabCompleter {

    private final DevTicket plugin;
    private final TicketManager ticketManager;
    private final ConfigManager configManager;

    public TicketCommand(DevTicket plugin, TicketManager ticketManager, ConfigManager configManager) {
        this.plugin = plugin;
        this.ticketManager = ticketManager;
        this.configManager = configManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(configManager.getPrefix() + "§cEste comando só pode ser usado por jogadores!");
                return true;
            }

            Player player = (Player) sender;
            if (!player.hasPermission("devticket.create")) {
                player.sendMessage(configManager.getMessage("no-permission"));
                return true;
            }

            player.openInventory(TicketGUI.createMainMenu(player));
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "create":
            case "criar":
                return handleCreateCommand(sender, args);

            case "list":
            case "listar":
                return handleListCommand(sender, args);

            case "view":
            case "ver":
                return handleViewCommand(sender, args);

            case "close":
            case "fechar":
                return handleCloseCommand(sender, args);

            case "assign":
            case "atribuir":
                return handleAssignCommand(sender, args);

            case "reopen":
            case "reabrir":
                return handleReopenCommand(sender, args);

            case "comment":
            case "comentar":
                return handleCommentCommand(sender, args);

            case "stats":
            case "estatisticas":
                return handleStatsCommand(sender, args);

            case "reload":
                return handleReloadCommand(sender, args);

            default:
                sendHelpMessage(sender);
                return true;
        }
    }

    private boolean handleCreateCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(configManager.getPrefix() + "§cEste comando só pode ser usado por jogadores!");
            return true;
        }

        Player player = (Player) sender;
        if (!player.hasPermission("devticket.create")) {
            player.sendMessage(configManager.getMessage("no-permission"));
            return true;
        }

        if (args.length < 3) {
            player.sendMessage(configManager.getPrefix() + "§cUso: /ticket create <categoria> <título> [descrição]");
            player.sendMessage(configManager.getPrefix() + "§eCategorias disponíveis: " + String.join(", ", configManager.getCategories()));
            return true;
        }

        String category = args[1];
        if (!configManager.getCategories().contains(category)) {
            player.sendMessage(configManager.getPrefix() + "§cCategoria inválida! Categorias disponíveis: " + String.join(", ", configManager.getCategories()));
            return true;
        }

        long cooldownRemaining = ticketManager.getCooldownRemaining(player.getUniqueId());
        if (cooldownRemaining > 0) {
            player.sendMessage(configManager.getMessage("cooldown-active", "time", String.valueOf(cooldownRemaining)));
            return true;
        }

        String title = args[2];
        String content = args.length > 3 ? String.join(" ", Arrays.copyOfRange(args, 3, args.length)) : "Sem descrição adicional";

        ticketManager.createTicket(player, title, content, category, "Média").thenAccept(ticket -> {
            if (ticket != null) {
                player.sendMessage(configManager.getMessage("ticket-created", "id", ticket.getShortId()));
            } else {
                player.sendMessage(configManager.getMessage("max-tickets-reached", "max", String.valueOf(configManager.getMaxTicketsPerPlayer())));
            }
        });

        return true;
    }

    private boolean handleListCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(configManager.getPrefix() + "§cEste comando só pode ser usado por jogadores!");
            return true;
        }

        Player player = (Player) sender;

        if (args.length > 1 && player.hasPermission("devticket.view.all")) {
            String targetName = args[1];
            Player target = Bukkit.getPlayer(targetName);
            if (target == null) {
                player.sendMessage(configManager.getPrefix() + "§cJogador não encontrado!");
                return true;
            }

            ticketManager.getPlayerTickets(target.getUniqueId()).thenAccept(tickets -> {
                if (tickets.isEmpty()) {
                    player.sendMessage(configManager.getPrefix() + "§e" + target.getName() + " não possui tickets.");
                    return;
                }

                player.sendMessage(configManager.getPrefix() + "§aTickets de " + target.getName() + ":");
                for (Ticket ticket : tickets) {
                    player.sendMessage("§7- §e#" + ticket.getShortId() + " §7| §f" + ticket.getTitle() + " §7| " + getStatusColor(ticket.getStatus()) + ticket.getStatus());
                }
            });
        } else {
            if (!player.hasPermission("devticket.view.own")) {
                player.sendMessage(configManager.getMessage("no-permission"));
                return true;
            }

            ticketManager.getPlayerTickets(player.getUniqueId()).thenAccept(tickets -> {
                if (tickets.isEmpty()) {
                    player.sendMessage(configManager.getPrefix() + "§eVocê não possui tickets.");
                    return;
                }

                player.sendMessage(configManager.getPrefix() + "§aSeus tickets:");
                for (Ticket ticket : tickets) {
                    player.sendMessage("§7- §e#" + ticket.getShortId() + " §7| §f" + ticket.getTitle() + " §7| " + getStatusColor(ticket.getStatus()) + ticket.getStatus());
                }
            });
        }

        return true;
    }

    private boolean handleViewCommand(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(configManager.getPrefix() + "§cUso: /ticket view <id>");
            return true;
        }

        String ticketIdStr = args[1].replace("#", "");

        ticketManager.getAllTickets().thenAccept(allTickets -> {
            Ticket ticket = allTickets.stream()
                    .filter(t -> t.getShortId().startsWith(ticketIdStr))
                    .findFirst()
                    .orElse(null);

            if (ticket == null) {
                sender.sendMessage(configManager.getMessage("ticket-not-found"));
                return;
            }

            if (sender instanceof Player) {
                Player player = (Player) sender;
                if (!player.hasPermission("devticket.view.all") &&
                        !ticket.getPlayerUUID().equals(player.getUniqueId())) {
                    sender.sendMessage(configManager.getMessage("no-permission"));
                    return;
                }
            }

            sender.sendMessage("§8§m" + "=".repeat(50));
            sender.sendMessage("§b§lTicket #" + ticket.getShortId());
            sender.sendMessage("§7Título: §f" + ticket.getTitle());
            sender.sendMessage("§7Autor: §f" + ticket.getPlayerName());
            sender.sendMessage("§7Categoria: §f" + ticket.getCategory());
            sender.sendMessage("§7Prioridade: §f" + ticket.getPriority());
            sender.sendMessage("§7Status: " + getStatusColor(ticket.getStatus()) + ticket.getStatus());

            if (ticket.getAssignedName() != null) {
                sender.sendMessage("§7Atribuído a: §f" + ticket.getAssignedName());
            }

            sender.sendMessage("§7Criado em: §f" + formatTimestamp(ticket.getCreatedAt()));
            sender.sendMessage("");
            sender.sendMessage("§7§lConteúdo Original:");
            sender.sendMessage("§f" + ticket.getContent());

            if (!ticket.getMessages().isEmpty()) {
                sender.sendMessage("");
                sender.sendMessage("§e§lHistórico de Mensagens:");
                sender.sendMessage("§8" + "─".repeat(30));

                for (me.devplugins.model.TicketMessage msg : ticket.getMessages()) {
                    String prefix = msg.isStaff() ? "§c[Staff]" : "§a[Player]";
                    String timestamp = formatTimestamp(msg.getTimestamp());

                    sender.sendMessage(prefix + " §f" + msg.getSenderName() + " §7(" + timestamp + "):");
                    sender.sendMessage("§f  " + msg.getMessage());
                    sender.sendMessage("");
                }
                sender.sendMessage("§8" + "─".repeat(30));
                sender.sendMessage("§7Total de mensagens: §f" + ticket.getMessages().size());
            } else {
                sender.sendMessage("");
                sender.sendMessage("§7§oNenhuma mensagem adicional neste ticket.");
            }

            sender.sendMessage("§8§m" + "=".repeat(50));
        });

        return true;
    }

    private boolean handleCloseCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("devticket.manage")) {
            sender.sendMessage(configManager.getMessage("no-permission"));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(configManager.getPrefix() + "§cUso: /ticket close <id> [motivo]");
            return true;
        }

        String ticketIdStr = args[1].replace("#", "");
        String reason = args.length > 2 ? String.join(" ", Arrays.copyOfRange(args, 2, args.length)) : "Fechado por staff";

        ticketManager.getAllTickets().thenAccept(allTickets -> {
            Ticket ticket = allTickets.stream()
                    .filter(t -> t.getShortId().startsWith(ticketIdStr))
                    .findFirst()
                    .orElse(null);

            if (ticket == null) {
                sender.sendMessage(configManager.getMessage("ticket-not-found"));
                return;
            }

            if (ticket.isClosed()) {
                sender.sendMessage(configManager.getPrefix() + "§cEste ticket já está fechado!");
                return;
            }

            ticketManager.closeTicket(ticket.getId(), reason).thenAccept(success -> {
                if (success) {
                    sender.sendMessage(configManager.getMessage("ticket-closed", "id", ticket.getShortId()));

                    Player player = Bukkit.getPlayer(ticket.getPlayerUUID());
                    if (player != null) {
                        player.sendMessage(configManager.getPrefix() + "§eSeu ticket #" + ticket.getShortId() + " foi fechado!");
                        player.sendMessage(configManager.getPrefix() + "§7Motivo: §f" + reason);
                    }
                } else {
                    sender.sendMessage(configManager.getPrefix() + "§cErro ao fechar o ticket!");
                }
            });
        });

        return true;
    }

    private boolean handleAssignCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("devticket.manage")) {
            sender.sendMessage(configManager.getMessage("no-permission"));
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage(configManager.getPrefix() + "§cUso: /ticket assign <id> <staff>");
            return true;
        }

        String ticketIdStr = args[1].replace("#", "");
        String staffName = args[2];

        Player staff = Bukkit.getPlayer(staffName);
        if (staff == null) {
            sender.sendMessage(configManager.getPrefix() + "§cStaff não encontrado!");
            return true;
        }

        ticketManager.getAllTickets().thenAccept(allTickets -> {
            Ticket ticket = allTickets.stream()
                    .filter(t -> t.getShortId().startsWith(ticketIdStr))
                    .findFirst()
                    .orElse(null);

            if (ticket == null) {
                sender.sendMessage(configManager.getMessage("ticket-not-found"));
                return;
            }

            ticketManager.assignTicket(ticket.getId(), staff.getUniqueId(), staff.getName()).thenAccept(success -> {
                if (success) {
                    sender.sendMessage(configManager.getMessage("ticket-assigned", "id", ticket.getShortId(), "staff", staff.getName()));

                    staff.sendMessage(configManager.getPrefix() + "§eVocê foi atribuído ao ticket #" + ticket.getShortId() + "!");

                    Player player = Bukkit.getPlayer(ticket.getPlayerUUID());
                    if (player != null) {
                        player.sendMessage(configManager.getPrefix() + "§eSeu ticket #" + ticket.getShortId() + " foi atribuído a " + staff.getName() + "!");
                    }
                } else {
                    sender.sendMessage(configManager.getPrefix() + "§cErro ao atribuir o ticket!");
                }
            });
        });

        return true;
    }

    private boolean handleReopenCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("devticket.manage")) {
            sender.sendMessage(configManager.getMessage("no-permission"));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(configManager.getPrefix() + "§cUso: /ticket reopen <id>");
            return true;
        }

        String ticketIdStr = args[1].replace("#", "");

        ticketManager.getAllTickets().thenAccept(allTickets -> {
            Ticket ticket = allTickets.stream()
                    .filter(t -> t.getShortId().startsWith(ticketIdStr))
                    .findFirst()
                    .orElse(null);

            if (ticket == null) {
                sender.sendMessage(configManager.getMessage("ticket-not-found"));
                return;
            }

            if (!ticket.isClosed()) {
                sender.sendMessage(configManager.getPrefix() + "§cEste ticket não está fechado!");
                return;
            }

            ticketManager.updateTicketStatus(ticket.getId(), TicketStatus.ABERTO.name()).thenAccept(success -> {
                if (success) {
                    sender.sendMessage(configManager.getPrefix() + "§aTicket #" + ticket.getShortId() + " foi reaberto!");

                    Player player = Bukkit.getPlayer(ticket.getPlayerUUID());
                    if (player != null) {
                        player.sendMessage(configManager.getPrefix() + "§eSeu ticket #" + ticket.getShortId() + " foi reaberto!");
                    }
                } else {
                    sender.sendMessage(configManager.getPrefix() + "§cErro ao reabrir o ticket!");
                }
            });
        });

        return true;
    }

    private boolean handleCommentCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(configManager.getPrefix() + "§cEste comando só pode ser usado por jogadores!");
            return true;
        }

        Player player = (Player) sender;

        if (args.length < 3) {
            player.sendMessage(configManager.getPrefix() + "§cUso: /ticket comment <id> <mensagem>");
            return true;
        }

        String ticketIdStr = args[1].replace("#", "");
        String message = String.join(" ", Arrays.copyOfRange(args, 2, args.length));

        ticketManager.getAllTickets().thenAccept(allTickets -> {
            Ticket ticket = allTickets.stream()
                    .filter(t -> t.getShortId().startsWith(ticketIdStr))
                    .findFirst()
                    .orElse(null);

            if (ticket == null) {
                player.sendMessage(configManager.getMessage("ticket-not-found"));
                return;
            }

            boolean isStaff = player.hasPermission("devticket.manage");
            if (!isStaff && !ticket.getPlayerUUID().equals(player.getUniqueId())) {
                player.sendMessage(configManager.getMessage("no-permission"));
                return;
            }

            ticketManager.addMessage(ticket.getId(), player.getUniqueId(), player.getName(), message, isStaff).thenAccept(success -> {
                if (success) {
                    player.sendMessage(configManager.getPrefix() + "§aMensagem adicionada ao ticket #" + ticket.getShortId() + "!");

                    if (isStaff) {
                        Player ticketOwner = Bukkit.getPlayer(ticket.getPlayerUUID());
                        if (ticketOwner != null) {
                            ticketOwner.sendMessage(configManager.getPrefix() + "§eNova mensagem no seu ticket #" + ticket.getShortId() + "!");
                        }
                    } else {
                        if (ticket.getAssignedTo() != null) {
                            Player assignedStaff = Bukkit.getPlayer(ticket.getAssignedTo());
                            if (assignedStaff != null) {
                                assignedStaff.sendMessage(configManager.getPrefix() + "§eNova mensagem no ticket #" + ticket.getShortId() + "!");
                            }
                        }
                    }
                } else {
                    player.sendMessage(configManager.getPrefix() + "§cErro ao adicionar mensagem!");
                }
            });
        });

        return true;
    }

    private boolean handleStatsCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("devticket.admin")) {
            sender.sendMessage(configManager.getMessage("no-permission"));
            return true;
        }

        ticketManager.getAllTickets().thenAccept(allTickets -> {
            long totalTickets = allTickets.size();
            long openTickets = allTickets.stream().filter(Ticket::isOpen).count();
            long closedTickets = allTickets.stream().filter(Ticket::isClosed).count();

            sender.sendMessage("§8§m" + "=".repeat(30));
            sender.sendMessage("§b§lEstatísticas do DevTicket");
            sender.sendMessage("§7Total de tickets: §f" + totalTickets);
            sender.sendMessage("§7Tickets abertos: §a" + openTickets);
            sender.sendMessage("§7Tickets fechados: §c" + closedTickets);
            sender.sendMessage("§8§m" + "=".repeat(30));
        });

        return true;
    }

    private boolean handleReloadCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("devticket.admin")) {
            sender.sendMessage(configManager.getMessage("no-permission"));
            return true;
        }

        configManager.reloadConfig();
        ticketManager.clearCache();
        sender.sendMessage(configManager.getPrefix() + "§aConfiguração recarregada com sucesso!");

        return true;
    }

    private boolean handleTestDiscordCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("devticket.admin")) {
            sender.sendMessage(configManager.getMessage("no-permission"));
            return true;
        }

        sender.sendMessage(configManager.getPrefix() + "§eTestando notificação Discord...");

        if (!configManager.isDiscordEnabled()) {
            sender.sendMessage(configManager.getPrefix() + "§cDiscord está desabilitado na configuração!");
            sender.sendMessage(configManager.getPrefix() + "§7Configure 'notifications.discord.enabled: true' no config.yml");
            return true;
        }

        String webhookUrl = configManager.getDiscordWebhook();
        if (webhookUrl == null || webhookUrl.trim().isEmpty()) {
            sender.sendMessage(configManager.getPrefix() + "§cURL do webhook não configurada!");
            sender.sendMessage(configManager.getPrefix() + "§7Configure 'notifications.discord.webhook-url' no config.yml");
            return true;
        }

        sender.sendMessage(configManager.getPrefix() + "§aConfiguração OK! Enviando teste...");

        me.devplugins.model.Ticket testTicket = new me.devplugins.model.Ticket(
                java.util.UUID.randomUUID(),
                sender instanceof org.bukkit.entity.Player ? ((org.bukkit.entity.Player) sender).getUniqueId() : java.util.UUID.randomUUID(),
                sender.getName(),
                "Teste de Notificação Discord",
                "Esta é uma mensagem de teste para verificar se as notificações Discord estão funcionando corretamente.",
                "Suporte Técnico",
                "Alta"
        );

        plugin.getDiscordNotifier().notifyNewTicket(testTicket);

        sender.sendMessage(configManager.getPrefix() + "§aTeste enviado! Verifique o canal do Discord.");
        sender.sendMessage(configManager.getPrefix() + "§7Se não recebeu a mensagem, verifique os logs do servidor.");

        return true;
    }

    private void sendHelpMessage(CommandSender sender) {
        sender.sendMessage("§8§m" + "=".repeat(50));
        sender.sendMessage("§b§lDevTicket - Comandos");
        sender.sendMessage("§e/ticket §7- Abrir menu principal");
        sender.sendMessage("§e/ticket create <categoria> <título> [descrição] §7- Criar ticket");
        sender.sendMessage("§e/ticket list [jogador] §7- Listar tickets");
        sender.sendMessage("§e/ticket view <id> §7- Ver detalhes do ticket");
        sender.sendMessage("§e/ticket comment <id> <mensagem> §7- Adicionar comentário");

        if (sender.hasPermission("devticket.manage")) {
            sender.sendMessage("§c§lComandos de Staff:");
            sender.sendMessage("§e/ticket assign <id> <staff> §7- Atribuir ticket");
            sender.sendMessage("§e/ticket close <id> [motivo] §7- Fechar ticket");
            sender.sendMessage("§e/ticket reopen <id> §7- Reabrir ticket");
        }

        if (sender.hasPermission("devticket.admin")) {
            sender.sendMessage("§4§lComandos de Admin:");
            sender.sendMessage("§e/ticket stats §7- Ver estatísticas");
            sender.sendMessage("§e/ticket reload §7- Recarregar configuração");
            sender.sendMessage("§e/ticket testdiscord §7- Testar notificações Discord");
        }

        sender.sendMessage("§8§m" + "=".repeat(50));
    }

    private String getStatusColor(String status) {
        switch (status.toUpperCase()) {
            case "ABERTO": return "§c";
            case "EM_ANDAMENTO": return "§e";
            case "FECHADO": return "§a";
            default: return "§7";
        }
    }

    private String formatTimestamp(long timestamp) {
        return new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm").format(new java.util.Date(timestamp));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            List<String> subCommands = Arrays.asList("create", "list", "view", "close", "assign", "reopen", "comment", "stats", "reload");
            return subCommands.stream()
                    .filter(cmd -> cmd.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2) {
            switch (args[0].toLowerCase()) {
                case "create":
                case "criar":
                    return configManager.getCategories().stream()
                            .filter(cat -> cat.toLowerCase().startsWith(args[1].toLowerCase()))
                            .collect(Collectors.toList());

                case "list":
                case "listar":
                    if (sender.hasPermission("devticket.view.all")) {
                        return Bukkit.getOnlinePlayers().stream()
                                .map(Player::getName)
                                .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                                .collect(Collectors.toList());
                    }
                    break;

                case "assign":
                case "atribuir":
                    if (sender.hasPermission("devticket.manage")) {
                        return Bukkit.getOnlinePlayers().stream()
                                .filter(p -> p.hasPermission("devticket.manage"))
                                .map(Player::getName)
                                .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                                .collect(Collectors.toList());
                    }
                    break;
            }
        }

        return completions;
    }
}