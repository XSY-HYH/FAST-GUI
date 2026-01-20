package me.fastgui.commands;

import me.fastgui.FastGUI;
import me.fastgui.managers.LanguageManager;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.util.*;

/**
 * FGCD命令处理器，用于管理世界命令执行事件
 * 支持添加、删除、列出命令事件
 */
public class FGCDCommand implements CommandExecutor, TabCompleter {

    private final FastGUI plugin;
    private final LanguageManager languageManager;
    private final File tableFile;
    private final Map<String, List<CommandEvent>> commandEvents = new HashMap<>();

    public Map<String, List<CommandEvent>> getCommandEvents() {
        return commandEvents;
    }

    public FGCDCommand(FastGUI plugin) {
        this.plugin = plugin;
        this.languageManager = plugin.getLanguageManager();
        this.tableFile = new File(FastGUI.getInstance().getDataFolder(), "FGCD_Table.dat");
        loadCommandEvents();
    }

    /**
     * 命令事件类，用于存储命令信息
     */
    public static class CommandEvent {
        public String id;
        public String command;
        public String executor;

        CommandEvent(String id, String command, String executor) {
            this.id = id;
            this.command = command;
            this.executor = executor;
        }
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        // 检查参数数量
        if (args.length < 1) {
            sendHelpMessage(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "add":
                handleAddCommand(sender, args);
                break;
            case "delete":
                handleDeleteCommand(sender, args);
                break;
            case "list":
                handleListCommand(sender, args);
                break;
            default:
                sendHelpMessage(sender);
                break;
        }

        return true;
    }

    /**
     * 处理add子命令
     */
    private void handleAddCommand(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(languageManager.getString("fgcd.usage_add"));
            return;
        }

        String worldName = args[1];
        StringBuilder commandBuilder = new StringBuilder();
        boolean inQuotes = false;
        int executorIndex = -1;

        // 构建命令字符串（处理引号内的内容）
        for (int i = 2; i < args.length; i++) {
            String arg = args[i];

            // 处理引号
            if (arg.startsWith("'")) {
                inQuotes = true;
                arg = arg.substring(1);
            }

            if (arg.endsWith("'")) {
                inQuotes = false;
                arg = arg.substring(0, arg.length() - 1);
            }

            if (arg.startsWith("\"")) {
                inQuotes = true;
                arg = arg.substring(1);
            }

            if (arg.endsWith("\"")) {
                inQuotes = false;
                arg = arg.substring(0, arg.length() - 1);
            }

            // 如果不在引号内且是执行体参数，记录索引并退出
            if (!inQuotes && (arg.equalsIgnoreCase("玩家") || arg.equalsIgnoreCase("player") || arg.equalsIgnoreCase("控制台") || arg.equalsIgnoreCase("console"))) {
                executorIndex = i;
                break;
            }

            commandBuilder.append(arg);
            if (i < args.length - 1 && (!inQuotes || (i + 1 < args.length && !args[i + 1].equalsIgnoreCase("玩家") && !args[i + 1].equalsIgnoreCase("player") && !args[i + 1].equalsIgnoreCase("控制台") && !args[i + 1].equalsIgnoreCase("console")))) {
                commandBuilder.append(" ");
            }
        }

        if (executorIndex == -1) {
            sender.sendMessage(languageManager.getString("fgcd.executor_not_found"));
            return;
        }

        String commandContent = commandBuilder.toString().trim();
        String executor = args[executorIndex].toLowerCase();

        // 验证世界是否存在
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            sender.sendMessage(languageManager.getString("fgcd.world_not_found", Map.of("world", worldName)));
            return;
        }

        // 验证执行体
        if (!executor.equals("玩家") && !executor.equals("player") && !executor.equals("控制台") && !executor.equals("console")) {
            sender.sendMessage(languageManager.getString("fgcd.invalid_executor"));
            return;
        }

        // 处理命令斜杠
        if (commandContent.startsWith("/")) {
            commandContent = commandContent.substring(1);
        }

