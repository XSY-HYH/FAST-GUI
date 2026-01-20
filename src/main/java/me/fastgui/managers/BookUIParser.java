package me.fastgui.managers;

import me.fastgui.FastGUI;
import me.fastgui.utils.UIItemParser.UIButton;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.Color;
import org.bukkit.enchantments.Enchantment;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 书与笔UI解析器
 * 负责从书与笔内容解析UI布局和按钮信息
 */
public class BookUIParser {

    private final FastGUI plugin;
    private final UIParser uiParser;
    private final NBTManager nbtManager;
    private final LogManager logManager;
    private final File fgBookFolder; // 书籍UI数据主文件夹

    // 解析模式：每行格式为 "槽位:物品ID:数量:数据值:显示名称:命令:关闭:权限"
    private static final Pattern ITEM_PATTERN = Pattern.compile(
            "(\\d+):([A-Z_]+)(?::(\\d+))?(?::(\\d+))?(?::(.+?))?(?::(.*?))?(?::(true|false))?(?::(.+?))?");

    /**
     * 构造函数
     * @param plugin FastGUI插件实例
     */
    public BookUIParser(FastGUI plugin) {
        this.plugin = plugin;
        this.uiParser = new UIParser(plugin, plugin.getConfigManager(), plugin.getLogManager());
        this.nbtManager = plugin.getNBTManager();
        this.logManager = plugin.getLogManager();
        // 初始化FGBook文件夹
        this.fgBookFolder = new File(FastGUI.getInstance().getDataFolder(), "FGBook");
        // 确保文件夹存在
        if (!fgBookFolder.exists() && !fgBookFolder.mkdirs()) {
            logManager.warning("无法创建FGBook文件夹: " + fgBookFolder.getPath());
        }
    }

    /**
     * 从书与笔中解析UI数据
     * @param bookMeta 书与笔的元数据
     * @return 解析出的UI物品数组
     */
    public ItemStack[] parseBookUI(BookMeta bookMeta) {
        // 创建一个54格的物品数组（大箱子大小）
        ItemStack[] contents = new ItemStack[54];

        if (bookMeta == null || !bookMeta.hasPages()) {
            logManager.warning("书与笔没有内容，无法解析UI");
            return contents;
        }

        int lineNumber = 0;
        int validItems = 0;

        try {
            // 遍历所有页面
            for (String page : bookMeta.getPages()) {
                // 按行分割内容
                String[] lines = page.split("\\n");
                
                for (String line : lines) {
                    lineNumber++;
                    line = line.trim();
                    
                    // 跳过空行和注释行
                    if (line.isEmpty() || line.startsWith("#") || line.startsWith("//")) {
                        continue;
                    }

                    // 尝试解析物品行
                    ItemStack item = parseItemLine(line, lineNumber);
                    if (item != null) {
                        // 获取槽位信息
                        Matcher matcher = ITEM_PATTERN.matcher(line);
                        if (matcher.matches()) {
                            try {
                                int slot = Integer.parseInt(matcher.group(1));
                                if (slot >= 0 && slot < contents.length) {
                                    contents[slot] = item;
                                    validItems++;
                                } else {
                                    logManager.warning("第" + lineNumber + "行: 槽位超出范围 (" + slot + ")");
                                }
                            } catch (NumberFormatException e) {
                                logManager.warning("第" + lineNumber + "行: 无效的槽位号");
                            }
                        }
                    }
                }
            }

            logManager.debugLog("成功解析 " + validItems + " 个物品到UI中");
        } catch (Exception e) {
            logManager.severe("解析书与笔UI时出错: " + e.getMessage());
            e.printStackTrace();
        }

        return contents;
    }

