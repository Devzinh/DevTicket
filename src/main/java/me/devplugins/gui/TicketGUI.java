package me.devplugins.gui;

import me.devplugins.DevTicket;
import me.devplugins.config.ConfigManager;
import me.devplugins.manager.TicketManager;
import me.devplugins.model.Ticket;
import me.devplugins.model.TicketMessage;
import me.devplugins.model.TicketStatus;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class TicketGUI {

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy HH:mm");

    public static Inventory createMainMenu(Player player) {
        Inventory gui = Bukkit.createInventory(null, 45, "§9§lDevTicket - Menu Principal");

        ItemStack createTicket = createItem(
                Material.EMERALD,
                "§a§lCriar Novo Ticket",
                "§7Precisa de ajuda? Reporte um problema?",
                "§7Clique aqui para abrir um ticket!",
                "",
                "§7Categorias disponíveis:",
                "§8• §fSuporte Técnico",
                "§8• §fDenúncia de Jogador", 
                "§8• §fSugestões",
                "§8• §fReporte de Bugs",
                "",
                "§a§lClique para começar!"
        );

        ItemStack myTickets = createItem(
                Material.WRITTEN_BOOK,
                "§e§lMeus Tickets",
                "§7Visualize e acompanhe seus tickets:",
                "§7• Status atual de cada ticket",
                "§7• Respostas da equipe",
                "§7• Histórico completo",
                "",
                "§e§lClique para visualizar!"
        );

        ItemStack stats = createItem(
                Material.ITEM_FRAME,
                "§b§lMinhas Estatísticas",
                "§7Acompanhe seus números:",
                "§7• Total de tickets criados",
                "§7• Tickets resolvidos",
                "§7• Tempo médio de resposta",
                "§7• Avaliação do atendimento",
                "",
                "§b§lClique para ver dados!"
        );

        ItemStack help = createItem(
                Material.ENCHANTED_BOOK,
                "§d§lAjuda & Informações",
                "§7Como usar o sistema de tickets:",
                "",
                "§e§lDicas importantes:",
                "§7• Seja específico no título",
                "§7• Descreva o problema detalhadamente",
                "§7• Escolha a categoria correta",
                "§7• Aguarde resposta da equipe",
                "",
                "§d§lClique para mais informações!"
        );

        if (player.hasPermission("devticket.manage")) {
            ItemStack staffPanel = createItem(
                    Material.COMMAND_BLOCK,
                    "§c§lPainel de Staff",
                    "§7Área administrativa do sistema:",
                    "§7• Gerenciar todos os tickets",
                    "§7• Atribuir responsáveis",
                    "§7• Ver estatísticas globais",
                    "§7• Ferramentas de moderação",
                    "",
                    "§c§lClique para acessar!"
            );
            gui.setItem(31, staffPanel);
        }

        gui.setItem(11, createTicket);
        gui.setItem(13, myTickets);
        gui.setItem(15, stats);
        gui.setItem(29, help);

        ItemStack close = createItem(
                Material.BARRIER,
                "§c§lFechar Menu",
                "§7Sair do sistema de tickets",
                "",
                "§cClique para fechar!"
        );
        gui.setItem(44, close);

        fillEmptySlots(gui, Material.CYAN_STAINED_GLASS_PANE, " ");

        return gui;
    }

    public static Inventory createCategoryMenu(Player player) {
        ConfigManager config = DevTicket.getInstance().getConfigManager();
        List<String> categories = config.getCategories();
        
        Inventory gui = Bukkit.createInventory(null, 45, "§9§lSelecionar Categoria");

        int slot = 11;
        
        for (String category : categories) {
            Material material;
            String[] lore;
            
            switch (category.toLowerCase()) {
                case "suporte técnico":
                    material = Material.REDSTONE_TORCH;
                    lore = new String[]{
                        "§7Problemas técnicos do servidor:",
                        "§7• Lag ou travamentos",
                        "§7• Problemas de conexão", 
                        "§7• Erros de comandos",
                        "§7• Questões de performance",
                        "",
                        "§a§lClique para selecionar!"
                    };
                    break;
                    
                case "denúncia":
                    material = Material.DIAMOND_SWORD;
                    lore = new String[]{
                        "§7Reportar jogadores infratores:",
                        "§7• Trapaça ou hacks",
                        "§7• Comportamento tóxico",
                        "§7• Griefing ou vandalismo",
                        "§7• Spam ou flood",
                        "",
                        "§c§lClique para reportar!"
                    };
                    break;
                    
                case "sugestão":
                    material = Material.WRITABLE_BOOK;
                    lore = new String[]{
                        "§7Compartilhe suas ideias:",
                        "§7• Novos recursos",
                        "§7• Melhorias no servidor",
                        "§7• Eventos ou atividades",
                        "§7• Mudanças no gameplay",
                        "",
                        "§e§lClique para sugerir!"
                    };
                    break;
                    
                case "bug report":
                    material = Material.SPIDER_EYE;
                    lore = new String[]{
                        "§7Reportar bugs e falhas:",
                        "§7• Itens duplicados",
                        "§7• Comandos quebrados",
                        "§7• Problemas de plugins",
                        "§7• Glitches no mapa",
                        "",
                        "§6§lClique para reportar!"
                    };
                    break;
                    
                case "outros":
                default:
                    material = Material.PAPER;
                    lore = new String[]{
                        "§7Outras questões:",
                        "§7• Dúvidas gerais",
                        "§7• Informações sobre o servidor",
                        "§7• Problemas não listados",
                        "§7• Questões administrativas",
                        "",
                        "§b§lClique para selecionar!"
                    };
                    break;
            }
            
            ItemStack item = createItem(material, "§f§l" + category, lore);
            gui.setItem(slot, item);
            slot += 2;
        }

        ItemStack close = createItem(
                Material.BARRIER,
                "§c§lFechar Menu",
                "§7Cancelar criação de ticket",
                "",
                "§cClique para fechar!"
        );
        gui.setItem(40, close);

        fillEmptySlots(gui, Material.LIGHT_BLUE_STAINED_GLASS_PANE, " ");

        return gui;
    }

    public static Inventory createPriorityMenu(Player player, String category) {
        Inventory gui = Bukkit.createInventory(null, 45, "§9§lDefinir Prioridade - " + category);

        ItemStack categoryInfo = createItem(
                Material.KNOWLEDGE_BOOK,
                "§b§lCategoria Selecionada",
                "§f" + category,
                "",
                "§7Agora escolha a prioridade do seu ticket:",
                "§7Isso ajuda nossa equipe a organizar",
                "§7o atendimento de forma eficiente."
        );
        gui.setItem(4, categoryInfo);

        ItemStack baixa = createItem(
                Material.LIME_DYE,
                "§a§lPrioridade Baixa",
                "§7Tempo de resposta: §f24-48 horas",
                "",
                "§7Ideal para:",
                "§7• Dúvidas gerais",
                "§7• Sugestões não urgentes",
                "§7• Problemas menores",
                "",
                "§a§lClique para selecionar!"
        );

        ItemStack media = createItem(
                Material.YELLOW_DYE,
                "§e§lPrioridade Média",
                "§7Tempo de resposta: §f12-24 horas",
                "",
                "§7Ideal para:",
                "§7• Problemas que afetam gameplay",
                "§7• Denúncias moderadas",
                "§7• Bugs não críticos",
                "",
                "§e§lClique para selecionar!"
        );

        ItemStack alta = createItem(
                Material.ORANGE_DYE,
                "§6§lPrioridade Alta",
                "§7Tempo de resposta: §f4-12 horas",
                "",
                "§7Ideal para:",
                "§7• Problemas sérios de gameplay",
                "§7• Denúncias graves",
                "§7• Bugs que afetam muitos jogadores",
                "",
                "§6§lClique para selecionar!"
        );

        ItemStack urgente = createItem(
                Material.RED_DYE,
                "§c§lPrioridade Urgente",
                "§7Tempo de resposta: §f1-4 horas",
                "",
                "§7Ideal para:",
                "§7• Problemas críticos do servidor",
                "§7• Trapaças graves",
                "§7• Bugs que quebram o jogo",
                "",
                "§c§lClique para selecionar!"
        );

        gui.setItem(19, baixa);
        gui.setItem(21, media);
        gui.setItem(23, alta);
        gui.setItem(25, urgente);

        ItemStack back = createItem(
                Material.ARROW,
                "§c§lVoltar",
                "§7Voltar à seleção de categoria",
                "",
                "§cClique para voltar!"
        );
        gui.setItem(36, back);

        ItemStack cancel = createItem(
                Material.BARRIER,
                "§c§lCancelar",
                "§7Cancelar criação do ticket",
                "",
                "§cClique para cancelar!"
        );
        gui.setItem(44, cancel);

        fillEmptySlots(gui, Material.PURPLE_STAINED_GLASS_PANE, " ");

        return gui;
    }

    public static Inventory createPlayerTicketsMenu(Player player, List<Ticket> tickets) {
        int size = Math.max(45, ((tickets.size() + 8) / 9) * 9);
        if (size > 54) size = 54;
        
        String title = tickets.isEmpty() ? "§9§lMeus Tickets - Nenhum ticket" : "§9§lMeus Tickets (" + tickets.size() + ")";
        Inventory gui = Bukkit.createInventory(null, size, title);

        if (tickets.isEmpty()) {
            ItemStack noTickets = createItem(
                    Material.PAPER,
                    "§7§lNenhum Ticket Encontrado",
                    "§7Você ainda não criou nenhum ticket.",
                    "",
                    "§7Para criar seu primeiro ticket:",
                    "§7• Clique em 'Voltar'",
                    "§7• Selecione 'Criar Novo Ticket'",
                    "§7• Escolha a categoria apropriada",
                    "",
                    "§aNossa equipe está pronta para ajudar!"
            );
            gui.setItem(22, noTickets);
        } else {
            for (int i = 0; i < Math.min(tickets.size(), size - 9); i++) {
                Ticket ticket = tickets.get(i);
                
                Material material = getTicketStatusMaterial(ticket.getStatus());
                
                List<String> lore = new ArrayList<>();
                lore.add("§8§m" + "─".repeat(25));
                lore.add("§7ID: §f#" + ticket.getShortId());
                lore.add("§7Categoria: §f" + ticket.getCategory());
                lore.add("§7Prioridade: " + getPriorityColor(ticket.getPriority()) + "●§f " + ticket.getPriority());
                lore.add("§7Status: " + getStatusColor(ticket.getStatus()) + "●§f " + ticket.getStatus());
                lore.add("§7Criado: §f" + DATE_FORMAT.format(new Date(ticket.getCreatedAt())));
                
                if (ticket.getAssignedName() != null) {
                    lore.add("§7Responsável: §f" + ticket.getAssignedName());
                }
                
                if (!ticket.getMessages().isEmpty()) {
                    lore.add("§7Mensagens: §b" + ticket.getMessages().size() + " nova(s)");
                }
                
                lore.add("§8§m" + "─".repeat(25));
                lore.add("§e§l▶ Clique para ver detalhes!");

                ItemStack item = createItem(material, "§f§l" + ticket.getTitle(), lore.toArray(new String[0]));
                gui.setItem(i, item);
            }

            if (tickets.size() > size - 9) {
                ItemStack moreTickets = createItem(
                        Material.SPECTRAL_ARROW,
                        "§e§lMais Tickets Disponíveis",
                        "§7Você tem §f" + tickets.size() + " §7tickets no total.",
                        "§7Mostrando apenas os primeiros §f" + (size - 9) + "§7.",
                        "",
                        "§7Use §f/ticket list §7no chat para",
                        "§7ver todos os seus tickets.",
                        "",
                        "§e§lClique para mais informações!"
                );
                gui.setItem(size - 5, moreTickets);
            }
        }

        ItemStack createNew = createItem(
                Material.EMERALD,
                "§a§lCriar Novo Ticket",
                "§7Precisa de mais ajuda?",
                "§7Clique para criar outro ticket!",
                "",
                "§a§lClique para criar!"
        );
        gui.setItem(size - 9, createNew);

        ItemStack back = createItem(
                Material.ARROW,
                "§c§lVoltar ao Menu",
                "§7Retornar ao menu principal",
                "",
                "§c§lClique para voltar!"
        );
        gui.setItem(size - 1, back);

        fillEmptySlots(gui, Material.LIGHT_GRAY_STAINED_GLASS_PANE, " ");

        return gui;
    }

    public static Inventory createTicketDetailsMenu(Player player, Ticket ticket) {
        Inventory gui = Bukkit.createInventory(null, 54, "§9§lTicket #" + ticket.getShortId());

        List<String> infoLore = new ArrayList<>();
        infoLore.add("§7ID: §f#" + ticket.getShortId());
        infoLore.add("§7Título: §f" + ticket.getTitle());
        infoLore.add("§7Categoria: §f" + ticket.getCategory());
        infoLore.add("§7Prioridade: " + getPriorityColor(ticket.getPriority()) + ticket.getPriority());
        infoLore.add("§7Status: " + getStatusColor(ticket.getStatus()) + ticket.getStatus());
        infoLore.add("§7Criado em: §f" + DATE_FORMAT.format(new Date(ticket.getCreatedAt())));
        infoLore.add("");
        infoLore.add("§7Conteúdo:");
        
        String content = ticket.getContent();
        if (content.length() > 40) {
            String[] words = content.split(" ");
            StringBuilder line = new StringBuilder();
            for (String word : words) {
                if (line.length() + word.length() > 40) {
                    infoLore.add("§f" + line.toString());
                    line = new StringBuilder(word + " ");
                } else {
                    line.append(word).append(" ");
                }
            }
            if (line.length() > 0) {
                infoLore.add("§f" + line.toString());
            }
        } else {
            infoLore.add("§f" + content);
        }
        
        ItemStack info = createItem(Material.PAPER, "§b§lInformações do Ticket", infoLore.toArray(new String[0]));
        gui.setItem(4, info);

        ItemStack author = createPlayerHead(
                ticket.getPlayerName(),
                "§a§lAutor do Ticket",
                "§7Jogador: §f" + ticket.getPlayerName(),
                "§7UUID: §f" + ticket.getPlayerUUID().toString().substring(0, 8) + "..."
        );
        gui.setItem(20, author);

        if (ticket.getAssignedName() != null) {
            ItemStack assignedStaff = createPlayerHead(
                    ticket.getAssignedName(),
                    "§c§lStaff Atribuído",
                    "§7Staff: §f" + ticket.getAssignedName()
            );
            gui.setItem(24, assignedStaff);
        }

        List<TicketMessage> messages = ticket.getMessages();
        if (!messages.isEmpty()) {
            int startIndex = Math.max(0, messages.size() - 3);
            int slot = 10;
            
            for (int i = startIndex; i < messages.size() && slot <= 16; i++) {
                TicketMessage message = messages.get(i);
                
                List<String> messageLore = new ArrayList<>();
                messageLore.add("§7De: " + (message.isStaff() ? "§c[Staff] " : "§a[Player] ") + "§f" + message.getSenderName());
                messageLore.add("§7Data: §f" + DATE_FORMAT.format(new Date(message.getTimestamp())));
                messageLore.add("");
                messageLore.add("§7Mensagem:");
                
                String msg = message.getMessage();
                if (msg.length() > 35) {
                    String[] words = msg.split(" ");
                    StringBuilder line = new StringBuilder();
                    for (String word : words) {
                        if (line.length() + word.length() > 35) {
                            messageLore.add("§f" + line.toString());
                            line = new StringBuilder(word + " ");
                        } else {
                            line.append(word).append(" ");
                        }
                    }
                    if (line.length() > 0) {
                        messageLore.add("§f" + line.toString());
                    }
                } else {
                    messageLore.add("§f" + msg);
                }
                
                Material material = message.isStaff() ? Material.RED_WOOL : Material.GREEN_WOOL;
                String title = message.isStaff() ? "§c§lMensagem do Staff" : "§a§lMensagem do Jogador";
                
                ItemStack messageItem = createItem(material, title, messageLore.toArray(new String[0]));
                gui.setItem(slot, messageItem);
                slot++;
            }
            
            if (messages.size() > 3) {
                ItemStack moreMessages = createItem(
                        Material.BOOK,
                        "§e§lMais Mensagens",
                        "§7Total de mensagens: §f" + messages.size(),
                        "§7Mostrando as últimas 3 mensagens",
                        "",
                        "§7Use §f/ticket view " + ticket.getShortId(),
                        "§7para ver todas as mensagens no chat"
                );
                gui.setItem(19, moreMessages);
            }
        } else {
            ItemStack noMessages = createItem(
                    Material.GRAY_WOOL,
                    "§7§lNenhuma Mensagem",
                    "§7Este ticket ainda não possui",
                    "§7mensagens adicionais.",
                    "",
                    "§7Use o botão abaixo para",
                    "§7adicionar um comentário!"
            );
            gui.setItem(13, noMessages);
        }

        if (ticket.isOpen()) {
            ItemStack comment = createItem(
                    Material.WRITABLE_BOOK,
                    "§e§lAdicionar Comentário",
                    "§7Adicione uma mensagem",
                    "§7ao ticket.",
                    "",
                    "§eClique para comentar!"
            );
            gui.setItem(40, comment);
        }
        
        if (!ticket.getMessages().isEmpty()) {
            ItemStack viewMessages = createItem(
                    Material.BOOK,
                    "§b§lVer Todas as Mensagens",
                    "§7Total de mensagens: §f" + ticket.getMessages().size(),
                    "§7Clique para ver o histórico",
                    "§7completo de mensagens.",
                    "",
                    "§bClique para ver!"
            );
            gui.setItem(37, viewMessages);
        }

        if (player.hasPermission("devticket.manage")) {
            if (ticket.isOpen()) {
                ItemStack close = createItem(
                        Material.BARRIER,
                        "§c§lFechar Ticket",
                        "§7Fechar este ticket",
                        "§7⚠ Requer ticket atribuído",
                        "",
                        "§cClique para fechar!"
                );
                gui.setItem(42, close);

                if (ticket.getAssignedTo() == null) {
                    ItemStack assign = createItem(
                            Material.NAME_TAG,
                            "§b§lAtribuir a Mim",
                            "§7Atribuir este ticket",
                            "§7para você.",
                            "",
                            "§bClique para atribuir!"
                    );
                    gui.setItem(38, assign);
                } else {
                    if (ticket.getAssignedTo().equals(player.getUniqueId())) {
                        ItemStack unassign = createItem(
                                Material.BARRIER,
                                "§6§lDesatribuir Ticket",
                                "§7Remover sua atribuição",
                                "§7deste ticket.",
                                "",
                                "§6Clique para desatribuir!"
                        );
                        gui.setItem(38, unassign);
                    } else {
                        ItemStack assignedInfo = createItem(
                                Material.PLAYER_HEAD,
                                "§e§lTicket Atribuído",
                                "§7Responsável: §f" + ticket.getAssignedName(),
                                "§7Apenas o responsável pode",
                                "§7desatribuir este ticket."
                        );
                        gui.setItem(38, assignedInfo);
                    }
                }
            } else if (ticket.isClosed()) {
                ItemStack reopen = createItem(
                        Material.LIME_DYE,
                        "§a§lReabrir Ticket",
                        "§7Reabrir este ticket",
                        "§7⚠ Requer ticket atribuído",
                        "",
                        "§aClique para reabrir!"
                );
                gui.setItem(42, reopen);
            }
        }

        ItemStack back = createItem(
                Material.ARROW,
                "§c§lVoltar",
                "§7Voltar à lista de tickets"
        );
        gui.setItem(49, back);

        fillEmptySlots(gui, Material.GRAY_STAINED_GLASS_PANE, " ");

        return gui;
    }

    public static Inventory createTicketMessagesMenu(Player player, Ticket ticket) {
        List<TicketMessage> messages = ticket.getMessages();
        int size = Math.max(27, ((messages.size() + 8) / 9) * 9);
        if (size > 54) size = 54;
        
        Inventory gui = Bukkit.createInventory(null, size, "§e§lMensagens - #" + ticket.getShortId());
        
        for (int i = 0; i < Math.min(messages.size(), size - 9); i++) {
            TicketMessage message = messages.get(i);
            
            List<String> lore = new ArrayList<>();
            lore.add("§7De: " + (message.isStaff() ? "§c[Staff] " : "§a[Player] ") + "§f" + message.getSenderName());
            lore.add("§7Data: §f" + DATE_FORMAT.format(new Date(message.getTimestamp())));
            lore.add("");
            lore.add("§7Mensagem:");
            
            String msg = message.getMessage();
            if (msg.length() > 40) {
                String[] words = msg.split(" ");
                StringBuilder line = new StringBuilder();
                for (String word : words) {
                    if (line.length() + word.length() > 40) {
                        lore.add("§f" + line.toString());
                        line = new StringBuilder(word + " ");
                    } else {
                        line.append(word).append(" ");
                    }
                }
                if (line.length() > 0) {
                    lore.add("§f" + line.toString());
                }
            } else {
                lore.add("§f" + msg);
            }
            
            Material material = message.isStaff() ? Material.RED_WOOL : Material.GREEN_WOOL;
            String title = "§f" + message.getSenderName();
            
            ItemStack messageItem = createItem(material, title, lore.toArray(new String[0]));
            gui.setItem(i, messageItem);
        }
        
        if (ticket.isOpen()) {
            ItemStack addMessage = createItem(
                    Material.WRITABLE_BOOK,
                    "§a§lAdicionar Mensagem",
                    "§7Clique para adicionar uma",
                    "§7nova mensagem ao ticket.",
                    "",
                    "§aClique para comentar!"
            );
            gui.setItem(size - 5, addMessage);
        }
        
        ItemStack back = createItem(
                Material.ARROW,
                "§c§lVoltar",
                "§7Voltar aos detalhes do ticket"
        );
        gui.setItem(size - 1, back);
        
        return gui;
    }

    public static Inventory createStaffMenu(Player player) {
        Inventory gui = Bukkit.createInventory(null, 45, "§c§lPainel de Staff");

        ItemStack openTickets = createItem(
                Material.RED_BANNER,
                "§c§lTickets Abertos",
                "§7Ver todos os tickets",
                "§7que estão abertos.",
                "",
                "§eClique para ver!"
        );

        ItemStack inProgressTickets = createItem(
                Material.YELLOW_BANNER,
                "§e§lTickets em Andamento",
                "§7Ver tickets que estão",
                "§7sendo processados.",
                "",
                "§eClique para ver!"
        );

        ItemStack myAssigned = createItem(
                Material.BLUE_BANNER,
                "§b§lMeus Tickets",
                "§7Ver tickets atribuídos",
                "§7a você.",
                "",
                "§eClique para ver!"
        );

        ItemStack stats = createItem(
                Material.BOOK,
                "§d§lEstatísticas",
                "§7Ver estatísticas gerais",
                "§7do sistema de tickets.",
                "",
                "§eClique para ver!"
        );

        if (player.hasPermission("devticket.admin")) {
            ItemStack config = createItem(
                    Material.COMMAND_BLOCK,
                    "§4§lConfigurações",
                    "§7Gerenciar configurações",
                    "§7do plugin.",
                    "",
                    "§cApenas para administradores!"
            );
            gui.setItem(40, config);
        }

        gui.setItem(10, openTickets);
        gui.setItem(12, inProgressTickets);
        gui.setItem(14, myAssigned);
        gui.setItem(16, stats);

        ItemStack back = createItem(
                Material.ARROW,
                "§c§lVoltar",
                "§7Voltar ao menu principal"
        );
        gui.setItem(44, back);

        fillEmptySlots(gui, Material.GRAY_STAINED_GLASS_PANE, " ");

        return gui;
    }

    private static ItemStack createItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(name);
            if (lore.length > 0) {
                List<String> loreList = new ArrayList<>();
                for (String line : lore) {
                    loreList.add(line);
                }
                meta.setLore(loreList);
            }
            item.setItemMeta(meta);
        }

        return item;
    }

    private static ItemStack createPlayerHead(String playerName, String displayName, String... lore) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();

        if (meta != null) {
            meta.setOwner(playerName);
            meta.setDisplayName(displayName);
            if (lore.length > 0) {
                List<String> loreList = new ArrayList<>();
                for (String line : lore) {
                    loreList.add(line);
                }
                meta.setLore(loreList);
            }
            head.setItemMeta(meta);
        }

        return head;
    }

    private static void fillEmptySlots(Inventory gui, Material material, String name) {
        ItemStack filler = createItem(material, name);
        for (int i = 0; i < gui.getSize(); i++) {
            if (gui.getItem(i) == null) {
                gui.setItem(i, filler);
            }
        }
    }

    private static Material getTicketStatusMaterial(String status) {
        switch (status.toUpperCase()) {
            case "ABERTO": 
                return Material.RED_CONCRETE;
            case "EM_ANDAMENTO": 
                return Material.YELLOW_CONCRETE;
            case "FECHADO": 
                return Material.GREEN_CONCRETE;
            default: 
                return Material.GRAY_CONCRETE;
        }
    }
    
    private static Material getTicketMaterial(String status) {
        return getTicketStatusMaterial(status);
    }

    private static String getStatusColor(String status) {
        switch (status.toUpperCase()) {
            case "ABERTO": return "§c";
            case "EM_ANDAMENTO": return "§e";
            case "FECHADO": return "§a";
            default: return "§7";
        }
    }

    private static String getPriorityColor(String priority) {
        switch (priority.toLowerCase()) {
            case "baixa": return "§a";
            case "média": return "§e";
            case "alta": return "§6";
            case "urgente": return "§c";
            default: return "§7";
        }
    }
}