package me.fastgui.managers;

import me.fastgui.FastGUI;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * 配置管理器
 * <p>负责管理插件的配置文件，包括加载、保存和获取配置值。</p>
 */
public class ConfigManager {
    
    private final FastGUI plugin;
    private FileConfiguration config;
    private final File configFile;
    private final Logger logger;
    
    // 默认配置值
    private static final boolean DEFAULT_PERMISSION_CHECK_ENABLED = true;
    private static final boolean DEFAULT_ENABLE_DEFAULT_PERMISSIONS = false;
    private static final int DEFAULT_INVENTORY_CACHE_SIZE = 50;
    private static final int DEFAULT_UI_ITEM_CACHE_SIZE = 100;
    private static final boolean DEFAULT_COMMAND_SECURITY_CHECK_ENABLED = true;
    private static final String DEFAULT_DISALLOWED_COMMANDS = "op,deop,give,ban,ipban,kick,stop,reload";
    private static final boolean DEFAULT_DEBUG_MODE_ENABLED = false;
    private static final String DEFAULT_LANGUAGE = "en.yml";
    
    /**
     * 构造函数
     * @param plugin 插件实例
     */
    public ConfigManager(FastGUI plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.configFile = new File(plugin.getDataFolder(), "config.yml");
    }
    
    /**
     * 加载配置文件
     * <p>如果配置文件不存在，创建默认配置文件。</p>
     */
    public void loadConfig() {
        // 确保插件目录存在
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }
        
        // 如果配置文件不存在，创建默认配置
        if (!configFile.exists()) {
            createDefaultConfig();
        }
        
        // 加载配置文件
        this.config = YamlConfiguration.loadConfiguration(configFile);
        logger.info("配置文件已成功加载");
        
