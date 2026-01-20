package me.fastgui.managers;

import me.fastgui.FastGUI;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.persistence.PersistentDataType;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;

public class NBTManager {
    
    private final FastGUI plugin;
    private final LogManager logManager;
    
    // 反射相关字段
    private Class<?> craftItemStackClass;
    private Class<?> nmsItemStackClass;
    private Class<?> nbtTagCompoundClass;
    private Method asNMSCopyMethod;
    private Method asBukkitCopyMethod;
    private Method getTagMethod;
    private Method setTagMethod;
    private Method hasKeyMethod;
    private Method setStringMethod;
    private Method getStringMethod;
    private Method setBooleanMethod;
    private Method getBooleanMethod;
    private Method getHandle;
    private Method getNBTTagCompound;
    private Constructor<?> nbtTagCompoundConstructor;
    
    // PDC相关字段
    private NamespacedKey borderKey;
    private NamespacedKey buttonKey;
    private NamespacedKey commandKey;
    private NamespacedKey closeOnClickKey;
    private NamespacedKey permissionKey;
    private NamespacedKey npcKey;
    private NamespacedKey npcCommandKey;
    private NamespacedKey npcPermissionKey;
    private NamespacedKey npcExecuteModeKey;
    private NamespacedKey npcExecutorKey;
    private NamespacedKey buttonItemKey;
    private NamespacedKey buttonItemCommandKey;
    private NamespacedKey buttonItemPermissionKey;
    private NamespacedKey buttonItemExecuteModeKey;
    
    private boolean useReflection = false;
    private boolean usePersistentData = true;
    
    // NBT标签常量
    private static final String NBT_PREFIX = "fastgui_";
    public static final String BORDER_TAG = NBT_PREFIX + "border";
    public static final String BUTTON_TAG = NBT_PREFIX + "button";
    public static final String COMMAND_TAG = NBT_PREFIX + "command";
    public static final String CLOSE_ON_CLICK_TAG = NBT_PREFIX + "close_on_click";
    public static final String PERMISSION_TAG = NBT_PREFIX + "permission";
    public static final String NPC_TAG = NBT_PREFIX + "npc";
    public static final String NPC_COMMAND_TAG = NBT_PREFIX + "npc_command";
    public static final String NPC_PERMISSION_TAG = NBT_PREFIX + "npc_permission";
    public static final String NPC_EXECUTE_MODE_TAG = NBT_PREFIX + "npc_execute_mode";
    public static final String NPC_EXECUTOR_TAG = NBT_PREFIX + "npc_executor";
    public static final String BUTTON_ITEM_TAG = NBT_PREFIX + "button_item";
    public static final String BUTTON_ITEM_COMMAND_TAG = NBT_PREFIX + "button_item_command";
    public static final String BUTTON_ITEM_PERMISSION_TAG = NBT_PREFIX + "button_item_permission";
    public static final String BUTTON_ITEM_EXECUTE_MODE_TAG = NBT_PREFIX + "button_item_execute_mode";
    
    public NBTManager(FastGUI plugin) {
        this.plugin = plugin;
        this.logManager = plugin.getLogManager();
        initialize();
    }
    
    /**
     * 初始化NBT管理器
     */
    private void initialize() {
        // 尝试初始化PDC方案
        if (!initializePersistentData()) {
            logWarning("PDC方案初始化失败，尝试使用反射方案");
            // 尝试初始化反射方案
            if (!initializeReflection()) {
                logError("反射方案初始化失败，NBT功能将不可用", null);
            }
        }
    }
    
    /**
     * 重新加载配置
     */
    public void reloadConfig() {
        // 重新加载配置
        usePersistentData = plugin.getConfigManager().getBoolean("nbt.use_persistent_data", true);
        
        // 重新初始化
        initialize();
    }
    
