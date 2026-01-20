package me.fastgui.commands;

import me.fastgui.FastGUI;
import me.fastgui.managers.UIManager;
import me.fastgui.managers.UIManager.InventoryData;
import me.fastgui.managers.UIOpener;
import me.fastgui.managers.BookUIParser;
import me.fastgui.managers.LanguageManager;
import me.fastgui.utils.ErrorHandler;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * FGBook命令处理器，用于从书与笔创建和管理UI
 * 支持将书与笔内容转换为UI界面
 */
public class FGBookCommand implements CommandExecutor, TabCompleter {

    private final FastGUI plugin;
    private final UIManager uiManager;
    private final UIOpener uiOpener;
    private final ErrorHandler errorHandler;
    private final BookUIParser bookUIParser;
    private final LanguageManager languageManager;

    /**
     * 构造函数
     * @param plugin FastGUI插件实例
     */
    public FGBookCommand(FastGUI plugin) {
        this.plugin = plugin;
        this.uiManager = plugin.getUIManager();
        this.uiOpener = plugin.getUIOpener();
        this.errorHandler = plugin.getErrorHandler();
        this.languageManager = plugin.getLanguageManager();
        // 使用插件提供的BookUIParser实例，确保与全局系统一致
        this.bookUIParser = plugin.getBookUIParser();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        // 检查命令发送者是否为玩家
        if (!(sender instanceof Player)) {
            errorHandler.handleError(sender, languageManager.getString("fgbook.player_only"));
            return true;
        }

        Player player = (Player) sender;

        // 检查参数数量
        if (args.length < 1) {
            sendHelpMessage(player);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "open":
                handleOpenCommand(player, args);
                break;
            case "delete":
                handleDeleteCommand(player, args);
                break;
            case "list":
                handleListCommand(player);
                break;
            default:
                // 如果不是open、delete或list命令，则认为是注册命令
                handleRegisterCommand(player, args[0]);
                break;
        }

        return true;
    }

    /**
     * 处理打开UI命令
     * @param player 执行命令的玩家
     * @param args 命令参数
     */
    private void handleOpenCommand(Player player, String[] args) {
        try {
            // 验证参数数量
            if (args.length < 2) {
                errorHandler.sendInfoMessage(player, languageManager.getString("fgbook.open.usage"));
                return;
            }

            String uiId = args[1];
            
            // 从FGBook文件夹加载书籍UI数据
            InventoryData data = bookUIParser.loadBookUI(uiId);
            
            if (data == null) {
                Map<String, String> variables = new HashMap<>();
                variables.put("uiId", uiId);
                errorHandler.handleError(player, languageManager.getString("fgbook.open.not_found", variables));
                plugin.getLogger().warning("书籍UI数据不存在或加载失败: " + uiId);
                return;
            }
            
            // 从缓存中获取完整的书籍物品
            ItemStack bookItem = bookUIParser.getCachedBookItem(uiId);
            
            if (bookItem == null) {
                // 如果缓存中没有，创建一个默认的
                bookItem = new ItemStack(Material.WRITTEN_BOOK);
                BookMeta meta = (BookMeta) bookItem.getItemMeta();
                meta.setTitle("FastGUI - " + uiId);
                meta.setAuthor("FastGUI");
                meta.addPage("UI错误了喵。。。。");
                bookItem.setItemMeta(meta);
            } else if (bookItem.getType() == Material.WRITABLE_BOOK) {
                // 转换WRITABLE_BOOK为WRITTEN_BOOK
                ItemStack newBookItem = new ItemStack(Material.WRITTEN_BOOK);
                BookMeta newMeta = (BookMeta) newBookItem.getItemMeta();
                BookMeta oldMeta = (BookMeta) bookItem.getItemMeta();
                
                // 复制所有元数据
                if (oldMeta.hasTitle()) {
                    newMeta.setTitle(oldMeta.getTitle());
                }
                if (oldMeta.hasAuthor()) {
                    newMeta.setAuthor(oldMeta.getAuthor());
                }
                if (oldMeta.hasPages()) {
                    newMeta.setPages(oldMeta.getPages());
                }
                newBookItem.setItemMeta(newMeta);
                bookItem = newBookItem;
            }
            
            // 打开书与笔界面
            player.openBook(bookItem);
            
            Map<String, String> variables = new HashMap<>();
            variables.put("uiId", uiId);
            errorHandler.sendInfoMessage(player, languageManager.getString("fgbook.open.success", variables));
            if (plugin.getConfigManager().isDebugModeEnabled()) {
                plugin.getLogger().info("玩家 " + player.getName() + " 打开了书与笔UI: " + uiId);
            }
        } catch (Exception e) {
            errorHandler.handleException(player, languageManager.getString("fgbook.open.error"), e);
        }
    }

