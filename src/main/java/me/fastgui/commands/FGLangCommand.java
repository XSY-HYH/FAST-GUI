package me.fastgui.commands;

import me.fastgui.FastGUI;
import me.fastgui.managers.LanguageManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * 语言切换命令处理器
 * 处理/fglang命令，用于切换插件语言
 */
public class FGLangCommand implements CommandExecutor, TabCompleter {

    private final FastGUI plugin;
    private final LanguageManager languageManager;

    /**
     * 构造函数
     * @param plugin FastGUI插件实例
     * @param languageManager 语言管理器实例
     */
    public FGLangCommand(FastGUI plugin, LanguageManager languageManager) {
        this.plugin = plugin;
        this.languageManager = languageManager;
    }

    /**
     * 命令执行处理
     * @param sender 命令发送者
     * @param command 命令对象
     * @param label 命令标签
     * @param args 命令参数
     * @return 命令是否执行成功
     */
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        // 检查权限
        if (!sender.hasPermission("fastgui.lang")) {
            sender.sendMessage(languageManager.getString("general.no_permission"));
            return true;
        }

        // 检查参数
        if (args.length == 0) {
            // 显示当前语言和可用语言列表
            String currentLang = languageManager.getCurrentLanguage();
            sender.sendMessage(languageManager.getString("fglang.current_language", 
                    java.util.Map.of("language", currentLang)));
            
            sender.sendMessage(languageManager.getString("fglang.available_languages"));
            for (String lang : languageManager.getAvailableLanguages()) {
                String indicator = lang.equals(currentLang) ? languageManager.getString("fglang.current_indicator") : "";
                sender.sendMessage("- " + lang + indicator);
            }
            
            sender.sendMessage(languageManager.getString("fglang.usage"));
            return true;
        }

        String languageFile = args[0];
        
        // 确保文件名以.yml结尾
        if (!languageFile.toLowerCase().endsWith(".yml")) {
            languageFile += ".yml";
        }

        // 检查语言文件是否存在
        File langFile = new File(plugin.getDataFolder(), "lang/" + languageFile);
        if (!langFile.exists()) {
            sender.sendMessage(languageManager.getString("fglang.language_not_found", 
                    java.util.Map.of("language", languageFile)));
            sender.sendMessage("提示: 将语言文件放入 lang/ 文件夹即可使用");
            return true;
        }

        // 重新加载语言文件列表，以包含新添加的语言文件
        languageManager.loadAllLanguages();

        // 切换语言
        String oldLanguage = languageManager.getCurrentLanguage();
        languageManager.setCurrentLanguage(languageFile);
        
        // 重新加载语言文件
        languageManager.reloadLanguage();
        
        // 发送成功消息
        sender.sendMessage(languageManager.getString("fglang.language_changed", 
                java.util.Map.of("old_language", oldLanguage, "new_language", languageFile)));
        
        return true;
    }

    /**
     * Tab补全处理
     * @param sender 命令发送者
     * @param command 命令对象
     * @param label 命令标签
     * @param args 命令参数
     * @return 补全建议列表
     */
    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();

        // 只在第一个参数提供补全
        if (args.length == 1) {
            String partial = args[0].toLowerCase();
            File langFolder = new File(plugin.getDataFolder(), "lang");
            
            if (langFolder.exists() && langFolder.isDirectory()) {
                File[] files = langFolder.listFiles((dir, name) -> name.toLowerCase().endsWith(".yml"));
                
                if (files != null) {
                    for (File file : files) {
                        String fileName = file.getName();
                        if (fileName.toLowerCase().startsWith(partial)) {
                            completions.add(fileName);
                        }
                    }
                }
            }
        }

        return completions;
    }
}