    /**
     * 解析单行物品定义
     * @param line 物品定义行
     * @param lineNumber 行号（用于错误报告）
     * @return 解析出的物品，如果解析失败则返回null
     */
    private ItemStack parseItemLine(String line, int lineNumber) {
        Matcher matcher = ITEM_PATTERN.matcher(line);
        
        if (!matcher.matches()) {
            logManager.warning("第" + lineNumber + "行: 无效的物品格式: " + line);
            return null;
        }

        try {
            // 获取物品ID
            String materialName = matcher.group(2);
            Material material = Material.matchMaterial(materialName);
            
            if (material == null) {
                logManager.warning("第" + lineNumber + "行: 未知的物品类型: " + materialName);
                return null;
            }

            // 获取数量（默认为1）
            int amount = 1;
            if (matcher.group(3) != null) {
                amount = Integer.parseInt(matcher.group(3));
                // 确保数量在有效范围内
                amount = Math.min(Math.max(1, amount), material.getMaxStackSize());
            }

            // 创建物品
            ItemStack item = new ItemStack(material, amount);
            ItemMeta meta = item.getItemMeta();

            // 处理皮革装备颜色（如果是皮革装备）
            if (matcher.group(4) != null && meta instanceof LeatherArmorMeta) {
                try {
                    int colorCode = Integer.parseInt(matcher.group(4));
                    LeatherArmorMeta leatherMeta = (LeatherArmorMeta) meta;
                    // 将整数转换为RGB颜色
                    int r = (colorCode >> 16) & 0xFF;
                    int g = (colorCode >> 8) & 0xFF;
                    int b = colorCode & 0xFF;
                    leatherMeta.setColor(Color.fromRGB(r, g, b));
                } catch (NumberFormatException e) {
                    logManager.warning("第" + lineNumber + "行: 无效的颜色代码");
                }
            }

            // 设置显示名称
            if (matcher.group(5) != null) {
                String displayName = matcher.group(5);
                // 替换颜色代码
                displayName = replaceColorCodes(displayName);
                meta.setDisplayName(displayName);
            }

            // 创建lore列表
            List<String> lore = new ArrayList<>();

            // 添加按钮信息到lore（如果有命令）
            if (matcher.group(6) != null) {
                String command = matcher.group(6);
                // 确保命令有斜杠前缀
                if (!command.startsWith("/")) {
                    command = "/" + command;
                }
                
                lore.add("§7FastGUI Button");
                lore.add("§7Command: " + command);
                
                // 设置closeOnClick属性
                boolean closeOnClick = false;
                if (matcher.group(7) != null) {
                    closeOnClick = "true".equalsIgnoreCase(matcher.group(7));
                }
                lore.add("§7CloseOnClick: " + closeOnClick);
                
                // 设置权限
                if (matcher.group(8) != null) {
                    String permission = matcher.group(8);
                    lore.add("§7Permission: " + permission);
                    
                    // 为按钮添加NBT属性
                    nbtManager.addButtonAttribute(item, command, closeOnClick);
                } else {
                    // 为按钮添加NBT属性（无权限）
                    nbtManager.addButtonAttribute(item, command, closeOnClick);
                }
            } else {
                // 检查是否是边框物品
                if (isBorderMaterial(material)) {
                    // 为边框物品添加NBT属性
                    nbtManager.addBorderAttribute(item);
                    lore.add("§7FastGUI Border");
                }
            }

            // 如果有lore，设置到物品上
            if (!lore.isEmpty()) {
                meta.setLore(lore);
            }

            // 设置元数据到物品
            item.setItemMeta(meta);

            return item;
        } catch (Exception e) {
            logManager.warning("第" + lineNumber + "行: 解析物品时出错: " + e.getMessage());
            return null;
        }
    }

    /**
     * 检查物品是否为常用的边框材料
     * @param material 物品类型
     * @return 如果是边框材料返回true
     */
    private boolean isBorderMaterial(Material material) {
        String typeName = material.name();
        return typeName.contains("STAINED_GLASS_PANE") ||
               typeName.contains("GLASS_PANE") ||
               typeName.contains("BARRIER") ||
               typeName.contains("BEDROCK");
    }

    /**
     * 替换颜色代码
     * @param text 文本
     * @return 替换后的文本
     */
    private String replaceColorCodes(String text) {
        // 替换&颜色代码为§
        return text.replace('&', '§');
    }

    /**
     * 从书与笔创建InventoryData对象
     * @param bookMeta 书与笔的元数据
     * @param worldName 世界名称
     * @param displayName UI显示名称
     * @return 创建的InventoryData对象
     */
    public UIManager.InventoryData createInventoryData(BookMeta bookMeta, String worldName, String displayName) {
        // 解析物品数组
        ItemStack[] contents = parseBookUI(bookMeta);
        
        // 生成槽位数据
        UIManager.InventorySlotData[] slotData = generateSlotData(contents);
        
        // 创建InventoryData对象
        UIManager.InventoryData data = new UIManager.InventoryData(worldName, contents, slotData);
        
        // 设置显示名称
        if (displayName != null) {
            data.setDisplayName(displayName);
        }
        
        return data;
    }

