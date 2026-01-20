package me.fastgui.commands;

import me.fastgui.FastGUI;
import me.fastgui.managers.UIManager;
import me.fastgui.managers.UIOpener;
import me.fastgui.managers.ConfigManager;
import me.fastgui.managers.LanguageManager;
import me.fastgui.utils.ErrorHandler;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Bukkit;

import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * FastGUI命令处理器，实现CommandExecutor接口
 * <p>负责处理所有与FastGUI相关的命令，包括：</p>
 * <ul>
 *   <li>添加新UI</li>
 *   <li>列出所有UI</li>
 *   <li>打开指定UI</li>
 *   <li>删除指定UI</li>
 * </ul>
 * <p>每个命令都包含完整的权限检查、参数验证和错误处理机制</p>
 */
public class FastGUICommand implements CommandExecutor, TabCompleter {
    
    private final FastGUI plugin;
    private final UIManager uiManager;
    private final UIOpener uiOpener;
    private final ErrorHandler errorHandler;
    private final ConfigManager configManager;
    private final LanguageManager languageManager;
    
    public FastGUICommand(FastGUI plugin) {
        this.plugin = plugin;
        this.uiManager = plugin.getUIManager();
        this.uiOpener = plugin.getUIOpener();
        this.errorHandler = plugin.getErrorHandler();
        this.configManager = plugin.getConfigManager();
        this.languageManager = plugin.getLanguageManager();
    }
    
    /**
     * 处理测试命令
     * <p>在指定位置生成一个大箱子，并填充模板物品（边框和按钮）</p>
     * 
     * @param player 执行命令的玩家
     * @param args 命令参数，格式：[test, x, y, z]
     */
    private void handleTestCommand(Player player, String[] args) {
        try {
            // 验证参数数量
            if (args.length < 4) {
                errorHandler.sendInfoMessage(player, languageManager.getString("fastgui.test.usage"));
                errorHandler.sendInfoMessage(player, languageManager.getString("fastgui.test.tip"));
                return;
            }
            
            // 解析坐标
            int x, y, z;
            try {
                x = Integer.parseInt(args[1]);
                y = Integer.parseInt(args[2]);
                z = Integer.parseInt(args[3]);
            } catch (NumberFormatException e) {
                errorHandler.handleError(player, languageManager.getString("fastgui.error.coordinates_must_be_numbers"));
                errorHandler.sendInfoMessage(player, languageManager.getString("fastgui.test.correct_format"));
                return;
            }
            
            // 生成测试大箱子
            boolean success = generateTestChest(player, x, y, z);
            
            if (success) {
                errorHandler.sendSuccessMessage(player, languageManager.getString("fastgui.test.template_generated"));
                errorHandler.sendSuccessMessage(player, languageManager.getString("fastgui.test.location", 
                        Map.of("x", String.valueOf(x), "y", String.valueOf(y), "z", String.valueOf(z))));
            } else {
                errorHandler.handleError(player, languageManager.getString("fastgui.test.template_generation_failed"));
            }
            
        } catch (Exception e) {
            errorHandler.handleException(player, languageManager.getString("fastgui.test.error"), e);
        }
    }
    
