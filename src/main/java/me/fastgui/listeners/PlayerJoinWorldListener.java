package me.fastgui.listeners;

import me.fastgui.FastGUI;
import me.fastgui.commands.FGCDCommand;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.List;

public class PlayerJoinWorldListener implements Listener {
    private final FastGUI plugin;
    private final FGCDCommand fgcdCommand;

    public PlayerJoinWorldListener(FastGUI plugin, FGCDCommand fgcdCommand) {
        this.plugin = plugin;
        this.fgcdCommand = fgcdCommand;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        World world = player.getWorld();
        executeWorldCommands(world, player);
    }

    @EventHandler
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        World world = player.getWorld();
        executeWorldCommands(world, player);
    }

    private void executeWorldCommands(World world, Player player) {
        List<FGCDCommand.CommandEvent> events = fgcdCommand.getCommandEvents().get(world.getName());
        if (events != null) {
            for (FGCDCommand.CommandEvent event : events) {
                String command = event.command;
                String executor = event.executor;

                // 替换变量
                command = command.replace("{player}", player.getName());
                command = command.replace("{player_uuid}", player.getUniqueId().toString());
                command = command.replace("{player_displayname}", player.getName());

                Location loc = player.getLocation();
                command = command.replace("{x}", String.valueOf(loc.getBlockX()));
                command = command.replace("{y}", String.valueOf(loc.getBlockY()));
                command = command.replace("{z}", String.valueOf(loc.getBlockZ()));
                command = command.replace("{world}", loc.getWorld().getName());

                // 执行命令
                if (executor.equalsIgnoreCase("控制台") || executor.equalsIgnoreCase("console")) {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
                } else if (executor.equalsIgnoreCase("玩家") || executor.equalsIgnoreCase("player")) {
                    Bukkit.dispatchCommand(player, command);
                }
            }
        }
    }
}