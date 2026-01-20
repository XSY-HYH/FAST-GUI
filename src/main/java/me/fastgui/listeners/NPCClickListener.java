package me.fastgui.listeners;

import me.fastgui.FastGUI;
import me.fastgui.managers.NBTManager;
import me.fastgui.managers.PermissionManager;
import org.bukkit.Bukkit;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.Listener;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 实体点击监听器，用于处理玩家与NPC实体的交互
 */
public class NPCClickListener implements Listener {

    private final FastGUI plugin;
    private final NBTManager nbtManager;
    private final PermissionManager permissionManager;
    private final ConsoleCommandSender consoleSender;
    
    // 用于防止重复触发的映射表：玩家UUID+实体UUID -> 最后触发时间
    private final Map<String, Long> lastInteractionTimes = new HashMap<>();
    // 防重复触发的时间阈值（毫秒）
    private static final long INTERACTION_THRESHOLD = 100;

    public NPCClickListener(FastGUI plugin) {
        this.plugin = plugin;
        this.nbtManager = plugin.getNBTManager();
        this.permissionManager = plugin.getPermissionManager();
        this.consoleSender = Bukkit.getConsoleSender();
    }

    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        try {
            Player player = event.getPlayer();
            Entity entity = event.getRightClicked();
            
            // 生成唯一的交互键
            String interactionKey = player.getUniqueId().toString() + ":" + entity.getUniqueId().toString();
            long currentTime = System.currentTimeMillis();
            
            // 检查是否在短时间内重复触发
            if (lastInteractionTimes.containsKey(interactionKey)) {
                long lastTime = lastInteractionTimes.get(interactionKey);
                if (currentTime - lastTime < INTERACTION_THRESHOLD) {
                    // 重复触发，直接返回
                    if (FastGUI.getInstance().getConfigManager().isDebugModeEnabled()) {
            plugin.getLogger().info("检测到重复的NPC交互，已忽略: " + player.getName() + " -> " + entity.getType());
        }
                    return;
                }
            }
            
            // 更新最后触发时间
            lastInteractionTimes.put(interactionKey, currentTime);
            
            // 清理过期的交互记录（定期执行，避免内存泄漏）
            if (lastInteractionTimes.size() > 100) {
                cleanupOldInteractions(currentTime);
            }

            // 检查实体是否有NPC标签和命令
            String command = nbtManager.getNPCCommand(entity);
            if (command != null && !command.isEmpty()) {
                // 检查权限
                if (!permissionManager.hasNPCPermission(player, entity)) {
                    String requiredPermission = nbtManager.getNPCPermission(entity);
                    permissionManager.sendNoPermissionMessage(player, requiredPermission);
                    return;
                }
                
                // 处理命令，替换变量
                String processedCommand = processCommand(command, player);
                
                // 获取执行体
                final String executor = nbtManager.getNPCExecutor(entity);
                final String finalExecutor = (executor == null) ? "player" : executor;
                
                // 调试信息
                if (FastGUI.getInstance().getConfigManager().isDebugModeEnabled()) {
                    String permission = nbtManager.getNPCPermission(entity);
                    String executeMode = nbtManager.getNPCExecuteMode(entity);
                    
                    plugin.getLogger().info("NPC交互调试信息:");
                    plugin.getLogger().info("  实体类型: " + entity.getType().name());
                    plugin.getLogger().info("  实体UUID: " + entity.getUniqueId().toString());
                    plugin.getLogger().info("  命令: " + command);
                    plugin.getLogger().info("  权限: " + (permission != null ? permission : "未设置"));
                    plugin.getLogger().info("  执行模式: " + (executeMode != null ? executeMode : "未设置"));
                    plugin.getLogger().info("  执行体: " + (executor != null ? executor : "未设置，使用默认: player"));
                    
                    // 检查属性是否缺失
                    if (permission == null) {
                        plugin.getLogger().warning("NPC属性警告: 权限属性缺失，使用默认权限");
                    }
                    if (executeMode == null) {
                        plugin.getLogger().warning("NPC属性警告: 执行模式属性缺失，将使用插件默认执行模式");
                    }
                    if (executor == null) {
                        plugin.getLogger().warning("NPC属性警告: 执行体属性缺失，使用默认执行体: player");
                    }
                }
                
                // 取消事件传播，防止重复触发
                event.setCancelled(true);
                
                // 使用延迟任务执行命令，避免重复触发
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    try {
                        // 执行命令
                        boolean success = executeCommand(processedCommand, player, entity, finalExecutor);
                        
                        if (success) {
                            if (FastGUI.getInstance().getConfigManager().isDebugModeEnabled()) {
                plugin.getLogger().info("NPC执行命令成功: '" + processedCommand + "' (由玩家 " + player.getName() + " 触发, 执行体: " + finalExecutor + ")");
            }
                        } else {
                            plugin.getLogger().warning("NPC执行命令失败: '" + processedCommand + "' (由玩家 " + player.getName() + " 触发, 执行体: " + finalExecutor + ")");
                        }
                    } catch (Exception e) {
                        plugin.getLogger().severe("执行延迟NPC命令时出错: " + e.getMessage());
                        e.printStackTrace();
                    }
                }, 1L); // 延迟1tick
            }
        } catch (Exception e) {
            plugin.getLogger().severe("处理实体点击事件时出错: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 清理过期的交互记录，避免内存泄漏
     * @param currentTime 当前时间（毫秒）
     */
    private void cleanupOldInteractions(long currentTime) {
        lastInteractionTimes.entrySet().removeIf(entry -> 
            currentTime - entry.getValue() > 10000 // 清理10秒前的记录
        );
    }

    /**
     * 处理命令，替换变量
     * @param command 原始命令
     * @param player 触发命令的玩家
     * @return 处理后的命令
     */
    private String processCommand(String command, Player player) {
        String processedCommand = command;
        
        // 替换玩家名变量
        processedCommand = processedCommand.replace("{player}", player.getName());
        processedCommand = processedCommand.replace("{player_uuid}", player.getUniqueId().toString());
        // 替换显示名称（在较新版本Bukkit中，建议使用player.getName()替代getDisplayName()）
        String playerName = player.getName();
        processedCommand = processedCommand.replace("{player_dr_displayname}", playerName);
        processedCommand = processedCommand.replace("{player_displayname}", playerName);
        
        // 替换坐标变量
        processedCommand = processedCommand.replace("{x}", String.format("%.2f", player.getLocation().getX()));
        processedCommand = processedCommand.replace("{y}", String.format("%.2f", player.getLocation().getY()));
        processedCommand = processedCommand.replace("{z}", String.format("%.2f", player.getLocation().getZ()));
        
        return processedCommand;
    }

    /**
     * 执行命令
     * @param command 要执行的命令
     * @param player 触发命令的玩家
     * @param entity 被点击的实体
     * @param executor 执行体（player/console）
     * @return 是否执行成功
     */
    private boolean executeCommand(String command, Player player, Entity entity, String executor) {
        try {
            // 移除命令前的斜杠（如果有）
            if (command.startsWith("/")) {
                command = command.substring(1);
            }
            
            // 根据执行体执行命令
            if (executor.equalsIgnoreCase("console") || executor.equalsIgnoreCase("控制台")) {
                // 控制台执行
                return Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
            } else {
                // 玩家执行
                return player.performCommand(command);
            }
        } catch (Exception e) {
            plugin.getLogger().severe("执行NPC命令时出错: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}