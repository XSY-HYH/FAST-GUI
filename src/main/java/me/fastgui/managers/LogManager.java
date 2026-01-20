package me.fastgui.managers;

import java.util.logging.Level;
import java.util.logging.Logger;

import me.fastgui.FastGUI;

public class LogManager {
    private static LogManager instance;
    private final Logger logger;
    private final ConfigManager configManager;
    private boolean isDebugEnabled;

    private LogManager(FastGUI plugin) {
        this.logger = plugin.getLogger();
        this.configManager = plugin.getConfigManager();
        // 初始化时读取配置文件确定是否启用debug模式
        reloadDebugStatus();
    }

    public static synchronized LogManager getInstance(FastGUI plugin) {
        if (instance == null) {
            instance = new LogManager(plugin);
        }
        return instance;
    }

    /**
     * 重新加载debug模式状态
     */
    public void reloadDebugStatus() {
        this.isDebugEnabled = configManager.isDebugModeEnabled();
    }

    /**
     * 输出debug日志
     * 如果debug模式关闭，则完全不记录任何日志
     */
    public void debugLog(String message) {
        if (isDebugEnabled && message != null) {
            logger.log(Level.FINE, "[DEBUG] " + message);
        }
    }

    /**
     * 输出info级别日志
     */
    public void info(String message) {
        if (message != null) {
            logger.info(message);
        }
    }

    /**
     * 输出warning级别日志
     */
    public void warning(String message) {
        if (message != null) {
            logger.warning(message);
        }
    }

    /**
     * 输出severe级别日志
     */
    public void severe(String message) {
        if (message != null) {
            logger.severe(message);
        }
    }

    /**
     * 检查debug模式是否启用
     */
    public boolean isDebugEnabled() {
        return isDebugEnabled;
    }
}