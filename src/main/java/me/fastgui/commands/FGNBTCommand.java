package me.fastgui.commands;

import me.fastgui.FastGUI;
import me.fastgui.managers.NBTManager;
import me.fastgui.managers.LanguageManager;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * FGNBT命令处理器，用于管理UI物品的NBT属性
 * 支持添加Border和Button标签到手持物品
 */
public class FGNBTCommand implements CommandExecutor, TabCompleter {

    private final FastGUI plugin;
    private final NBTManager nbtManager;
    private final LanguageManager languageManager;

    /**
     * 构造函数
     * @param plugin FastGUI插件实例
     */
    public FGNBTCommand(FastGUI plugin) {
        this.plugin = plugin;
        this.nbtManager = plugin.getNBTManager();
        this.languageManager = plugin.getLanguageManager();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        // 检查命令发送者是否为玩家
        if (!(sender instanceof Player)) {
            sender.sendMessage(languageManager.getString("general.player_only"));
            return true;
        }

        Player player = (Player) sender;
        ItemStack item = player.getInventory().getItemInMainHand();

        // 检查参数数量
        if (args.length < 1) {
            sendHelpMessage(player);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "border":
                handleBorderCommand(player);
                break;
            case "button":
                handleButtonCommand(player, args);
                break;
            case "npc":
                handleNPCCommand(player, args);
                break;
            case "buttonitem":
                handleButtonItemCommand(player, args);
                break;
            // buttonblock 子命令已移除，因为方块交互功能已不再使用
            default:
                sendHelpMessage(player);
                break;
        }

        return true;
    }

    /**
     * 处理Border子命令，给手持物品添加Border标签
     * @param player 执行命令的玩家
     */
    private void handleBorderCommand(Player player) {
        try {
            ItemStack item = player.getInventory().getItemInMainHand();

            if (item == null || item.getType().isAir()) {
                player.sendMessage(languageManager.getString("fgnbt.hold_item"));
                return;
            }

            // 检查物品是否已有Button属性
            boolean hadButton = nbtManager.isButtonItem(item);
            String oldCommand = hadButton ? nbtManager.getButtonCommand(item) : null;
            Boolean closeOnClick = hadButton ? nbtManager.getCloseOnClick(item) : null;

            // 添加Border标签
            boolean success = nbtManager.addBorderAttribute(item);

            if (success) {
                // 如果之前有Button属性，保留这些属性
                if (hadButton && oldCommand != null) {
                    nbtManager.addButtonAttribute(item, oldCommand, 
                            closeOnClick != null ? closeOnClick : false);
                }
                
                player.getInventory().setItemInMainHand(item);
                if (hadButton && oldCommand != null) {
                    player.sendMessage(languageManager.getString("fgnbt.border_set"));
                    player.sendMessage(languageManager.getString("fgnbt.button_preserved", 
                            Map.of("command", oldCommand)));
                    if (plugin.getConfigManager().isDebugModeEnabled()) {
                        plugin.getLogger().info("玩家 " + player.getName() + " 为物品添加了Border标签，并保留了Button属性");
                    }
                } else {
                    player.sendMessage(languageManager.getString("fgnbt.border_set"));
                    if (plugin.getConfigManager().isDebugModeEnabled()) {
                        plugin.getLogger().info("玩家 " + player.getName() + " 为物品添加了Border标签");
                    }
                }
            } else {
                player.sendMessage(languageManager.getString("error.save_failed", 
                        Map.of("error", "添加Border标签失败")));
            }
        } catch (Exception e) {
            player.sendMessage(languageManager.getString("general.error_occurred", 
                    Map.of("error", e.getMessage())));
            plugin.getLogger().severe("设置Border标签时出错: " + e.getMessage());
        }
    }

