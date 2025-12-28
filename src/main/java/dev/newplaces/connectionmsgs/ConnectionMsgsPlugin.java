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
        
        // Register command executor
        getCommand("toggleconnectionmsgs").setExecutor(this);
    }

    private void setupConfigDefaults() {
        config.addDefault("messages.toggle-off", "&aСообщения подключения были скрыты.");
        config.addDefault("messages.toggle-on", "&aСообщения подключения снова видны.");
        config.addDefault("messages.join", "&7%player% &fприсоединился к серверу");
        config.addDefault("messages.quit", "&7%player% &fотключился от сервера");
        config.addDefault("messages.console-error", "&cЭта команда может быть использована только игроком в игре.");
        
        config.options().copyDefaults(true);
        saveConfig();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("toggleconnectionmsgs")) {
            return false;
        }
        
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                config.getString("messages.console-error", "&cЭта команда может быть использована только игроком в игре.")));
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
            message = message.replace(replacements[i], replacements[i + 1]);
        }
        
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}