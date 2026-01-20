package me.fastgui.listeners;

import me.fastgui.FastGUI;
import me.fastgui.managers.UIOpener;
import me.fastgui.managers.UIManager;
import me.fastgui.utils.ErrorHandler;
import me.fastgui.managers.ConfigManager;
import me.fastgui.managers.LogManager;
import me.fastgui.managers.NBTManager;
import me.fastgui.managers.PermissionManager;
// ChatColor导入已移除，使用颜色代码替代
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.UUID;

public class InventoryClickListener implements Listener {

    private final UIOpener uiOpener;
    private final ErrorHandler errorHandler;
    private final UIManager uiManager;
    private final ConfigManager configManager;
    private final NBTManager nbtManager;
    private final LogManager logManager;
    private final PermissionManager permissionManager;

    public InventoryClickListener(FastGUI plugin) {
        this.uiOpener = plugin.getUIOpener();
        this.errorHandler = plugin.getErrorHandler();
        this.uiManager = plugin.getUIManager();
        this.configManager = plugin.getConfigManager();
        this.nbtManager = plugin.getNBTManager();
        this.logManager = plugin.getLogManager();
        this.permissionManager = new PermissionManager(plugin, nbtManager, plugin.getLanguageManager());
    }

    private void debugLog(String message) {
        if (configManager.isDebugModeEnabled()) {
            logManager.info("[InventoryClick] " + message);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        try {
            Player player = (Player) event.getWhoClicked();
            String title = event.getView().title().toString();
            
            // 检查是否为FastGUI的GUI - 简化检查逻辑
            if (title != null && title.contains("FastGUI")) {
                // 取消事件，防止玩家移动物品
                event.setCancelled(true);
                
                // 获取被点击的物品
                ItemStack clickedItem = event.getCurrentItem();
                
                if (clickedItem != null && !clickedItem.getType().isAir()) {
                    // 记录物品详情（调试模式）
                    logItemDetails(clickedItem);
                    
                    // 获取UI ID和槽位数据
                    String uiId = extractUIIdFromTitle(title);
                    debugLog("处理UI点击: " + uiId + " 槽位: " + event.getRawSlot());
                    
                    // 尝试从UIManager获取槽位数据
                    UIManager.InventorySlotData slotData = null;
                    try {
                        // 暂时创建默认的槽位数据对象
                        // 注意：UIManager需要提供合适的方法来获取UI数据
                        // 创建一个基本的按钮槽位数据（如果是按钮）
                        slotData = new UIManager.InventorySlotData(false, clickedItem, "normal", null, false, "");
                        debugLog("创建默认槽位数据: 槽位=" + event.getRawSlot());
                    } catch (Exception e) {
                        debugLog("创建槽位数据时出错: " + e.getMessage());
                    }
                    
                    // 处理UI物品点击
                    handleUIItemClick(player, slotData, uiId);
                }
            }
        } catch (Exception e) {
            errorHandler.handleException(event.getWhoClicked(), "处理物品点击事件时出错", e);
            logManager.severe("处理物品点击事件时出错: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void logItemDetails(ItemStack item) {
        try {
            debugLog("物品类型: " + item.getType().name());
            debugLog("物品数量: " + item.getAmount());
            
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                debugLog("物品元数据存在");
                if (meta.hasDisplayName()) {
                    debugLog("显示名称: " + meta.displayName().toString());
                }
                if (meta.hasLore()) {
                    debugLog("物品Lore:");
                    List<String> loreList = new ArrayList<>();
                for (Object component : meta.lore()) {
                    loreList.add(component.toString());
                }
                for (String line : loreList) {
                        debugLog("  - " + line);
                        // 使用正则表达式去除颜色代码
                        debugLog("    (无颜色): " + line.replaceAll("\\u00A7[0-9a-fk-or]", ""));
                    }
                }
            }
        } catch (Exception e) {
            FastGUI.getInstance().getLogger().warning("记录物品详情时出错: " + e.getMessage());
        }
    }

    /**
     * 处理UI物品点击事件
     * @param player 点击的玩家
     * @param slotData 槽位数据
     * @param uiId UI的唯一标识符
     */
    private void handleUIItemClick(Player player, UIManager.InventorySlotData slotData, String uiId) {
        try {
            if (slotData == null || slotData.isEmpty()) {
                debugLog("槽位数据为空，跳过处理");
                return;
            }
            
            ItemStack item = slotData.getItem();
            if (item == null) {
                debugLog("物品为空，跳过处理");
                return;
            }
            
            // 直接从NBTManager获取按钮命令
            String command = nbtManager.getButtonCommand(item);
            debugLog("从NBT获取命令: " + (command != null ? command : "无"));
            
            // 处理命令
            if (command != null && !command.isEmpty()) {
                // 检查权限
                if (!permissionManager.hasButtonPermission(player, item)) {
                    String requiredPermission = nbtManager.getButtonPermission(item);
                    permissionManager.sendNoPermissionMessage(player, requiredPermission);
                    return;
                }
                
                // 执行命令
                executeCommand(player, command);
            }
            
            // 检查是否需要关闭界面
            Boolean closeOnClick = nbtManager.getCloseOnClick(item);
            if (closeOnClick != null && closeOnClick) {
                debugLog("点击后关闭界面");
                player.closeInventory();
            }
            
            // 目前版本的InventorySlotData不支持页面导航和音效功能
            // 这些功能将在未来版本中实现
            
        } catch (Exception e) {
            errorHandler.handleException(player, "处理UI物品点击时出错", e);
            logManager.severe("处理UI物品点击时出错: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void executeCommand(Player player, String command) {
        try {
            debugLog("执行控制台命令: " + command);
            
            // 移除命令前的斜杠
            if (command.startsWith("/")) {
                command = command.substring(1);
            }
            
            // 替换变量
            String processedCommand = processCommandVariables(player, command);
            
            // 直接在控制台执行命令
            boolean success = player.getServer().dispatchCommand(
                player.getServer().getConsoleSender(), 
                processedCommand
            );
            
            if (!success) {
                logManager.warning("执行控制台命令失败: " + processedCommand);
            } else {
                debugLog("控制台命令执行成功: " + processedCommand);
            }
        } catch (Exception e) {
            logManager.severe("执行控制台命令时出错: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String processCommandVariables(Player player, String command) {
        String processed = command;
        
        // 替换玩家变量
        processed = processed.replace("{player}", player.getName());
        processed = processed.replace("{player_uuid}", player.getUniqueId().toString());
        processed = processed.replace("{player_displayname}", player.getName());
        
        // 替换坐标变量
        Location loc = player.getLocation();
        processed = processed.replace("{x}", String.valueOf(loc.getBlockX()));
        processed = processed.replace("{y}", String.valueOf(loc.getBlockY()));
        processed = processed.replace("{z}", String.valueOf(loc.getBlockZ()));
        processed = processed.replace("{world}", loc.getWorld().getName());
        
        return processed;
    }
    
    /**
     * 从标题中提取UI ID的简单方法
     */
    private String extractUIIdFromTitle(String title) {
        if (title != null) {
            // 简单实现，实际逻辑可能需要根据UI标题格式调整
            return "default_ui"; // 返回默认UI ID
        }
        return null;
    }
}