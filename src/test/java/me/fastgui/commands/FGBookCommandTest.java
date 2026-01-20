package me.fastgui.commands;

import me.fastgui.FastGUI;
import me.fastgui.managers.BookUIParser;
import me.fastgui.managers.LogManager;
import me.fastgui.managers.UIManager;
import me.fastgui.managers.UIManager.InventoryData;
import me.fastgui.managers.UIOpener;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FGBookCommandTest {

    private FGBookCommand fgBookCommand;
    private FastGUI plugin;
    private UIManager uiManager;
    private UIOpener uiOpener;
    private BookUIParser bookUIParser;
    private LogManager logManager;
    
    // 模拟对象
    private Player player;
    private CommandSender sender;
    private ItemStack bookItem;
    private BookMeta bookMeta;
    private InventoryData inventoryData;
    
    @BeforeEach
    void setUp() {
        // 准备模拟对象
        plugin = mock(FastGUI.class);
        uiManager = mock(UIManager.class);
        uiOpener = mock(UIOpener.class);
        bookUIParser = mock(BookUIParser.class);
        logManager = mock(LogManager.class);
        
        // 配置模拟对象行为
        when(plugin.getUIManager()).thenReturn(uiManager);
        when(plugin.getUIOpener()).thenReturn(uiOpener);
        when(plugin.getLogManager()).thenReturn(logManager);
        when(plugin.getBookUIParser()).thenReturn(bookUIParser);
        
        // 初始化命令对象
        fgBookCommand = new FGBookCommand(plugin);
        
        // 准备测试用的模拟对象
        player = mock(Player.class);
        sender = mock(CommandSender.class);
        bookItem = mock(ItemStack.class);
        bookMeta = mock(BookMeta.class);
        inventoryData = mock(InventoryData.class);
        
        // 配置物品行为
        when(bookItem.getType()).thenReturn(Material.WRITABLE_BOOK);
        when(bookItem.getItemMeta()).thenReturn(bookMeta);
        
        // 配置玩家行为
        when(player.getItemInHand()).thenReturn(bookItem);
    }
    
    @Test
    void testOnCommandOpenSuccess() {
        // 准备参数
        String[] args = {"open", "testUI"};
        
        // 配置UIOpener行为
        when(uiOpener.openUI(player, "testUI")).thenReturn(true);
        
        // 执行命令
        boolean result = fgBookCommand.onCommand(player, null, "fgBook", args);
        
        // 验证结果
        assertTrue(result, "命令执行应返回true");
        verify(uiOpener).openUI(player, "testUI");
        // 不应该有发送消息的行为，因为openUI方法返回true
        verify(player, never()).sendMessage(anyString());
    }
    
    @Test
    void testOnCommandOpenFailure() {
        // 准备参数
        String[] args = {"open", "nonExistentUI"};
        
        // 配置UIOpener行为
        when(uiOpener.openUI(player, "nonExistentUI")).thenReturn(false);
        
        // 执行命令
        boolean result = fgBookCommand.onCommand(player, null, "fgBook", args);
        
        // 验证结果
        assertTrue(result, "命令执行应返回true");
        verify(uiOpener).openUI(player, "nonExistentUI");
        verify(player).sendMessage(contains("加载失败"));
    }
    
    @Test
    void testOnCommandRegisterSuccess() {
        // 准备参数
        String[] args = {"testUI"};
        
        // 配置BookUIParser和UIManager行为
        when(bookUIParser.createInventoryData(bookMeta, anyString(), eq("testUI"))).thenReturn(inventoryData);
        when(inventoryData.getContents()).thenReturn(new ItemStack[54]);
        when(uiManager.addUI(eq("testUI"), any(ItemStack[].class), anyString())).thenReturn("testUI");
        
        // 执行命令
        boolean result = fgBookCommand.onCommand(player, null, "fgBook", args);
        
        // 验证结果
        assertTrue(result, "命令执行应返回true");
        verify(bookUIParser).createInventoryData(bookMeta, anyString(), eq("testUI"));
        verify(uiManager).addUI(eq("testUI"), any(ItemStack[].class), anyString());
        verify(player).sendMessage(contains("testUI"));
        verify(player).sendMessage(contains("成功注册"));
    }
    
    @Test
    void testOnCommandRegisterNoBook() {
        // 准备参数
        String[] args = {"testUI"};
        
        // 配置玩家没有手持书与笔
        when(player.getItemInHand()).thenReturn(new ItemStack(Material.STONE));
        
        // 执行命令
        boolean result = fgBookCommand.onCommand(player, null, "fgBook", args);
        
        // 验证结果
        assertTrue(result, "命令执行应返回true");
        verify(player).sendMessage(contains("请手持书与笔"));
        verify(bookUIParser, never()).createInventoryData(any(), anyString(), anyString());
        verify(uiManager, never()).addUI(anyString(), any(ItemStack[].class), anyString());
    }
    
    @Test
    void testOnCommandNotPlayer() {
        // 准备参数
        String[] args = {"testUI"};
        
        // 执行命令
        boolean result = fgBookCommand.onCommand(sender, null, "fgBook", args);
        
        // 验证结果
        assertTrue(result, "命令执行应返回true");
        verify(sender).sendMessage(contains("只有玩家可以使用此命令"));
    }
    
    @Test
    void testOnCommandInvalidArgs() {
        // 准备空参数
        String[] args = {};
        
        // 执行命令
        boolean result = fgBookCommand.onCommand(player, null, "fgBook", args);
        
        // 验证结果
        assertTrue(result, "命令执行应返回true");
        verify(player).sendMessage(contains("用法: /fgBook <ID> 或 /fgBook open <ID>"));
    }
    
    @Test
    void testOnTabCompleteOpen() {
        // 准备参数
        String[] args = {"open"};
        
        // 配置UIManager行为
        when(uiManager.getUITable().keySet()).thenReturn(java.util.Collections.emptySet());
        // 配置BookUIParser行为
        when(bookUIParser.getAvailableBookUIs()).thenReturn(java.util.Arrays.asList("test1", "test2"));
        
        // 执行Tab补全
        java.util.List<String> completions = fgBookCommand.onTabComplete(player, null, "fgBook", args);
        
        // 验证结果
        assertNotNull(completions, "返回的补全列表不应为null");
        assertTrue(completions.contains("test1"), "补全列表应包含'test1'");
        assertTrue(completions.contains("test2"), "补全列表应包含'test2'");
    }
    
    @Test
    void testOnTabCompleteRegister() {
        // 准备空参数
        String[] args = {};
        
        // 执行Tab补全
        java.util.List<String> completions = fgBookCommand.onTabComplete(player, null, "fgBook", args);
        
        // 验证结果
        assertNotNull(completions, "返回的补全列表不应为null");
        assertTrue(completions.contains("open"), "补全列表应包含'open'");
    }
}