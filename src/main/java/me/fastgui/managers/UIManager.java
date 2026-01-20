package me.fastgui.managers;

import me.fastgui.FastGUI;
import me.fastgui.managers.ConfigManager;
import me.fastgui.managers.LogManager;
import me.fastgui.managers.NBTManager;
import me.fastgui.managers.UIParser;
import me.fastgui.utils.UIItemParser;
import me.fastgui.utils.UIItemParser.UIItem;
import me.fastgui.utils.UIItemParser.UIButton;
// ChatColor导入已移除，使用颜色代码替代
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * UI管理器类
 * <p>负责UI的创建、存储、加载和管理，提供UI数据的序列化与反序列化功能。</p>
 */
public class UIManager {
    
    private final FastGUI plugin;
    private final ConfigManager configManager;
    private final NBTManager nbtManager;
    private final LogManager logManager;
    private final UIParser uiParser;
    private final Map<String, String> uiTable; // UI ID -> 文件名
    private final Map<String, InventoryData> loadedUIs; // UI ID -> 库存数据
    private final File tableFile; // 存储UI映射关系的表文件
    private final File fastGUIFolder; // UI数据主文件夹
    
    /**
     * 构造函数，初始化UI管理器的核心组件
     * @param plugin 插件实例
     * @param configManager 配置管理器
     */
    public UIManager(FastGUI plugin, ConfigManager configManager, NBTManager nbtManager, LogManager logManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.nbtManager = nbtManager;
        this.logManager = logManager;
        this.uiParser = new UIParser(plugin, configManager, logManager);
        uiTable = new HashMap<>();
        loadedUIs = new HashMap<>();
        tableFile = new File(FastGUI.getInstance().getDataFolder(), "Table.dat");
        fastGUIFolder = new File(FastGUI.getInstance().getDataFolder(), "Fast GUI");
    }
    
    /**
     * 仅在调试模式启用时输出日志
     * @param message 日志消息
     */
    private void debugLog(String message) {
        logManager.debugLog(message);
    }
    
    /**
     * 加载UI表数据
     * <p>从文件中读取UI ID与文件名的映射关系，并进行验证。</p>
     */
    public void loadTable() {
        if (!tableFile.exists()) {
            debugLog("UI表文件不存在，创建新的表文件");
            saveTable(); // 创建新的表文件
            return;
        }
        
        try (DataInputStream dis = new DataInputStream(new FileInputStream(tableFile))) {
            int size = dis.readInt();
            debugLog("开始加载UI表，共 " + size + " 个UI");
            
            for (int i = 0; i < size; i++) {
                try {
                    String id = dis.readUTF();
                    String fileName = dis.readUTF();
                    
                    // 验证文件名格式
                    if (!fileName.endsWith(".dat")) {
                        FastGUI.getInstance().getLogger().warning("无效的UI文件名格式: " + fileName);
                        continue;
                    }
                    
                    uiTable.put(id, fileName);
                    FastGUI.getInstance().getLogger().fine("加载UI: ID=" + id + ", 文件名=" + fileName);
                } catch (Exception e) {
                    FastGUI.getInstance().getLogger().warning("加载UI表项时出错: " + e.getMessage());
                }
            }
            
            debugLog("UI表加载完成，成功加载 " + uiTable.size() + " 个UI");
            
        } catch (IOException e) {
            FastGUI.getInstance().getLogger().severe("加载UI表失败: " + e.getMessage());
            // 尝试修复表文件
            try {
                debugLog("尝试重新创建UI表文件");
                saveTable();
            } catch (Exception ex) {
                FastGUI.getInstance().getLogger().severe("重新创建UI表文件失败: " + ex.getMessage());
            }
        }
    }
    
    /**
     * 保存UI表数据
     * <p>将当前UI映射表写入文件，确保数据持久化。</p>
     */
    public void saveTable() {
        try {
            // 确保文件目录存在
            if (!tableFile.getParentFile().exists()) {
                tableFile.getParentFile().mkdirs();
            }
            
            try (DataOutputStream dos = new DataOutputStream(new FileOutputStream(tableFile))) {
                dos.writeInt(uiTable.size());
                for (Map.Entry<String, String> entry : uiTable.entrySet()) {
                    dos.writeUTF(entry.getKey());
                    dos.writeUTF(entry.getValue());
                }
            }
            
            debugLog("UI表保存成功，共 " + uiTable.size() + " 个UI");
            
        } catch (IOException e) {
            FastGUI.getInstance().getLogger().severe("保存UI表失败: " + e.getMessage());
        }
    }
    /**
     * 添加新UI
     * @param name UI名称
     * @param contents UI内容（物品数组）
     * @param worldName 世界名称
     * @param containerType 容器类型
     * @return 生成的UI唯一ID
     * @throws RuntimeException 如果添加UI失败
     */
    public String addUI(String name, ItemStack[] contents, String worldName, String containerType) {
        // 直接使用UI名称作为ID
        return addUI(name, contents, worldName, containerType, null);
    }
    
    /**
     * 添加新UI（支持权限节点）
     * @param name UI名称（将直接作为ID使用）
     * @param contents UI内容（物品数组）
     * @param worldName 世界名称
     * @param containerType 容器类型
     * @param permission 权限节点（op: 仅OP可打开, np: 所有玩家可打开）
     * @return UI的ID（即清理后的UI名称）
     * @throws RuntimeException 如果添加UI失败
     */
    public String addUI(String name, ItemStack[] contents, String worldName, String containerType, String permission) {
        return addUI(name, contents, worldName, containerType, permission, null);
    }
    