    /**
     * 生成测试用的大箱子，并填充模板物品
     * 
     * @param player 执行命令的玩家
     * @param x 第一个小箱子的X坐标
     * @param y 第一个小箱子的Y坐标
     * @param z 第一个小箱子的Z坐标
     * @return 是否生成成功
     */
    private boolean generateTestChest(Player player, int x, int y, int z) {
        try {
            org.bukkit.World world = player.getWorld();
            
            // 生成第一个小箱子（在目标坐标）
            Location firstChestLoc = new Location(world, x, y, z);
            world.getBlockAt(firstChestLoc).setType(Material.CHEST);
            
            // 生成第二个小箱子（向右偏移，x+1）
            Location secondChestLoc = new Location(world, x + 1, y, z);
            world.getBlockAt(secondChestLoc).setType(Material.CHEST);
            
            // 获取大箱子的库存
            Chest firstChest = (Chest) world.getBlockAt(firstChestLoc).getState();
            Chest secondChest = (Chest) world.getBlockAt(secondChestLoc).getState();
            
            // 等待大箱子形成
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                try {
                    // 重新获取大箱子的库存（确保大箱子已经形成）
                    Chest chest = (Chest) world.getBlockAt(firstChestLoc).getState();
                    Inventory inventory = chest.getInventory();
                    
                    // 清空库存
                    inventory.clear();
                    
                    // 生成并填充边框（玻璃板，带NBT标签）
                    fillBorder(inventory);
                    
                    // 生成并填充中间按钮（绿色玻璃板，带NBT标签，命令是say qwq）
                    fillButton(inventory);
                    
                    // 更新箱子状态
                    chest.update();
                    
                    if (plugin.getConfigManager().isDebugModeEnabled()) {
                        plugin.getLogger().info("测试UI模板已成功填充物品");
                    }
                } catch (Exception e) {
                    plugin.getLogger().severe("填充测试箱子物品时发生错误: " + e.getMessage());
                }
            }, 1L);
            
