package me.fastgui.managers;

import me.fastgui.FastGUI;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 * 语言管理器，负责处理插件的多语言支持
 * 支持从YAML文件动态加载语言内容
 */
public class LanguageManager {
    
    private final FastGUI plugin;
    private final Logger logger;
    private final Map<String, Map<String, String>> languages;
    private String currentLanguage;
    
    // 默认语言文件
    private static final String DEFAULT_LANGUAGE = "zh.yml";
    private static final String LANGUAGES_FOLDER = "lang";
    
    /**
     * 构造函数
     * @param plugin FastGUI插件实例
     */
    public LanguageManager(FastGUI plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.languages = new HashMap<>();
        
        // 初始化语言文件夹
        initializeLanguageFolder();
        
        // 从配置文件获取语言设置
        String language = plugin.getConfigManager().getLanguage();
        if (language == null || language.isEmpty()) {
            language = DEFAULT_LANGUAGE;
            plugin.getConfigManager().setLanguage(language);
        }
        
        // 加载所有可用的语言文件
        loadAllLanguages();
        
        // 设置当前语言
        setCurrentLanguage(language);
    }
    
    /**
     * 初始化语言文件夹
     */
    private void initializeLanguageFolder() {
        File languageFolder = new File(plugin.getDataFolder(), LANGUAGES_FOLDER);
        if (!languageFolder.exists()) {
            languageFolder.mkdirs();
            logger.info("创建语言文件夹: " + languageFolder.getPath());
        }
        
        // 检查是否有语言文件，如果没有则从插件内解压默认文件
        extractDefaultLanguageFilesIfNeeded();
    }
    
    /**
     * 检查是否需要解压默认语言文件
     */
    private void extractDefaultLanguageFilesIfNeeded() {
        File langFolder = new File(plugin.getDataFolder(), LANGUAGES_FOLDER);
        File[] files = langFolder.listFiles((dir, name) -> name.toLowerCase().endsWith(".yml"));
        
        if (files == null || files.length == 0) {
            // 没有语言文件，解压默认的语言文件
            extractLanguageFile("zh.yml");
            extractLanguageFile("en.yml");
            
            // 设置默认语言为zh.yml
            plugin.getConfigManager().setLanguage(DEFAULT_LANGUAGE);
            logger.info("未找到语言文件，已解压默认语言文件并设置为" + DEFAULT_LANGUAGE);
        }
    }
    
    /**
     * 从插件内解压语言文件
     * @param fileName 文件名
     */
    private void extractLanguageFile(String fileName) {
        File languageFile = new File(plugin.getDataFolder(), LANGUAGES_FOLDER + File.separator + fileName);
        
        if (!languageFile.exists()) {
            try (InputStream inputStream = plugin.getResource("lang/" + fileName)) {
                if (inputStream != null) {
                    // 从插件资源复制语言文件
                    FileConfiguration config = YamlConfiguration.loadConfiguration(new InputStreamReader(inputStream));
                    config.save(languageFile);
                    logger.info("解压语言文件: " + fileName);
                } else {
                    logger.warning("无法在插件资源中找到语言文件: " + fileName);
                }
            } catch (IOException e) {
                logger.warning("解压语言文件失败 " + fileName + ": " + e.getMessage());
            }
        }
    }
    
    /**
     * 加载所有可用的语言文件
     */
    public void loadAllLanguages() {
        File languageFolder = new File(plugin.getDataFolder(), LANGUAGES_FOLDER);
        File[] files = languageFolder.listFiles((dir, name) -> name.toLowerCase().endsWith(".yml"));
        
        if (files == null) {
            logger.warning("无法访问语言文件夹: " + languageFolder.getPath());
            return;
        }
        
        languages.clear();
        
        for (File file : files) {
            String languageCode = file.getName();
            loadLanguageFile(languageCode);
        }
        
        logger.info("已加载 " + languages.size() + " 个语言文件");
    }
    
