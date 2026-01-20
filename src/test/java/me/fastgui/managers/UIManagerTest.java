package me.fastgui.managers;

import me.fastgui.FastGUI;
import me.fastgui.utils.ErrorHandler;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UIManagerTest {
    
    private UIManager uiManager;
    private ErrorHandler errorHandler;
    private File dataFolder;
    private LogManager logManager;
    private FastGUI plugin;
    private ConfigManager configManager;
    private NBTManager nbtManager;
    
    @BeforeEach
    void setUp() {
        // 创建模拟的ErrorHandler
        errorHandler = mock(ErrorHandler.class);
        
        // 创建临时数据文件夹
        try {
            dataFolder = File.createTempFile("FastGUITest", "");
            dataFolder.delete();
            dataFolder.mkdir();
        } catch (IOException e) {
            fail("创建测试文件夹失败: " + e.getMessage());
        }
        
        // 准备模拟对象
        plugin = mock(FastGUI.class);
        configManager = mock(ConfigManager.class);
        nbtManager = mock(NBTManager.class);
        logManager = mock(LogManager.class);
        
        // 初始化UIManager
        uiManager = new UIManager(plugin, configManager, nbtManager, logManager);
    }
    
    @Test
    void testAddUI() {
        // 创建测试物品数组
        ItemStack[] contents = new ItemStack[54];
        ItemStack testItem = new ItemStack(Material.STONE);
        contents[0] = testItem;
        
        // 添加UI
        String id = uiManager.addUI("TestUI", contents, "world");
        
        // 验证结果
        assertNotNull(id, "UI ID不应为null");
        assertFalse(id.isEmpty(), "UI ID不应为空字符串");
        
        // 验证UI表中包含了新添加的UI
        Map<String, String> uiTable = uiManager.getUITable();
        assertTrue(uiTable.containsKey(id), "UI表中应包含新添加的UI ID");
        assertTrue(uiTable.get(id).contains("TestUI"), "UI表中应包含正确的UI名称");
    }
    
    @Test
    void testDeleteUI() {
        // 添加UI
        ItemStack[] contents = new ItemStack[54];
        String uiName = uiManager.addUI("TestUI", contents, "world");
        
        // 验证UI存在
        assertTrue(uiManager.getUITable().containsKey(uiName), "添加后UI应存在");
        
        // 删除UI
        boolean deleted = uiManager.deleteUI(uiName);
        
        // 验证删除成功
        assertTrue(deleted, "删除UI应返回true");
        assertFalse(uiManager.getUITable().containsKey(uiName), "删除后UI不应存在于UI表中");
    }
    
    @Test
    void testDeleteNonExistentUI() {
        // 尝试删除不存在的UI
        boolean deleted = uiManager.deleteUI("nonExistentName");
        
        // 验证删除失败
        assertFalse(deleted, "删除不存在的UI应返回false");
    }
    
    @Test
    void testSaveAndLoadTable() {
        // 添加几个UI
        ItemStack[] contents = new ItemStack[54];
        String id1 = uiManager.addUI("UI1", contents, "world");
        String id2 = uiManager.addUI("UI2", contents, "world");
        
        // 保存表
        uiManager.saveTable();
        
        // 重新创建UIManager以加载表
        UIManager newUIManager = new UIManager(mock(FastGUI.class), mock(ConfigManager.class), mock(NBTManager.class), mock(LogManager.class));
        
        // 验证加载的表包含之前添加的UI
        Map<String, String> uiTable = newUIManager.getUITable();
        assertTrue(uiTable.containsKey(id1), "加载的UI表应包含第一个UI ID");
        assertTrue(uiTable.containsKey(id2), "加载的UI表应包含第二个UI ID");
    }
    
    @AfterEach
    void tearDown() {
        // 清理测试文件
        deleteDirectory(dataFolder);
    }
    
    private void deleteDirectory(File directory) {
        if (directory != null && directory.exists()) {
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
            directory.delete();
        }
    }
}