    /**
     * 处理注册UI命令
     * @param player 执行命令的玩家
     * @param uiId UI的唯一标识符
     */
    private void handleRegisterCommand(Player player, String uiId) {
        try {
            // 获取玩家主手物品
            ItemStack item = player.getInventory().getItemInMainHand();

            // 检查物品是否为书与笔
            if (item == null || item.getType() != Material.WRITABLE_BOOK && item.getType() != Material.WRITTEN_BOOK) {
                errorHandler.handleError(player, languageManager.getString("fgbook.register.hold_book"));
                return;
            }

            // 检查物品元数据
            if (!(item.getItemMeta() instanceof BookMeta)) {
                errorHandler.handleError(player, languageManager.getString("fgbook.register.invalid_book"));
                return;
            }

            BookMeta bookMeta = (BookMeta) item.getItemMeta();
            
            // 检查书是否有内容
            if (!bookMeta.hasPages()) {
                errorHandler.handleError(player, languageManager.getString("fgbook.register.empty_book"));
                return;
            }

            Map<String, String> variables = new HashMap<>();
            variables.put("uiId", uiId);
            errorHandler.sendInfoMessage(player, languageManager.getString("fgbook.register.creating", variables));
            
            // 从书与笔内容解析UI数据
            InventoryData inventoryData = bookUIParser.createInventoryData(
                    bookMeta, 
                    player.getWorld().getName(), 
                    bookMeta.hasTitle() ? bookMeta.getTitle() : uiId
            );
            
            if (inventoryData == null) {
                errorHandler.handleError(player, languageManager.getString("fgbook.register.parse_failed"));
                return;
            }
            
            // 保存书籍UI数据到FGBook文件夹
            boolean saved = bookUIParser.saveBookUI(uiId, inventoryData, item);
            
            if (saved) {
                errorHandler.sendSuccessMessage(player, languageManager.getString("fgbook.register.success", variables));
                errorHandler.sendInfoMessage(player, languageManager.getString("fgbook.register.open_command", variables));
                
                if (plugin.getConfigManager().isDebugModeEnabled()) {
                    plugin.getLogger().info("玩家 " + player.getName() + " 成功从书与笔创建了书籍UI: " + uiId);
                }
            } else {
                errorHandler.handleError(player, languageManager.getString("fgbook.register.failed"));
                
                if (plugin.getConfigManager().isDebugModeEnabled()) {
                    plugin.getLogger().warning("玩家 " + player.getName() + " 创建书籍UI失败: " + uiId);
                }
            }
            
        } catch (Exception e) {
            errorHandler.handleException(player, languageManager.getString("fgbook.register.error"), e);
        }
    }

    /**
     * 处理删除UI命令
     * @param player 执行命令的玩家
     * @param args 命令参数
     */
    private void handleDeleteCommand(Player player, String[] args) {
        try {
            // 验证参数数量
            if (args.length < 2) {
                errorHandler.sendInfoMessage(player, languageManager.getString("fgbook.delete.usage"));
                return;
            }

            String uiId = args[1];
            
            // 检查UI是否存在
            InventoryData data = bookUIParser.loadBookUI(uiId);
            if (data == null) {
                Map<String, String> variables = new HashMap<>();
                variables.put("uiId", uiId);
                errorHandler.handleError(player, languageManager.getString("fgbook.delete.not_found", variables));
                return;
            }
            
            // 执行删除操作
            boolean deleted = bookUIParser.deleteBookUI(uiId);
            
            if (deleted) {
                Map<String, String> variables = new HashMap<>();
                variables.put("uiId", uiId);
                errorHandler.sendSuccessMessage(player, languageManager.getString("fgbook.delete.success", variables));
                if (plugin.getConfigManager().isDebugModeEnabled()) {
                    plugin.getLogger().info("玩家 " + player.getName() + " 删除了书籍UI: " + uiId);
                }
            } else {
                Map<String, String> variables = new HashMap<>();
                variables.put("uiId", uiId);
                errorHandler.handleError(player, languageManager.getString("fgbook.delete.failed", variables));
                if (plugin.getConfigManager().isDebugModeEnabled()) {
                    plugin.getLogger().warning("玩家 " + player.getName() + " 删除书籍UI失败: " + uiId);
                }
            }
        } catch (Exception e) {
            errorHandler.handleException(player, languageManager.getString("fgbook.delete.error"), e);
        }
    }
    
