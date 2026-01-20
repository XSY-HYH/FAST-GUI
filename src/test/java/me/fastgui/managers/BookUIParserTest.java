package me.fastgui.managers;

import me.fastgui.FastGUI;
import me.fastgui.managers.UIManager.InventoryData;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BookUIParserTest {

    private BookUIParser bookUIParser;
    private FastGUI plugin;
    private NBTManager nbtManager;
    private LogManager logManager;
    private ConfigManager configManager;
    
    @BeforeEach
    void setUp() {
        // 准备模拟对象
        plugin = mock(FastGUI.class);
        configManager = mock(ConfigManager.class);
        nbtManager = mock(NBTManager.class);
        logManager = mock(LogManager.class);
        
        // 配置模拟对象行为
        when(plugin.getNBTManager()).thenReturn(nbtManager);
        when(plugin.getLogManager()).thenReturn(logManager);
        when(plugin.getConfigManager()).thenReturn(configManager);
        when(configManager.isDebugModeEnabled()).thenReturn(false);
        
        // 初始化BookUIParser
        bookUIParser = new BookUIParser(plugin);
    }
    
    @Test
    void testParseSimpleButton() {
        // 创建模拟的BookMeta
        BookMeta bookMeta = mock(BookMeta.class);
        String pageContent = "0:STONE:1:0:测试按钮:/say 你好:true";
        when(bookMeta.getPages()).thenReturn(Collections.singletonList(pageContent));
        
        // 执行测试
        InventoryData inventoryData = bookUIParser.createInventoryData(bookMeta, "world", "TestUI");
        
        // 验证结果
        assertNotNull(inventoryData, "InventoryData不应为null");
        assertEquals("world", inventoryData.getWorldName(), "世界名称应正确");
        assertNotNull(inventoryData.getContents(), "物品内容数组不应为null");
        assertEquals(54, inventoryData.getContents().length, "物品内容数组长度应为54");
        
        // 验证物品是否被正确设置
        ItemStack item = inventoryData.getContents()[0];
        assertNotNull(item, "槽位0应包含物品");
        assertEquals(Material.STONE, item.getType(), "物品类型应为STONE");
        assertEquals(1, item.getAmount(), "物品数量应为1");
        
        // 验证NBT管理器是否被调用
        verify(nbtManager, atLeastOnce()).addButtonAttribute(any(ItemStack.class), eq("/say 你好"), eq(true));
    }
    
    @Test
    void testParseBorderItem() {
        // 创建模拟的BookMeta
        BookMeta bookMeta = mock(BookMeta.class);
        String pageContent = "1:BLACK_STAINED_GLASS_PANE:1:0:边框";
        when(bookMeta.getPages()).thenReturn(Collections.singletonList(pageContent));
        
        // 执行测试
        InventoryData inventoryData = bookUIParser.createInventoryData(bookMeta, "world", "TestUI");
        
        // 验证结果
        assertNotNull(inventoryData, "InventoryData不应为null");
        ItemStack item = inventoryData.getContents()[1];
        assertNotNull(item, "槽位1应包含物品");
        assertEquals(Material.BLACK_STAINED_GLASS_PANE, item.getType(), "物品类型应为BLACK_STAINED_GLASS_PANE");
        
        // 验证NBT管理器是否被调用
        verify(nbtManager, atLeastOnce()).addBorderAttribute(any(ItemStack.class));
    }
    
    @Test
    void testParseMultipleItems() {
        // 创建模拟的BookMeta
        BookMeta bookMeta = mock(BookMeta.class);
        String page1 = "0:STONE:1:0:按钮1:/say 按钮1:true\n" +
                      "1:DIAMOND:1:0:按钮2:/say 按钮2:false";
        String page2 = "9:RED_STAINED_GLASS_PANE:1:0:红色边框\n" +
                      "18:GREEN_STAINED_GLASS_PANE:1:0:绿色边框";
        when(bookMeta.getPages()).thenReturn(Arrays.asList(page1, page2));
        
        // 执行测试
        InventoryData inventoryData = bookUIParser.createInventoryData(bookMeta, "world", "TestUI");
        
        // 验证结果
        assertNotNull(inventoryData, "InventoryData不应为null");
        ItemStack[] contents = inventoryData.getContents();
        
        // 验证第一个按钮
        assertNotNull(contents[0], "槽位0应包含物品");
        assertEquals(Material.STONE, contents[0].getType(), "槽位0物品类型应为STONE");
        
        // 验证第二个按钮
        assertNotNull(contents[1], "槽位1应包含物品");
        assertEquals(Material.DIAMOND, contents[1].getType(), "槽位1物品类型应为DIAMOND");
        
        // 验证边框物品
        assertNotNull(contents[9], "槽位9应包含物品");
        assertEquals(Material.RED_STAINED_GLASS_PANE, contents[9].getType(), "槽位9物品类型应为RED_STAINED_GLASS_PANE");
        
        assertNotNull(contents[18], "槽位18应包含物品");
        assertEquals(Material.GREEN_STAINED_GLASS_PANE, contents[18].getType(), "槽位18物品类型应为GREEN_STAINED_GLASS_PANE");
    }
    
    @Test
    void testParseInvalidFormat() {
        // 创建格式错误的BookMeta
        BookMeta bookMeta = mock(BookMeta.class);
        String invalidPage = "这是一个无效的行\n" +  // 格式错误
                            "999:INVALID_ITEM:1:0:无效物品" + // 无效的槽位和物品类型
                            "-1:STONE:1:0:负槽位";
        when(bookMeta.getPages()).thenReturn(Collections.singletonList(invalidPage));
        
        // 执行测试
        InventoryData inventoryData = bookUIParser.createInventoryData(bookMeta, "world", "TestUI");
        
        // 即使有格式错误，也应该返回有效的InventoryData
        assertNotNull(inventoryData, "即使有格式错误，InventoryData也不应为null");
        assertNotNull(inventoryData.getContents(), "物品内容数组不应为null");
        
        // 验证错误的行不会影响其他行
        // 因为所有行都是错误的，所以所有槽位都应该是空的
        ItemStack[] contents = inventoryData.getContents();
        for (int i = 0; i < contents.length; i++) {
            // 无效行对应的槽位应为空
            if (contents[i] != null && !contents[i].getType().isAir()) {
                fail("无效行对应的槽位应为空");
            }
        }
    }
    
    @Test
    void testEmptyBook() {
        // 创建空的BookMeta
        BookMeta bookMeta = mock(BookMeta.class);
        when(bookMeta.getPages()).thenReturn(Collections.emptyList());
        
        // 执行测试
        InventoryData inventoryData = bookUIParser.createInventoryData(bookMeta, "world", "TestUI");
        
        // 验证结果
        assertNotNull(inventoryData, "空书也应该返回有效的InventoryData");
        assertNotNull(inventoryData.getContents(), "物品内容数组不应为null");
        assertEquals(54, inventoryData.getContents().length, "物品内容数组长度应为54");
    }
}