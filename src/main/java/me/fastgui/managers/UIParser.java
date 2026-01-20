package me.fastgui.managers;

import me.fastgui.FastGUI;
import me.fastgui.managers.LogManager;
import me.fastgui.utils.UIItemParser;
import me.fastgui.utils.UIItemParser.UIButton;
// ChatColor导入已移除，使用颜色代码替代
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * UI物品解析器
 * <p>负责物品解析、按钮识别和边框检测等功能。</p>
 */
public class UIParser {
    
    private final FastGUI plugin;
    private final ConfigManager configManager;
    private final LogManager logManager;
    
    /**
     * 构造函数
     * @param plugin 插件实例
     * @param configManager 配置管理器
     */
    public UIParser(FastGUI plugin, ConfigManager configManager, LogManager logManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.logManager = logManager;
    }
    
    /**
     * 仅在调试模式启用时输出日志
     * @param message 日志消息
     */
    private void debugLog(String message) {
        logManager.debugLog(message);
    }
    
    /**
     * 判断一个物品是否为边框物品
     * @param item 要检查的物品
     * @return 如果是边框物品返回true，否则返回false
     */
    public boolean isBorderItem(ItemStack item) {
        try {
            // 1. 检查lore中是否有特定的边框标识
            ItemMeta meta = item.getItemMeta();
            if (meta != null && meta.hasLore()) {
                List<String> lore = new ArrayList<>();
                for (Object component : meta.lore()) {
                    lore.add(component.toString());
                }
                for (String line : lore) {
                    String cleanLine = line.replaceAll("\\u00A7[0-9a-fk-or]", "").trim().toLowerCase();
                    if (cleanLine.contains("border") || cleanLine.contains("边框")) {
                        return true;
                    }
                }
            }
            
        } catch (Exception e) {
            FastGUI.getInstance().getLogger().warning("检查边框物品时出错: " + e.getMessage());
        }
        
        return false;
    }
    

    
    /**
     * 从物品的lore中解析按钮信息
     * @param lore 物品的lore列表
     * @return 解析出的按钮对象，如果不是按钮返回null
     */
    public UIButton parseButtonFromLore(List<String> lore) {
        if (lore == null || lore.isEmpty()) {
            return null;
        }
        
        String command = null;
        boolean closeOnClick = false;
        boolean isFastGUIButton = false;
        
        for (String line : lore) {
            String cleanLine = line.replaceAll("\\u00A7[0-9a-fk-or]", "").trim().toLowerCase();
            
            // 检查是否是FastGUI按钮
            if (cleanLine.equals("fastgui button")) {
                isFastGUIButton = true;
            }
            
            // 解析命令
            if (cleanLine.startsWith("command: ")) {
                command = cleanLine.substring(9).trim();
            } else if (cleanLine.startsWith("execute: ")) {
                command = cleanLine.substring(9).trim();
            } else if (command == null && cleanLine.startsWith("/")) {
                // 支持直接写命令（如 " /say hello "）
                command = cleanLine.substring(1).trim();
            }
            
            // 解析CloseOnClick
            if (cleanLine.startsWith("closeonclick: ")) {
                String value = cleanLine.substring(14).trim();
                closeOnClick = "true".equals(value);
            }
        }
        
        // 如果是FastGUI按钮或有命令，则返回按钮对象
        if (isFastGUIButton || (command != null && !command.isEmpty())) {
            // 确保命令有斜杠前缀
            if (command != null && !command.startsWith("/")) {
                command = "/" + command;
            }
            return new UIButton("Button", command, closeOnClick);
        }
        
        return null;
    }
    

    
    /**
     * 从物品的lore中提取权限信息
     * @param lore 物品的lore列表
     * @return 权限字符串，如果没有权限要求返回空字符串
     */
    public String extractPermissionFromLore(List<String> lore) {
        if (lore == null || lore.isEmpty()) {
            return "";
        }
        
        for (String line : lore) {
            String cleanLine = line.replaceAll("\\u00A7[0-9a-fk-or]", "").trim().toLowerCase();
            if (cleanLine.startsWith("permission: ")) {
                return cleanLine.substring(11).trim();
            }
        }
        
        return "";
    }
}