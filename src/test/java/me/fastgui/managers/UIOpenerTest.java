package me.fastgui.managers;

import me.fastgui.FastGUI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UIOpenerTest {
    
    @Mock
    private FastGUI plugin;
    
    @Mock
    private Player player;
    
    @Mock
    private Inventory inventory;
    
    @Mock
    private UIManager uiManager;
    
    @Mock
    private ConfigManager configManager;
    
    @Mock
    private LanguageManager languageManager;
    
    private UIOpener uiOpener;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // 创建UIOpener，匹配实际构造函数
        uiOpener = new UIOpener(plugin, uiManager, configManager, languageManager);
        
        // 模拟Bukkit创建库存
        when(Bukkit.createInventory(player, 54, "测试界面")).thenReturn(inventory);
        
        // 模拟玩家打开库存
        doNothing().when(player).openInventory(inventory);
    }
    
    @Test
    void testIsFastGUI() {
        // 普通库存不是FastGUI
        assertFalse(uiOpener.isFastGUI(inventory), "未打开的库存不应被标记为FastGUI");
    }
    
    @Test
    void testGetUIId() {
        // 普通库存没有UI ID
        assertNull(uiOpener.getUIId(inventory), "未打开的库存不应有关联的UI ID");
    }
}