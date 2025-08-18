package me.devplugins;

import me.devplugins.commands.TicketCommand;
import me.devplugins.compatibility.VersionCompatibility;
import me.devplugins.config.ConfigManager;
import me.devplugins.database.DatabaseManager;
import me.devplugins.integrations.DiscordNotifier;
import me.devplugins.listeners.TicketListener;
import me.devplugins.manager.TicketManager;
import me.devplugins.api.TicketAPI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;

public final class DevTicket extends JavaPlugin {

    private static DevTicket instance;
    private ConfigManager configManager;
    private DatabaseManager databaseManager;
    private TicketManager ticketManager;
    private TicketAPI ticketAPI;
    private DiscordNotifier discordNotifier;

    public static DevTicket getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;

        getLogger().info("========================================");
        getLogger().info(ChatColor.AQUA + "        [DevTicket] - v" + getDescription().getVersion());
        getLogger().info(ChatColor.GREEN + "      Sistema de tickets avançado!");
        getLogger().info(ChatColor.YELLOW + "      Desenvolvido por: " + getDescription().getAuthors().get(0));
        getLogger().info("========================================");

        initializeComponents();

        TicketCommand ticketCommand = new TicketCommand(this, ticketManager, configManager);
        getCommand("ticket").setExecutor(ticketCommand);
        getCommand("ticket").setTabCompleter(ticketCommand);

        Bukkit.getPluginManager().registerEvents(new TicketListener(this, ticketManager, configManager), this);

        checkVersionCompatibility();

        startAutomaticTasks();

        getLogger().info("DevTicket iniciado com sucesso!");
        getLogger().info("Recursos disponíveis:");
        getLogger().info("- Sistema de armazenamento YAML nativo (100% independente!)");
        getLogger().info("- " + configManager.getCategories().size() + " categorias configuradas");
        getLogger().info("- Sistema de prioridades avançado");
        getLogger().info("- Interface gráfica aprimorada");
        getLogger().info("- API para desenvolvedores");
        getLogger().info("Digite /ticket para começar a usar!");
    }

    @Override
    public void onDisable() {
        getLogger().info("========================================");
        getLogger().info(ChatColor.RED + "      [DevTicket] - Desativando...");
        
        if (databaseManager != null) {
            databaseManager.close();
            getLogger().info("      Sistema de armazenamento YAML finalizado.");
        }
        
        if (ticketManager != null) {
            ticketManager.clearCache();
            getLogger().info("      Cache de tickets limpo.");
        }
        
        getLogger().info("      Plugin desativado com sucesso!");
        getLogger().info("      Obrigado por usar o DevTicket!");
        getLogger().info("========================================");
    }

    private void initializeComponents() {
        getLogger().info("Inicializando componentes...");
        
        this.configManager = new ConfigManager(this);
        getLogger().info("✓ Gerenciador de configuração carregado");
        
        this.databaseManager = new DatabaseManager(this);
        this.databaseManager.initialize();
        getLogger().info("✓ Sistema de armazenamento YAML inicializado");
        
        this.ticketManager = new TicketManager(this, databaseManager, configManager);
        getLogger().info("✓ Gerenciador de tickets carregado");
        
        this.ticketAPI = new TicketAPI(this, ticketManager);
        getLogger().info("✓ API do DevTicket disponível");
        
        this.discordNotifier = new DiscordNotifier(this, configManager);
        getLogger().info("✓ Sistema de notificações carregado");
    }

    private void checkVersionCompatibility() {
        getLogger().info("Verificando compatibilidade de versão...");
        getLogger().info(VersionCompatibility.getVersionInfo());
        
        if (!VersionCompatibility.isVersionSupported()) {
            getLogger().warning("========================================");
            getLogger().warning(ChatColor.RED + "⚠ AVISO DE COMPATIBILIDADE ⚠");
            getLogger().warning(ChatColor.RED + "O DevTicket suporta Minecraft 1.18.x até 1.21.x");
            getLogger().warning(ChatColor.RED + "Versão atual: " + Bukkit.getBukkitVersion());
            getLogger().warning(ChatColor.RED + "O plugin pode não funcionar corretamente!");
            getLogger().warning("========================================");
        } else {
            getLogger().info(ChatColor.GREEN + "✓ Versão compatível detectada!");
            
            if (VersionCompatibility.is1_21Plus()) {
                getLogger().info(ChatColor.AQUA + "✓ Minecraft 1.21+ - Todas as funcionalidades disponíveis!");
            } else if (VersionCompatibility.is1_20Plus()) {
                getLogger().info(ChatColor.AQUA + "✓ Minecraft 1.20+ - Funcionalidades completas!");
            } else if (VersionCompatibility.is1_19Plus()) {
                getLogger().info(ChatColor.AQUA + "✓ Minecraft 1.19+ - Totalmente compatível!");
            } else {
                getLogger().info(ChatColor.AQUA + "✓ Minecraft 1.18+ - Versão base suportada!");
            }
        }
    }

    private void startAutomaticTasks() {
        if (configManager.getAutoCloseDays() > 0) {
            Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
                ticketManager.getAllTickets().thenAccept(allTickets -> {
                    long cutoffTime = System.currentTimeMillis() - (configManager.getAutoCloseDays() * 24 * 60 * 60 * 1000L);
                    
                    allTickets.stream()
                            .filter(ticket -> ticket.isOpen() && ticket.getCreatedAt() < cutoffTime)
                            .forEach(ticket -> {
                                ticketManager.closeTicket(ticket.getId(), "Fechado automaticamente por inatividade");
                                getLogger().info("Ticket #" + ticket.getShortId() + " fechado automaticamente por inatividade");
                            });
                });
            }, 20L * 60 * 60, 20L * 60 * 60 * 6);
        }

        if (configManager.isOldTicketsAlert()) {
            Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
                ticketManager.getTicketsByStatus("ABERTO").thenAccept(openTickets -> {
                    long cutoffTime = System.currentTimeMillis() - (configManager.getOldTicketDays() * 24 * 60 * 60 * 1000L);
                    
                    long oldTicketsCount = openTickets.stream()
                            .filter(ticket -> ticket.getCreatedAt() < cutoffTime)
                            .count();
                    
                    if (oldTicketsCount > 0) {
                        Bukkit.getOnlinePlayers().stream()
                                .filter(player -> player.hasPermission("devticket.manage"))
                                .forEach(player -> {
                                    player.sendMessage(configManager.getPrefix() + 
                                            "§e⚠ Há " + oldTicketsCount + " ticket(s) antigo(s) aguardando resposta!");
                                });
                    }
                });
            }, 20L * 60 * 30, 20L * 60 * 60 * 2);
        }
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public TicketManager getTicketManager() {
        return ticketManager;
    }

    public TicketAPI getTicketAPI() {
        return ticketAPI;
    }
    
    public DiscordNotifier getDiscordNotifier() {
        return discordNotifier;
    }
}