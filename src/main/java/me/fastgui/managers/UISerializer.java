package me.fastgui.managers;

import me.fastgui.FastGUI;
// 移除错误的导入，将在代码中使用完整路径
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.*;
import java.util.UUID;

/**
 * UI序列化管理器
 * <p>负责UI数据的序列化、保存和相关文件操作。</p>
 */
public class UISerializer {
    
    private final FastGUI plugin;
    private final ConfigManager configManager;
    private final LogManager logManager;
    private final File fastGUIFolder;
    
    /**
     * 构造函数
     * @param plugin 插件实例
     * @param configManager 配置管理器
     */
    public UISerializer(FastGUI plugin, ConfigManager configManager, LogManager logManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.logManager = logManager;
        this.fastGUIFolder = new File(FastGUI.getInstance().getDataFolder(), "Fast GUI");
    }
    
    /**
     * 仅在调试模式启用时输出日志
     * @param message 日志消息
     */
    private void debugLog(String message) {
        logManager.debugLog(message);
    }
    
    // 不再需要generateUniqueId方法，直接使用UI名称作为ID
    
    /**
     * 清理文件名，移除无效字符
     * @param name 原始文件名
     * @return 安全的文件名（移除特殊字符并限制长度）
     */
    public String sanitizeFileName(String name) {
        // 移除文件系统不允许的字符
        String sanitized = name.replaceAll("[<>:\"/\\|?*]", "_");
        // 限制长度
        if (sanitized.length() > 50) {
            sanitized = sanitized.substring(0, 50);
        }
        // 确保不为空
        if (sanitized.trim().isEmpty()) {
            sanitized = "unnamed_ui";
        }
        return sanitized;
    }
    
    /**
     * 保存UI数据到文件
     * @param fileName 文件名
     * @param contents 物品内容数组
     * @param worldName 世界名称
     * @param slotData 槽位数据数组
     */
    public void saveUIData(String fileName, ItemStack[] contents, String worldName, me.fastgui.managers.UIManager.InventorySlotData[] slotData) {
        saveUIData(fileName, contents, worldName, slotData, null, null, "np");
    }
    
    /**
     * 保存UI数据到文件（支持权限节点）
     * @param fileName 文件名
     * @param contents 物品内容数组
     * @param worldName 世界名称
     * @param slotData 槽位数据数组
     * @param displayName 显示名称
     * @param containerType 容器类型
     * @param permission 权限节点
     */
    public void saveUIData(String fileName, ItemStack[] contents, String worldName, me.fastgui.managers.UIManager.InventorySlotData[] slotData, String displayName, String containerType, String permission) {
        File uiFolder = new File(fastGUIFolder, fileName.replace(".dat", ""));
        
        try {
            // 确保文件夹存在
            if (!uiFolder.exists() && !uiFolder.mkdirs()) {
                throw new IOException("无法创建UI文件夹: " + uiFolder.getPath());
            }
            
            File dataFile = new File(uiFolder, fileName);
            
            try (DataOutputStream dos = new DataOutputStream(new FileOutputStream(dataFile))) {
                // 写入版本号，现在使用版本6表示支持权限节点
                dos.writeInt(6); // 版本6 - 支持权限节点
                
                // 保存世界名称
                dos.writeUTF(worldName);
                
                // 保存显示名称（版本4新增）
                dos.writeBoolean(displayName != null && !displayName.isEmpty());
                if (displayName != null && !displayName.isEmpty()) {
                    dos.writeUTF(displayName);
                }
                
                // 保存容器类型（版本5新增）
                dos.writeBoolean(containerType != null && !containerType.equals("CHEST"));
                if (containerType != null && !containerType.equals("CHEST")) {
                    dos.writeUTF(containerType);
                }
                
                // 保存权限节点（版本6新增）
                dos.writeBoolean(permission != null && !permission.equals("np"));
                if (permission != null && !permission.equals("np")) {
                    dos.writeUTF(permission);
                }
                
                // 保存UI大小
                dos.writeInt(contents.length);
                
                debugLog("开始保存UI版本3数据: 大小=" + contents.length);
                
                // 第一部分：UI展示界面数据（渲染用的物品）
                debugLog("保存UI展示界面数据...");
                for (int i = 0; i < contents.length; i++) {
                    ItemStack item = contents[i];
                    try {
                        // 保存槽位是否为空
                        boolean isEmpty = (item == null || item.getType() == null || item.getType().isAir());
                        dos.writeBoolean(isEmpty);
                        
                        if (!isEmpty) {
                            try {
                                // 使用Bukkit提供的serializeAsBytes方法保存物品展示数据
                                byte[] itemData = item.serializeAsBytes();
                                
                                // 验证序列化数据大小
                                if (itemData != null && itemData.length > 0 && itemData.length < 1024 * 1024) { // 限制大小为1MB
                                    dos.writeInt(itemData.length);
                                    dos.write(itemData);
                                    debugLog("保存展示物品 (槽位 " + i + "): " + item.getType().name());
                                } else {
                                    FastGUI.getInstance().getLogger().warning("保存展示物品时出错 (槽位 " + i + "): 物品数据无效");
                                    dos.writeInt(0);
                                }
                            } catch (Exception e) {
                                FastGUI.getInstance().getLogger().warning("保存展示物品时出错 (槽位 " + i + "): " + e.getMessage());
                                dos.writeInt(0);
                            }
                        }
                    } catch (Exception e) {
                        FastGUI.getInstance().getLogger().warning("保存展示槽位时出错 (" + i + "): " + e.getMessage());
                        try {
                            dos.writeBoolean(true); // 标记为空
                        } catch (Exception inner) {
                            FastGUI.getInstance().getLogger().severe("保存展示槽位标记时出错 (" + i + "): " + inner.getMessage());
                        }
                    }
                }
                
                // 第二部分：UI实际数据（交互信息）
                debugLog("保存UI实际数据（交互信息）...");
                for (int i = 0; i < slotData.length; i++) {
                    me.fastgui.managers.UIManager.InventorySlotData slot = slotData[i];
                    try {
                        // 保存槽位类型
                        dos.writeUTF(slot.getType());
                        
                        // 保存是否为按钮
                        boolean isButton = slot.isButton();
                        dos.writeBoolean(isButton);
                        
                        // 如果是按钮，保存按钮属性
                        if (isButton) {
                            dos.writeUTF(slot.getCommand() != null ? slot.getCommand() : "");
                            dos.writeBoolean(slot.isCloseOnClick());
                            dos.writeUTF(slot.getPermission() != null ? slot.getPermission() : "");
                            debugLog("保存按钮交互数据 (槽位 " + i + "): 命令=" + slot.getCommand());
                        }
                    } catch (Exception e) {
                        FastGUI.getInstance().getLogger().warning("保存交互数据时出错 (槽位 " + i + "): " + e.getMessage());
                        try {
                            dos.writeUTF("normal"); // 默认类型
                            dos.writeBoolean(false); // 不是按钮
                        } catch (Exception inner) {
                            FastGUI.getInstance().getLogger().severe("保存交互数据标记时出错 (槽位 " + i + "): " + inner.getMessage());
                        }
                    }
                }
                

            }
            
            debugLog("UI数据保存成功: " + fileName);
            
        } catch (IOException e) {
            FastGUI.getInstance().getLogger().severe("保存UI数据失败: " + e.getMessage());
        }
    }
}