    /**
     * 处理Button子命令，给手持物品添加Button和command标签
     * @param player 执行命令的玩家
     * @param args 命令参数
     */
    /**
     * 处理NPC子命令，给手持的生物刷怪蛋添加NPC标签和命令
     * @param player 执行命令的玩家
     * @param args 命令参数
     */
    private void handleNPCCommand(Player player, String[] args) {
        try {
            // 验证参数
            if (args.length < 2) {
                player.sendMessage(languageManager.getString("fgnbt.usage.npc"));
                return;
            }

            ItemStack item = player.getInventory().getItemInMainHand();

            if (item == null || item.getType().isAir()) {
                player.sendMessage(languageManager.getString("fgnbt.hold_item"));
                return;
            }

            // 检查是否是生物刷怪蛋
            if (!item.getType().name().endsWith("_SPAWN_EGG") && 
                !item.getType().name().equals("SPAWN_EGG")) {
                player.sendMessage(languageManager.getString("fgnbt.need_spawn_egg"));
                return;
            }

            // 解析权限、执行模式和执行体
            String permission = "op"; // 默认op权限（只有op玩家可交互）
            String executeMode = "op"; // 默认op模式（控制台执行）
            String executor = "控制台"; // 默认控制台执行
            int commandStartIndex = 1;
            int executorIndex = -1;
            
            // 检查是否有权限参数
            if (args[1].equalsIgnoreCase("op") || args[1].equalsIgnoreCase("np")) {
                permission = args[1].toLowerCase();
                executeMode = args[1].toLowerCase(); // 设置执行模式与权限参数一致
                commandStartIndex = 2;
                
                // 查找执行体参数
                for (int i = commandStartIndex; i < args.length; i++) {
                    if ((args[i].equalsIgnoreCase("玩家") || args[i].equalsIgnoreCase("player") || 
                         args[i].equalsIgnoreCase("控制台") || args[i].equalsIgnoreCase("console")) && 
                        !args[i].startsWith("-p")) {
                        executorIndex = i;
                        executor = args[i].toLowerCase();
                        break;
                    }
                }
                
                if (executorIndex == -1) {
                    player.sendMessage(languageManager.getString("fgnbt.executor_not_found"));
                    return;
                }
            } else {
                // 没有权限参数，直接查找执行体
                for (int i = 1; i < args.length; i++) {
                    if ((args[i].equalsIgnoreCase("玩家") || args[i].equalsIgnoreCase("player") || 
                         args[i].equalsIgnoreCase("控制台") || args[i].equalsIgnoreCase("console")) && 
                        !args[i].startsWith("-p")) {
                        executorIndex = i;
                        executor = args[i].toLowerCase();
                        break;
                    }
                }
                
                if (executorIndex == -1) {
                    player.sendMessage(languageManager.getString("fgnbt.executor_not_found"));
                    return;
                }
            }
            
            // 构建命令字符串（处理引号内的内容）
            StringBuilder commandBuilder = new StringBuilder();
            boolean inQuotes = false;
            
            for (int i = commandStartIndex; i < executorIndex; i++) {
                String arg = args[i];
                
                // 处理引号
                if (arg.startsWith("\"")) {
                    inQuotes = true;
                    arg = arg.substring(1);
                }
                
                if (arg.endsWith("\"")) {
                    inQuotes = false;
                    arg = arg.substring(0, arg.length() - 1);
                }
                
                commandBuilder.append(arg);
                
                if (i < executorIndex - 1) {
                    commandBuilder.append(" ");
                }
            }
            
            String command = commandBuilder.toString().trim();
            
            if (command.isEmpty()) {
                player.sendMessage(languageManager.getString("fgnbt.command_empty"));
                return;
            }
            
            // 处理命令斜杠 - 如果命令以/开头，去掉它
            if (command.startsWith("/")) {
                command = command.substring(1);
                if (plugin.getConfigManager().isDebugModeEnabled()) {
                    plugin.getLogger().info("去除命令前的斜杠，设置NPC命令: " + command);
                }
            }
            
            // 不再使用-p标志，权限参数已通过op/np参数设置
            // 保持permission变量不变，已在上面的逻辑中设置
            
            // 设置NPC属性
            boolean success = nbtManager.addNPCAttribute(item, command, permission, executeMode, executor);
            
            if (success) {
                player.getInventory().setItemInMainHand(item);
                player.sendMessage(languageManager.getString("fgnbt.npc_success"));
                player.sendMessage(languageManager.getString("fgnbt.npc_set", 
                        Map.of("command", command)));
                if (!permission.isEmpty()) {
                    player.sendMessage(languageManager.getString("fgnbt.permission_" + permission.toLowerCase()));
                }
                player.sendMessage(languageManager.getString("fgnbt.execute_mode", 
                        Map.of("mode", executeMode)));
                player.sendMessage(languageManager.getString("fgnbt.executor", 
                        Map.of("executor", executor)));
                if (plugin.getConfigManager().isDebugModeEnabled()) {
                    plugin.getLogger().info("玩家 " + player.getName() + " 为生物刷怪蛋添加了NPC标签: " + command);
                }
            } else {
                player.sendMessage(languageManager.getString("error.save_failed", 
                        Map.of("error", "添加NPC标签失败")));
            }
        } catch (Exception e) {
            player.sendMessage(languageManager.getString("general.error_occurred", 
                    Map.of("error", e.getMessage())));
            plugin.getLogger().severe("设置NPC标签时出错: " + e.getMessage());
        }
    }
    