    /**
     * 添加新UI（支持权限节点和自定义ID）
     * @param name UI名称（将直接作为ID使用）
     * @param contents UI内容（物品数组）
     * @param worldName 世界名称
     * @param containerType 容器类型
     * @param permission 权限节点（op: 仅OP可打开, np: 所有玩家可打开）
     * @param customId 自定义ID参数（已废弃，将被忽略）
     * @return UI的ID（即清理后的UI名称）
     * @throws RuntimeException 如果添加UI失败
     */
    public String addUI(String name, ItemStack[] contents, String worldName, String containerType, String permission, String customId) {
        try {
            // 验证参数
            if (name == null || name.trim().isEmpty()) {
                throw new IllegalArgumentException("UI名称不能为空");
            }
            
            if (contents == null || contents.length <= 0) {
                throw new IllegalArgumentException("UI内容不能为空");
            }
            
            if (worldName == null || worldName.trim().isEmpty()) {
                throw new IllegalArgumentException("世界名称不能为空");
            }
            
            // 验证权限节点
            if (permission == null || permission.trim().isEmpty()) {
                permission = "np"; // 默认所有玩家可打开
            }
            
            if (!permission.equals("op") && !permission.equals("np")) {
                throw new IllegalArgumentException("权限节点必须是op或np");
            }
            
            // 记录自定义ID参数已被废弃
            if (customId != null && !customId.trim().isEmpty()) {
                debugLog("警告: 自定义ID参数已被废弃，将直接使用UI名称作为ID: " + customId);
            }
            
            // 直接使用清理后的UI名称作为ID
            String id = sanitizeFileName(name);
            
            // 文件名直接使用ID + .dat
            String safeFileName = id + ".dat";
            
            boolean isOverwrite = false;
            
            // 检查是否已存在相同ID的UI
            if (uiTable.containsKey(id)) {
                isOverwrite = true;
                debugLog("发现同名UI，准备覆盖: ID/名称=" + id);
                
                // 从缓存中移除旧UI数据
                if (loadedUIs.containsKey(id)) {
                    loadedUIs.remove(id);
                    debugLog("从缓存中移除旧UI数据: ID=" + id);
                }
                
                // 删除旧文件（确保不会有残留）
                File oldFile = new File(fastGUIFolder, safeFileName);
                if (oldFile.exists()) {
                    oldFile.delete();
                    debugLog("删除旧UI文件: " + safeFileName);
                }
            }
            
            // 保存UI数据（使用新的权限节点参数）
            saveUIData(safeFileName, contents, worldName, null, containerType, permission);
            
            // 更新表（添加或替换）
            uiTable.put(id, safeFileName);
            saveTable();
            
            if (isOverwrite) {
                debugLog("成功覆盖UI: ID/名称=" + id + ", 权限=" + permission);
            } else {
                debugLog("成功添加新UI: ID/名称=" + id + ", 权限=" + permission);
            }
            return id;
            
        } catch (Exception e) {
            FastGUI.getInstance().getLogger().severe("添加UI失败: " + e.getMessage());
            throw new RuntimeException("添加UI失败", e);
        }
    }
    

    
    /**
     * 清理文件名，移除无效字符
     * @param name 原始文件名
     * @return 安全的文件名（移除特殊字符并限制长度）
     */
    private String sanitizeFileName(String name) {
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
     * @param displayName 显示名称
     * @param containerType 容器类型
     * @throws IOException 如果保存失败
     */
    private void saveUIData(String fileName, ItemStack[] contents, String worldName, String displayName, String containerType, String permission) {
        File uiFolder = new File(fastGUIFolder, fileName.replace(".dat", ""));
        
        try {
            // 确保文件夹存在
            if (!uiFolder.exists() && !uiFolder.mkdirs()) {
                throw new IOException("无法创建UI文件夹: " + uiFolder.getPath());
            }
            
            File dataFile = new File(uiFolder, fileName);
            
            // 序列化前处理物品，确保按钮信息被正确保存
            ItemStack[] processedContents = processItemsForSave(contents);
            
            // 生成槽位详细数据
            InventorySlotData[] slotData = generateSlotData(processedContents);
            
            try (DataOutputStream dos = new DataOutputStream(new FileOutputStream(dataFile))) {
                // 写入版本号，现在使用版本6表示支持权限节点的格式
                dos.writeInt(6); // 版本6 - 支持权限节点的新格式
                
                // 保存世界名称
                dos.writeUTF(worldName);
                
                // 保存显示名称（版本4新增）
                boolean hasDisplayName = displayName != null;
                dos.writeBoolean(hasDisplayName);
                if (hasDisplayName) {
                    dos.writeUTF(displayName);
                }
                
                // 保存容器类型（版本5新增）
                boolean hasContainerType = containerType != null && !containerType.equals("CHEST");
                dos.writeBoolean(hasContainerType);
                if (hasContainerType) {
                    dos.writeUTF(containerType);
                }
                
                // 保存权限节点（版本6新增）
                boolean hasPermission = permission != null && !permission.equals("np");
                dos.writeBoolean(hasPermission);
                if (hasPermission) {
                    dos.writeUTF(permission);
                }
                
                // 保存UI大小
                dos.writeInt(processedContents.length);
                
                debugLog("开始保存UI版本3数据: 大小=" + processedContents.length);
                
                // 第一部分：UI展示界面数据（渲染用的物品）
                debugLog("保存UI展示界面数据...");
                for (int i = 0; i < processedContents.length; i++) {
                    ItemStack item = processedContents[i];
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
                    InventorySlotData slot = slotData[i];
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
    
    /**
     * 为每个槽位生成详细数据
     * @param items 处理后的物品数组
     * @return 槽位详细数据数组
     */
    private InventorySlotData[] generateSlotData(ItemStack[] items) {
        if (items == null) {
            return new InventorySlotData[0];
        }
        
        InventorySlotData[] slotData = new InventorySlotData[items.length];
        
        for (int i = 0; i < items.length; i++) {
            ItemStack item = items[i];
            
            if (item == null || item.getType() == null || item.getType().isAir()) {
                // 空槽位
                slotData[i] = new InventorySlotData(true, null, "air", null, false, null);
                continue;
            }
            
            try {
                // 检查物品是否是按钮
                String type = "normal";
                String command = null;
                boolean closeOnClick = false;
                String permission = "";
                
                // 仅通过NBT属性验证物品类型
                if (nbtManager != null) {
                    // 检查是否为边框物品
                    Boolean isBorder = nbtManager.isBorderItem(item);
                    if (isBorder != null && isBorder) {
                        type = "border";
                        debugLog("从NBT中识别到边框物品 (槽位 " + i + ")");
                    } else {
                        // 检查是否为按钮物品
                        String nbtCommand = nbtManager.getButtonCommand(item);
                        if (nbtCommand != null) {
                            type = "button";
                            command = nbtCommand;
                            Boolean closeOnClickBoolean = nbtManager.getCloseOnClick(item);
                            closeOnClick = closeOnClickBoolean != null ? closeOnClickBoolean : false;
                            debugLog("从NBT中识别到按钮 (槽位 " + i + "): 命令='" + command + "'");
                        }
                    }
                }
                
                slotData[i] = new InventorySlotData(false, item, type, command, closeOnClick, permission);
                
            } catch (Exception e) {
                FastGUI.getInstance().getLogger().warning("生成槽位数据时出错 (" + i + "): " + e.getMessage());
                // 出错时，将其视为普通物品
                slotData[i] = new InventorySlotData(false, item, "normal", null, false, null);
            }
        }
        
        return slotData;
    }
    
    /**
     * 判断一个物品是否为边框物品
     * 这里使用简单的逻辑，实际可以根据需要调整
     */
    public boolean isBorderItem(ItemStack item) {
        try {
            // 1. 首先检查NBT属性中是否标记为Border
            if (nbtManager != null) {
                Boolean isBorder = nbtManager.isBorderItem(item);
                if (isBorder != null && isBorder) {
                    debugLog("从NBT中识别到边框物品: " + item.getType().name());
                    return true;
                }
            }
            
            // 2. 检查物品类型（玻璃、屏障等常用于边框）
            String typeName = item.getType().name();
            if (typeName.contains("STAINED_GLASS_PANE") || 
                typeName.contains("GLASS_PANE") || 
                typeName.contains("BARRIER") || 
                typeName.contains("BEDROCK") || 
                typeName.contains("BLACK_STAINED_GLASS_PANE")) {
                return true;
            }
            
            // 4. 检查物品lore中是否有边框标识
            ItemMeta meta = item.getItemMeta();
            if (meta != null && meta.hasLore()) {
                List<String> loreList = new ArrayList<>();
                for (Object component : meta.lore()) {
                    loreList.add(component.toString());
                }
                for (String line : loreList) {
                    String cleanLine = line.replaceAll("\\u00A7[0-9a-fk-or]", "").trim().toLowerCase();
                    if (cleanLine.contains("border") || cleanLine.contains("框架")) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            // 忽略错误
        }
        return false;
    }
    
    /**
     * 处理要保存的物品数组，从NBT标签中提取按钮属性，为按钮添加完整元数据信息
     * <p>确保UI保存到dat文件时，每个按钮的完整定义都被保存到物品的lore中</p>
     * 
     * @param items 原始物品数组
     * @return 处理后的物品数组
     */
    private ItemStack[] processItemsForSave(ItemStack[] items) {
        if (items == null) {
            return new ItemStack[0];
        }
        
        ItemStack[] processedItems = new ItemStack[items.length];
        
        for (int i = 0; i < items.length; i++) {
            ItemStack item = items[i];
            if (item == null || item.getType() == null || item.getType().isAir()) {
                processedItems[i] = null;
                continue;
            }
            
            try {
                // 克隆物品以避免修改原始物品
                ItemStack clone = item.clone();
                ItemMeta meta = clone.getItemMeta();
                
                if (meta == null) {
                    processedItems[i] = clone;
                    continue;
                }
                
                // 获取现有lore或创建新的
                List<String> lore = new ArrayList<>();
            if (meta.hasLore()) {
                for (Object component : meta.lore()) {
                    lore.add(component.toString());
                }
            }
                
                // 初始化按钮对象
                UIButton button = null;
                
                // 仅通过NBT属性验证按钮
                if (nbtManager != null) {
                    String command = nbtManager.getButtonCommand(item);
                    if (command != null) {
                        // 从NBT中获取命令和关闭属性
                        Boolean closeOnClickBoolean = nbtManager.getCloseOnClick(item);
                        boolean closeOnClick = closeOnClickBoolean != null ? closeOnClickBoolean : false;
                        
                        button = new UIButton("", command, closeOnClick);
                        debugLog("从NBT中识别到按钮定义 (槽位 " + i + "): 命令='" + command + "', CloseOnClick=" + closeOnClick);
                    }
                }
                
                // 如果识别到按钮，保留原有的非FastGUI调试lore，不添加额外信息
                if (button != null) {
                    // 清除现有的按钮相关调试lore
                    List<String> nonButtonLore = new ArrayList<>();
                    for (String line : lore) {
                        String cleanLine = line.replaceAll("\\u00A7[0-9a-fk-or]", "").trim().toLowerCase();
                        if (!cleanLine.contains("command:") && !cleanLine.contains("execute:") && 
                            !cleanLine.contains("closeonclick") && !cleanLine.contains("fastgui button")) {
                            nonButtonLore.add(line);
                        }
                    }
                    
                    // 应用清理后的lore，不添加任何调试信息
                    // 使用传统API设置lore以确保兼容性
                meta.setLore(nonButtonLore);
                    clone.setItemMeta(meta);
                    
                    debugLog("成功保存按钮定义: 命令='" + button.getCommand() + "', CloseOnClick=" + button.shouldCloseOnClick());
                } else {
                    // 普通物品，确保保存完整元数据
                    if (meta.hasLore()) {
                        clone.setItemMeta(meta);
                        debugLog("保存普通物品 (槽位 " + i + "): " + item.getType().name());
                    }
                }
                
                processedItems[i] = clone;
            } catch (Exception e) {
                logManager.warning("处理物品保存时出错 (槽位 " + i + "): " + e.getMessage());
                e.printStackTrace();
                processedItems[i] = item; // 出错时使用原始物品
            }
        }
        
        return processedItems;
    }
    

    
    /**
     * 从lore中解析按钮信息
     * 用于在保存前识别可能的按钮定义
     */
    private UIButton parseButtonFromLore(String commandLine, List<String> allLore) {
        try {
            String cleanLine = commandLine.replaceAll("\\u00A7[0-9a-fk-or]", "").trim();
            String command = "";
            boolean closeOnClick = false;
            
            // 提取命令
            if (cleanLine.toLowerCase().startsWith("command: ")) {
                command = cleanLine.substring(9).trim();
            } else if (cleanLine.toLowerCase().startsWith("execute: ")) {
                command = cleanLine.substring(9).trim();
            } else if (cleanLine.startsWith("/")) {
                command = cleanLine;
            }
            
            // 确保命令有前缀/
            if (!command.isEmpty() && !command.startsWith("/")) {
                command = "/" + command;
            }
            
            // 检查CloseOnClick属性
            for (String line : allLore) {
                String cleanLore = line.replaceAll("\\u00A7[0-9a-fk-or]", "").trim().toLowerCase();
                if (cleanLore.equals("closeonclick: true") || 
                    (cleanLore.contains("close") && cleanLore.contains("true"))) {
                    closeOnClick = true;
                    break;
                }
            }
            
            if (!command.isEmpty()) {
                return new UIButton("Button", command, closeOnClick);
            }
        } catch (Exception e) {
            FastGUI.getInstance().getLogger().warning("从lore解析按钮时出错: " + e.getMessage());
        }
        return null;
    }
    
    /**
     * 加载UI数据
     * <p>优先从内存缓存获取，如果缓存未命中则从文件加载。</p>
     * @param id UI唯一标识符
     * @return 加载的UI数据，如果加载失败则返回null
     */
    // 不再需要findUIIdByName方法，因为直接使用UI名称作为ID
    
    public InventoryData loadUI(String uiName) {
        debugLog("开始加载UI: " + uiName);
        try {
            // 直接使用UI名称作为ID
            
            // 每次都从文件加载最新UI数据，先移除旧缓存（如果存在）
            loadedUIs.remove(uiName);
            debugLog("准备从文件加载最新UI数据: " + uiName);
            
            // 获取文件名
            String fileName = uiTable.get(uiName);
            if (fileName == null) {
                logManager.warning("未找到UI: " + uiName);
                return null;
            }
            
            // 构建文件路径
            File uiFolder = new File(fastGUIFolder, fileName.replace(".dat", ""));
            File dataFile = new File(uiFolder, fileName);
            debugLog("UI文件路径: " + dataFile.getPath());
            
            if (!dataFile.exists()) {
                logManager.warning("UI文件不存在: " + dataFile.getPath());
                // 从表中移除不存在的UI
                uiTable.remove(uiName);
                saveTable();
                return null;
            }
            
            // 读取数据
            try (DataInputStream dis = new DataInputStream(new FileInputStream(dataFile))) {
                // 读取版本号
                int version = dis.readInt();
                debugLog("加载UI版本: " + version);
                
                String worldName = dis.readUTF();
                
                // 读取显示名称（版本4新增）
                String displayName = null;
                if (version >= 4) {
                    boolean hasDisplayName = dis.readBoolean();
                    if (hasDisplayName) {
                        displayName = dis.readUTF();
                    }
                }
                
                // 读取容器类型（版本5新增）
                String containerType = "CHEST";
                if (version >= 5) {
                    boolean hasContainerType = dis.readBoolean();
                    if (hasContainerType) {
                        containerType = dis.readUTF();
                    }
                }
                
                // 读取权限节点（版本6新增）
                String permission = "np";
                if (version >= 6) {
                    boolean hasPermission = dis.readBoolean();
                    if (hasPermission) {
                        permission = dis.readUTF();
                    }
                } else {
                    // 对于旧版本，跳过权限节点读取
                    if (version >= 5) {
                        // 版本5有容器类型，但没有权限节点
                        // 跳过权限节点读取
                    }
                }
                
                int size = dis.readInt();
                
                debugLog("UI元数据: 世界=" + worldName + ", 大小=" + size);
                
                // 不再强制限制大小为54，支持不同容器的大小
                if (size <= 0) {
                    FastGUI.getInstance().getLogger().warning("UI大小不正确: " + size + ", 默认为54");
                    size = 54;
                }
                
                ItemStack[] contents = new ItemStack[size];
                InventorySlotData[] slotData = new InventorySlotData[size];
                
                int itemsLoaded = 0;
                int emptySlotsCount = 0;
                int errorSlotsCount = 0;
                
                // 根据版本号选择不同的读取方式
                if (version >= 5 && version <= 6) {
                    debugLog("使用版本" + version + "格式读取UI数据（支持容器类型和权限节点）");
                    // 版本5-6格式读取方式，支持容器类型和权限节点
                    slotData = loadVersion3Data(dis, size, contents, itemsLoaded, emptySlotsCount, errorSlotsCount);
                } else {
                    // 版本不是5-6，返回null表示不兼容
                    FastGUI.getInstance().getLogger().warning("UI '" + uiName + "' 的版本 (" + version + ") 太低，Fast GUI不兼容，请重新创建");
                    return null;
                }
                
                debugLog("UI加载成功: " + uiName + ", 统计: 有效物品=" + itemsLoaded + ", 空槽位=" + emptySlotsCount + ", 错误槽位=" + errorSlotsCount);
                
                // 使用支持权限节点的构造函数创建InventoryData对象
                InventoryData data = new InventoryData(worldName, contents, slotData, containerType, permission);
                
                // 如果有显示名称，设置它
                if (displayName != null) {
                    data.setDisplayName(displayName);
                }
                
                loadedUIs.put(uiName, data);
                debugLog("UI已缓存: " + uiName + (displayName != null ? " (显示名称: " + displayName + ")" : ""));
                return data;
            }
            
        } catch (Exception e) {
            FastGUI.getInstance().getLogger().severe("加载UI数据失败 (名称: " + uiName + "): " + e.getMessage());
            // 添加更详细的异常信息
            if (e.getCause() != null) {
                FastGUI.getInstance().getLogger().severe("原因: " + e.getCause().getMessage());
            }
            return null;
        }
    }
    
    /**
     * 读取版本1格式的UI数据（旧格式）
     */

    
    /**
     * 读取版本2格式的UI数据（新版本，包含槽位详细信息）
     */

    
    /**
     * 读取版本3格式的UI数据（分离UI展示界面和UI实际数据）
     */
    private InventorySlotData[] loadVersion3Data(DataInputStream dis, int size, ItemStack[] contents, 
                                               int itemsLoaded, int emptySlotsCount, int errorSlotsCount) throws IOException {
        InventorySlotData[] slotData = new InventorySlotData[size];
        debugLog("开始加载UI版本3数据: 大小=" + size);
        
        // 第一部分：加载UI展示界面数据
        debugLog("加载UI展示界面数据...");
        for (int i = 0; i < size; i++) {
            try {
                // 读取槽位是否为空
                boolean isEmpty = dis.readBoolean();
                ItemStack item = null;
                
                if (!isEmpty) {
                    // 读取物品数据
                    try {
                        int length = dis.readInt();
                        if (length > 0 && length < 1024 * 1024) {
                            byte[] itemData = new byte[length];
                            dis.readFully(itemData);
                            
                            try {
                                item = ItemStack.deserializeBytes(itemData);
                                if (item != null && item.getType() != null && !item.getType().isAir()) {
                                    contents[i] = item;
                                    itemsLoaded++;
                                    debugLog("成功加载展示物品 (槽位 " + i + "): " + item.getType().name());
                                } else {
                                    FastGUI.getInstance().getLogger().warning("加载展示物品时出错 (槽位 " + i + "): 反序列化后的物品无效");
                                    errorSlotsCount++;
                                    isEmpty = true;
                                }
                            } catch (Exception e) {
                                FastGUI.getInstance().getLogger().warning("加载展示物品时出错 (槽位 " + i + "): " + e.getMessage());
                                errorSlotsCount++;
                                isEmpty = true;
                            }
                        } else {
                            FastGUI.getInstance().getLogger().warning("加载展示物品时出错 (槽位 " + i + "): 无效的数据长度");
                            errorSlotsCount++;
                            isEmpty = true;
                        }
                    } catch (Exception e) {
                        FastGUI.getInstance().getLogger().warning("加载展示物品数据时出错 (槽位 " + i + "): " + e.getMessage());
                        errorSlotsCount++;
                        isEmpty = true;
                    }
                } else {
                    emptySlotsCount++;
                }
                
                // 先创建基本的槽位数据，后面再填充交互信息
                slotData[i] = new InventorySlotData(isEmpty, item, "normal", null, false, "");
                
            } catch (Exception e) {
                FastGUI.getInstance().getLogger().warning("加载展示槽位时出错 (" + i + "): " + e.getMessage());
                errorSlotsCount++;
                slotData[i] = new InventorySlotData(true, null, "air", null, false, null);
            }
        }
        
        // 第二部分：加载UI实际数据（交互信息）
        debugLog("加载UI实际数据（交互信息）...");
        for (int i = 0; i < size; i++) {
            try {
                // 读取槽位类型
                String type = "normal";
                try {
                    type = dis.readUTF();
                } catch (Exception e) {
                    FastGUI.getInstance().getLogger().warning("读取槽位类型时出错 (槽位 " + i + "): " + e.getMessage());
                }
                
                // 读取是否为按钮
                boolean isButton = false;
                String command = null;
                boolean closeOnClick = false;
                String permission = "";
                
                try {
                    isButton = dis.readBoolean();
                    if (isButton) {
                        command = dis.readUTF();
                        closeOnClick = dis.readBoolean();
                        permission = dis.readUTF();
                        debugLog("加载按钮交互数据 (槽位 " + i + "): 命令=" + command);
                    }
                } catch (Exception e) {
                    FastGUI.getInstance().getLogger().warning("读取交互数据时出错 (槽位 " + i + "): " + e.getMessage());
                }
                
                // 更新槽位数据中的交互信息
                InventorySlotData existingSlot = slotData[i];
                slotData[i] = new InventorySlotData(
                    existingSlot.isEmpty(),
                    existingSlot.getItem(),
                    type,
                    command,
                    closeOnClick,
                    permission
                );
                
                debugLog("更新槽位交互数据 (" + i + "): 类型=" + type + ", 是按钮=" + isButton);
                
            } catch (Exception e) {
                FastGUI.getInstance().getLogger().warning("加载交互数据时出错 (槽位 " + i + "): " + e.getMessage());
            }
        }
        
        debugLog("UI版本3数据加载完成: 有效物品=" + itemsLoaded + ", 空槽位=" + emptySlotsCount + ", 错误槽位=" + errorSlotsCount);
        
        return slotData;
    }
    
    /**
     * 删除指定的UI
     * 删除UI
     * @param uiName UI名称（作为ID）
     * @return 是否成功删除
     */
    /**
     * 重命名UI
     * @param oldName 原UI名称
     * @param newName 新UI名称
     * @return 是否重命名成功
     */
    /**
     * 更新UI的显示名称
     * @param uiId UI的ID
     * @param displayName 新的显示名称（可以是普通文本或NBT格式）
     * @return 是否更新成功
     */
    public boolean updateUIDisplayName(String uiId, String displayName) {
        try {
            // 参数验证
            if (uiId == null || uiId.trim().isEmpty()) {
                FastGUI.getInstance().getLogger().warning("UI ID不能为空");
                return false;
            }
            
            // 检查UI是否存在
            if (!uiTable.containsKey(uiId)) {
                FastGUI.getInstance().getLogger().warning("未找到UI: " + uiId);
                return false;
            }
            
            // 加载UI数据
            InventoryData data = loadUI(uiId);
            if (data == null) {
                FastGUI.getInstance().getLogger().warning("加载UI失败: " + uiId);
                return false;
            }
            
            // 设置新的显示名称
            data.setDisplayName(displayName);
            
            // 更新缓存
            loadedUIs.put(uiId, data);
            
            // 重新保存UI数据
            String fileName = uiTable.get(uiId);
            saveUIData(fileName, data.getContents(), data.getWorldName(), data.getDisplayName(), data.getContainerType(), data.getPermission());
            
            debugLog("成功更新UI显示名称: ID=" + uiId);
            return true;
            
        } catch (Exception e) {
            FastGUI.getInstance().getLogger().severe("更新UI显示名称失败: " + e.getMessage());
            return false;
        }
    }
    
    public boolean renameUI(String oldName, String newName) {
        try {
            // 验证参数
            if (oldName == null || oldName.trim().isEmpty() || newName == null || newName.trim().isEmpty()) {
                FastGUI.getInstance().getLogger().warning("尝试重命名UI时名称为空");
                return false;
            }
            
            // 检查原UI是否存在
            String oldFileName = uiTable.get(oldName);
            if (oldFileName == null) {
                FastGUI.getInstance().getLogger().warning("未找到要重命名的UI: " + oldName);
                return false;
            }
            
            // 检查新名称是否已存在
            if (uiTable.containsKey(newName)) {
                FastGUI.getInstance().getLogger().warning("新UI名称已存在: " + newName);
                return false;
            }
            
            // 清理新名称
            String sanitizedNewName = sanitizeFileName(newName);
            String newFileName = sanitizedNewName + ".dat";
            
            // 记录文件信息
            String oldFolderName = oldFileName.replace(".dat", "");
            File oldFolder = new File(fastGUIFolder, oldFolderName);
            File oldDataFile = new File(oldFolder, oldFileName);
            File newFolder = new File(fastGUIFolder, sanitizedNewName);
            File newDataFile = new File(newFolder, newFileName);
            
            // 确保新文件夹存在
            if (!newFolder.exists() && !newFolder.mkdirs()) {
                FastGUI.getInstance().getLogger().warning("无法创建新UI文件夹: " + newFolder.getPath());
                return false;
            }
            
            // 复制文件内容
            if (oldDataFile.exists()) {
                try (InputStream in = new FileInputStream(oldDataFile);
                     OutputStream out = new FileOutputStream(newDataFile)) {
                    byte[] buffer = new byte[1024];
                    int length;
                    while ((length = in.read(buffer)) > 0) {
                        out.write(buffer, 0, length);
                    }
                }
            }
            
            // 更新表
            uiTable.remove(oldName);
            uiTable.put(sanitizedNewName, newFileName);
            saveTable();
            
            // 更新缓存
            if (loadedUIs.containsKey(oldName)) {
                InventoryData data = loadedUIs.remove(oldName);
                loadedUIs.put(sanitizedNewName, data);
            }
            
            // 删除旧文件
            if (oldDataFile.exists()) {
                oldDataFile.delete();
            }
            if (oldFolder.exists() && oldFolder.list().length == 0) {
                oldFolder.delete();
            }
            
            debugLog("UI重命名成功: 从" + oldName + " 到 " + sanitizedNewName);
            return true;
            
        } catch (Exception e) {
            FastGUI.getInstance().getLogger().severe("重命名UI时出错: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    public boolean deleteUI(String uiName) {
        try {
            // 验证UI名称
            if (uiName == null || uiName.trim().isEmpty()) {
                FastGUI.getInstance().getLogger().warning("尝试删除空名称的UI");
                return false;
            }
            
            String fileName = uiTable.get(uiName);
            if (fileName == null) {
                FastGUI.getInstance().getLogger().warning("未找到要删除的UI: " + uiName);
                return false;
            }
            
            // 记录要删除的文件信息
            String uiFolderName = fileName.replace(".dat", "");
            File uiFolder = new File(fastGUIFolder, uiFolderName);
            File dataFile = new File(uiFolder, fileName);
            
            // 从表中移除
            uiTable.remove(uiName);
            boolean saveSuccess = true;
            try {
                saveTable();
            } catch (Exception e) {
                FastGUI.getInstance().getLogger().warning("删除UI时保存表失败，但仍继续删除文件: " + uiName);
                saveSuccess = false;
            }
            
            // 从缓存中移除
            loadedUIs.remove(uiName);
            
            // 删除对应的UI文件
            boolean fileDeleted = true;
            if (dataFile.exists()) {
                fileDeleted = dataFile.delete();
                if (!fileDeleted) {
                    FastGUI.getInstance().getLogger().warning("无法删除UI数据文件: " + dataFile.getPath());
                }
            }
            
            // 如果文件夹存在且为空，删除文件夹
            boolean folderDeleted = true;
            if (uiFolder.exists()) {
                // 尝试删除文件夹中的所有文件
                File[] files = uiFolder.listFiles();
                if (files != null) {
                    for (File file : files) {
                        if (!file.delete()) {
                            FastGUI.getInstance().getLogger().warning("无法删除文件: " + file.getPath());
                        }
                    }
                }
                
                // 删除空文件夹
                if (uiFolder.list().length == 0) {
                    folderDeleted = uiFolder.delete();
                    if (!folderDeleted) {
                        FastGUI.getInstance().getLogger().warning("无法删除UI文件夹: " + uiFolder.getPath());
                    }
                }
            }
            
            debugLog("UI删除成功: 名称=" + uiName + ", 文件名=" + fileName);
            return saveSuccess && fileDeleted && folderDeleted;
            
        } catch (Exception e) {
            FastGUI.getInstance().getLogger().severe("删除UI时出错 (名称: " + uiName + "): " + e.getMessage());
            // 即使出现异常，也尝试恢复
            try {
                saveTable();
            } catch (Exception ex) {
                FastGUI.getInstance().getLogger().severe("删除后尝试保存表失败: " + ex.getMessage());
            }
            return false;
        }
    }
    
    /**
     * 清理可能存在的孤立文件
     * <p>删除所有未在UI表中引用的UI文件夹，避免磁盘空间浪费。</p>
     */
    public void cleanupOrphanedFiles() {
        try {
            if (!fastGUIFolder.exists()) {
                return;
            }
            
            File[] directories = fastGUIFolder.listFiles(File::isDirectory);
            if (directories == null) {
                return;
            }
            
            int orphanedDeleted = 0;
            for (File dir : directories) {
                String dirName = dir.getName();
                boolean isReferenced = false;
                
                // 检查是否在表中被引用
                for (String fileName : uiTable.values()) {
                    if (fileName.replace(".dat", "").equals(dirName)) {
                        isReferenced = true;
                        break;
                    }
                }
                
                if (!isReferenced && !dirName.equals(".git")) { // 避免删除.git文件夹
                    // 删除孤立文件夹
                    if (deleteDirectory(dir)) {
                        orphanedDeleted++;
                        debugLog("删除孤立UI文件夹: " + dirName);
                    }
                }
            }
            
            if (orphanedDeleted > 0) {
                debugLog("清理完成，删除了 " + orphanedDeleted + " 个孤立UI文件夹");
            }
            
        } catch (Exception e) {
            FastGUI.getInstance().getLogger().severe("清理孤立文件时出错: " + e.getMessage());
        }
    }
    
    /**
     * 递归删除目录
     * @param directory 要删除的目录
     * @return 是否删除成功
     */
    private boolean deleteDirectory(File directory) {
        if (directory == null || !directory.exists() || !directory.isDirectory()) {
            return false;
        }
        
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else {
                    file.delete();
                }
            }
        }
        
        return directory.delete();
    }
    
    /**
     * 获取当前已加载的UI数量
     * @return 已加载的UI数量
     */
    public int getLoadedUIsCount() {
        return loadedUIs.size();
    }

    /**
     * 获取所有已加载的UI名称列表
     * 
     * @return UI名称列表
     */
    public List<String> getUIList() {
        return new ArrayList<>(loadedUIs.keySet());
    }
    
    /**
     * 清除内存中的UI缓存
     * <p>释放内存资源，重新加载将从文件读取。</p>
     */
    public void clearCache() {
        loadedUIs.clear();
        debugLog("UI缓存已清除");
    }
    
    /**
     * 添加资源清理方法，在插件卸载时调用
     */
    public void cleanupResources() {
        // 清除缓存
        clearCache();
        // 清空UI表
        uiTable.clear();
        debugLog("UIManager资源已清理");
    }
    
    /**
     * 获取UI表的副本
     * @return UI ID到文件名的映射（返回副本以防止外部修改）
     */
    public Map<String, String> getUITable() {
        return new HashMap<>(uiTable);
    }
    
    /**
     * 保存所有已加载的UI数据
     * 包括UI表和所有已加载的UI内容
     */
    public void saveAll() {
        try {
            // 保存UI表
            saveTable();
            
            // 保存所有已加载的UI数据
            for (Map.Entry<String, InventoryData> entry : loadedUIs.entrySet()) {
                String uiId = entry.getKey();
                InventoryData data = entry.getValue();
                
                // 从UI表中获取文件名
            if (uiTable.containsKey(uiId)) {
                String fileName = uiTable.get(uiId);
                saveUIData(fileName, data.getContents(), data.getWorldName(), data.getDisplayName(), data.getContainerType(), data.getPermission());
            }
            }
            
            debugLog("成功保存所有UI数据");
        } catch (Exception e) {
            FastGUI.getInstance().getLogger().severe("保存UI数据时出错: " + e.getMessage());
        }
    }
    
    /**
     * 槽位数据类，表示UI中单个槽位的完整信息
     */
    public static class InventorySlotData {
        private final boolean isEmpty; // 槽位是否为空
        private final ItemStack item; // 物品数据
        private final String type; // 槽位类型：normal, border, button
        private final String command; // 按钮命令（如果是按钮）
        private final boolean closeOnClick; // 点击后是否关闭界面
        private final String permission; // 所需权限
        
        /**
         * 构造函数
         */
        public InventorySlotData(boolean isEmpty, ItemStack item, String type, String command, boolean closeOnClick, String permission) {
            this.isEmpty = isEmpty;
            this.item = item;
            this.type = type;
            this.command = command;
            this.closeOnClick = closeOnClick;
            this.permission = permission;
        }
        
        public boolean isEmpty() { return isEmpty; }
        public ItemStack getItem() { return item; }
        public String getType() { return type; }
        public String getCommand() { return command; }
        public boolean isCloseOnClick() { return closeOnClick; }
        public String getPermission() { return permission; }
        
        /**
         * 检查是否是按钮
         */
        public boolean isButton() {
            return "button".equals(type) && command != null && !command.isEmpty();
        }
        
        /**
         * 检查是否是边框
         */
        public boolean isBorder() {
            return "border".equals(type);
        }
    }
    
    /**
     * 库存数据类
     * <p>封装UI的世界信息、物品内容和槽位详细数据。</p>
     */
    public static class InventoryData {
        private final String worldName; // UI关联的世界名称
        private final ItemStack[] contents; // UI物品内容数组
        private final InventorySlotData[] slotData; // 槽位详细数据
        private String displayName; // UI显示名称（可以是自定义的，支持NBT格式）
        private String containerType; // UI对应的容器类型
        private String permission; // UI权限节点（op: 仅OP可打开, np: 所有玩家可打开）
        
        /**
         * 构造函数（兼容旧版本）
         * @param worldName 世界名称
         * @param contents 物品内容数组
         */
        public InventoryData(String worldName, ItemStack[] contents) {
            this.worldName = worldName;
            this.contents = contents;
            this.displayName = null; // 默认不设置显示名称，使用ID作为显示名称
            this.containerType = "CHEST"; // 默认容器类型为箱子
            this.permission = "np"; // 默认所有玩家可打开
            // 对于旧版本，创建默认的槽位数据
            this.slotData = new InventorySlotData[contents.length];
            for (int i = 0; i < contents.length; i++) {
                ItemStack item = contents[i];
                if (item == null || item.getType().isAir()) {
                    slotData[i] = new InventorySlotData(true, null, "air", null, false, null);
                } else {
                    slotData[i] = new InventorySlotData(false, item, "normal", null, false, null);
                }
            }
        }
        
        /**
         * 构造函数（新版本）
         * @param worldName 世界名称
         * @param contents 物品内容数组
         * @param slotData 槽位详细数据
         */
        public InventoryData(String worldName, ItemStack[] contents, InventorySlotData[] slotData) {
            this.worldName = worldName;
            this.contents = contents;
            this.slotData = slotData;
            this.displayName = null; // 默认不设置显示名称，使用ID作为显示名称
            this.containerType = "CHEST"; // 默认容器类型为箱子
            this.permission = "np"; // 默认所有玩家可打开
        }
        
        /**
         * 构造函数（支持容器类型）
         * @param worldName 世界名称
         * @param contents 物品内容数组
         * @param slotData 槽位详细数据
         * @param containerType 容器类型
         */
        public InventoryData(String worldName, ItemStack[] contents, InventorySlotData[] slotData, String containerType) {
            this.worldName = worldName;
            this.contents = contents;
            this.slotData = slotData;
            this.displayName = null; // 默认不设置显示名称，使用ID作为显示名称
            this.containerType = containerType != null ? containerType : "CHEST";
            this.permission = "np"; // 默认所有玩家可打开
        }
        
        /**
         * 构造函数（支持权限节点）
         * @param worldName 世界名称
         * @param contents 物品内容数组
         * @param slotData 槽位详细数据
         * @param containerType 容器类型
         * @param permission 权限节点（op: 仅OP可打开, np: 所有玩家可打开）
         */
        public InventoryData(String worldName, ItemStack[] contents, InventorySlotData[] slotData, String containerType, String permission) {
            this.worldName = worldName;
            this.contents = contents;
            this.slotData = slotData;
            this.displayName = null; // 默认不设置显示名称，使用ID作为显示名称
            this.containerType = containerType != null ? containerType : "CHEST";
            this.permission = permission != null ? permission : "np"; // 默认所有玩家可打开
        }
        
        /**
         * 获取UI显示名称
         * @return 显示名称，如果未设置则返回null
         */
        public String getDisplayName() {
            return displayName;
        }
        
        /**
         * 设置UI显示名称
         * @param displayName 显示名称（可以是普通文本或NBT格式）
         */
        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }
        
        /**
         * 获取世界名称
         * @return 世界名称
         */
        public String getWorldName() {
            return worldName;
        }
        
        /**
         * 获取物品内容
         * @return 物品数组
         */
        public ItemStack[] getContents() {
            return contents;
        }
        
        /**
         * 获取槽位详细数据
         * @return 槽位详细数据数组
         */
        public InventorySlotData[] getSlotData() {
            return slotData;
        }
        
        /**
         * 获取指定槽位的数据
         * @param slot 槽位索引
         * @return 槽位数据，如果索引无效返回null
         */
        public InventorySlotData getSlotData(int slot) {
            if (slotData != null && slot >= 0 && slot < slotData.length) {
                return slotData[slot];
            }
            return null;
        }
        
        /**
         * 获取容器类型
         * @return 容器类型
         */
        public String getContainerType() {
            return containerType;
        }
        
        /**
         * 设置容器类型
         * @param containerType 容器类型
         */
        public void setContainerType(String containerType) {
            this.containerType = containerType;
        }
        
        /**
         * 获取权限节点
         * @return 权限节点（op: 仅OP可打开, np: 所有玩家可打开）
         */
        public String getPermission() {
            return permission;
        }
        
        /**
         * 设置权限节点
         * @param permission 权限节点（op: 仅OP可打开, np: 所有玩家可打开）
         */
        public void setPermission(String permission) {
            this.permission = permission;
        }
    }
    
    /**
     * 检查玩家是否有权限打开UI
     * @param player 玩家
     * @param uiId UI的ID
     * @return 是否有权限
     */
    public boolean hasUIPermission(Player player, String uiId) {
        // 加载UI数据
        InventoryData data = loadUI(uiId);
        if (data == null) {
            return false; // UI不存在，无权限
        }
        
        // 获取UI的权限要求
        String requiredPermission = data.getPermission();
        if (requiredPermission == null || requiredPermission.isEmpty()) {
            return true; // 没有权限要求，所有人都可以使用
        }
        
        // 检查权限
        if (requiredPermission.equalsIgnoreCase("op")) {
            // 如果要求OP权限，检查玩家是否是OP
            return player.isOp();
        }
        
        // 如果是np或空，所有人都可以使用
        return true;
    }
}