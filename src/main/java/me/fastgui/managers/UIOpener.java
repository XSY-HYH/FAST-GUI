package me.fastgui.managers;

import me.fastgui.FastGUI;
import me.fastgui.managers.UIManager.InventoryData;
import me.fastgui.utils.UIItemParser;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import me.fastgui.managers.NBTManager;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

/**
 * UI打开管理器，负责将保存的UI数据转换为玩家可见的界面
 */
public class UIOpener {
    private final FastGUI plugin;
    private final UIManager uiManager;
    private final ConfigManager configManager;
    private final LanguageManager languageManager;
    
    // 记录当前打开的UI，用于识别哪些界面是FastGUI的
    private final Map<Inventory, String> openUIs = new HashMap<>();
    // 不再缓存Inventory对象，每个玩家打开时创建新实例
    
    public UIOpener(FastGUI plugin, UIManager uiManager, ConfigManager configManager, LanguageManager languageManager) {
        this.plugin = plugin;
        this.uiManager = uiManager;
        this.configManager = configManager;
        this.languageManager = languageManager;
    }
    
    /**
     * 打开UI界面
     * @param player 要打开界面的玩家
     * @param uiName UI名称
     * @return 是否成功打开
     */
    public boolean openUI(Player player, String uiName) {
        try {
            // 检查权限
            if (!uiManager.hasUIPermission(player, uiName)) {
                player.sendMessage(languageManager.getString("uiopener.no_permission", 
                        java.util.Map.of("ui_name", uiName)));
                plugin.getLogger().warning("玩家 " + player.getName() + " 没有权限打开UI: " + uiName);
                return false;
            }
            
            // 加载UI数据
            InventoryData data = uiManager.loadUI(uiName);
            
            if (data == null) {
                player.sendMessage(languageManager.getString("uiopener.ui_not_found", 
                        java.util.Map.of("ui_name", uiName)));
                plugin.getLogger().warning("UI数据不存在或加载失败: " + uiName);
                return false;
            }
            
            // 为每个玩家创建新的独立Inventory实例
            // 优先使用显示名称，如果没有则使用默认格式
            String displayName = data.getDisplayName() != null ? data.getDisplayName() : "FastGUI - " + uiName;
            
            // 根据容器类型创建对应的Inventory
            Inventory inventory;
            String containerType = data.getContainerType();
            int size = data.getContents().length;
            
            // 根据容器类型创建不同的容器界面
            // 注意：Bukkit无法直接创建大型箱子，会自动合并两个相邻的小箱子
            if (containerType != null) {
                switch (containerType.toUpperCase()) {
                    case "DISPENSER":
                    case "DROPPER":
                        // 发射器和投掷器都是3x3九宫格结构
                        inventory = Bukkit.createInventory(null, 9, displayName);
                        break;
                    case "LARGE_CHEST":
                        // 大型箱子需要54格
                        inventory = Bukkit.createInventory(null, 54, displayName);
                        break;
                    case "CHEST":
                    case "TRAPPED_CHEST":
                    default:
                        // 普通箱子默认27格，如果size是54则创建大型箱子
                        inventory = Bukkit.createInventory(null, size, displayName);
                        break;
                }
            } else {
                // 默认创建普通箱子
                inventory = Bukkit.createInventory(null, size, displayName);
            }
            
            // 填充物品
            boolean hasValidItems = populateInventory(inventory, data.getContents());
            
            if (!hasValidItems) {
                plugin.getLogger().warning("UI中没有有效的物品配置: " + uiName);
            }
            
            // 记录打开的UI
            openUIs.put(inventory, uiName);
            
            // 打开界面给玩家
            player.openInventory(inventory);
            
            // 仅在调试模式下记录玩家打开UI的信息
            if (configManager.isDebugModeEnabled()) {
                plugin.getLogger().info("玩家 " + player.getName() + " 打开了UI: " + uiName);
            }
            return true;
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "打开UI时出错 (名称: " + uiName + ", Player: " + player.getName() + ")", e);
            player.sendMessage(languageManager.getString("uiopener.open_error"));
            return false;
        }
    }
    
    /**
     * 填充容器界面的物品
     * @param inventory 要填充的容器
     * @param contents 物品内容
     * @return 是否包含有效的UI物品
     */
    private boolean populateInventory(Inventory inventory, ItemStack[] contents) {
        boolean hasValidItems = false;
        
        if (contents == null || contents.length <= 0) {
            plugin.getLogger().warning("UI内容数组无效，预期至少1个物品");
            return false;
        }
        
        // 确保内容数组大小与容器大小匹配
        if (inventory.getSize() != contents.length) {
            // 对于发射器和投掷器，直接使用前9个物品填充
            if ((inventory.getSize() == 9) && contents.length >= 9) {
                plugin.getLogger().warning("UI内容数组大小与容器大小不匹配，将使用前9个物品填充: " + contents.length + " != 9");
            } else {
                plugin.getLogger().warning("UI内容数组大小与容器大小不匹配: " + contents.length + " != " + inventory.getSize());
                return false;
            }
        }
        
        // 获取NBT管理器实例
        NBTManager nbtManager = plugin.getNBTManager();
        
        int maxSlots = Math.min(contents.length, inventory.getSize());
        for (int i = 0; i < maxSlots; i++) {
            ItemStack item = contents[i];
            if (item != null && item.getType() != null && !item.getType().isAir()) {
                // 克隆物品以避免修改原始数据
                ItemStack clonedItem = item.clone();
                
                // 将任何非空物品都视为有效物品
                hasValidItems = true;
                plugin.getLogger().fine("UIOpener: 正在处理物品 (槽位 " + i + "): " + clonedItem.getType().name() + ", 数量: " + clonedItem.getAmount());
                
                try {
                    // 确保NBT属性在UI渲染过程中被正确保留
                    // NBTManager已经在保存时添加了相关属性，这里确保它们在渲染时被保留
                    if (nbtManager != null) {
                        // 验证并确保NBT数据完整性
                        if (nbtManager.isButtonItem(clonedItem)) {
                            plugin.getLogger().fine("UIOpener: 确认物品为按钮 (槽位 " + i + ")");
                        } else if (nbtManager.isBorderItem(clonedItem)) {
                            plugin.getLogger().fine("UIOpener: 确认物品为边框 (槽位 " + i + ")");
                        }
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("UIOpener: 处理物品NBT数据时出错 (槽位 " + i + "): " + e.getMessage());
                }
                
                inventory.setItem(i, clonedItem);
                plugin.getLogger().fine("UIOpener: 物品已设置到槽位 " + i);
            }
        }
        
        return hasValidItems;
    }
    
    /**
     * 获取UI的显示名称
     * @param uiId UI的ID（现在直接是UI名称）
     * @return 显示名称
     */
    private String getUIName(String uiId) {
        // 由于我们直接使用UI名称作为ID，所以直接返回ID即可
        return uiId;
    }
    
    /**
     * 检查容器是否是FastGUI的UI界面
     * @param inventory 要检查的容器
     * @return 是否是FastGUI的UI
     */
    public boolean isFastGUI(Inventory inventory) {
        return openUIs.containsKey(inventory);
    }
    
    /**
     * 获取容器对应的UI ID
     * @param inventory 容器
     * @return UI ID，如果不是FastGUI则返回null
     */
    public String getUIId(Inventory inventory) {
        return openUIs.get(inventory);
    }
    
    /**
     * 当容器关闭时调用，清理记录
     * @param inventory 关闭的容器
     */
    public void onInventoryClose(Inventory inventory) {
        String uiId = openUIs.remove(inventory);
        if (uiId != null) {
            plugin.getLogger().fine("UI关闭: " + uiId);
        }
    }
    
    /**
     * 清理资源（已移除缓存机制）
     */
    public void clearInventoryCache() {
        // 不再需要清理缓存，因为每个玩家都获得独立实例
        plugin.getLogger().fine("Inventory缓存机制已移除，每个玩家获得独立实例");
    }
    
    /**
     * 资源清理方法
     */
    public void cleanupResources() {
        clearAllOpenUIs();
        clearInventoryCache();
        if (configManager.isDebugModeEnabled()) {
            plugin.getLogger().info("UIOpener资源已清理");
        }
    }
    
    /**
     * 清理所有打开的UI记录
     * 用于插件重载或关闭时
     */
    public void clearAllOpenUIs() {
        openUIs.clear();
        if (configManager.isDebugModeEnabled()) {
            plugin.getLogger().info("已清除所有打开的UI记录");
        }
    }
    
    /**
     * 关闭所有玩家当前打开的FastGUI界面
     * 遍历所有在线玩家，检查并关闭他们打开的FastGUI界面
     */
    public void closeAllUIs() {
        int closedCount = 0;
        
        // 遍历所有在线玩家
        for (Player player : Bukkit.getOnlinePlayers()) {
            Inventory inventory = player.getOpenInventory().getTopInventory();
            
            // 检查是否是FastGUI界面
            if (isFastGUI(inventory)) {
                player.closeInventory();
                closedCount++;
            }
        }
        
        // 清除所有打开的UI记录
        openUIs.clear();
        
        if (configManager.isDebugModeEnabled()) {
            plugin.getLogger().info("已关闭 " + closedCount + " 个FastGUI界面");
        }
    }
    
    /**
     * 获取当前打开的UI数量
     * @return 打开的UI数量
     */
    public int getOpenUICount() {
        return openUIs.size();
    }
}