        // 验证并更新配置（如果需要）
        validateAndUpdateConfig();
    }
    
    /**
     * 创建默认配置文件
     */
    private void createDefaultConfig() {
        try {
            configFile.createNewFile();
            FileConfiguration defaultConfig = YamlConfiguration.loadConfiguration(configFile);
            
            // 设置默认配置值
            defaultConfig.set("settings.permission-check-enabled", DEFAULT_PERMISSION_CHECK_ENABLED);
            defaultConfig.set("settings.enable-default-permissions", DEFAULT_ENABLE_DEFAULT_PERMISSIONS);
            defaultConfig.set("settings.inventory-cache-size", DEFAULT_INVENTORY_CACHE_SIZE);
            defaultConfig.set("settings.ui-item-cache-size", DEFAULT_UI_ITEM_CACHE_SIZE);
            defaultConfig.set("settings.command-security-check-enabled", DEFAULT_COMMAND_SECURITY_CHECK_ENABLED);
            defaultConfig.set("settings.disallowed-commands", DEFAULT_DISALLOWED_COMMANDS);
            defaultConfig.set("settings.debug-mode-enabled", DEFAULT_DEBUG_MODE_ENABLED);
            defaultConfig.set("settings.language", DEFAULT_LANGUAGE);
            
            // 配置文件将在首次生成时包含默认值
            
            defaultConfig.save(configFile);
            logger.info("默认配置文件已创建");
        } catch (IOException e) {
            logger.severe("创建默认配置文件时出错: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 验证并更新配置文件
     * <p>确保所有必要的配置项都存在，如果不存在则添加默认值。</p>
     */
    private void validateAndUpdateConfig() {
        boolean configUpdated = false;
        
        // 检查权限检查设置
        if (!config.contains("settings.permission-check-enabled")) {
            config.set("settings.permission-check-enabled", DEFAULT_PERMISSION_CHECK_ENABLED);
            configUpdated = true;
        }
        
        // 检查默认权限设置
        if (!config.contains("settings.enable-default-permissions")) {
            config.set("settings.enable-default-permissions", DEFAULT_ENABLE_DEFAULT_PERMISSIONS);
            configUpdated = true;
        }
        
        // 检查缓存大小设置
        if (!config.contains("settings.inventory-cache-size")) {
            config.set("settings.inventory-cache-size", DEFAULT_INVENTORY_CACHE_SIZE);
            configUpdated = true;
        }
        
        // 检查UI物品缓存大小设置
        if (!config.contains("settings.ui-item-cache-size")) {
            config.set("settings.ui-item-cache-size", DEFAULT_UI_ITEM_CACHE_SIZE);
            configUpdated = true;
        }
        
        // 检查命令安全检查设置
        if (!config.contains("settings.command-security-check-enabled")) {
            config.set("settings.command-security-check-enabled", DEFAULT_COMMAND_SECURITY_CHECK_ENABLED);
            configUpdated = true;
        }
        
        // 检查禁止命令设置
        if (!config.contains("settings.disallowed-commands")) {
            config.set("settings.disallowed-commands", DEFAULT_DISALLOWED_COMMANDS);
            configUpdated = true;
        }
        
        // 检查调试模式设置
        if (!config.contains("settings.debug-mode-enabled")) {
            config.set("settings.debug-mode-enabled", DEFAULT_DEBUG_MODE_ENABLED);
            configUpdated = true;
        }
        
        // 检查语言设置
        if (!config.contains("settings.language")) {
            config.set("settings.language", DEFAULT_LANGUAGE);
            configUpdated = true;
        }
        
        // 如果配置已更新，保存配置文件
        if (configUpdated) {
            try {
                config.save(configFile);
                logger.info("配置文件已更新");
            } catch (IOException e) {
                logger.severe("保存配置文件时出错: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
    
    /**
     * 重新加载配置文件
     */
    public void reloadConfig() {
        this.config = YamlConfiguration.loadConfiguration(configFile);
        logger.info("配置文件已重新加载");
    }
    
    /**
     * 保存配置文件
     */
    public void saveConfig() {
        try {
            config.save(configFile);
            logger.info("配置文件已保存");
        } catch (IOException e) {
            logger.severe("保存配置文件时出错: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    // 配置获取方法
    
    /**
     * 是否启用UI交互权限检查
     * @return true 如果启用，false 否则
     */
    public boolean isPermissionCheckEnabled() {
        return config.getBoolean("settings.permission-check-enabled", DEFAULT_PERMISSION_CHECK_ENABLED);
    }
    
    /**
     * 是否默认授予所有玩家基础权限
     * @return true 如果启用，false 否则
     */
    public boolean isDefaultPermissionsEnabled() {
        return config.getBoolean("settings.enable-default-permissions", DEFAULT_ENABLE_DEFAULT_PERMISSIONS);
    }
    
    /**
     * 获取Inventory缓存大小
     * @return 缓存大小
     */
    public int getInventoryCacheSize() {
        return config.getInt("settings.inventory-cache-size", DEFAULT_INVENTORY_CACHE_SIZE);
    }
    
    /**
     * 获取UI物品缓存大小
     * @return 缓存大小
     */
    public int getUIItemCacheSize() {
        return config.getInt("settings.ui-item-cache-size", DEFAULT_UI_ITEM_CACHE_SIZE);
    }
    
    /**
     * 是否启用命令安全检查
     * @return true 如果启用，false 否则
     */
    public boolean isCommandSecurityCheckEnabled() {
        return config.getBoolean("settings.command-security-check-enabled", DEFAULT_COMMAND_SECURITY_CHECK_ENABLED);
    }
    
    /**
     * 获取不允许执行的命令列表
     * @return 不允许的命令数组
     */
    public String[] getDisallowedCommands() {
        String disallowedCommands = config.getString("settings.disallowed-commands", DEFAULT_DISALLOWED_COMMANDS);
        return disallowedCommands.split(",");
    }
    
    /**
     * 获取配置文件对象
     * @return 配置文件对象
     */
    public FileConfiguration getConfig() {
        return config;
    }
    
    /**
     * 是否启用调试模式
     * @return true 如果启用，false 否则
     */
    public boolean isDebugModeEnabled() {
        return config.getBoolean("settings.debug-mode-enabled", DEFAULT_DEBUG_MODE_ENABLED);
    }
    
    /**
     * 获取配置值
     * @param path 配置路径
     * @param defaultValue 默认值
     * @return 配置值
     */
    public boolean getBoolean(String path, boolean defaultValue) {
        return config.getBoolean(path, defaultValue);
    }
    
    /**
     * 设置调试模式
     * @param enabled 是否启用调试模式
     */
    public void setDebugModeEnabled(boolean enabled) {
        config.set("settings.debug-mode-enabled", enabled);
        saveConfig();
    }
    
    /**
     * 获取当前语言设置
     * @return 语言代码
     */
    public String getLanguage() {
        return config.getString("settings.language", DEFAULT_LANGUAGE);
    }
    
    /**
     * 设置语言
     * @param languageCode 语言代码
     */
    public void setLanguage(String languageCode) {
        config.set("settings.language", languageCode);
        saveConfig();
    }
}