    /**
     * 处理列出所有UI命令
     * @param player 执行命令的玩家
     */
    private void handleListCommand(Player player) {
        try {
            // 获取所有可用的书籍UI列表
            List<String> availableBookUIs = bookUIParser.getAvailableBookUIs();
            
            if (availableBookUIs.isEmpty()) {
                errorHandler.sendInfoMessage(player, languageManager.getString("fgbook.list.empty"));
                return;
            }
            
            errorHandler.sendInfoMessage(player, languageManager.getString("fgbook.list.header"));
            for (String uiId : availableBookUIs) {
                Map<String, String> variables = new HashMap<>();
                variables.put("uiId", uiId);
                errorHandler.sendInfoMessage(player, languageManager.getString("fgbook.list.item", variables));
            }
            errorHandler.sendInfoMessage(player, languageManager.getString("fgbook.list.separator"));
            Map<String, String> countVariables = new HashMap<>();
            countVariables.put("count", String.valueOf(availableBookUIs.size()));
            errorHandler.sendInfoMessage(player, languageManager.getString("fgbook.list.count", countVariables));
            
            if (plugin.getConfigManager().isDebugModeEnabled()) {
                plugin.getLogger().info("玩家 " + player.getName() + " 查看了书籍UI列表，共 " + availableBookUIs.size() + " 个UI");
            }
        } catch (Exception e) {
            errorHandler.handleException(player, languageManager.getString("fgbook.list.error"), e);
        }
    }
    
    /**
     * 发送帮助信息
     * @param sender 命令发送者
     */
    private void sendHelpMessage(CommandSender sender) {
        errorHandler.sendInfoMessage(sender, languageManager.getString("fgbook.help.header"));
        errorHandler.sendInfoMessage(sender, languageManager.getString("fgbook.help.register"));
        errorHandler.sendInfoMessage(sender, languageManager.getString("fgbook.help.open"));
        errorHandler.sendInfoMessage(sender, languageManager.getString("fgbook.help.delete"));
        errorHandler.sendInfoMessage(sender, languageManager.getString("fgbook.help.list"));
        errorHandler.sendInfoMessage(sender, languageManager.getString("fgbook.help.separator"));
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();

        // 获取所有可用的书籍UI列表
        List<String> availableBookUIs = bookUIParser.getAvailableBookUIs();

        if (args.length == 1) {
            // 第一个参数的补全选项：open, delete, list + 已注册的书籍UI列表
            completions.add("open");
            completions.add("delete");
            completions.add("list");
            completions.addAll(availableBookUIs);
        } else if (args.length == 2) {
            // 第二个参数的补全选项
            if (args[0].equalsIgnoreCase("open") || args[0].equalsIgnoreCase("delete")) {
                // open或delete命令后的补全选项：已注册的书籍UI列表
                completions.addAll(availableBookUIs);
            }
        }

        // 过滤补全列表
        return filterCompletions(completions, args[args.length - 1]);
    }

    /**
     * 过滤补全列表，只保留以输入内容开头的选项
     */
    private List<String> filterCompletions(List<String> completions, String input) {
        List<String> filtered = new ArrayList<>();
        for (String completion : completions) {
            if (completion.toLowerCase().startsWith(input.toLowerCase())) {
                filtered.add(completion);
            }
        }
        return filtered;
    }
}