    private void handleButtonCommand(Player player, String[] args) {
        try {
            // 验证参数
            if (args.length < 4) {
                player.sendMessage(languageManager.getString("fgnbt.usage.button"));
                return;
            }

            ItemStack item = player.getInventory().getItemInMainHand();

            if (item == null || item.getType().isAir()) {
                player.sendMessage(languageManager.getString("fgnbt.hold_item"));
                return;
            }

            // 解析权限节点和执行人
            String permission = args[1].toLowerCase();
            String executor = args[2].toLowerCase();
            
            // 验证权限节点
            if (!permission.equals("op") && !permission.equals("np")) {
                player.sendMessage(languageManager.getString("fgnbt.invalid_permission"));
                return;
            }
            
            // 验证执行人
            if (!executor.equals("玩家") && !executor.equals("player") && 
                !executor.equals("控制台") && !executor.equals("console")) {
                player.sendMessage(languageManager.getString("fgnbt.invalid_executor"));
                return;
            }

            // 构建命令字符串（处理引号内的内容）
            StringBuilder commandBuilder = new StringBuilder();
            boolean inQuotes = false;
            
            // 构建命令部分（从第3个参数开始）
            for (int i = 3; i < args.length; i++) {
                String arg = args[i];
                
                // 处理引号
                if (arg.startsWith("\"")) {
                    inQuotes = true;
                    arg = arg.substring(1);
                }
                
                if (arg.endsWith("\"")) {
                    inQuotes = false;
                    arg = arg.substring(0, arg.length() - 1);
                }
                
                commandBuilder.append(arg);
                
                if (i < args.length - 1) {
                    commandBuilder.append(" ");
                }
            }
            
            String command = commandBuilder.toString().trim();
            
            if (command.isEmpty()) {
                player.sendMessage(languageManager.getString("fgnbt.command_empty"));
                return;
            }
            
            // 处理命令斜杠 - 如果命令以/开头，去掉它
            if (command.startsWith("/")) {
                command = command.substring(1);
                if (plugin.getConfigManager().isDebugModeEnabled()) {
                    plugin.getLogger().info("去除命令前的斜杠，设置命令: " + command);
                }
            }
            
            // 设置执行模式（固定为op，表示控制台执行）
            String executeMode = "op";
            
            // 检查物品是否已有Border属性
            boolean hadBorder = nbtManager.isBorderItem(item);
            
            // 设置按钮属性（Button命令不支持closeOnClick参数）
            boolean success = nbtManager.addButtonAttribute(item, command, false, permission);
            
            if (success) {
                // 如果之前有Border属性，保留这个属性
                if (hadBorder) {
                    nbtManager.addBorderAttribute(item);
                }
                
                player.getInventory().setItemInMainHand(item);
                Map<String, String> variables = new HashMap<>();
                variables.put("command", command);
                String message = languageManager.getString("fgnbt.button_set", variables);
                if (hadBorder) {
                    message += " " + languageManager.getString("fgnbt.border_preserved");
                }
                player.sendMessage(message);
                
                // 添加额外参数信息
                StringBuilder infoBuilder = new StringBuilder(languageManager.getString("fgnbt.command_info", variables));
                if (!permission.isEmpty()) {
                    Map<String, String> permVars = new HashMap<>();
                    permVars.put("permission", permission);
                    infoBuilder.append(" | ").append(languageManager.getString("fgnbt.permission_info", permVars));
                }
                player.sendMessage(infoBuilder.toString());
                
                if (plugin.getConfigManager().isDebugModeEnabled()) {
                    plugin.getLogger().info("玩家 " + player.getName() + " 为物品添加了Button标签: " + infoBuilder.toString());
                }
            } else {
                player.sendMessage(languageManager.getString("fgnbt.add_button_failed"));
            }
        } catch (Exception e) {
            Map<String, String> variables = new HashMap<>();
            variables.put("error", e.getMessage());
            player.sendMessage(languageManager.getString("fgnbt.error_adding_button", variables));
            plugin.getLogger().severe("设置Button标签时出错: " + e.getMessage());
        }
    }

