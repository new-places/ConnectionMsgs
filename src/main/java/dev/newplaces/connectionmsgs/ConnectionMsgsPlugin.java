package dev.newplaces.connectionmsgs;

import java.util.HashSet;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class ConnectionMsgsPlugin extends JavaPlugin implements Listener {
    private HashSet<UUID> enabledPlayers = new HashSet<>();
    private FileConfiguration config;

    @Override
    public void onEnable() {
        // Generate default config
        saveDefaultConfig();
        config = getConfig();
        setupConfigDefaults();
        
        // Load enabled players from previous session
        for (Player player : Bukkit.getServer().getOnlinePlayers()) {
            enabledPlayers.add(player.getUniqueId());
        }
        
        Bukkit.getServer().getPluginManager().registerEvents(this, this);
        
        // Register command executors
        getCommand("toggleconnectionmsgs").setExecutor(this);
        getCommand("connectionmsgs").setExecutor(this);
    }

    private void setupConfigDefaults() {
        config.addDefault("messages.toggle-off", "&aСообщения подключения были скрыты.");
        config.addDefault("messages.toggle-on", "&aСообщения подключения снова видны.");
        config.addDefault("messages.join", "&7%player% присоединился к серверу");
        config.addDefault("messages.quit", "&7%player% отключился от сервера");
        config.addDefault("messages.console-error", "&cЭта команда может быть использована только игроком в игре.");
        
        // Admin command messages
        config.addDefault("messages.admin-help", "&eConnectionMsgs команды:\n&b/connectionmsgs reload &7- перезагрузить конфигурацию\n&b/connectionmsgs version &7- показать версию плагина");
        config.addDefault("messages.config-reloaded", "&aКонфигурация ConnectionMsgs успешно перезагружена!");
        config.addDefault("messages.version-info", "&eConnectionMsgs версия: &b%version%\n&eАвтор: &b%author%");
        config.addDefault("messages.unknown-subcommand", "&cНеизвестная подкоманда. Используйте /connectionmsgs без параметров для справки.");
        config.addDefault("messages.console-only", "&cЭта команда доступна только в консоли сервера.");
        
        config.options().copyDefaults(true);
        saveConfig();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Handle toggleconnectionmsgs command
        if (command.getName().equalsIgnoreCase("toggleconnectionmsgs")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(getFormattedMessage("messages.console-error"));
                return true;
            }

            Player player = (Player) sender;
            UUID playerId = player.getUniqueId();

            if (enabledPlayers.contains(playerId)) {
                enabledPlayers.remove(playerId);
                player.sendMessage(getFormattedMessage("messages.toggle-off"));
            } else {
                enabledPlayers.add(playerId);
                player.sendMessage(getFormattedMessage("messages.toggle-on"));
            }
            return true;
        }
        
        // Handle connectionmsgs command (console only)
        if (command.getName().equalsIgnoreCase("connectionmsgs")) {
            // Check if command is executed from console
            if (sender instanceof Player) {
                sender.sendMessage(getFormattedMessage("messages.console-only"));
                return true;
            }
            
            if (args.length == 0) {
                sender.sendMessage(getFormattedMessage("messages.admin-help"));
                return true;
            }
            
            if (args[0].equalsIgnoreCase("reload")) {
                // Reload configuration
                reloadConfig();
                config = getConfig();
                setupConfigDefaults(); // Ensure defaults are set
                
                sender.sendMessage(getFormattedMessage("messages.config-reloaded"));
                return true;
            }
            
            if (args[0].equalsIgnoreCase("version")) {
                String versionInfo = getFormattedMessage("messages.version-info", 
                    "%version%", getDescription().getVersion(),
                    "%author%", String.join(", ", getDescription().getAuthors()));
                sender.sendMessage(versionInfo);
                return true;
            }
            
            sender.sendMessage(getFormattedMessage("messages.unknown-subcommand"));
            return true;
        }
        
        return false;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        enabledPlayers.add(player.getUniqueId());
        event.setJoinMessage(null);

        String joinMessage = getFormattedMessage("messages.join", "%player%", player.getName());
        
        for (Player onlinePlayer : Bukkit.getServer().getOnlinePlayers()) {
            if (enabledPlayers.contains(onlinePlayer.getUniqueId())) {
                onlinePlayer.sendMessage(joinMessage);
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        
        if (enabledPlayers.contains(playerId)) {
            enabledPlayers.remove(playerId);
        }
        event.setQuitMessage(null);

        String quitMessage = getFormattedMessage("messages.quit", "%player%", player.getName());
        
        for (Player onlinePlayer : Bukkit.getServer().getOnlinePlayers()) {
            if (enabledPlayers.contains(onlinePlayer.getUniqueId())) {
                onlinePlayer.sendMessage(quitMessage);
            }
        }
    }

    private String getFormattedMessage(String path, String... replacements) {
        String message = config.getString(path, "");
        
        // Process replacements in pairs (search, replace)
        for (int i = 0; i < replacements.length - 1; i += 2) {
            String placeholder = replacements[i];
            String replacement = replacements[i + 1];
            message = message.replace(placeholder, replacement);
        }
        
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}