    /**
     * 为物品数组生成槽位数据
     * @param contents 物品数组
     * @return 槽位数据数组
     */
    /**
     * 保存书籍UI数据到FGBook文件夹
     * @param uiId UI的唯一标识符
     * @param data 要保存的InventoryData
     * @param bookItem 玩家手持的书籍物品
     * @return 是否保存成功
     */
    public boolean saveBookUI(String uiId, UIManager.InventoryData data, ItemStack bookItem) {
        try {
            // 创建UI特定的文件夹
            File uiFolder = new File(fgBookFolder, uiId);
            if (!uiFolder.exists() && !uiFolder.mkdirs()) {
                logManager.warning("无法创建UI文件夹: " + uiFolder.getPath());
                return false;
            }
            
            // 创建数据文件
            File dataFile = new File(uiFolder, uiId + ".dat");
            
            // 保存数据（使用与UIManager类似的格式）
            try (DataOutputStream dos = new DataOutputStream(new FileOutputStream(dataFile))) {
                // 保存世界名称
                dos.writeUTF(data.getWorldName());
                
                // 保存书籍的完整NBT数据
                if (bookItem != null && bookItem.getType() != null && bookItem.getType().name().contains("BOOK")) {
                    dos.writeBoolean(true); // 标记存在书籍数据
                    try {
                        byte[] bookNBTData = bookItem.serializeAsBytes();
                        dos.writeInt(bookNBTData.length);
                        dos.write(bookNBTData);
                        logManager.debugLog("成功序列化书籍NBT数据: " + bookItem.getType().name());
                    } catch (Exception e) {
                        logManager.warning("序列化书籍NBT数据失败: " + e.getMessage());
                        dos.writeInt(0); // 保存空数据作为回退
                    }
                } else {
                    dos.writeBoolean(false); // 标记不存在书籍数据
                }
                
                // 保存显示名称
                if (data.getDisplayName() != null) {
                    dos.writeBoolean(true);
                    dos.writeUTF(data.getDisplayName());
                } else {
                    dos.writeBoolean(false);
                }
                
                // 保存物品数组
                ItemStack[] contents = data.getContents();
                if (contents != null) {
                    dos.writeInt(contents.length);
                    for (int i = 0; i < contents.length; i++) {
                        ItemStack item = contents[i];
                        if (item != null && item.getType() != null && !item.getType().isAir()) {
                            dos.writeBoolean(false); // 标记为非空
                            try {
                                // 使用Bukkit提供的serializeAsBytes方法保存物品完整的NBT数据
                                byte[] itemData = item.serializeAsBytes();
                                dos.writeInt(itemData.length);
                                dos.write(itemData);
                                logManager.debugLog("成功序列化物品 (槽位 " + i + "): " + item.getType().name());
                            } catch (Exception e) {
                                logManager.warning("序列化物品失败 (槽位 " + i + "): " + e.getMessage());
                                // 如果序列化失败，使用空物品作为回退
                                dos.writeInt(0);
                            }
                        } else {
                            dos.writeBoolean(true); // 标记为空
                        }
                    }
                } else {
                    dos.writeInt(0);
                }
                
                // 保存槽位数据数组
                UIManager.InventorySlotData[] slotData = data.getSlotData();
                if (slotData != null) {
                    dos.writeInt(slotData.length);
                    for (int i = 0; i < slotData.length; i++) {
                        UIManager.InventorySlotData slot = slotData[i];
                        
                        // 保存槽位基本信息
                        dos.writeBoolean(slot.isEmpty());
                        dos.writeUTF(slot.getType());
                        
                        // 如果不是空槽位且是按钮，保存按钮信息
                        if (!slot.isEmpty() && slot.isButton()) {
                            dos.writeBoolean(true);
                            dos.writeUTF(slot.getCommand());
                            dos.writeBoolean(slot.isCloseOnClick());
                            dos.writeUTF(slot.getPermission() != null ? slot.getPermission() : "");
                        } else {
                            dos.writeBoolean(false);
                        }
                    }
                } else {
                    dos.writeInt(0);
                }
            }
            
            logManager.debugLog("成功保存书籍UI: " + uiId + " 到 " + dataFile.getPath());
            return true;
        } catch (IOException e) {
            logManager.severe("保存书籍UI时出错 (名称: " + uiId + "): " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * 从FGBook文件夹加载书籍UI数据
     * @param uiId UI的唯一标识符
     * @return 加载的InventoryData，如果加载失败则返回null
     */
    /**
     * 兼容旧版本的保存方法
     */
    public boolean saveBookUI(String uiId, UIManager.InventoryData data) {
        return saveBookUI(uiId, data, null);
    }
    
    // 保存加载的书籍物品缓存
    private final Map<String, ItemStack> cachedBookItems = new HashMap<>();
    
    public UIManager.InventoryData loadBookUI(String uiId) {
        try {
            // 创建数据文件路径
            File uiFolder = new File(fgBookFolder, uiId);
            File dataFile = new File(uiFolder, uiId + ".dat");
            
            // 检查文件是否存在
            if (!dataFile.exists()) {
                logManager.warning("书籍UI文件不存在: " + dataFile.getPath());
                return null;
            }
            
            // 清空之前的书籍缓存
            cachedBookItems.remove(uiId);
            
            // 加载数据
            DataInputStream dis = null;
            try {
                dis = new DataInputStream(new FileInputStream(dataFile));
                
                // 读取世界名称
                String worldName = dis.readUTF();
                
                // 尝试读取书籍NBT数据（新格式）
                ItemStack bookItem = null;
                boolean newFormat = true;
                
                try {
                    boolean hasBookData = dis.readBoolean();
                    if (hasBookData) {
                        int dataLength = dis.readInt();
                        if (dataLength > 0) {
                            byte[] bookNBTData = new byte[dataLength];
                            dis.readFully(bookNBTData);
                            bookItem = ItemStack.deserializeBytes(bookNBTData);
                            logManager.debugLog("成功加载书籍NBT数据: " + (bookItem != null ? bookItem.getType().name() : "null"));
                            // 缓存书籍物品
                            if (bookItem != null && bookItem.getType() != null && !bookItem.getType().isAir()) {
                                cachedBookItems.put(uiId, bookItem.clone());
                            }
                        }
                    }
                } catch (Exception e) {
                    // 如果读取书籍数据失败，可能是旧版本文件，重置文件指针尝试读取旧格式
                    logManager.debugLog("读取书籍NBT数据失败，可能是旧版本文件: " + e.getMessage());
                    // 关闭当前流
                    if (dis != null) {
                        try {
                            dis.close();
                        } catch (IOException ignored) {}
                    }
                    // 重新打开文件流
                    dis = new DataInputStream(new FileInputStream(dataFile));
                    // 重新读取世界名称
                    worldName = dis.readUTF();
                    newFormat = false;
                }
                
                // 读取显示名称
                String displayName = null;
                if (dis.readBoolean()) {
                    displayName = dis.readUTF();
                }
                
                // 读取物品数组
                int contentsLength = dis.readInt();
                ItemStack[] contents = new ItemStack[contentsLength];
                for (int i = 0; i < contentsLength; i++) {
                    if (!dis.readBoolean()) { // 非空物品
                        try {
                            // 读取物品数据长度
                            int dataLength = dis.readInt();
                            if (dataLength > 0) {
                                // 读取物品数据字节数组
                                byte[] itemData = new byte[dataLength];
                                dis.readFully(itemData);
                                
                                // 使用Bukkit提供的deserializeBytes方法加载完整的物品数据和NBT
                                ItemStack deserializedItem = ItemStack.deserializeBytes(itemData);
                                if (deserializedItem != null && deserializedItem.getType() != null && !deserializedItem.getType().isAir()) {
                                    contents[i] = deserializedItem;
                                    logManager.debugLog("成功加载物品 (槽位 " + i + "): " + deserializedItem.getType().name());
                                } else {
                                    contents[i] = null;
                                    logManager.warning("加载的物品数据无效 (槽位 " + i + ")");
                                }
                            } else {
                                contents[i] = null;
                            }
                        } catch (Exception e) {
                            logManager.warning("加载物品失败 (槽位 " + i + "): " + e.getMessage());
                            contents[i] = null;
                        }
                    } else { // 空物品
                        contents[i] = null;
                    }
                }
                
                // 读取槽位数据数组
                int slotDataLength = dis.readInt();
                UIManager.InventorySlotData[] slotData = new UIManager.InventorySlotData[slotDataLength];
                for (int i = 0; i < slotDataLength; i++) {
                    boolean isEmpty = dis.readBoolean();
                    String type = dis.readUTF();
                    
                    ItemStack item = isEmpty ? null : (i < contentsLength ? contents[i] : null);
                    String command = null;
                    boolean closeOnClick = false;
                    String permission = null;
                    
                    // 读取是否有按钮信息（与保存格式保持一致）
                    boolean hasButtonInfo = dis.readBoolean();
                    
                    // 如果有按钮信息，读取按钮详细数据
                    if (hasButtonInfo) {
                        command = dis.readUTF();
                        closeOnClick = dis.readBoolean();
                        permission = dis.readUTF();
                    }
                    
                    slotData[i] = new UIManager.InventorySlotData(
                            isEmpty, item, type, command, closeOnClick, permission
                    );
                }
                
                // 创建并返回InventoryData对象
                UIManager.InventoryData data = new UIManager.InventoryData(worldName, contents, slotData);
                if (displayName != null) {
                    data.setDisplayName(displayName);
                }
                
                logManager.debugLog("成功加载书籍UI: " + uiId + " 从 " + dataFile.getPath());
                return data;
            } finally {
                // 确保关闭流
                if (dis != null) {
                    try {
                        dis.close();
                    } catch (IOException ignored) {}
                }
            }
        } catch (IOException e) {
            logManager.severe("加载书籍UI时出错 (名称: " + uiId + "): " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * 获取保存的书籍物品
     * @param uiId UI的唯一标识符
     * @return 保存的书籍物品，如果不存在则返回null
     */
    public ItemStack getCachedBookItem(String uiId) {
        ItemStack bookItem = cachedBookItems.get(uiId);
        if (bookItem != null) {
            return bookItem.clone(); // 返回克隆以避免修改缓存
        }
        return null;
    }
    
    /**
     * 获取所有可用的书籍UI ID列表
     * @return 书籍UI ID列表
     */
    public List<String> getAvailableBookUIs() {
        List<String> uiIds = new ArrayList<>();
        
        if (!fgBookFolder.exists() || !fgBookFolder.isDirectory()) {
            return uiIds;
        }
        
        File[] uiFolders = fgBookFolder.listFiles(File::isDirectory);
        if (uiFolders != null) {
            for (File uiFolder : uiFolders) {
                String uiId = uiFolder.getName();
                File dataFile = new File(uiFolder, uiId + ".dat");
                if (dataFile.exists()) {
                    uiIds.add(uiId);
                }
            }
        }
        
        return uiIds;
    }
    
    /**
     * 删除指定的书籍UI
     * @param uiId UI的唯一标识符
     * @return 是否删除成功
     */
    public boolean deleteBookUI(String uiId) {
        try {
            File uiFolder = new File(fgBookFolder, uiId);
            File dataFile = new File(uiFolder, uiId + ".dat");
            
            boolean fileDeleted = true;
            if (dataFile.exists() && !dataFile.delete()) {
                logManager.warning("无法删除书籍UI数据文件: " + dataFile.getPath());
                fileDeleted = false;
            }
            
            boolean folderDeleted = true;
            if (uiFolder.exists()) {
                // 尝试删除空文件夹
                if (uiFolder.list().length == 0 && !uiFolder.delete()) {
                    logManager.warning("无法删除书籍UI文件夹: " + uiFolder.getPath());
                    folderDeleted = false;
                }
            }
            
            logManager.debugLog("成功删除书籍UI: " + uiId);
            return fileDeleted && folderDeleted;
        } catch (Exception e) {
            logManager.severe("删除书籍UI时出错 (名称: " + uiId + "): " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * 为物品数组生成槽位数据
     * @param contents 物品数组
     * @return 槽位数据数组
     */
    private UIManager.InventorySlotData[] generateSlotData(ItemStack[] contents) {
        if (contents == null) {
            return new UIManager.InventorySlotData[0];
        }
        
        UIManager.InventorySlotData[] slotData = new UIManager.InventorySlotData[contents.length];
        
        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            
            if (item == null || item.getType().isAir()) {
                // 空槽位
                slotData[i] = new UIManager.InventorySlotData(true, null, "air", null, false, null);
                continue;
            }
            
            try {
                // 检查物品类型
                String type = "normal";
                String command = null;
                boolean closeOnClick = false;
                String permission = "";
                
                // 通过NBT检查物品类型
                if (nbtManager.isBorderItem(item)) {
                    type = "border";
                } else if (nbtManager.isButtonItem(item)) {
                    type = "button";
                    command = nbtManager.getButtonCommand(item);
                    Boolean closeOnClickBoolean = nbtManager.getCloseOnClick(item);
                    closeOnClick = closeOnClickBoolean != null ? closeOnClickBoolean : false;
                }
                
                // 创建槽位数据
                slotData[i] = new UIManager.InventorySlotData(
                        false, item, type, command, closeOnClick, permission
                );
            } catch (Exception e) {
                logManager.warning("生成槽位数据时出错 (槽位 " + i + "): " + e.getMessage());
                // 创建默认槽位数据
                slotData[i] = new UIManager.InventorySlotData(false, item, "normal", null, false, null);
            }
        }
        
        return slotData;
    }
}