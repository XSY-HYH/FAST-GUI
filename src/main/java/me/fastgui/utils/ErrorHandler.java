package me.fastgui.utils;

import me.fastgui.FastGUI;
import me.fastgui.managers.LogManager;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import java.util.logging.Level;

/**
 * 错误处理器，负责统一管理错误处理和异常报告
 */
public class ErrorHandler {
    
    private final FastGUI plugin;
    private final LogManager logManager;
    
    /**
     * 构造函数
     * @param plugin 插件实例
     */
    public ErrorHandler(FastGUI plugin, LogManager logManager) {
        this.plugin = plugin;
        this.logManager = logManager;
    }
    
    /**
     * 处理一般错误，记录日志并向发送者显示消息
     * @param sender 命令发送者
     * @param message 用户看到的错误消息
     * @param logMessage 记录到日志的消息
     */
    public void handleError(CommandSender sender, String message, String logMessage) {
        // 向用户发送错误消息
        if (sender != null) {
            sender.sendMessage("§c" + message);
        }
        
        // 记录错误日志
        if (logMessage != null) {
            plugin.getLogger().warning(logMessage);
        }
    }
    
    /**
     * 处理一般错误，记录日志并向发送者显示消息
     * @param sender 命令发送者
     * @param message 用户看到的错误消息
     */
    public void handleError(CommandSender sender, String message) {
        handleError(sender, message, message);
    }
    
    /**
     * 处理异常，记录详细异常信息并向发送者显示友好消息
     * @param sender 命令发送者
     * @param userMessage 用户看到的错误消息
     * @param ex 异常对象
     */
    public void handleException(CommandSender sender, String userMessage, Exception ex) {
        // 向用户发送友好错误消息
        if (sender != null) {
            sender.sendMessage("§c" + userMessage);
        }
        
        // 记录异常到日志
        logManager.severe("发生错误: " + userMessage + ": " + ex.getMessage());
    }
    
    /**
     * 处理玩家特定的错误
     * @param player 玩家
     * @param message 错误消息
     */
    public void handlePlayerError(Player player, String message) {
        if (player != null) {
            player.sendMessage("§c" + message);
        }
        plugin.getLogger().warning("玩家 " + (player != null ? player.getName() : "未知") + " 错误: " + message);
    }
    
    /**
     * 记录调试信息
     * @param message 调试消息
     */
    public void debug(String message) {
        logManager.debugLog("[Debug] " + message);
    }
    
    /**
     * 记录警告信息
     * @param message 警告消息
     */
    public void warn(String message) {
        plugin.getLogger().warning(message);
    }
    
    /**
     * 记录严重错误
     * @param message 错误消息
     * @param ex 异常对象
     */
    public void severe(String message, Exception ex) {
        plugin.getLogger().log(Level.SEVERE, message, ex);
    }
    
    /**
     * 验证参数不为空
     * @param param 参数值
     * @param paramName 参数名
     * @param sender 命令发送者
     * @return 参数是否有效
     */
    public boolean validateNotNull(Object param, String paramName, CommandSender sender) {
        if (param == null) {
            handleError(sender, paramName + " 不能为空！");
            return false;
        }
        return true;
    }
    
    /**
     * 验证字符串参数不为空且不为空字符串
     * @param param 参数字符串
     * @param paramName 参数名
     * @param sender 命令发送者
     * @return 参数是否有效
     */
    public boolean validateNotEmpty(String param, String paramName, CommandSender sender) {
        if (param == null || param.trim().isEmpty()) {
            handleError(sender, paramName + " 不能为空！");
            return false;
        }
        return true;
    }
    
    /**
     * 验证参数是否在指定范围内
     * @param value 参数值
     * @param min 最小值
     * @param max 最大值
     * @param paramName 参数名
     * @param sender 命令发送者
     * @return 参数是否有效
     */
    public boolean validateRange(int value, int min, int max, String paramName, CommandSender sender) {
        if (value < min || value > max) {
            handleError(sender, paramName + " 必须在 " + min + " 到 " + max + " 之间！");
            return false;
        }
        return true;
    }
    
    /**
     * 发送成功消息
     * @param sender 命令发送者
     * @param message 成功消息
     */
    public void sendSuccessMessage(CommandSender sender, String message) {
        if (sender != null) {
            sender.sendMessage("§a" + message);
        }
    }
    
    /**
     * 发送信息消息
     * @param sender 命令发送者
     * @param message 信息消息
     */
    public void sendInfoMessage(CommandSender sender, String message) {
        if (sender != null) {
            sender.sendMessage("§e" + message);
        }
    }
}