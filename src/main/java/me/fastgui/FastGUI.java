package me.fastgui;

import me.fastgui.commands.FastGUICommand;
import me.fastgui.commands.FGNBTCommand;
import me.fastgui.commands.FGBookCommand;
import me.fastgui.commands.FGLangCommand;
import me.fastgui.commands.FGCDCommand;
import me.fastgui.listeners.PlayerJoinWorldListener;
import me.fastgui.listeners.InventoryClickListener;
import me.fastgui.listeners.InventoryCloseListener;
import me.fastgui.listeners.NPCClickListener;
import me.fastgui.listeners.EntitySpawnListener;
import me.fastgui.listeners.PlayerInteractListener;
// 方块点击监听器已移除
import me.fastgui.managers.ConfigManager;
import me.fastgui.managers.LogManager;
import me.fastgui.managers.NBTManager;
import me.fastgui.managers.PermissionManager;
import me.fastgui.managers.UIManager;
import me.fastgui.managers.UIParser;
import me.fastgui.managers.UIOpener;
import me.fastgui.managers.BookUIParser;
import me.fastgui.managers.LanguageManager;
import me.fastgui.utils.ErrorHandler;
import org.bukkit.Bukkit;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.persistence.PersistentDataType;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import org.bukkit.plugin.PluginManager;
import org.bukkit.event.Listener;
import org.bukkit.event.Event;
import org.bukkit.plugin.RegisteredListener;

import java.util.logging.Logger;

/**
 * FastGUI 插件主类
 * <p>提供基于原版箱子界面的自定义UI系统，允许玩家创建、管理和交互自定义界面。</p>
 * 
 * @author XSY团队
 * @version 6.8.6
 */
public class FastGUI extends JavaPlugin {

    // 管理器实例
    private UIManager uiManager;            // UI管理系统
    private UIOpener uiOpener;              // UI打开系统
    private ErrorHandler errorHandler;      // 错误处理系统
    private ConfigManager configManager;    // 配置管理系统
    private NBTManager nbtManager;          // NBT管理系统
    private PermissionManager permissionManager; // 权限管理系统
    private LogManager logManager;          // 日志管理系统
    private BookUIParser bookUIParser;      // 书籍UI解析器
    private LanguageManager languageManager; // 语言管理系统
    private static FastGUI instance;        // 插件单例

    /**
     * 插件启用时调用的方法
     * <p>初始化所有系统组件，注册命令和监听器，并加载数据。</p>
     */
    @Override
    public void onEnable() {
        instance = this;
        
        // 加载配置文件
        this.configManager = new ConfigManager(this);
        configManager.loadConfig();
        
        // 初始化语言管理器（必须在其他管理器之前，因为它们可能需要使用语言系统）
        this.languageManager = new LanguageManager(this);
        
        // 初始化核心管理器
        initializeManagers();
        
        // 注册命令和监听器
        registerCommandsAndListeners();
        
        // 注册权限节点
        registerPermissions();
        
        // 加载UI数据
        loadData();
        
        // 发送启动消息
        sendStartupMessage();
    }