    /**
     * 加载单个语言文件
     * @param fileName 文件名
     */
    private void loadLanguageFile(String fileName) {
        File languageFile = new File(plugin.getDataFolder(), LANGUAGES_FOLDER + File.separator + fileName);
        
        if (languageFile.exists()) {
            try {
                FileConfiguration config = YamlConfiguration.loadConfiguration(languageFile);
                
                // 将配置转换为映射
                Map<String, String> langMap = new HashMap<>();
                for (String key : config.getKeys(true)) {
                    if (config.isString(key)) {
                        langMap.put(key, config.getString(key));
                    }
                }
                
                languages.put(fileName, langMap);
                logger.info("已加载语言文件: " + fileName);
            } catch (Exception e) {
                logger.warning("加载语言文件失败 " + fileName + ": " + e.getMessage());
            }
        }
    }
    
    /**
     * 重新加载当前语言
     */
    public void reloadLanguage() {
        loadAllLanguages();
        // 确保当前语言仍然有效
        if (!languages.containsKey(currentLanguage)) {
            if (languages.containsKey(DEFAULT_LANGUAGE)) {
                currentLanguage = DEFAULT_LANGUAGE;
                plugin.getConfigManager().setLanguage(currentLanguage);
            } else if (!languages.isEmpty()) {
                currentLanguage = languages.keySet().iterator().next();
                plugin.getConfigManager().setLanguage(currentLanguage);
            }
        }
    }
    
    /**
     * 获取本地化字符串
     * @param key 键
     * @return 本地化字符串
     */
    public String getString(String key) {
        return getString(key, new HashMap<>());
    }
    
    /**
     * 获取本地化字符串，支持变量替换
     * @param key 键
     * @param variables 变量映射
     * @return 本地化字符串
     */
    public String getString(String key, Map<String, String> variables) {
        Map<String, String> currentLangMap = languages.get(currentLanguage);
        
        if (currentLangMap == null || !currentLangMap.containsKey(key)) {
            // 如果当前语言没有找到，尝试使用默认语言
            Map<String, String> defaultLangMap = languages.get(DEFAULT_LANGUAGE);
            if (defaultLangMap != null && defaultLangMap.containsKey(key)) {
                String message = defaultLangMap.get(key);
                return replaceVariables(message, variables);
            }
            
            // 如果默认语言也没有找到，尝试其他语言
            for (Map<String, String> langMap : languages.values()) {
                if (langMap.containsKey(key)) {
                    String message = langMap.get(key);
                    return replaceVariables(message, variables);
                }
            }
            
            // 如果都没有找到，返回键本身
            return key;
        }
        
        String message = currentLangMap.get(key);
        return replaceVariables(message, variables);
    }
    
    /**
     * 替换字符串中的变量
     * @param message 原始消息
     * @param variables 变量映射
     * @return 替换后的消息
     */
    private String replaceVariables(String message, Map<String, String> variables) {
        if (message == null) {
            return "";
        }
        
        String result = message;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        
        return result;
    }
    
    /**
     * 获取当前语言代码
     * @return 当前语言代码
     */
    public String getCurrentLanguage() {
        return currentLanguage;
    }
    
    /**
     * 设置当前语言
     * @param languageCode 语言代码
     */
    public void setCurrentLanguage(String languageCode) {
        // 确保文件名以.yml结尾
        if (!languageCode.toLowerCase().endsWith(".yml")) {
            languageCode += ".yml";
        }
        
        // 检查语言文件是否存在
        if (languages.containsKey(languageCode)) {
            currentLanguage = languageCode;
            plugin.getConfigManager().setLanguage(languageCode);
            logger.info("已设置语言为: " + languageCode);
        } else {
            logger.warning("语言文件不存在: " + languageCode + "，使用当前语言: " + currentLanguage);
            // 保持当前语言，而不是强制使用默认语言
        }
    }
    
    /**
     * 获取所有可用语言
     * @return 可用语言列表
     */
    public Set<String> getAvailableLanguages() {
        return languages.keySet();
    }
}