        // 生成id
        String worldAbbreviation = worldName.length() > 2 ? worldName.substring(0, 2) : worldName;
        int eventCount = commandEvents.getOrDefault(worldName, new ArrayList<>()).size() + 1;
        String eventId = worldAbbreviation + eventCount;

        // 添加命令事件
        commandEvents.computeIfAbsent(worldName, k -> new ArrayList<>()).add(new CommandEvent(eventId, commandContent, executor));
        saveCommandEvents();

        Map<String, String> variables = new HashMap<>();
        variables.put("world", worldName);
        variables.put("command", commandContent);
        variables.put("executor", executor);
        variables.put("id", eventId);
        sender.sendMessage(languageManager.getString("fgcd.add_success", variables));
    }

    /**
     * 处理delete子命令
     */
    private void handleDeleteCommand(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(languageManager.getString("fgcd.usage_delete"));
            return;
        }

        String target = args[1];
        boolean removed = false;

        // 尝试按id删除
        for (List<CommandEvent> events : commandEvents.values()) {
            Iterator<CommandEvent> iterator = events.iterator();
            while (iterator.hasNext()) {
                CommandEvent event = iterator.next();
                if (event.id.equals(target)) {
                    iterator.remove();
                    removed = true;
                    break;
                }
            }
            if (removed) {
                break;
            }
        }

        // 如果按id删除失败，尝试按世界名和命令删除
        if (!removed) {
            if (args.length < 3) {
                sender.sendMessage(languageManager.getString("fgcd.command_not_found"));
                return;
            }

            String worldName = args[1];
            StringBuilder commandBuilder = new StringBuilder();
            boolean inQuotes = false;

            // 构建命令字符串（处理引号内的内容）
            for (int i = 2; i < args.length; i++) {
                String arg = args[i];

                // 处理引号
                if (arg.startsWith("'")) {
                    inQuotes = true;
                    arg = arg.substring(1);
                }

                if (arg.endsWith("'")) {
                    inQuotes = false;
                    arg = arg.substring(0, arg.length() - 1);
                }

                if (arg.startsWith("\"")) {
                    inQuotes = true;
                    arg = arg.substring(1);
                }

                if (arg.endsWith("\"")) {
                    inQuotes = false;
                    arg = arg.substring(0, arg.length() - 1);
                }

                commandBuilder.append(arg);
                if (i < args.length - 1 && !inQuotes) {
                    commandBuilder.append(" ");
                }
            }

            String commandContent = commandBuilder.toString().trim();

            // 处理命令斜杠
            if (commandContent.startsWith("/")) {
                commandContent = commandContent.substring(1);
            }

            // 删除命令事件
            List<CommandEvent> events = commandEvents.get(worldName);
            if (events != null) {
                Iterator<CommandEvent> iterator = events.iterator();
                while (iterator.hasNext()) {
                    CommandEvent event = iterator.next();
                    if (event.command.equals(commandContent)) {
                        iterator.remove();
                        removed = true;
                        break;
                    }
                }
            }
        }

        if (removed) {
            saveCommandEvents();
            Map<String, String> variables = new HashMap<>();
            variables.put("id", target);
            sender.sendMessage(languageManager.getString("fgcd.delete_success_by_id", variables));
        } else {
            sender.sendMessage(languageManager.getString("fgcd.command_not_found"));
        }
    }

    /**
     * 处理list子命令
     */
    private void handleListCommand(CommandSender sender, String[] args) {
        if (args.length == 1) {
            // 列出所有命令事件
            sender.sendMessage(languageManager.getString("fgcd.list_all_header"));
            for (Map.Entry<String, List<CommandEvent>> entry : commandEvents.entrySet()) {
                sender.sendMessage(languageManager.getString("fgcd.list_world", Map.of("world", entry.getKey())));
                for (CommandEvent event : entry.getValue()) {
                    Map<String, String> variables = new HashMap<>();
                    variables.put("id", event.id);
                    variables.put("command", event.command);
                    variables.put("executor", event.executor);
                    sender.sendMessage(languageManager.getString("fgcd.list_item_indented_with_id", variables));
                }
            }
            sender.sendMessage(languageManager.getString("fgcd.list_separator"));
        } else if (args.length == 2) {
            // 列出特定世界的命令事件
            String worldName = args[1];
            List<CommandEvent> events = commandEvents.get(worldName);
            if (events != null && !events.isEmpty()) {
                sender.sendMessage(languageManager.getString("fgcd.list_world_header", Map.of("world", worldName)));
                for (CommandEvent event : events) {
                    Map<String, String> variables = new HashMap<>();
                    variables.put("id", event.id);
                    variables.put("command", event.command);
                    variables.put("executor", event.executor);
                    sender.sendMessage(languageManager.getString("fgcd.list_item_with_id", variables));
                }
                sender.sendMessage(languageManager.getString("fgcd.list_separator"));
            } else {
                sender.sendMessage(languageManager.getString("fgcd.world_no_events"));
            }
        } else {
            sender.sendMessage(languageManager.getString("fgcd.usage_list"));
        }
    }

    /**
     * 发送帮助消息
     */
    private void sendHelpMessage(CommandSender sender) {
        sender.sendMessage(languageManager.getString("fgcd.help_header"));
        sender.sendMessage(languageManager.getString("fgcd.help_add"));
        sender.sendMessage(languageManager.getString("fgcd.help_delete"));
        sender.sendMessage(languageManager.getString("fgcd.help_list"));
        sender.sendMessage(languageManager.getString("fgcd.help_separator"));
    }

    /**
     * 加载命令事件
     */
    private void loadCommandEvents() {
        if (!tableFile.exists()) {
            return;
        }

        try (DataInputStream dis = new DataInputStream(new FileInputStream(tableFile))) {
            int size = dis.readInt();
            for (int i = 0; i < size; i++) {
                String worldName = dis.readUTF();
                String id = dis.readUTF();
                String command = dis.readUTF();
                String executor = dis.readUTF();
                commandEvents.computeIfAbsent(worldName, k -> new ArrayList<>()).add(new CommandEvent(id, command, executor));
            }
        } catch (IOException e) {
            plugin.getLogger().severe("加载FGCD命令事件失败: " + e.getMessage());
        }
    }

    /**
     * 保存命令事件
     */
    private void saveCommandEvents() {
        try {
            if (!tableFile.getParentFile().exists()) {
                tableFile.getParentFile().mkdirs();
            }

            try (DataOutputStream dos = new DataOutputStream(new FileOutputStream(tableFile))) {
                int totalSize = 0;
                for (List<CommandEvent> events : commandEvents.values()) {
                    totalSize += events.size();
                }
                dos.writeInt(totalSize);
                for (Map.Entry<String, List<CommandEvent>> entry : commandEvents.entrySet()) {
                    for (CommandEvent event : entry.getValue()) {
                        dos.writeUTF(entry.getKey());
                        dos.writeUTF(event.id);
                        dos.writeUTF(event.command);
                        dos.writeUTF(event.executor);
                    }
                }
            }
        } catch (IOException e) {
            plugin.getLogger().severe("保存FGCD命令事件失败: " + e.getMessage());
        }
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            // 补全子命令
            completions.addAll(Arrays.asList("add", "delete", "list"));
        } else if (args.length == 2) {
            // 补全世界名字
            for (World world : Bukkit.getWorlds()) {
                completions.add(world.getName());
            }
        } else if (args[0].equalsIgnoreCase("add")) {
            // 检查是否在引号内
            boolean inQuotes = false;
            for (int i = 2; i < args.length - 1; i++) {
                String arg = args[i];
                if (arg.startsWith("'") || arg.startsWith("\"")) {
                    inQuotes = true;
                }
                if (arg.endsWith("'") || arg.endsWith("\"")) {
                    inQuotes = false;
                }
            }
            
            // 如果不在引号内，补全执行体
            if (!inQuotes) {
                completions.addAll(Arrays.asList("玩家", "控制台", "player", "console"));
            }
        }

        return completions;
    }
}