    /**
     * 发送帮助信息
     * @param sender 命令发送者
     */
    /**
     * 处理ButtonItem子命令，为手持物品添加ButtonItem标签和命令
     * @param player 执行命令的玩家
     * @param args 命令参数
     */
    private void handleButtonItemCommand(Player player, String[] args) {
        try {
            // 验证参数
            if (args.length < 4) {
                player.sendMessage(languageManager.getString("fgnbt.usage.buttonitem"));
                player.sendMessage(languageManager.getString("fgnbt.usage.buttonitem_desc"));
                return;
            }

            ItemStack item = player.getInventory().getItemInMainHand();

            if (item == null || item.getType().isAir()) {
                player.sendMessage(languageManager.getString("fgnbt.hold_item"));
                return;
            }

            // 解析权限节点和执行人
            String permission = args[1].toLowerCase();
            String executor = args[2].toLowerCase();
            
            // 验证权限节点
            if (!permission.equals("op") && !permission.equals("np")) {
                player.sendMessage(languageManager.getString("fgnbt.invalid_permission"));
                return;
            }
            
            // 验证执行人
            if (!executor.equals("玩家") && !executor.equals("player") && 
                !executor.equals("控制台") && !executor.equals("console")) {
                player.sendMessage(languageManager.getString("fgnbt.invalid_executor"));
                return;
            }

            // 构建命令字符串（处理引号内的内容）
            StringBuilder commandBuilder = new StringBuilder();
            boolean inQuotes = false;
            
            // 构建命令部分（从第3个参数开始）
            for (int i = 3; i < args.length; i++) {
                String arg = args[i];
                
                // 处理引号
                if (arg.startsWith("\"")) {
                    inQuotes = true;
                    arg = arg.substring(1);
                }
                
                if (arg.endsWith("\"")) {
                    inQuotes = false;
                    arg = arg.substring(0, arg.length() - 1);
                }
                
                commandBuilder.append(arg);
                
                if (i < args.length - 1) {
                    commandBuilder.append(" ");
                }
            }
            
            String command = commandBuilder.toString().trim();
            
            if (command.isEmpty()) {
                player.sendMessage(languageManager.getString("fgnbt.command_empty"));
                return;
            }

            // 处理命令斜杠 - 如果命令以/开头，去掉它
            if (command.startsWith("/")) {
                command = command.substring(1);
                if (plugin.getConfigManager().isDebugModeEnabled()) {
                    plugin.getLogger().info("去除命令前的斜杠，设置ButtonItem命令: " + command);
                }
            }

            // 设置执行模式（根据执行人参数设置）
            String executeMode = "op"; // 默认控制台执行
            if (executor.equals("玩家") || executor.equals("player")) {
                executeMode = "np"; // 玩家执行
            }

            // 设置ButtonItem属性
            boolean success = nbtManager.addButtonItemAttribute(item, command, permission, executeMode);
            
            if (success) {
                player.getInventory().setItemInMainHand(item);
                Map<String, String> variables = new HashMap<>();
                variables.put("command", command);
                player.sendMessage(languageManager.getString("fgnbt.buttonitem_success", variables));
                
                Map<String, String> commandVars = new HashMap<>();
                commandVars.put("command", command);
                player.sendMessage(languageManager.getString("fgnbt.command_info", commandVars));
                
                if (!permission.isEmpty()) {
                    Map<String, String> permVars = new HashMap<>();
                    permVars.put("permission", permission);
                    player.sendMessage(languageManager.getString("fgnbt.permission_info", permVars));
                }
                player.sendMessage(languageManager.getString("fgnbt.buttonitem_usage"));
                if (plugin.getConfigManager().isDebugModeEnabled()) {
                    plugin.getLogger().info("玩家 " + player.getName() + " 为物品添加了ButtonItem标签: " + command);
                }
            } else {
                player.sendMessage(languageManager.getString("fgnbt.add_buttonitem_failed"));
            }
        } catch (Exception e) {
            Map<String, String> variables = new HashMap<>();
            variables.put("error", e.getMessage());
            player.sendMessage(languageManager.getString("fgnbt.error_adding_buttonitem", variables));
            plugin.getLogger().severe("设置ButtonItem标签时出错: " + e.getMessage());
        }
    }
    