    /**
     * 初始化PDC方案
     */
    private boolean initializePersistentData() {
        try {
            // 检查Bukkit版本是否支持PDC
            Class.forName("org.bukkit.persistence.PersistentDataContainer");
            
            // 初始化所有的NamespacedKey
            borderKey = new NamespacedKey(plugin, "border");
            buttonKey = new NamespacedKey(plugin, "button");
            commandKey = new NamespacedKey(plugin, "command");
            closeOnClickKey = new NamespacedKey(plugin, "close_on_click");
            permissionKey = new NamespacedKey(plugin, "permission");
            npcKey = new NamespacedKey(plugin, "npc");
            npcCommandKey = new NamespacedKey(plugin, "npc_command");
            npcPermissionKey = new NamespacedKey(plugin, "npc_permission");
            npcExecuteModeKey = new NamespacedKey(plugin, "npc_execute_mode");
            npcExecutorKey = new NamespacedKey(plugin, "npc_executor");
            buttonItemKey = new NamespacedKey(plugin, "button_item");
            buttonItemCommandKey = new NamespacedKey(plugin, "button_item_command");
            buttonItemPermissionKey = new NamespacedKey(plugin, "button_item_permission");
            buttonItemExecuteModeKey = new NamespacedKey(plugin, "button_item_execute_mode");
            // 方块交互相关的键已移除
            
            usePersistentData = true;
            useReflection = false;
            logInfo("PDC方案初始化成功");
            return true;
        } catch (Exception e) {
            logWarning("PDC方案初始化失败: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 初始化反射方案
     */
    private boolean initializeReflection() {
        try {
            loadReflectionMethods();
            useReflection = true;
            usePersistentData = false;
            logInfo("反射方案初始化成功");
            return true;
        } catch (Exception e) {
            logError("反射方案初始化失败", e);
            return false;
        }
    }
    
    /**
     * 加载反射所需的方法
     */
    private void loadReflectionMethods() throws Exception {
        // 获取服务器版本
        String version = getServerVersion();
        logInfo("正在加载反射方法，服务器版本: " + (version.isEmpty() ? "未知" : version));
        
        // 加载CraftItemStack类（这个通常保持不变）
        try {
            craftItemStackClass = Class.forName("org.bukkit.craftbukkit." + (version.isEmpty() ? "" : version + ".") + "inventory.CraftItemStack");
            logFine("成功加载CraftItemStack类");
        } catch (ClassNotFoundException e) {
            // 尝试不同的CraftItemStack路径
            try {
                craftItemStackClass = Class.forName("org.bukkit.craftbukkit." + (version.isEmpty() ? "" : version + ".") + "inventory.CraftItemStack");
                logFine("成功加载CraftItemStack类");
            } catch (ClassNotFoundException ex) {
                logError("无法加载CraftItemStack类", ex);
                throw ex;
            }
        }
        
        // 尝试加载NMS类，支持新版和旧版Minecraft
        boolean loadedOldVersion = false;
        try {
            // 尝试旧版路径 (1.16及以下)
            nmsItemStackClass = Class.forName("net.minecraft.server." + (version.isEmpty() ? "" : version + ".") + "ItemStack");
            nbtTagCompoundClass = Class.forName("net.minecraft.server." + (version.isEmpty() ? "" : version + ".") + "NBTTagCompound");
            logFine("成功加载旧版Minecraft类结构 (net.minecraft.server)");
            loadedOldVersion = true;
        } catch (ClassNotFoundException e) {
            logFine("尝试加载新版Minecraft类结构...");
            try {
                // 尝试新版路径 (1.17及以上)
                nmsItemStackClass = Class.forName("net.minecraft.world.item.ItemStack");
                nbtTagCompoundClass = Class.forName("net.minecraft.nbt.CompoundTag");
                logFine("成功加载新版Minecraft类结构 (net.minecraft.world.item 和 net.minecraft.nbt)");
            } catch (ClassNotFoundException ex) {
                logError("无法加载Minecraft类: " + ex.getMessage(), ex);
                throw ex; // 重新抛出异常以便上层处理
            }
        }
        
        // 加载CraftItemStack的方法
        try {
            asNMSCopyMethod = craftItemStackClass.getDeclaredMethod("asNMSCopy", ItemStack.class);
            asNMSCopyMethod.setAccessible(true);
            logFine("成功加载asNMSCopy方法");
        } catch (NoSuchMethodException e) {
            logError("无法加载asNMSCopy方法", e);
            throw e;
        }
        
        try {
            asBukkitCopyMethod = craftItemStackClass.getDeclaredMethod("asBukkitCopy", nmsItemStackClass);
            asBukkitCopyMethod.setAccessible(true);
            logFine("成功加载asBukkitCopy方法");
        } catch (NoSuchMethodException e) {
            logError("无法加载asBukkitCopy方法", e);
            throw e;
        }
        
        // 加载NMS ItemStack的方法
        String[] getTagMethodNames = {"getTag", "getNBTTagCompound", "getOrCreateTag", "getTagElement", "getOrCreateTagElement", "hasTag", "getPersistentDataContainer", "getNBT", "getCompoundTag"};
        String[] setTagMethodNames = {"setTag", "setNBTTagCompound"};
        String[] hasKeyMethodNames = {"hasKey", "hasKeyOfType", "contains"};
        String[] setStringMethodNames = {"setString"};
        String[] getStringMethodNames = {"getString", "getStringNBT", "getTagString", "get"};
        String[] setBooleanMethodNames = {"setBoolean", "putBoolean"};
        String[] getBooleanMethodNames = {"getBoolean", "getBooleanOr"};
        
        getTagMethod = findMethod(nmsItemStackClass, getTagMethodNames, "获取NBT标签的方法");
        setTagMethod = findMethod(nmsItemStackClass, setTagMethodNames, "设置NBT标签的方法", nbtTagCompoundClass);
        hasKeyMethod = findMethod(nbtTagCompoundClass, hasKeyMethodNames, "检查键是否存在的方法", String.class);
        setStringMethod = findMethod(nbtTagCompoundClass, setStringMethodNames, "设置字符串值的方法", String.class, String.class);
        getStringMethod = findMethod(nbtTagCompoundClass, getStringMethodNames, "获取字符串值的方法", String.class);
        setBooleanMethod = findMethod(nbtTagCompoundClass, setBooleanMethodNames, "设置布尔值的方法", String.class, boolean.class);
        getBooleanMethod = findMethod(nbtTagCompoundClass, getBooleanMethodNames, "获取布尔值的方法", String.class);
        
        // 加载NBTTagCompound构造器或工厂方法
        try {
            // 先尝试构造器
            nbtTagCompoundConstructor = nbtTagCompoundClass.getDeclaredConstructor();
            nbtTagCompoundConstructor.setAccessible(true);
            logFine("成功加载NBTTagCompound构造器");
        } catch (NoSuchMethodException e) {
            logFine("未找到NBTTagCompound构造器，尝试查找工厂方法");
            // 对于某些版本，可能使用静态工厂方法
            try {
                Method createMethod = nbtTagCompoundClass.getDeclaredMethod("create");
                createMethod.setAccessible(true);
                // 创建一个实例来保存
                Object nbtInstance = createMethod.invoke(null);
                nbtTagCompoundConstructor = nbtTagCompoundClass.getDeclaredConstructor();
                nbtTagCompoundConstructor.setAccessible(true);
                logFine("成功使用工厂方法创建NBTTagCompound实例");
            } catch (Exception ex) {
                logWarning("无法加载NBTTagCompound构造器或工厂方法: " + ex.getMessage());
                throw ex;
            }
        }
        
        // 尝试加载方块实体相关方法
        try {
            String worldClassName = "org.bukkit.craftbukkit." + (version.isEmpty() ? "" : version + ".") + "CraftWorld";
            Class<?> worldClass = Class.forName(worldClassName);
            getHandle = worldClass.getDeclaredMethod("getHandle");
            getHandle.setAccessible(true);
            
            // 增强方块实体反射方法加载的兼容性
            try {
                // 尝试加载获取NBT标签的方法
                String[] getNBTTagCompoundNames = {"getNBTTagCompound", "save", "getPersistentDataContainer", 
                                                "getTileData", "getBlockData", "getNbt", "getTag"};
                
                // 尝试不同的方块实体类路径和方法名
                String[] tileEntityClassNames = {
                    "net.minecraft.server." + version + ".TileEntity",
                    "net.minecraft.world.level.block.entity.TileEntity",
                    "org.bukkit.craftbukkit." + version + ".block.CraftBlockEntityState"
                };
                
                for (String className : tileEntityClassNames) {
                    try {
                        Class<?> tileEntityClass = Class.forName(className);
                        // 尝试为方块实体类加载getNBTTagCompound方法
                        for (String methodName : getNBTTagCompoundNames) {
                            try {
                                Method method = tileEntityClass.getDeclaredMethod(methodName);
                                method.setAccessible(true);
                                logFine("成功加载方块实体的" + methodName + "方法: " + className);
                                // 保存第一个找到的方法
                                if (getNBTTagCompound == null) {
                                    getNBTTagCompound = method;
                                }
                            } catch (NoSuchMethodException ignored) {
                            }
                        }
                    } catch (ClassNotFoundException ignored) {
                    }
                }
                
                if (getNBTTagCompound != null) {
                    logFine("成功初始化方块实体NBT方法");
                } else {
                    logFine("方块实体相关方法延迟加载，将在使用时动态尝试");
                }
            } catch (Exception e) {
                logFine("方块实体相关方法加载失败: " + e.getMessage());
            }
        } catch (Exception e) {
            logFine("方块实体相关方法加载失败: " + e.getMessage());
        }
    }
    
    /**
     * 查找方法，支持多个可能的方法名
     */
    private Method findMethod(Class<?> clazz, String[] methodNames, String methodDesc, Class<?>... parameterTypes) throws NoSuchMethodException {
        // 收集所有可能的方法名，包括新版Minecraft中可能的替代方法名
        String[] extendedMethodNames = extendMethodNames(methodNames, clazz.getName());
        
        for (String methodName : extendedMethodNames) {
            try {
                Method method = clazz.getDeclaredMethod(methodName, parameterTypes);
                method.setAccessible(true);
                logFine("找到方法: " + methodName + " in " + clazz.getSimpleName());
                return method;
            } catch (NoSuchMethodException e) {
                logFine("方法不存在: " + methodName);
            }
        }
        throw new NoSuchMethodException("无法找到" + methodDesc);
    }
    
    /**
     * 根据类名扩展可能的方法名列表，支持新版Minecraft的方法名变化
     */
    private String[] extendMethodNames(String[] originalNames, String className) {
        // 为新版Minecraft添加替代方法名
        if (className.contains("CompoundTag") || className.contains("NBTTagCompound")) {
            // 对于CompoundTag/NBTTagCompound，添加更多可能的方法名变体
            return mergeArrays(originalNames, new String[]{"get", "contains", "hasKeyOfType", "hasString", "getStringNBT", "getTagString"});
        } else if (className.contains("ItemStack")) {
            // 对于ItemStack，添加更多可能的方法名变体
            return mergeArrays(originalNames, new String[]{
                "getOrCreateTag", "getTagElement", "getOrCreateTagElement", 
                "hasTag", "hasTagElement", "getTagData", "getPersistentDataContainer"
            });
        } else if (className.contains("CraftBlockState")) {
            // 为方块实体添加更多可能的方法名变体
            return mergeArrays(originalNames, new String[]{"getTileEntity", "getBlockEntity", "getTileData"});
        }
        return originalNames;
    }
    
    /**
     * 合并两个字符串数组
     */
    private String[] mergeArrays(String[] first, String[] second) {
        String[] result = new String[first.length + second.length];
        System.arraycopy(first, 0, result, 0, first.length);
        System.arraycopy(second, 0, result, first.length, second.length);
        return result;
    }
    
    /**
     * 为物品添加边框属性
     */
    public boolean addBorderAttribute(ItemStack item) {
        if (!validateItem(item)) {
            return false;
        }
        
        if (usePersistentData) {
            return addBorderAttributePDC(item);
        } else {
            return addBorderAttributeReflection(item);
        }
    }
    
    /**
     * 使用PDC为物品添加边框属性
     */
    private boolean addBorderAttributePDC(ItemStack item) {
        try {
            ItemMeta meta = item.getItemMeta();
            if (meta == null) {
                logWarning("物品没有ItemMeta");
                return false;
            }
            
            meta.getPersistentDataContainer().set(borderKey, PersistentDataType.BOOLEAN, true);
            item.setItemMeta(meta);
            return true;
        } catch (Exception e) {
            logWarning("使用PDC添加边框属性失败: " + e.getMessage());
            // 切换到反射模式
            usePersistentData = false;
            useReflection = true;
            return addBorderAttributeReflection(item);
        }
    }
    
    /**
     * 使用反射为物品添加边框属性
     */
    private boolean addBorderAttributeReflection(ItemStack item) {
        try {
            // 检查并重新初始化反射方法
            if (!checkAndReinitializeReflection()) {
                return false;
            }
            
            // 确保所有必要的方法都已初始化
            if (craftItemStackClass == null || asNMSCopyMethod == null || asBukkitCopyMethod == null || 
                nmsItemStackClass == null || nbtTagCompoundClass == null || getTagMethod == null || 
                setTagMethod == null || hasKeyMethod == null || setStringMethod == null || 
                nbtTagCompoundConstructor == null) {
                logError("必要的反射方法未正确初始化", null);
                return false;
            }
            
            // 转换为NMS ItemStack
            Object nmsItemStack = asNMSCopyMethod.invoke(null, item);
            if (nmsItemStack == null) {
                logWarning("转换为NMS ItemStack失败");
                return false;
            }
            
            // 获取或创建NBT标签
            Object nbtTagCompound = getTagMethod.invoke(nmsItemStack);
            if (nbtTagCompound == null) {
                nbtTagCompound = nbtTagCompoundConstructor.newInstance();
            }
            
            // 设置标签
            setStringMethod.invoke(nbtTagCompound, BORDER_TAG, "true");
            
            // 保存NBT标签
            setTagMethod.invoke(nmsItemStack, nbtTagCompound);
            
            // 转换回Bukkit ItemStack
            ItemStack result = (ItemStack) asBukkitCopyMethod.invoke(null, nmsItemStack);
            item.setItemMeta(result.getItemMeta());
            
            return true;
        } catch (Exception e) {
            logWarning("使用反射添加边框属性失败: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 为物品添加按钮属性
     */
    public boolean addButtonAttribute(ItemStack item, String command, Boolean closeOnClick) {
        return addButtonAttribute(item, command, closeOnClick, null);
    }
    
    /**
     * 为物品添加按钮属性
     */
    public boolean addButtonAttribute(ItemStack item, String command, Boolean closeOnClick, String permission) {
        if (!validateItem(item)) {
            return false;
        }
        
        if (usePersistentData) {
            return addButtonAttributePDC(item, command, closeOnClick, permission);
        } else {
            return addButtonAttributeReflection(item, command, closeOnClick, permission);
        }
    }
    
    /**
     * 使用PDC为物品添加按钮属性
     */
    private boolean addButtonAttributePDC(ItemStack item, String command, Boolean closeOnClick, String permission) {
        try {
            ItemMeta meta = item.getItemMeta();
            if (meta == null) {
                logWarning("物品没有ItemMeta");
                return false;
            }
            
            // 设置按钮标记
            meta.getPersistentDataContainer().set(buttonKey, PersistentDataType.BOOLEAN, true);
            
            // 设置命令
            if (command != null) {
                meta.getPersistentDataContainer().set(commandKey, PersistentDataType.STRING, command);
            }
            
            // 设置点击关闭
            if (closeOnClick != null) {
                meta.getPersistentDataContainer().set(closeOnClickKey, PersistentDataType.BOOLEAN, closeOnClick);
            }
            
            // 设置权限等级
            if (permission != null && !permission.isEmpty()) {
                meta.getPersistentDataContainer().set(permissionKey, PersistentDataType.STRING, permission);
            }
            
            item.setItemMeta(meta);
            return true;
        } catch (Exception e) {
            logWarning("使用PDC添加按钮属性失败: " + e.getMessage());
            // 切换到反射模式
            usePersistentData = false;
            useReflection = true;
            return addButtonAttributeReflection(item, command, closeOnClick, permission);
        }
    }
    
    /**
     * 使用反射为物品添加按钮属性
     */
    private boolean addButtonAttributeReflection(ItemStack item, String command, Boolean closeOnClick, String permission) {
        try {
            // 检查反射方法是否已初始化
            if (craftItemStackClass == null || asNMSCopyMethod == null || asBukkitCopyMethod == null || 
                nmsItemStackClass == null || nbtTagCompoundClass == null || getTagMethod == null || 
                setTagMethod == null || hasKeyMethod == null || setStringMethod == null || 
                setBooleanMethod == null || nbtTagCompoundConstructor == null) {
                logWarning("反射方法未初始化，尝试重新初始化");
                try {
                    loadReflectionMethods();
                } catch (Exception e) {
                    logWarning("重新初始化反射方法失败: " + e.getMessage());
                    return false;
                }
            }
            
            // 转换为NMS ItemStack
            Object nmsItemStack = asNMSCopyMethod.invoke(null, item);
            if (nmsItemStack == null) {
                logWarning("转换为NMS ItemStack失败");
                return false;
            }
            
            // 获取或创建NBT标签
            Object nbtTagCompound = getTagMethod.invoke(nmsItemStack);
            if (nbtTagCompound == null) {
                nbtTagCompound = nbtTagCompoundConstructor.newInstance();
            }
            
            // 设置标签
            setStringMethod.invoke(nbtTagCompound, BUTTON_TAG, "true");
            
            // 设置命令
            if (command != null) {
                setStringMethod.invoke(nbtTagCompound, COMMAND_TAG, command);
            }
            
            // 设置点击关闭
            if (closeOnClick != null) {
                setBooleanMethod.invoke(nbtTagCompound, CLOSE_ON_CLICK_TAG, closeOnClick);
            }
            
            // 设置权限等级
            if (permission != null && !permission.isEmpty()) {
                setStringMethod.invoke(nbtTagCompound, PERMISSION_TAG, permission);
            }
            
            // 保存NBT标签
            setTagMethod.invoke(nmsItemStack, nbtTagCompound);
            
            // 转换回Bukkit ItemStack
            ItemStack result = (ItemStack) asBukkitCopyMethod.invoke(null, nmsItemStack);
            item.setItemMeta(result.getItemMeta());
            
            return true;
        } catch (Exception e) {
            logWarning("使用反射添加按钮属性失败: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 检查物品是否有边框属性
     */
    public boolean isBorderItem(ItemStack item) {
        if (!validateItem(item)) {
            return false;
        }
        
        if (usePersistentData) {
            return hasAttributePDC(item, BORDER_TAG);
        } else {
            return hasAttributeReflection(item, BORDER_TAG);
        }
    }
    
    /**
     * 检查物品是否有按钮属性
     */
    public boolean isButtonItem(ItemStack item) {
        if (!validateItem(item)) {
            return false;
        }
        
        if (usePersistentData) {
            return hasAttributePDC(item, BUTTON_ITEM_TAG);
        } else {
            return hasAttributeReflection(item, BUTTON_ITEM_TAG);
        }
    }
    
    /**
     * 使用PDC检查物品是否有指定属性
     */
    private boolean hasAttributePDC(ItemStack item, String key) {
        try {
            ItemMeta meta = item.getItemMeta();
            if (meta == null) {
                return false;
            }
            
            NamespacedKey namespacedKey;
            if (key.equals(BORDER_TAG)) {
                namespacedKey = borderKey;
            } else if (key.equals(BUTTON_TAG)) {
                namespacedKey = buttonKey;
            } else if (key.equals(NPC_TAG)) {
                namespacedKey = npcKey;
            } else if (key.equals(BUTTON_ITEM_TAG)) {
                namespacedKey = buttonItemKey;
            } else {
                return false;
            }
            
            return meta.getPersistentDataContainer().has(namespacedKey, PersistentDataType.BOOLEAN);
        } catch (Exception e) {
            logWarning("使用PDC检查属性失败: " + e.getMessage());
            // 切换到反射模式
            usePersistentData = false;
            useReflection = true;
            return hasAttributeReflection(item, key);
        }
    }
    
    /**
     * 使用反射检查物品是否有指定属性
     */
    private boolean hasAttributeReflection(ItemStack item, String key) {
        try {
            // 检查反射方法是否已初始化
            if (craftItemStackClass == null || asNMSCopyMethod == null || 
                nmsItemStackClass == null || getTagMethod == null || 
                hasKeyMethod == null) {
                logWarning("反射方法未初始化，尝试重新初始化");
                try {
                    loadReflectionMethods();
                } catch (Exception e) {
                    logWarning("重新初始化反射方法失败: " + e.getMessage());
                    return false;
                }
            }
            
            // 转换为NMS ItemStack
            Object nmsItemStack = asNMSCopyMethod.invoke(null, item);
            if (nmsItemStack == null) {
                return false;
            }
            
            // 获取NBT标签
            Object nbtTagCompound = getTagMethod.invoke(nmsItemStack);
            if (nbtTagCompound == null) {
                return false;
            }
            
            // 检查是否有指定键
            return (boolean) hasKeyMethod.invoke(nbtTagCompound, key);
        } catch (Exception e) {
            logWarning("使用反射检查属性失败: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 获取按钮命令
     */
    public String getButtonCommand(ItemStack item) {
        if (!validateItem(item)) {
            return null;
        }
        
        if (usePersistentData) {
            return getStringAttributePDC(item, COMMAND_TAG);
        } else {
            return getStringAttributeReflection(item, COMMAND_TAG);
        }
    }
    
    /**
     * 获取按钮权限要求
     */
    public String getButtonPermission(ItemStack item) {
        if (!validateItem(item)) {
            return null;
        }
        
        if (usePersistentData) {
            return getStringAttributePDC(item, PERMISSION_TAG);
        } else {
            return getStringAttributeReflection(item, PERMISSION_TAG);
        }
    }
    
    /**
     * 获取ButtonItem权限要求
     */
    public String getButtonItemPermission(ItemStack item) {
        if (!validateItem(item)) {
            return null;
        }
        
        if (usePersistentData) {
            return getStringAttributePDC(item, BUTTON_ITEM_PERMISSION_TAG);
        } else {
            return getStringAttributeReflection(item, BUTTON_ITEM_PERMISSION_TAG);
        }
    }
    
    /**
     * 获取点击关闭设置
     */
    public Boolean getCloseOnClick(ItemStack item) {
        if (!validateItem(item)) {
            return null;
        }
        
        if (usePersistentData) {
            try {
                ItemMeta meta = item.getItemMeta();
                if (meta == null) {
                    return null;
                }
                
                if (meta.getPersistentDataContainer().has(closeOnClickKey, PersistentDataType.BOOLEAN)) {
                    return meta.getPersistentDataContainer().get(closeOnClickKey, PersistentDataType.BOOLEAN);
                }
                return null;
            } catch (Exception e) {
                logWarning("使用PDC获取点击关闭设置失败: " + e.getMessage());
                // 切换到反射模式
                usePersistentData = false;
                useReflection = true;
            }
        }
        
        // 使用反射获取
        try {
            // 检查反射方法是否已初始化
            if (craftItemStackClass == null || asNMSCopyMethod == null || 
                nmsItemStackClass == null || getTagMethod == null || 
                hasKeyMethod == null || getBooleanMethod == null) {
                logWarning("反射方法未初始化，尝试重新初始化");
                try {
                    loadReflectionMethods();
                } catch (Exception e) {
                    logWarning("重新初始化反射方法失败: " + e.getMessage());
                    return null;
                }
            }
            
            // 转换为NMS ItemStack
            Object nmsItemStack = asNMSCopyMethod.invoke(null, item);
            if (nmsItemStack == null) {
                return null;
            }
            
            // 获取NBT标签
            Object nbtTagCompound = getTagMethod.invoke(nmsItemStack);
            if (nbtTagCompound == null) {
                return null;
            }
            
            // 检查是否有指定键
            if ((boolean) hasKeyMethod.invoke(nbtTagCompound, CLOSE_ON_CLICK_TAG)) {
                return (boolean) getBooleanMethod.invoke(nbtTagCompound, CLOSE_ON_CLICK_TAG);
            }
            return null;
        } catch (Exception e) {
            logWarning("使用反射获取点击关闭设置失败: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * 使用PDC获取字符串属性
     */
    private String getStringAttributePDC(ItemStack item, String key) {
        try {
            ItemMeta meta = item.getItemMeta();
            if (meta == null) {
                logWarning("getStringAttributePDC: 物品没有ItemMeta");
                return null;
            }
            
            NamespacedKey namespacedKey;
            if (key.equals(COMMAND_TAG)) {
                namespacedKey = commandKey;
            } else if (key.equals(NPC_COMMAND_TAG)) {
                namespacedKey = npcCommandKey;
            } else if (key.equals(BUTTON_ITEM_COMMAND_TAG)) {
                namespacedKey = buttonItemCommandKey;
            } else if (key.equals(BUTTON_ITEM_PERMISSION_TAG)) {
                namespacedKey = buttonItemPermissionKey;
            } else if (key.equals(NPC_PERMISSION_TAG)) {
                namespacedKey = npcPermissionKey;
            } else if (key.equals(NPC_EXECUTE_MODE_TAG)) {
                namespacedKey = npcExecuteModeKey;
            } else if (key.equals(NPC_EXECUTOR_TAG)) {
                namespacedKey = npcExecutorKey;
            } else {
                // 检查是否使用的是常量名称而不是键对象
                if (key.equals("fastgui_command")) {
                    namespacedKey = commandKey;
                } else if (key.equals("fastgui_npc_command")) {
                    namespacedKey = npcCommandKey;
                } else if (key.equals("fastgui_button_item_command")) {
                    namespacedKey = buttonItemCommandKey;
                } else if (key.equals("fastgui_button_item_permission")) {
                    namespacedKey = buttonItemPermissionKey;
                } else if (key.equals("fastgui_npc_permission")) {
                    namespacedKey = npcPermissionKey;
                } else if (key.equals("fastgui_npc_execute_mode")) {
                    namespacedKey = npcExecuteModeKey;
                } else if (key.equals("fastgui_npc_executor")) {
                    namespacedKey = npcExecutorKey;
                } else {
                    logWarning("getStringAttributePDC: 未知的key: " + key);
                    return null;
                }
            }
            
            if (FastGUI.getInstance().getConfigManager().isDebugModeEnabled()) {
                logInfo("getStringAttributePDC: key=" + key + ", namespacedKey=" + namespacedKey);
            }
            
            if (meta.getPersistentDataContainer().has(namespacedKey, PersistentDataType.STRING)) {
                String result = meta.getPersistentDataContainer().get(namespacedKey, PersistentDataType.STRING);
                if (FastGUI.getInstance().getConfigManager().isDebugModeEnabled()) {
                    logInfo("getStringAttributePDC: 找到属性 " + key + " = " + result);
                }
                return result;
            } else {
                if (FastGUI.getInstance().getConfigManager().isDebugModeEnabled()) {
                    logInfo("getStringAttributePDC: 未找到属性 " + key);
                }
                return null;
            }
        } catch (Exception e) {
            logWarning("使用PDC获取字符串属性失败: " + e.getMessage());
            // 切换到反射模式
            usePersistentData = false;
            useReflection = true;
            return getStringAttributeReflection(item, key);
        }
    }
    
    /**
     * 使用反射获取字符串属性
     */
    private String getStringAttributeReflection(ItemStack item, String key) {
        try {
            // 检查反射方法是否已初始化
            if (craftItemStackClass == null || asNMSCopyMethod == null || 
                nmsItemStackClass == null || getTagMethod == null || 
                hasKeyMethod == null || getStringMethod == null) {
                logWarning("反射方法未初始化，尝试重新初始化");
                try {
                    loadReflectionMethods();
                } catch (Exception e) {
                    logWarning("重新初始化反射方法失败: " + e.getMessage());
                    return null;
                }
            }
            
            // 转换为NMS ItemStack
            Object nmsItemStack = asNMSCopyMethod.invoke(null, item);
            if (nmsItemStack == null) {
                return null;
            }
            
            // 获取NBT标签
            Object nbtTagCompound = getTagMethod.invoke(nmsItemStack);
            if (nbtTagCompound == null) {
                return null;
            }
            
            // 检查是否有指定键
            if ((boolean) hasKeyMethod.invoke(nbtTagCompound, key)) {
                return (String) getStringMethod.invoke(nbtTagCompound, key);
            }
            return null;
        } catch (Exception e) {
            logWarning("使用反射获取字符串属性失败: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * 验证物品是否有效
     */
    private boolean validateItem(ItemStack item) {
        return item != null && !item.getType().isAir();
    }
    
    /**
     * 为物品添加按钮属性（简化版）
     */
    public boolean addButtonAttribute(ItemStack item, String command) {
        return addButtonAttribute(item, command, null);
    }
    
    /**
     * 检查物品是否有NPC属性
     */
    public boolean isNPCItem(ItemStack item) {
        if (!validateItem(item)) {
            return false;
        }
        
        if (usePersistentData) {
            return hasAttributePDC(item, NPC_TAG);
        } else {
            return hasAttributeReflection(item, NPC_TAG);
        }
    }
    
    /**
     * 为物品添加NPC属性
     */
    public boolean addNPCAttribute(ItemStack item, String command, String permission, String executeMode, String executor) {
        if (!validateItem(item)) {
            return false;
        }
        
        if (usePersistentData) {
            return addNPCAttributePDC(item, command, permission, executeMode, executor);
        } else {
            return addNPCAttributeReflection(item, command, permission, executeMode, executor);
        }
    }
    
    /**
     * 使用PDC为物品添加NPC属性
     */
    private boolean addNPCAttributePDC(ItemStack item, String command, String permission, String executeMode, String executor) {
        try {
            ItemMeta meta = item.getItemMeta();
            if (meta == null) {
                logWarning("物品没有ItemMeta");
                return false;
            }
            
            // 调试信息：显示传入的参数
            if (FastGUI.getInstance().getConfigManager().isDebugModeEnabled()) {
                logInfo("addNPCAttributePDC 参数: command=" + command + ", permission=" + permission + ", executeMode=" + executeMode + ", executor=" + executor);
            }
            
            // 设置NPC标记
            meta.getPersistentDataContainer().set(npcKey, PersistentDataType.BOOLEAN, true);
            
            // 设置命令
            if (command != null) {
                meta.getPersistentDataContainer().set(npcCommandKey, PersistentDataType.STRING, command);
                if (FastGUI.getInstance().getConfigManager().isDebugModeEnabled()) {
                    logInfo("设置NPC命令: " + command);
                }
            }
            
            // 设置权限等级
            if (permission != null && !permission.isEmpty()) {
                meta.getPersistentDataContainer().set(npcPermissionKey, PersistentDataType.STRING, permission);
                if (FastGUI.getInstance().getConfigManager().isDebugModeEnabled()) {
                    logInfo("设置NPC权限: " + permission);
                }
            } else {
                if (FastGUI.getInstance().getConfigManager().isDebugModeEnabled()) {
                    logInfo("跳过设置NPC权限: permission为空或null");
                }
            }
            
            // 设置执行模式
            if (executeMode != null) {
                meta.getPersistentDataContainer().set(npcExecuteModeKey, PersistentDataType.STRING, executeMode);
                if (FastGUI.getInstance().getConfigManager().isDebugModeEnabled()) {
                    logInfo("设置NPC执行模式: " + executeMode);
                }
            }
            
            // 设置执行体
            if (executor != null) {
                meta.getPersistentDataContainer().set(npcExecutorKey, PersistentDataType.STRING, executor);
                if (FastGUI.getInstance().getConfigManager().isDebugModeEnabled()) {
                    logInfo("设置NPC执行体: " + executor);
                }
            }
            
            item.setItemMeta(meta);
            if (FastGUI.getInstance().getConfigManager().isDebugModeEnabled()) {
                logInfo("addNPCAttributePDC 完成");
            }
            return true;
        } catch (Exception e) {
            logWarning("使用PDC添加NPC属性失败: " + e.getMessage());
            // 切换到反射模式
            usePersistentData = false;
            useReflection = true;
            return addNPCAttributeReflection(item, command, permission, executeMode, executor);
        }
    }
    
    /**
     * 使用反射为物品添加NPC属性
     */
    private boolean addNPCAttributeReflection(ItemStack item, String command, String permission, String executeMode, String executor) {
        try {
            // 检查反射方法是否已初始化
            if (craftItemStackClass == null || asNMSCopyMethod == null || asBukkitCopyMethod == null || 
                nmsItemStackClass == null || nbtTagCompoundClass == null || getTagMethod == null || 
                setTagMethod == null || setStringMethod == null || nbtTagCompoundConstructor == null) {
                logWarning("反射方法未初始化，尝试重新初始化");
                try {
                    loadReflectionMethods();
                } catch (Exception e) {
                    logWarning("重新初始化反射方法失败: " + e.getMessage());
                    return false;
                }
            }
            
            // 转换为NMS ItemStack
            Object nmsItemStack = asNMSCopyMethod.invoke(null, item);
            if (nmsItemStack == null) {
                logWarning("转换为NMS ItemStack失败");
                return false;
            }
            
            // 获取或创建NBT标签
            Object nbtTagCompound = getTagMethod.invoke(nmsItemStack);
            if (nbtTagCompound == null) {
                nbtTagCompound = nbtTagCompoundConstructor.newInstance();
            }
            
            // 设置标签
            setStringMethod.invoke(nbtTagCompound, NPC_TAG, "true");
            
            // 设置命令
            if (command != null) {
                setStringMethod.invoke(nbtTagCompound, NPC_COMMAND_TAG, command);
            }
            
            // 设置权限等级
            if (permission != null && !permission.isEmpty()) {
                setStringMethod.invoke(nbtTagCompound, NPC_PERMISSION_TAG, permission);
            }
            
            // 设置执行模式
            if (executeMode != null) {
                setStringMethod.invoke(nbtTagCompound, NPC_EXECUTE_MODE_TAG, executeMode);
            }
            
            // 设置执行体
            if (executor != null) {
                setStringMethod.invoke(nbtTagCompound, NPC_EXECUTOR_TAG, executor);
            }
            
            // 保存NBT标签
            setTagMethod.invoke(nmsItemStack, nbtTagCompound);
            
            // 转换回Bukkit ItemStack
            ItemStack result = (ItemStack) asBukkitCopyMethod.invoke(null, nmsItemStack);
            item.setItemMeta(result.getItemMeta());
            
            return true;
        } catch (Exception e) {
            logWarning("使用反射添加NPC属性失败: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 获取NPC命令
     */
    public String getNPCCommand(ItemStack item) {
        if (!validateItem(item)) {
            return null;
        }
        
        if (usePersistentData) {
            return getStringAttributePDC(item, NPC_COMMAND_TAG);
        } else {
            return getStringAttributeReflection(item, NPC_COMMAND_TAG);
        }
    }
    
    /**
     * 获取NPC权限要求
     */
    public String getNPCPermission(ItemStack item) {
        if (!validateItem(item)) {
            logWarning("getNPCPermission: 物品验证失败");
            return null;
        }
        
        if (usePersistentData) {
            String result = getStringAttributePDC(item, NPC_PERMISSION_TAG);
            if (FastGUI.getInstance().getConfigManager().isDebugModeEnabled()) {
                logInfo("getNPCPermission PDC调用结果: " + (result != null ? "权限: " + result : "null"));
            }
            return result;
        } else {
            String result = getStringAttributeReflection(item, NPC_PERMISSION_TAG);
            if (FastGUI.getInstance().getConfigManager().isDebugModeEnabled()) {
                logInfo("getNPCPermission 反射调用结果: " + (result != null ? "权限: " + result : "null"));
            }
            return result;
        }
    }
    
    /**
     * 获取NPC执行模式
     */
    public String getNPCExecuteMode(ItemStack item) {
        if (!validateItem(item)) {
            return null;
        }
        
        if (usePersistentData) {
            return getStringAttributePDC(item, NPC_EXECUTE_MODE_TAG);
        } else {
            return getStringAttributeReflection(item, NPC_EXECUTE_MODE_TAG);
        }
    }
    
    /**
     * 获取NPC执行体
     */
    public String getNPCExecutor(ItemStack item) {
        if (!validateItem(item)) {
            return null;
        }
        
        if (usePersistentData) {
            return getStringAttributePDC(item, NPC_EXECUTOR_TAG);
        } else {
            return getStringAttributeReflection(item, NPC_EXECUTOR_TAG);
        }
    }
    
    /**
     * 为物品添加按钮物品属性
     */
    public boolean addButtonItemAttribute(ItemStack item, String command, String permission, String executeMode) {
        if (!validateItem(item)) {
            return false;
        }
        
        if (usePersistentData) {
            return addButtonItemAttributePDC(item, command, permission, executeMode);
        } else {
            return addButtonItemAttributeReflection(item, command, permission, executeMode);
        }
    }
    
    /**
     * 使用PDC为物品添加按钮物品属性
     */
    private boolean addButtonItemAttributePDC(ItemStack item, String command, String permission, String executeMode) {
        try {
            ItemMeta meta = item.getItemMeta();
            if (meta == null) {
                logWarning("物品没有ItemMeta");
                return false;
            }
            
            // 设置按钮物品标记
            meta.getPersistentDataContainer().set(buttonItemKey, PersistentDataType.BOOLEAN, true);
            
            // 设置命令
            if (command != null) {
                meta.getPersistentDataContainer().set(buttonItemCommandKey, PersistentDataType.STRING, command);
            }
            
            // 设置权限等级
            if (permission != null && !permission.isEmpty()) {
                meta.getPersistentDataContainer().set(buttonItemPermissionKey, PersistentDataType.STRING, permission);
            }
            
            // 设置执行模式
            if (executeMode != null && !executeMode.isEmpty()) {
                meta.getPersistentDataContainer().set(buttonItemExecuteModeKey, PersistentDataType.STRING, executeMode);
            }
            
            item.setItemMeta(meta);
            return true;
        } catch (Exception e) {
            logWarning("使用PDC添加按钮物品属性失败: " + e.getMessage());
            // 切换到反射模式
            usePersistentData = false;
            useReflection = true;
            return addButtonItemAttributeReflection(item, command, permission, executeMode);
        }
    }
    
    /**
     * 使用反射为物品添加按钮物品属性
     */
    private boolean addButtonItemAttributeReflection(ItemStack item, String command, String permission, String executeMode) {
        try {
            // 检查反射方法是否已初始化
            if (craftItemStackClass == null || asNMSCopyMethod == null || asBukkitCopyMethod == null || 
                nmsItemStackClass == null || nbtTagCompoundClass == null || getTagMethod == null || 
                setTagMethod == null || setStringMethod == null || nbtTagCompoundConstructor == null) {
                logWarning("反射方法未初始化，尝试重新初始化");
                try {
                    loadReflectionMethods();
                } catch (Exception e) {
                    logWarning("重新初始化反射方法失败: " + e.getMessage());
                    return false;
                }
            }
            
            // 转换为NMS ItemStack
            Object nmsItemStack = asNMSCopyMethod.invoke(null, item);
            if (nmsItemStack == null) {
                logWarning("转换为NMS ItemStack失败");
                return false;
            }
            
            // 获取或创建NBT标签
            Object nbtTagCompound = getTagMethod.invoke(nmsItemStack);
            if (nbtTagCompound == null) {
                nbtTagCompound = nbtTagCompoundConstructor.newInstance();
            }
            
            // 设置标签
            setStringMethod.invoke(nbtTagCompound, BUTTON_ITEM_TAG, "true");
            
            // 设置命令
            if (command != null) {
                setStringMethod.invoke(nbtTagCompound, BUTTON_ITEM_COMMAND_TAG, command);
            }
            
            // 设置权限等级
            if (permission != null && !permission.isEmpty()) {
                setStringMethod.invoke(nbtTagCompound, BUTTON_ITEM_PERMISSION_TAG, permission);
            }
            
            // 设置执行模式
            if (executeMode != null && !executeMode.isEmpty()) {
                setStringMethod.invoke(nbtTagCompound, BUTTON_ITEM_EXECUTE_MODE_TAG, executeMode);
            }
            
            // 保存NBT标签
            setTagMethod.invoke(nmsItemStack, nbtTagCompound);
            
            // 转换回Bukkit ItemStack
            ItemStack result = (ItemStack) asBukkitCopyMethod.invoke(null, nmsItemStack);
            item.setItemMeta(result.getItemMeta());
            
            return true;
        } catch (Exception e) {
            logWarning("使用反射添加按钮物品属性失败: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 获取按钮物品命令
     */
    public String getButtonItemCommand(ItemStack item) {
        if (!validateItem(item)) {
            return null;
        }
        
        if (usePersistentData) {
            return getStringAttributePDC(item, BUTTON_ITEM_COMMAND_TAG);
        } else {
            return getStringAttributeReflection(item, BUTTON_ITEM_COMMAND_TAG);
        }
    }
    
    /**
     * 获取ButtonItem执行模式
     */
    public String getButtonItemExecuteMode(ItemStack item) {
        if (!validateItem(item)) {
            return null;
        }
        
        if (usePersistentData) {
            return getStringAttributePDC(item, BUTTON_ITEM_EXECUTE_MODE_TAG);
        } else {
            return getStringAttributeReflection(item, BUTTON_ITEM_EXECUTE_MODE_TAG);
        }
    }
    
    /**
     * 获取实体的NPC命令
     */
    public String getNPCCommand(Entity entity) {
        if (entity == null) {
            return null;
        }
        
        try {
            if (entity instanceof org.bukkit.persistence.PersistentDataHolder) {
                org.bukkit.persistence.PersistentDataHolder holder = (org.bukkit.persistence.PersistentDataHolder) entity;
                if (holder.getPersistentDataContainer().has(npcCommandKey, PersistentDataType.STRING)) {
                    return holder.getPersistentDataContainer().get(npcCommandKey, PersistentDataType.STRING);
                }
            }
        } catch (Exception e) {
            logWarning("获取实体NPC命令失败: " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * 获取实体的NPC权限要求
     * @param entity 实体
     * @return 权限要求，如果没有设置则返回null
     */
    public String getNPCPermission(Entity entity) {
        if (entity == null) {
            return null;
        }
        
        try {
            if (entity instanceof org.bukkit.persistence.PersistentDataHolder) {
                org.bukkit.persistence.PersistentDataHolder holder = (org.bukkit.persistence.PersistentDataHolder) entity;
                if (holder.getPersistentDataContainer().has(npcPermissionKey, PersistentDataType.STRING)) {
                    return holder.getPersistentDataContainer().get(npcPermissionKey, PersistentDataType.STRING);
                }
            }
        } catch (Exception e) {
            logWarning("获取实体NPC权限失败: " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * 获取实体的NPC执行体
     * @param entity 实体
     * @return 执行体，如果没有设置则返回null
     */
    public String getNPCExecutor(Entity entity) {
        if (entity == null) {
            return null;
        }
        
        try {
            if (entity instanceof org.bukkit.persistence.PersistentDataHolder) {
                org.bukkit.persistence.PersistentDataHolder holder = (org.bukkit.persistence.PersistentDataHolder) entity;
                if (holder.getPersistentDataContainer().has(npcExecutorKey, PersistentDataType.STRING)) {
                    return holder.getPersistentDataContainer().get(npcExecutorKey, PersistentDataType.STRING);
                }
            }
        } catch (Exception e) {
            logWarning("获取实体NPC执行体失败: " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * 获取实体的NPC执行模式
     * @param entity 实体
     * @return 执行模式，如果没有设置则返回null
     */
    public String getNPCExecuteMode(Entity entity) {
        if (entity == null) {
            return null;
        }
        
        try {
            if (entity instanceof org.bukkit.persistence.PersistentDataHolder) {
                org.bukkit.persistence.PersistentDataHolder holder = (org.bukkit.persistence.PersistentDataHolder) entity;
                if (holder.getPersistentDataContainer().has(npcExecuteModeKey, PersistentDataType.STRING)) {
                    return holder.getPersistentDataContainer().get(npcExecuteModeKey, PersistentDataType.STRING);
                }
            }
        } catch (Exception e) {
            logWarning("获取实体NPC执行模式失败: " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * 为实体添加NPC属性
     */
    public boolean addNPCAttributeToEntity(Entity entity, String command) {
        return addNPCAttributeToEntity(entity, command, null, null, null);
    }
    
    /**
     * 为实体添加NPC属性
     * @param entity 实体
     * @param command 命令
     * @param permission 权限要求
     * @return 是否成功
     */
    public boolean addNPCAttributeToEntity(Entity entity, String command, String permission) {
        return addNPCAttributeToEntity(entity, command, permission, null, null);
    }
    
    /**
     * 为实体添加NPC属性
     * @param entity 实体
     * @param command 命令
     * @param permission 权限要求
     * @param executeMode 执行模式
     * @param executor 执行体
     * @return 是否成功
     */
    public boolean addNPCAttributeToEntity(Entity entity, String command, String permission, String executeMode, String executor) {
        if (entity == null) {
            return false;
        }
        
        try {
            if (entity instanceof org.bukkit.persistence.PersistentDataHolder) {
                return addNPCAttributeToEntityPDC(entity, command, permission, executeMode, executor);
            } else {
                // 对于不支持PDC的实体，尝试使用反射
                return addNPCAttributeToEntityReflection(entity, command, permission, executeMode, executor);
            }
        } catch (Exception e) {
            logWarning("为实体添加NPC属性失败: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 使用PDC为实体添加NPC属性
     */
    private boolean addNPCAttributeToEntityPDC(Entity entity, String command) {
        return addNPCAttributeToEntityPDC(entity, command, null, null, null);
    }
    
    /**
     * 使用PDC为实体添加NPC属性
     * @param entity 实体
     * @param command 命令
     * @param permission 权限要求
     * @return 是否成功
     */
    private boolean addNPCAttributeToEntityPDC(Entity entity, String command, String permission) {
        return addNPCAttributeToEntityPDC(entity, command, permission, null, null);
    }
    
    /**
     * 使用PDC为实体添加NPC属性
     * @param entity 实体
     * @param command 命令
     * @param permission 权限要求
     * @param executeMode 执行模式
     * @param executor 执行体
     * @return 是否成功
     */
    private boolean addNPCAttributeToEntityPDC(Entity entity, String command, String permission, String executeMode, String executor) {
        try {
            org.bukkit.persistence.PersistentDataHolder holder = (org.bukkit.persistence.PersistentDataHolder) entity;
            
            // 设置NPC标记
            holder.getPersistentDataContainer().set(npcKey, PersistentDataType.BOOLEAN, true);
            
            // 设置命令
            if (command != null) {
                holder.getPersistentDataContainer().set(npcCommandKey, PersistentDataType.STRING, command);
            }
            
            // 设置权限要求
            if (permission != null && !permission.isEmpty()) {
                holder.getPersistentDataContainer().set(npcPermissionKey, PersistentDataType.STRING, permission);
            }
            
            // 设置执行模式
            if (executeMode != null) {
                holder.getPersistentDataContainer().set(npcExecuteModeKey, PersistentDataType.STRING, executeMode);
            }
            
            // 设置执行体
            if (executor != null) {
                holder.getPersistentDataContainer().set(npcExecutorKey, PersistentDataType.STRING, executor);
            }
            
            return true;
        } catch (Exception e) {
            logWarning("使用PDC为实体添加NPC属性失败: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 使用反射为实体添加NPC属性
     */
    private boolean addNPCAttributeToEntityReflection(Entity entity, String command) {
        return addNPCAttributeToEntityReflection(entity, command, null, null, null);
    }
    
    /**
     * 使用反射为实体添加NPC属性
     * @param entity 实体
     * @param command 命令
     * @param permission 权限要求
     * @return 是否成功
     */
    private boolean addNPCAttributeToEntityReflection(Entity entity, String command, String permission) {
        return addNPCAttributeToEntityReflection(entity, command, permission, null, null);
    }
    
    /**
     * 使用反射为实体添加NPC属性
     * @param entity 实体
     * @param command 命令
     * @param permission 权限要求
     * @param executeMode 执行模式
     * @param executor 执行体
     * @return 是否成功
     */
    private boolean addNPCAttributeToEntityReflection(Entity entity, String command, String permission, String executeMode, String executor) {
        try {
            // 这里需要实现反射获取实体的NBT标签并设置属性
            // 由于不同版本的实现差异较大，这里暂时返回false
            logWarning("实体不支持PDC且反射实现暂未完成");
            return false;
        } catch (Exception e) {
            logWarning("使用反射为实体添加NPC属性失败: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 创建NBTManager实例
     */
    public static NBTManager createInstance(FastGUI plugin) {
        return new NBTManager(plugin);
    }
    

    

    

    
    /**
     * 使用反射为方块添加ButtonBlock属性和命令
     */

    

    
    /**
     * 检查方块是否是ButtonBlock
     */

    
    /**
     * 获取ButtonBlock命令
     */

    
    /**
     * 获取当前使用的方案信息
     */
    public String getImplementationInfo() {
        return "NBTManager - PDC: " + usePersistentData + ", Reflection: " + useReflection; 
    }
    
    /**
     * 安全地获取服务器版本
     * @return 服务器版本字符串
     */
    private String getServerVersion() {
        try {
            // 详细日志记录开始获取版本
            logFine("开始获取服务器版本信息...");
            
            // 1. 从服务器类包名获取（标准Bukkit/Spigot/Paper方法）
            String packageName = plugin.getServer().getClass().getPackage().getName();      
            logFine("服务器类包名: " + packageName);
            
            if (packageName != null && packageName.contains(".")) {
                String[] parts = packageName.split("\\.");
                logFine("包名分割后部分数: " + parts.length);
                
                // 检查所有部分以找到版本格式
                for (int i = 0; i < parts.length; i++) {
                    logFine("包名部分[" + i + "]: " + parts[i]);
                    
                    // 匹配v1_16_R3, v1_18_2_R1等格式
                    if (parts[i].matches("v\\d+_\\d+(_\\d+)?_R\\d+")) {
                        logInfo("成功从包名获取服务器版本: " + parts[i]);
                        return parts[i];
                    }
                }
                
                // 备选方案: 检查任何包含数字的部分
                for (int i = 0; i < parts.length; i++) {
                    if (parts[i].matches(".*\\d.*")) {
                        logInfo("从包名获取备选版本: " + parts[i]);
                        return parts[i];
                    }
                }
                
                // 如果有足够的部分，返回第3部分（传统方法）
                if (parts.length >= 4) {
                    logInfo("从包名获取默认版本部分: " + parts[3]);
                    return parts[3];
                }
            }
            
            // 2. 尝试从服务器版本字符串获取
            String minecraftVersion = plugin.getServer().getVersion();
            logInfo("服务器版本字符串: " + minecraftVersion);
            
            if (minecraftVersion != null && !minecraftVersion.isEmpty()) {
                // 多种版本字符串格式的正则匹配
                String[][] patterns = {
                    {"MC: (\\d+\\.\\d+(?:\\.\\d+)?)", "标准MC版本格式"},  // 标准格式: "git-Paper-123 (MC: 1.20.1)"
                    {"\\(.*\\b(\\d+\\.\\d+(?:\\.\\d+)?)\\b.*\\)", "括号内版本"}, // 括号内任意位置
                    {"\\b(\\d+\\.\\d+(?:\\.\\d+)?)\\b", "直接版本号"} // 任何位置的版本号
                };
                
                for (String[] patternInfo : patterns) {
                    String patternStr = patternInfo[0];
                    String patternDesc = patternInfo[1];
                    
                    try {
                        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(patternStr);
                        java.util.regex.Matcher matcher = pattern.matcher(minecraftVersion);
                        
                        if (matcher.find()) {
                            String version = matcher.group(1);
                            logInfo("从" + patternDesc + "提取MC版本: " + version);
                            return version;
                        }
                    } catch (Exception e) {
                        logFine("正则模式匹配失败 (" + patternDesc + "): " + e.getMessage());
                    }
                }
            }
            
            // 3. 尝试从服务器名称获取
            String serverName = plugin.getServer().getName();
            logFine("服务器名称: " + serverName);
            
            if (serverName != null && !serverName.isEmpty()) {
                // 检查服务器名称中是否包含版本信息
                if (serverName.contains("1.16")) return "v1_16_R3";
                else if (serverName.contains("1.17")) return "v1_17_R1";
                else if (serverName.contains("1.18")) return "v1_18_R2";
                else if (serverName.contains("1.19")) return "v1_19_R3";
                else if (serverName.contains("1.20")) return "v1_20_R3";
                else if (serverName.contains("1.21")) return "v1_21_R1";
            }
            
            // 4. 尝试从Bukkit版本获取
            String bukkitVersion = plugin.getServer().getBukkitVersion();
            logFine("Bukkit版本: " + bukkitVersion);
            
            if (bukkitVersion != null && !bukkitVersion.isEmpty()) {
                // 匹配Bukkit版本格式
                java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("^(\\d+\\.\\d+(?:\\.\\d+)?)");
                java.util.regex.Matcher matcher = pattern.matcher(bukkitVersion);
                
                if (matcher.find()) {
                    String version = matcher.group(1);
                    logInfo("从Bukkit版本提取: " + version);
                    return version;
                }
            }
        } catch (Exception e) {
            logWarning("获取服务器版本时发生异常: " + e.getMessage());
            logError("版本检测详细错误", e);
        }
        
        logWarning("无法确定服务器版本，将使用通用反射方法集合");
        return "generic"; // 使用"generic"替代"unknown"，表示使用通用方法
    }
    

    
    // 日志方法
    private void logFine(String message) {
        logManager.debugLog("[NBTManager] " + message);
    }
    
    private void logInfo(String message) {
        logManager.info("[NBTManager] " + message);
    }
    
    private void logWarning(String message) {
        logManager.warning("[NBTManager] " + message);
    }
    
    private void logError(String message, Exception e) {
        logManager.severe("[NBTManager] " + message + ": " + (e != null ? e.getMessage() : "未知错误"));
        // 记录完整的堆栈跟踪，帮助调试
        if (e != null && logManager.isDebugEnabled()) {
            logManager.debugLog("[NBTManager] 堆栈跟踪:");
            for (StackTraceElement element : e.getStackTrace()) {
                logManager.debugLog("[NBTManager]     " + element.toString());
            }
        }
    }
    
    /**
     * 检查反射方法是否初始化，如未初始化则尝试重新初始化
     */
    private boolean checkAndReinitializeReflection() {
        // 检查所有关键的反射组件，包括类和方法
        if (nmsItemStackClass == null || craftItemStackClass == null || getTagMethod == null || setTagMethod == null) {
            logWarning("反射方法未完全初始化，尝试重新初始化");
            logFine("检查状态 - nmsItemStackClass: " + (nmsItemStackClass != null ? "已初始化" : "未初始化"));
            logFine("检查状态 - craftItemStackClass: " + (craftItemStackClass != null ? "已初始化" : "未初始化"));
            logFine("检查状态 - getTagMethod: " + (getTagMethod != null ? "已初始化" : "未初始化"));
            logFine("检查状态 - setTagMethod: " + (setTagMethod != null ? "已初始化" : "未初始化"));
            
            try {
                // 重置所有反射字段以确保干净的重新初始化
                resetReflectionFields();
                
                // 确保设置useReflection为true以启用反射
                useReflection = true;
                
                // 获取服务器版本信息
                String version = getServerVersion();
                logInfo("正在重新初始化反射方法，服务器版本: " + version);
                
                boolean reinitialized = initializeReflection();
                if (reinitialized) {
                    logInfo("反射方法重新初始化成功");
                    // 输出成功初始化的详细信息
                    logFine("初始化后检查 - nmsItemStackClass: " + (nmsItemStackClass != null ? "已初始化" : "未初始化"));
                    logFine("初始化后检查 - craftItemStackClass: " + (craftItemStackClass != null ? "已初始化" : "未初始化"));
                    logFine("初始化后检查 - getTagMethod: " + (getTagMethod != null ? "已初始化" : "未初始化"));
                    logFine("初始化后检查 - setTagMethod: " + (setTagMethod != null ? "已初始化" : "未初始化"));
                } else {
                    logWarning("反射方法重新初始化失败");
                    // 如果反射失败，尝试切换到PDC模式
                    if (usePersistentData) {
                        logWarning("尝试使用PersistentDataContainer替代反射方法");
                    }
                }
                return reinitialized;
            } catch (Exception e) {
                logError("重新初始化反射方法失败: " + e.getMessage(), e);
                
                // 记录更详细的错误信息以帮助诊断
                logWarning("错误详情: " + e.getClass().getName());
                if (e instanceof NoSuchMethodException) {
                    logWarning("无法找到方法，请检查服务器版本兼容性");
                } else if (e instanceof ClassNotFoundException) {
                    logWarning("无法找到类，请检查服务器版本兼容性");
                }
                
                // 尝试切换到PDC模式
                if (usePersistentData) {
                    logWarning("尝试使用PersistentDataContainer替代反射方法");
                }
            }
            return false;
        }
        return true;
    }
    
    private void resetReflectionFields() {
        // 重置所有反射字段
        craftItemStackClass = null;
        nmsItemStackClass = null;
        nbtTagCompoundClass = null;
        asNMSCopyMethod = null;
        asBukkitCopyMethod = null;
        getTagMethod = null;
        setTagMethod = null;
        hasKeyMethod = null;
        setStringMethod = null;
        getStringMethod = null;
        setBooleanMethod = null;
        getBooleanMethod = null;
        getHandle = null;
        getNBTTagCompound = null;
        nbtTagCompoundConstructor = null;
        logFine("所有反射字段已重置");
    }
}