            return true;
        } catch (Exception e) {
            plugin.getLogger().severe("生成测试箱子时发生错误: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * 填充边框物品（玻璃板，带NBT标签）
     * 
     * @param inventory 要填充的库存
     */
    private void fillBorder(Inventory inventory) {
        // 填充第一行和最后一行（边框）
        for (int i = 0; i < 9; i++) {
            inventory.setItem(i, createBorderItem());
            inventory.setItem(i + 45, createBorderItem());
        }
        
        // 填充第一列和最后一列（边框）
        for (int i = 9; i < 45; i += 9) {
            inventory.setItem(i, createBorderItem());
            inventory.setItem(i + 8, createBorderItem());
        }
    }
    
    /**
     * 填充中间按钮（绿色玻璃板，属性为按钮，命令是say qwq）
     * 
     * @param inventory 要填充的库存
     */
    private void fillButton(Inventory inventory) {
        // 在中心位置放置按钮（第3行第5个位置，索引为22）
        inventory.setItem(22, createButtonItem("say qwq"));
    }
    
    /**
     * 创建边框物品（玻璃板）
     * 
     * @return 边框物品
     */
    private ItemStack createBorderItem() {
        ItemStack item = new ItemStack(Material.GLASS_PANE);
        org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            // 设置边框物品的显示名称
            meta.setDisplayName("边框");
            item.setItemMeta(meta);
        }
        
        // 使用NBT管理器添加边框标签
        plugin.getNBTManager().addBorderAttribute(item);
        
        return item;
    }
    
    /**
     * 创建按钮物品（绿色玻璃板）
     * 
     * @param command 按钮命令
     * @return 按钮物品
     */
    private ItemStack createButtonItem(String command) {
        ItemStack item = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
        org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            // 设置按钮物品的显示名称
            meta.setDisplayName("按钮");
            item.setItemMeta(meta);
        }
        
        // 使用NBT管理器添加按钮标签和命令
        plugin.getNBTManager().addButtonAttribute(item, command);
        
        return item;
    }
    
    /**
     * 命令执行入口方法，根据不同的子命令调用相应的处理方法
     * <p>命令格式: /fg <子命令> [参数...]</p>
     * 
     * @param sender 命令发送者（玩家或控制台）
     * @param command 命令对象
     * @param label 命令别名
     * @param args 命令参数数组
     * @return 命令执行是否成功（始终返回true，因为错误已在内部处理）
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // 处理帮助命令
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sendHelpMessage(sender);
            return true;
        }
        
        // 处理重载命令
        if (args[0].equalsIgnoreCase("reload")) {
            // 检查权限
            if (!sender.hasPermission("fastgui.reload")) {
                errorHandler.handleError(sender, languageManager.getString("fastgui.error.no_permission"));
                return true;
            }
            
            // 重新加载配置
            plugin.reloadConfig();
            
            // 重新初始化配置管理器
            plugin.getConfigManager().reloadConfig();
            
            // 重新加载语言（如果语言文件不存在会自动生成）
            plugin.getLanguageManager().reloadLanguage();
            
            // 重新初始化NBT管理器
            plugin.getNBTManager().reloadConfig();
            
            // 发送成功消息
            sender.sendMessage(languageManager.getString("fastgui.success.reload"));
            return true;
        }
        
        // 对于非控制台命令，检查是否是玩家
        if (args.length > 0 && !(sender instanceof Player) && 
            (args[0].equalsIgnoreCase("add") || args[0].equalsIgnoreCase("open"))) {
            errorHandler.handleError(sender, languageManager.getString("fastgui.error.player_only_command"));
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        // 处理不同的子命令
        switch (subCommand) {
            case "add":
                if (sender instanceof Player) {
                    handleAddCommand((Player)sender, args);
                }
                break;
            case "list":
                handleListCommand(sender);
                break;
            case "open":
                if (sender instanceof Player) {
                    handleOpenCommand((Player)sender, args);
                }
                break;
            case "delete":
                handleDeleteCommand(sender, args);
                break;
            case "test":
                if (sender instanceof Player) {
                    handleTestCommand((Player)sender, args);
                }
                break;
            case "set":
                handleSetCommand(sender, args);
                break;
            case "refresh":
                handleRefreshCommand(sender);
                break;
            case "debug":
                handleDebugCommand(sender);
                break;
            default:
                errorHandler.handleError(sender, languageManager.getString("fastgui.error.unknown_subcommand", 
                        Map.of("subcommand", subCommand)));
                sendHelpMessage(sender);
                break;
        }
        
        return true;
    }
    
    /**
     * 发送帮助信息到命令发送者
     * <p>显示所有可用的命令及其用法</p>
     * 
     * @param sender 命令发送者
     */
    private void sendHelpMessage(CommandSender sender) {
        errorHandler.sendInfoMessage(sender, languageManager.getString("fastgui.help.header"));
        errorHandler.sendInfoMessage(sender, languageManager.getString("fastgui.help.add"));
        errorHandler.sendInfoMessage(sender, languageManager.getString("fastgui.help.list"));
        errorHandler.sendInfoMessage(sender, languageManager.getString("fastgui.help.open"));
        errorHandler.sendInfoMessage(sender, languageManager.getString("fastgui.help.delete"));
        errorHandler.sendInfoMessage(sender, languageManager.getString("fastgui.help.set_id"));
        errorHandler.sendInfoMessage(sender, languageManager.getString("fastgui.help.refresh"));
        errorHandler.sendInfoMessage(sender, languageManager.getString("fastgui.help.test"));
        errorHandler.sendInfoMessage(sender, languageManager.getString("fastgui.help.debug"));
        errorHandler.sendInfoMessage(sender, languageManager.getString("fastgui.help.reload"));
        errorHandler.sendInfoMessage(sender, languageManager.getString("fastgui.help.command"));
        errorHandler.sendInfoMessage(sender, languageManager.getString("fastgui.help.separator"));
    }
    
    /**
     * 处理添加新UI的命令
     * <p>从玩家指定位置的大箱子中读取物品配置，创建新的UI界面</p>
     * 
     * @param player 执行命令的玩家
     * @param args 命令参数，格式：[add, 权限节点, x, y, z, ui名称]
     */
    private void handleAddCommand(Player player, String[] args) {
        try {
            // 验证参数数量
            if (args.length < 6) {
                errorHandler.sendInfoMessage(player, languageManager.getString("fastgui.add.usage"));
                errorHandler.sendInfoMessage(player, languageManager.getString("fastgui.add.tip"));
                return;
            }
            
            // 解析权限节点
            String permission = args[1].toLowerCase();
            if (!permission.equals("op") && !permission.equals("np")) {
                errorHandler.handleError(player, languageManager.getString("fastgui.error.invalid_permission"));
                return;
            }
            
            // 解析坐标
            int x, y, z;
            try {
                x = Integer.parseInt(args[2]);
                y = Integer.parseInt(args[3]);
                z = Integer.parseInt(args[4]);
            } catch (NumberFormatException e) {
                errorHandler.handleError(player, languageManager.getString("fastgui.error.coordinates_must_be_numbers"));
                errorHandler.sendInfoMessage(player, languageManager.getString("fastgui.add.correct_format"));
                return;
            }
            
            String name;
            try {
                // UI名称
                name = args[5];
                
                // 验证名称
                if (name.trim().isEmpty()) {
                    errorHandler.handleError(player, languageManager.getString("fastgui.error.name_cannot_be_empty"));
                    return;
                }
                
                // 获取指定坐标的方块
                Block block = player.getWorld().getBlockAt(x, y, z);
                Material blockType = block.getType();
                
                // 检查是否是支持的容器类型
                boolean isSupportedContainer = false;
                
                // 只支持特定容器类型：小箱子、大箱子、发射器（兼容投掷器）
                if (blockType == Material.CHEST || 
                    blockType == Material.TRAPPED_CHEST || 
                    blockType == Material.DISPENSER || 
                    blockType == Material.DROPPER) {
                    isSupportedContainer = true;
                }
                
                if (!isSupportedContainer) {
                    errorHandler.handleError(player, languageManager.getString("fastgui.error.not_supported_container"));
                    return;
                }
                
                // 获取容器的物品栏
                InventoryHolder inventoryHolder = (InventoryHolder) block.getState();
                Inventory inventory = inventoryHolder.getInventory();
                
                // 通过NBT验证箱子中的按钮物品
                String id;
                try {
                    // 获取容器类型并调用addUI方法
                    String containerType = blockType.name();
                    id = uiManager.addUI(name, inventory.getContents(), player.getWorld().getName(), containerType, permission);
                    errorHandler.sendSuccessMessage(player, languageManager.getString("fastgui.add.ui_created_successfully"));
                    errorHandler.sendSuccessMessage(player, languageManager.getString("fastgui.add.ui_id", 
                            Map.of("id", id)));
                    errorHandler.sendSuccessMessage(player, languageManager.getString("fastgui.add.ui_open_command", 
                            Map.of("id", id)));
                    
                    // 对于发射器和投掷器，提示技术限制
                    if (blockType == Material.DISPENSER || blockType == Material.DROPPER) {
                        errorHandler.sendInfoMessage(player, languageManager.getString("fastgui.add.dispenser_technical_limit"));
                    }
                } catch (Exception e) {
                    errorHandler.handleError(player, languageManager.getString("fastgui.error.ui_creation_failed", 
                            Map.of("error", e.getMessage())));
                    return;
                }
            } catch (Exception e) {
                errorHandler.handleException(player, languageManager.getString("fastgui.add.error_processing_command"), e);
            }
            
        } catch (Exception e) {
            errorHandler.handleException(player, languageManager.getString("fastgui.add.error_creating_ui"), e);
        }
    }
    
    /**
     * 移除字符串中的颜色代码
     */
    private String removeColorCodes(String text) {
        if (text == null) return null;
        return text.replaceAll("(?i)§[0-9a-fk-or]", "");
    }
    
    /**
     * 处理列出所有UI的命令
     * <p>显示所有已注册的UI界面</p>
     * 
     * @param sender 命令发送者（可以是玩家或控制台）
     */
    private void handleListCommand(CommandSender sender) {
        try {
            Map<String, String> uiTable = uiManager.getUITable();
            
            if (uiTable.isEmpty()) {
                errorHandler.sendInfoMessage(sender, languageManager.getString("fastgui.list.empty"));
                errorHandler.sendInfoMessage(sender, languageManager.getString("fastgui.list.create_tip"));
                return;
            }
            
            errorHandler.sendInfoMessage(sender, languageManager.getString("fastgui.list.header"));
            errorHandler.sendInfoMessage(sender, languageManager.getString("fastgui.list.total", 
                    Map.of("count", String.valueOf(uiTable.size()))));
            errorHandler.sendInfoMessage(sender, "");
            
            for (Map.Entry<String, String> entry : uiTable.entrySet()) {
                String name = entry.getValue().replace(".dat", "");
                errorHandler.sendInfoMessage(sender, languageManager.getString("fastgui.list.name", 
                        Map.of("name", name, "id", entry.getKey())));
            }
            
            errorHandler.sendInfoMessage(sender, "");
            errorHandler.sendInfoMessage(sender, languageManager.getString("fastgui.list.open_tip"));
            errorHandler.sendInfoMessage(sender, languageManager.getString("fastgui.list.delete_tip"));
            errorHandler.sendInfoMessage(sender, languageManager.getString("fastgui.list.footer"));
        } catch (Exception e) {
            errorHandler.handleException(sender, languageManager.getString("fastgui.list.error_getting_list"), e);
        }
    }
    
    /**
     * 处理打开UI的命令
     * <p>为玩家打开指定名称的UI界面</p>
     * 
     * @param player 执行命令的玩家
     * @param args 命令参数，格式：[open, uiName]
     */
    private void handleOpenCommand(Player player, String[] args) {
        try {
            // 验证参数数量
            if (args.length < 2) {
                errorHandler.sendInfoMessage(player, languageManager.getString("fastgui.open.usage"));
                errorHandler.sendInfoMessage(player, languageManager.getString("fastgui.open.list_tip"));
                return;
            }
            
            String uiName = args[1];
            
            // 验证名称
            if (!errorHandler.validateNotEmpty(uiName, "UI名称", player)) {
                return;
            }
            
            // 使用UIOpener打开UI
            if (uiOpener.openUI(player, uiName)) {
                errorHandler.sendSuccessMessage(player, languageManager.getString("fastgui.open.ui_opened", 
                        Map.of("ui_name", uiName)));
            } else {
                errorHandler.handleError(player, languageManager.getString("fastgui.error.ui_not_found", 
                        Map.of("ui_name", uiName)));
                errorHandler.sendInfoMessage(player, languageManager.getString("fastgui.open.check_name"));
            }
            
        } catch (Exception e) {
            errorHandler.handleException(player, languageManager.getString("fastgui.open.error_opening_ui"), e);
        }
    }
    
    /**
     * 处理删除UI的命令
     * <p>删除指定名称的UI界面及其所有相关文件</p>
     * 
     * @param sender 命令发送者（可以是玩家或控制台）
     * @param args 命令参数，格式：[delete, uiName]
     */
    private void handleDeleteCommand(CommandSender sender, String[] args) {
        try {
            // 验证参数数量
            if (args.length < 2) {
                errorHandler.sendInfoMessage(sender, languageManager.getString("fastgui.delete.usage"));
                errorHandler.sendInfoMessage(sender, languageManager.getString("fastgui.delete.list_tip"));
                return;
            }
            
            String uiName = args[1];
            
            // 验证名称
            if (!errorHandler.validateNotEmpty(uiName, "UI名称", sender)) {
                return;
            }
            
            // 由于我们已经将UI名称作为ID，直接使用名称删除
            if (uiManager.deleteUI(uiName)) {
                errorHandler.sendSuccessMessage(sender, languageManager.getString("fastgui.delete.ui_deleted_successfully"));
                errorHandler.sendSuccessMessage(sender, languageManager.getString("fastgui.delete.ui_deleted", 
                        Map.of("ui_name", uiName)));
                if (plugin.getConfigManager().isDebugModeEnabled()) {
                    plugin.getLogger().info(languageManager.getString("fastgui.delete.debug_ui_deleted", 
                            Map.of("ui_name", uiName)));
                }
            } else {
                errorHandler.handleError(sender, languageManager.getString("fastgui.error.ui_not_found", 
                        Map.of("ui_name", uiName)));
                errorHandler.sendInfoMessage(sender, languageManager.getString("fastgui.delete.check_name"));
            }
            
        } catch (Exception e) {
            errorHandler.handleException(sender, languageManager.getString("fastgui.delete.error_deleting_ui"), e);
        }
    }
    
    /**
     * 处理调试模式控制命令
     * <p>启用或禁用调试日志输出</p>
     * 
     * @param sender 命令发送者
     * @param args 命令参数，格式：[debug, <on/off/toggle>]
     */
    /**
     * 处理设置UI属性的命令
     * <p>支持修改UI的ID和界面显示ID</p>
     * 
     * @param sender 命令发送者
     * @param args 命令参数，格式：[set, uiID, property, value]
     */
    private void handleSetCommand(CommandSender sender, String[] args) {
        try {
            // 验证参数数量
            if (args.length < 4) {
                errorHandler.sendInfoMessage(sender, languageManager.getString("fastgui.set.usage"));
                return;
            }
            
            // 验证操作类型（目前只支持id）
            String operation = args[1].toLowerCase();
            if (!operation.equals("id")) {
                errorHandler.handleError(sender, languageManager.getString("fastgui.set.unknown_property", 
                        Map.of("property", operation)));
                errorHandler.sendInfoMessage(sender, languageManager.getString("fastgui.set.available_properties"));
                return;
            }
            
            String oldName = args[2];
            String newName = args[3];
            
            // 验证UI是否存在
            if (!uiManager.getUITable().containsKey(oldName)) {
                errorHandler.handleError(sender, languageManager.getString("fastgui.error.ui_not_found", 
                        Map.of("ui_name", oldName)));
                return;
            }
            
            // 调用修改ID的命令
            handleSetIDCommand(sender, oldName, newName);
            
        } catch (Exception e) {
            errorHandler.handleException(sender, languageManager.getString("fastgui.set.error_setting_property"), e);
        }
    }
    
    /**
     * 处理修改UI ID的命令
     */
    private void handleSetIDCommand(CommandSender sender, String oldId, String newId) {
        try {
            // 验证新ID
            if (!errorHandler.validateNotEmpty(newId, "新ID", sender)) {
                return;
            }
            
            // 检查新ID是否已存在
            if (uiManager.getUITable().containsKey(newId)) {
                errorHandler.handleError(sender, languageManager.getString("fastgui.set.id_already_exists", 
                        Map.of("new_id", newId)));
                return;
            }
            
            // 执行重命名
            if (uiManager.renameUI(oldId, newId)) {
                errorHandler.sendSuccessMessage(sender, languageManager.getString("fastgui.set.id_renamed_successfully"));
                errorHandler.sendSuccessMessage(sender, languageManager.getString("fastgui.set.id_renamed", 
                        Map.of("old_id", oldId, "new_id", newId)));
                if (plugin.getConfigManager().isDebugModeEnabled()) {
                    plugin.getLogger().info(languageManager.getString("fastgui.set.debug_id_renamed", 
                            Map.of("sender", sender.getName(), "old_id", oldId, "new_id", newId)));
                }
            } else {
                errorHandler.handleError(sender, languageManager.getString("fastgui.set.id_rename_failed"));
            }
            
        } catch (Exception e) {
            errorHandler.handleException(sender, languageManager.getString("fastgui.set.error_modifying_id"), e);
        }
    }
    
    /**
     * 处理修改UI显示名称的命令
     */
    private void handleSetDisplayCommand(CommandSender sender, String uiName, String displayName) {
        try {
            // 检查UI是否存在
            if (!uiManager.getUITable().containsKey(uiName)) {
                errorHandler.handleError(sender, languageManager.getString("fastgui.error.ui_not_found", 
                        Map.of("ui_name", uiName)));
                return;
            }
            
            // 更新UI显示名称
            boolean success = uiManager.updateUIDisplayName(uiName, displayName);
            
            if (success) {
                errorHandler.sendSuccessMessage(sender, languageManager.getString("fastgui.set.display_updated_successfully", 
                        Map.of("ui_name", uiName)));
                errorHandler.sendSuccessMessage(sender, languageManager.getString("fastgui.set.new_display_name", 
                        Map.of("display_name", displayName)));
                if (plugin.getConfigManager().isDebugModeEnabled()) {
                    plugin.getLogger().info(languageManager.getString("fastgui.set.debug_display_updated", 
                            Map.of("sender", sender.getName(), "ui_name", uiName, "display_name", displayName)));
                }
            } else {
                errorHandler.handleError(sender, languageManager.getString("fastgui.set.display_update_failed"));
            }
            
        } catch (Exception e) {
            errorHandler.handleException(sender, languageManager.getString("fastgui.set.error_modifying_display"), e);
        }
    }
    
    /**
     * 处理刷新UI缓存的命令
     * <p>清空已加载UI的所有缓存，强制下次访问时从文件重新加载</p>
     * 
     * @param sender 命令发送者
     */
    private void handleRefreshCommand(CommandSender sender) {
        try {
            // 获取清空前已加载的UI数量
            int loadedCount = uiManager.getLoadedUIsCount();
            
            // 清空UI缓存
            uiManager.clearCache();
            
            errorHandler.sendSuccessMessage(sender, languageManager.getString("fastgui.refresh.cache_cleared", 
                    Map.of("count", String.valueOf(loadedCount))));
            
            if (plugin.getConfigManager().isDebugModeEnabled()) {
                plugin.getLogger().info(languageManager.getString("fastgui.refresh.debug_cache_cleared", 
                        Map.of("sender", sender.getName(), "count", String.valueOf(loadedCount))));
            }
            
        } catch (Exception e) {
            errorHandler.handleException(sender, languageManager.getString("fastgui.refresh.error_clearing_cache"), e);
        }
    }
    
    private void handleDebugCommand(CommandSender sender) {
        try {
            boolean currentState = configManager.isDebugModeEnabled();
            boolean newState = !currentState;
            
            configManager.setDebugModeEnabled(newState);
            
            if (newState) {
                errorHandler.sendSuccessMessage(sender, languageManager.getString("fastgui.debug.enabled"));
                plugin.getLogger().info(languageManager.getString("fastgui.debug.debug_enabled_log", 
                        Map.of("sender", sender.getName())));
            } else {
                errorHandler.sendSuccessMessage(sender, languageManager.getString("fastgui.debug.disabled"));
                plugin.getLogger().info(languageManager.getString("fastgui.debug.debug_disabled_log", 
                        Map.of("sender", sender.getName())));
            }
            
        } catch (Exception e) {
            errorHandler.handleException(sender, languageManager.getString("fastgui.debug.error_controlling_debug"), e);
        }
    }

    /**
     * 处理命令的tab补全
     * <p>为FastGUI的所有子命令提供tab补全支持</p>
     * 
     * @param sender 命令发送者
     * @param command 命令
     * @param alias 命令别名
     * @param args 命令参数
     * @return 补全建议列表
     */
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            // 第一个参数是子命令
            List<String> subCommands = Arrays.asList("add", "list", "open", "delete", "test", "set", "refresh", "debug", "reload", "command");
            String partial = args[0].toLowerCase();
            
            for (String subCommand : subCommands) {
                if (subCommand.startsWith(partial)) {
                    completions.add(subCommand);
                }
            }
        } else if (args.length == 2) {
            // 第二个参数
            String subCommand = args[0].toLowerCase();
            
            switch (subCommand) {
                case "add":
                    // /fg add <权限节点> ...
                    List<String> permissions = Arrays.asList("op", "np");
                    String partialPermission = args[1].toLowerCase();
                    
                    for (String permission : permissions) {
                        if (permission.startsWith(partialPermission)) {
                            completions.add(permission);
                        }
                    }
                    break;
                case "open":
                case "delete":
                    // /fg open <ui名称> 或 /fg delete <ui名称>
                    completions.addAll(uiManager.getUIList());
                    break;
                case "set":
                    // /fg set id <源UI名> <要改的名字>
                    String partial = args[1].toLowerCase();
                    if ("id".startsWith(partial)) {
                        completions.add("id");
                    }
                    break;
            }
        } else if (args.length == 3) {
            // 第三个参数
            String subCommand = args[0].toLowerCase();
            
            if (subCommand.equals("set")) {
                // /fg set id <源UI名> ...
                // 补全源UI名称
                completions.addAll(uiManager.getUIList());
            } else if (subCommand.equals("open") || subCommand.equals("delete")) {
                // 这些命令只有两个参数，所以不提供补全
            }
        }
        
        return completions;
    }
}