    /**
     * 插件禁用时调用的方法
     * <p>保存数据并清理资源。</p>
     */
    @Override
    public void onDisable() {
        try {
            // 保存UI数据
            if (uiManager != null) {
                uiManager.saveAll();
            }
            
            // 清理打开的UI
            if (uiOpener != null) {
                uiOpener.closeAllUIs();
            }
            
            if (configManager.isDebugModeEnabled()) {
            getLogger().info("FastGUI 插件已成功卸载！");
        }
        } catch (Exception e) {
            getLogger().severe("卸载插件时出错: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // 清理资源，避免内存泄漏
            cleanupResources();
        }
    }
    
    /**
     * 初始化所有管理器组件
     */
    private void initializeManagers() {
        this.logManager = LogManager.getInstance(this);
        this.errorHandler = new ErrorHandler(this, logManager);
        this.nbtManager = new NBTManager(this);
        this.permissionManager = new PermissionManager(this, nbtManager, languageManager);
        
        // 先创建UIParser，因为UIManager会用到它
        UIParser uiParser = new UIParser(this, configManager, logManager);
        this.uiManager = new UIManager(this, configManager, nbtManager, logManager);
        this.uiOpener = new UIOpener(this, this.uiManager, configManager, languageManager);
        
        // 初始化书籍UI解析器 - 用于独立处理书籍UI的解析和存储
        this.bookUIParser = new BookUIParser(this);
        logManager.info("书籍UI系统初始化完成，使用独立存储路径: FGBook/");
    }
    
    /**
     * 注册插件命令和监听器
     */
    private void registerCommandsAndListeners() {
        // 注册主命令
        FastGUICommand fgCommand = new FastGUICommand(this);
        this.getCommand("fg").setExecutor(fgCommand);
        this.getCommand("fg").setTabCompleter(fgCommand);
        
        // 注册NBT命令
        this.getCommand("fgnbt").setExecutor(new FGNBTCommand(this));
        this.getCommand("fgnbt").setTabCompleter(new FGNBTCommand(this));
        
        // 注册书UI命令
        this.getCommand("fgBook").setExecutor(new FGBookCommand(this));
        this.getCommand("fgBook").setTabCompleter(new FGBookCommand(this));
        
        // 注册语言切换命令
        this.getCommand("fglang").setExecutor(new FGLangCommand(this, this.languageManager));
        this.getCommand("fglang").setTabCompleter(new FGLangCommand(this, this.languageManager));
        
        // 注册FGCD命令
        FGCDCommand fgcdCommand = new FGCDCommand(this);
        this.getCommand("fgcd").setExecutor(fgcdCommand);
        this.getCommand("fgcd").setTabCompleter(fgcdCommand);
        
        // 注册事件监听器
        registerEventListeners(fgcdCommand);
    }
    
    /**
     * 注册事件监听器（可重复调用）
     */
    public void registerEventListeners() {
        // 先取消所有已注册的监听器
        try {
            Field pluginManagerField = Bukkit.class.getDeclaredField("pluginManager");
            pluginManagerField.setAccessible(true);
            PluginManager pluginManager = (PluginManager) pluginManagerField.get(Bukkit.class);
            
            // 获取所有已注册的监听器
            Field listenersField = pluginManager.getClass().getDeclaredField("listeners");
            listenersField.setAccessible(true);
            Map<Class<? extends Event>, List<RegisteredListener>> listeners = 
                (Map<Class<? extends Event>, List<RegisteredListener>>) listenersField.get(pluginManager);
            
            // 移除本插件注册的所有监听器
            for (List<RegisteredListener> registeredListeners : listeners.values()) {
                registeredListeners.removeIf(rl -> rl.getPlugin().equals(this));
            }
        } catch (Exception e) {
            getLogger().warning("清理旧监听器时出错: " + e.getMessage());
        }
        
        // 重新注册所有监听器
        // 注册事件监听器
        this.getServer().getPluginManager().registerEvents(new InventoryClickListener(this), this);
        this.getServer().getPluginManager().registerEvents(new InventoryCloseListener(uiOpener), this);
        // 注册NPC相关监听器
        this.getServer().getPluginManager().registerEvents(new NPCClickListener(this), this);
        this.getServer().getPluginManager().registerEvents(new EntitySpawnListener(this), this);
        this.getServer().getPluginManager().registerEvents(new PlayerInteractListener(this), this);
        // 注册玩家进入世界监听器
        this.getServer().getPluginManager().registerEvents(new PlayerJoinWorldListener(this, new FGCDCommand(this)), this);
        // 注册方块点击监听器
        // 方块点击监听器已移除
    }
    
    /**
     * 注册事件监听器（带FGCD命令实例）
     */
    private void registerEventListeners(FGCDCommand fgcdCommand) {
        // 注册事件监听器
        this.getServer().getPluginManager().registerEvents(new InventoryClickListener(this), this);
        this.getServer().getPluginManager().registerEvents(new InventoryCloseListener(uiOpener), this);
        // 注册NPC相关监听器
        this.getServer().getPluginManager().registerEvents(new NPCClickListener(this), this);
        this.getServer().getPluginManager().registerEvents(new EntitySpawnListener(this), this);
        this.getServer().getPluginManager().registerEvents(new PlayerInteractListener(this), this);
        // 注册玩家进入世界监听器
        this.getServer().getPluginManager().registerEvents(new PlayerJoinWorldListener(this, fgcdCommand), this);
        // 注册方块点击监听器
        // 方块点击监听器已移除
    }
    
    /**
     * 加载插件数据
     */
    private void loadData() {
        try {
            uiManager.loadTable();
            if (configManager.isDebugModeEnabled()) {
            getLogger().info("成功加载UI表数据");
        }
        } catch (Exception e) {
            errorHandler.handleException(Bukkit.getConsoleSender(), "加载UI表时出错", e);
            getLogger().severe("加载UI表时出错: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 发送插件启动消息到控制台
     */
    private void sendStartupMessage() {
        ConsoleCommandSender console = Bukkit.getConsoleSender();
        console.sendMessage("\n----------------------------------------");
        console.sendMessage("| FastGUI 插件已成功启用!                   |");
        console.sendMessage("| 版本: 6.8.6                             |");
        console.sendMessage("| 作者: XSY团队                             |");
        console.sendMessage("----------------------------------------\n");
    }
    
    /**
     * 清理插件资源
     */
    private void cleanupResources() {
        // 调用UIManager的资源清理方法
        if (uiManager != null) {
            uiManager.cleanupResources();
        }
        
        // 调用UIOpener的资源清理方法
        if (uiOpener != null) {
            uiOpener.cleanupResources();
        }
        
        // 清理管理器实例引用
        uiManager = null;
        uiOpener = null;
        errorHandler = null;
        bookUIParser = null; // 清理书籍UI解析器引用
        ConfigManager localConfigManager = configManager;
        configManager = null;
        if (localConfigManager != null && localConfigManager.isDebugModeEnabled()) {
            getLogger().info("所有资源已清理");
        }
    }

    /**
     * 注册插件权限节点
     */
    private void registerPermissions() {
        // 权限系统已移除，此方法留空
    }
    
    // Getter方法
    
    /**
     * 获取UI管理器实例
     * @return UI管理器
     */
    public UIManager getUIManager() {
        return uiManager;
    }
    
    /**
     * 获取UI打开器实例
     * @return UI打开器
     */
    public UIOpener getUIOpener() {
        return uiOpener;
    }
    
    /**
     * 获取错误处理器实例
     * @return 错误处理器
     */
    public ConfigManager getConfigManager() {
        return configManager;
    }
    
    public ErrorHandler getErrorHandler() {
        return errorHandler;
    }
    
    /**
     * 获取NBT管理器实例
     * @return NBT管理器
     */
    public NBTManager getNBTManager() {
        return nbtManager;
    }
    
    /**
     * 获取权限管理器
     */
    public PermissionManager getPermissionManager() {
        return permissionManager;
    }
    
    /**
     * 获取日志管理器
     */
    public LogManager getLogManager() {
        return logManager;
    }
    
    /**
     * 获取书籍UI解析器实例
     * @return 书籍UI解析器
     */
    public BookUIParser getBookUIParser() {
        return bookUIParser;
    }
    
    /**
     * 获取语言管理器实例
     * @return 语言管理器
     */
    public LanguageManager getLanguageManager() {
        return languageManager;
    }
    
    /**
     * 获取插件单例
     * @return 插件实例
     */
    public static FastGUI getInstance() {
        return instance;
    }
    
    /**
     * 获取日志记录器
     * @return 日志记录器
     */
    @Override
    public Logger getLogger() {
        return super.getLogger();
    }
}