    /**
     * 处理ButtonBlock子命令，为指定坐标的方块添加ButtonBlock标签和命令
     * @param player 执行命令的玩家
     * @param args 命令参数
     */
    // handleButtonBlockCommand 方法已移除，因为方块交互功能已不再使用
    
    /**
     * 验证坐标是否有效
     */
    private boolean isValidCoordinate(int coord) {
        // Minecraft坐标限制
        return coord >= -30000000 && coord <= 30000000;
    }
    
    /**
     * 简单的命令安全检查
     */
    private boolean isCommandSafe(String command) {
        // 黑名单命令检查
        String[] dangerousCommands = {
            "stop", "restart", "reload",
            "op", "deop", "ban",
            "kick", "pardon", "give"
        };
        
        String lowerCommand = command.toLowerCase();
        for (String dangerous : dangerousCommands) {
            if (lowerCommand.startsWith(dangerous + " ") || lowerCommand.equals(dangerous)) {
                return false;
            }
        }
        
        // 检测潜在的有害字符序列
        String[] dangerousPatterns = {"//", ";", "&&", "||"};
        for (String pattern : dangerousPatterns) {
            if (lowerCommand.contains(pattern)) {
                return false;
            }
        }
        
        return true;
    }

    /**
     * 发送帮助信息
     * @param sender 命令发送者
     */
    private void sendHelpMessage(CommandSender sender) {
        sender.sendMessage(languageManager.getString("fgnbt.help.header"));
        sender.sendMessage(languageManager.getString("fgnbt.help.border"));
        sender.sendMessage(languageManager.getString("fgnbt.help.button"));
        sender.sendMessage(languageManager.getString("fgnbt.help.npc"));
        sender.sendMessage(languageManager.getString("fgnbt.help.buttonitem"));
        sender.sendMessage(languageManager.getString("fgnbt.help.buttonblock"));
        sender.sendMessage(languageManager.getString("fgnbt.help.advanced"));
        sender.sendMessage(languageManager.getString("fgnbt.help.close_on_click"));
        sender.sendMessage(languageManager.getString("fgnbt.help.permission"));
        sender.sendMessage(languageManager.getString("fgnbt.help.combined"));
        sender.sendMessage(languageManager.getString("fgnbt.help.separator"));
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();

        // 第一个参数的补全
        if (args.length == 1) {
            completions.add("Border");
            completions.add("Button");
            completions.add("npc");
            completions.add("Buttonitem");
            // ButtonBlock已移除
            return filterCompletions(completions, args[0]);
        }
        
        // 第二个参数的补全（Button子命令）
        if (args.length == 2 && args[0].equalsIgnoreCase("Button")) {
            completions.add("command");
            return filterCompletions(completions, args[1]);
        }
        
        // Buttonitem命令的权限节点和执行人补全
        if (args.length >= 2 && args[0].equalsIgnoreCase("Buttonitem")) {
            // 补全权限节点
            if (args.length == 2) {
                completions.addAll(Arrays.asList("op", "np"));
                return filterCompletions(completions, args[1]);
            }
            
            // 补全执行人
            if (args.length == 3 && (args[1].equalsIgnoreCase("op") || args[1].equalsIgnoreCase("np"))) {
                completions.addAll(Arrays.asList("玩家", "player", "控制台", "console"));
                return filterCompletions(completions, args[2]);
            }
        }
        
        // npc命令的补全
        if (args.length >= 2 && args[0].equalsIgnoreCase("npc")) {
            // 补全执行模式
            if (args.length == 2) {
                completions.addAll(Arrays.asList("op", "np"));
                return filterCompletions(completions, args[1]);
            }
            
            // 补全执行体
            if (args.length >= 3 && (args[1].equalsIgnoreCase("op") || args[1].equalsIgnoreCase("np"))) {
                // 检查是否已经有执行体参数
                boolean hasExecutor = false;
                for (int i = 2; i < args.length; i++) {
                    if ((args[i].equalsIgnoreCase("玩家") || args[i].equalsIgnoreCase("player") || 
                         args[i].equalsIgnoreCase("控制台") || args[i].equalsIgnoreCase("console")) && 
                        !args[i].startsWith("-p")) {
                        hasExecutor = true;
                        break;
                    }
                }
                
                if (!hasExecutor) {
                    completions.addAll(Arrays.asList("玩家", "控制台", "player", "console"));
                    return filterCompletions(completions, args[args.length - 1]);
                }
            }
            
            // 补全-p参数
            boolean hasPermissionParam = false;
            for (int i = 1; i < args.length; i++) {
                if (args[i].equals("-p")) {
                    hasPermissionParam = true;
                    // 如果当前是-p参数的下一个位置，提供权限等级建议
                    if (i == args.length - 1) {
                        completions.add("0");
                        completions.add("1");
                        completions.add("2");
                        completions.add("3");
                        completions.add("4");
                        return filterCompletions(completions, "");
                    }
                    break;
                }
            }
            
            // 如果没有-p参数且当前参数位置允许添加，建议-p参数
            if (!hasPermissionParam && args.length > 1) {
                completions.add("-p");
                return filterCompletions(completions, args[args.length - 1]);
            }
        }
        
        // 第四个参数的补全（closeOnClick选项）
        if (args.length >= 4 && args[0].equalsIgnoreCase("Button") && args[1].equalsIgnoreCase("command")) {
            // 检查前面的参数是否已经有closeOnClick
            boolean hasCloseOnClick = false;
            for (int i = 3; i < args.length - 1; i++) {
                if (args[i].startsWith("closeOnClick:")) {
                    hasCloseOnClick = true;
                    break;
                }
            }
            
            if (!hasCloseOnClick) {
                completions.add("closeOnClick:true");
                completions.add("closeOnClick:false");
            }
            
            // 过滤补全选项
            if (args.length > 1 && completions.size() > 0) {
                String lastArg = args[args.length - 1];
                List<String> filtered = new ArrayList<>();
                
                for (String completion : completions) {
                    if (completion.toLowerCase().startsWith(lastArg.toLowerCase())) {
                        filtered.add(completion);
                    }
                }
                
                return filtered;
            }
            
            return completions;
        }
        

        
        return Collections.emptyList();
    }

    /**
     * 根据当前输入过滤补全选项
     * @param completions 所有可能的补全选项
     * @param input 当前用户输入
     * @return 过滤后的补全选项
     */
    private List<String> filterCompletions(List<String> completions, String input) {
        List<String> filtered = new ArrayList<>();
        String lowerInput = input.toLowerCase();
        
        for (String completion : completions) {
            if (completion.toLowerCase().startsWith(lowerInput)) {
                filtered.add(completion);
            }
        }
        
        return filtered;
    }
}