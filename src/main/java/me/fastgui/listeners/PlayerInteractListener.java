package me.fastgui.listeners;

import me.fastgui.FastGUI;
import me.fastgui.managers.NBTManager;
import me.fastgui.managers.PermissionManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.event.block.Action;

/**
 * 玩家交互监听器，用于跟踪刷怪蛋的使用
 */
public class PlayerInteractListener implements Listener {

    private final FastGUI plugin;
    private final NBTManager nbtManager;
    private final PermissionManager permissionManager;
    
    public PlayerInteractListener(FastGUI plugin) {
        this.plugin = plugin;
        this.nbtManager = plugin.getNBTManager();
        this.permissionManager = new PermissionManager(plugin, nbtManager, plugin.getLanguageManager());
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        try {
            Player player = event.getPlayer();
            ItemStack item = player.getInventory().getItemInMainHand();

            // 检查是否是生物刷怪蛋
            if (item != null && item.getType() != Material.AIR) {
                String itemTypeName = item.getType().name();
                if (itemTypeName.endsWith("_SPAWN_EGG") || itemTypeName.equals("SPAWN_EGG")) {
                    // 记录刷怪蛋使用信息
                    EntitySpawnListener.recordEggUse(player, item);
                }
                
                // 检查是否是ButtonItem，并且只在对着空气点击时触发
                if (event.getAction() == Action.RIGHT_CLICK_AIR && nbtManager.isButtonItem(item)) {
                    if (plugin.getConfigManager().isDebugModeEnabled()) {
                        plugin.getLogger().info("检测到ButtonItem使用，开始权限检查");
                        plugin.getLogger().info("PlayerInteractListener: 调用 permissionManager.hasButtonItemPermission");
                    }
                    
                    // 检查权限
                    boolean hasPermission = permissionManager.hasButtonItemPermission(player, item);
                    if (plugin.getConfigManager().isDebugModeEnabled()) {
                        plugin.getLogger().info("PlayerInteractListener: hasButtonItemPermission 返回: " + hasPermission);
                    }
                    
                    if (!hasPermission) {
                        String requiredPermission = nbtManager.getButtonItemPermission(item);
                        if (plugin.getConfigManager().isDebugModeEnabled()) {
                            plugin.getLogger().info("PlayerInteractListener: 权限检查失败，调用 sendNoPermissionMessage");
                        }
                        permissionManager.sendNoPermissionMessage(player, requiredPermission);
                        event.setCancelled(true);
                        return;
                    }
                    
                    if (plugin.getConfigManager().isDebugModeEnabled()) {
                        plugin.getLogger().info("权限检查通过，执行命令");
                    }
                    
                    // 获取ButtonItem的命令
                    String command = nbtManager.getButtonItemCommand(item);
                    if (command != null && !command.isEmpty()) {
                        // 阻止原交互事件触发
                        event.setCancelled(true);
                        
                        // 获取执行模式
                        String executeMode = nbtManager.getButtonItemExecuteMode(item);
                        if (executeMode == null) {
                            executeMode = "op"; // 默认控制台执行
                        }
                        
                        // 执行命令
                        executeCommand(player, command, executeMode);
                        
                        if (plugin.getConfigManager().isDebugModeEnabled()) {
                            plugin.getLogger().info("玩家 " + player.getName() + " 使用ButtonItem执行了命令: " + command + " (执行模式: " + executeMode + ")");
                        }
                    }
                }
            }

        } catch (Exception e) {
            plugin.getLogger().severe("处理玩家交互事件时出错: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 执行命令，支持变量替换
     */
    private void executeCommand(Player player, String command, String executeMode) {
        if (command == null || command.isEmpty()) {
            return;
        }
        
        // 移除命令前的斜杠
        if (command.startsWith("/")) {
            command = command.substring(1);
        }
        
        String processedCommand = processCommandVariables(player, command);
        
        if (executeMode.equalsIgnoreCase("np")) {
            // 以玩家身份执行命令
            plugin.getServer().dispatchCommand(player, processedCommand);
        } else {
            // 默认以控制台身份执行命令
            plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), processedCommand);
        }
    }
        private String processCommandVariables(Player player, String command) {
        String processed = command;
        
     
        processed = processed.replace("{player}", player.getName());
        processed = processed.replace("%player%", player.getName());
        processed = processed.replace("{player_uuid}", player.getUniqueId().toString());
        processed = processed.replace("%uuid%", player.getUniqueId().toString());
        processed = processed.replace("{player_displayname}", player.getName());
        processed = processed.replace("%displayname%", player.getName());
        
      
        processed = processed.replace("{x}", String.valueOf(player.getLocation().getBlockX()));
        processed = processed.replace("%x%", String.valueOf(player.getLocation().getBlockX()));
        processed = processed.replace("{y}", String.valueOf(player.getLocation().getBlockY()));
        processed = processed.replace("%y%", String.valueOf(player.getLocation().getBlockY()));
        processed = processed.replace("{z}", String.valueOf(player.getLocation().getBlockZ()));
        processed = processed.replace("%z%", String.valueOf(player.getLocation().getBlockZ()));
        processed = processed.replace("{world}", player.getWorld().getName());
        processed = processed.replace("%world%", player.getWorld().getName());
        
        return processed;
    }
}