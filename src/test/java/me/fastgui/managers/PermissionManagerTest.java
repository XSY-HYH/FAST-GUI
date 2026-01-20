package me.fastgui.managers;

import me.fastgui.FastGUI;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PermissionManagerTest {
    
    private PermissionManager permissionManager;
    private Player adminPlayer;
    private Player normalPlayer;
    private Player noPermPlayer;
    private FastGUI plugin;
    private NBTManager nbtManager;
    private LanguageManager languageManager;
    
    @BeforeEach
    void setUp() {
        // 模拟插件实例
        plugin = mock(FastGUI.class);
        Logger logger = mock(Logger.class);
        when(plugin.getLogger()).thenReturn(logger);
        
        // 模拟NBTManager
        nbtManager = mock(NBTManager.class);
        
        // 模拟LanguageManager
        languageManager = mock(LanguageManager.class);
        when(languageManager.getString(anyString())).thenReturn("Test message");
        when(languageManager.getString(anyString(), any())).thenReturn("Test message");
        
        // 创建权限管理器
        permissionManager = new PermissionManager(plugin, nbtManager, languageManager);
        
        // 创建模拟的管理员玩家
        adminPlayer = mock(Player.class);
        when(adminPlayer.isOp()).thenReturn(true);
        
        // 创建模拟的普通玩家
        normalPlayer = mock(Player.class);
        when(normalPlayer.isOp()).thenReturn(false);
        
        // 创建模拟的无权限玩家
        noPermPlayer = mock(Player.class);
        when(noPermPlayer.isOp()).thenReturn(false);
    }
    
    @Test
    void testHasButtonPermission() {
        // 测试非按钮物品
        ItemStack nonButtonItem = mock(ItemStack.class);
        when(nbtManager.isButtonItem(nonButtonItem)).thenReturn(false);
        assertTrue(permissionManager.hasButtonPermission(normalPlayer, nonButtonItem), "非按钮物品应始终有权限");
        
        // 测试按钮物品，无权限要求
        ItemStack buttonNoPerm = mock(ItemStack.class);
        when(nbtManager.isButtonItem(buttonNoPerm)).thenReturn(true);
        when(nbtManager.getButtonPermission(buttonNoPerm)).thenReturn("");
        assertTrue(permissionManager.hasButtonPermission(normalPlayer, buttonNoPerm), "无权限要求的按钮应允许所有玩家使用");
        
        // 测试按钮物品，要求OP权限，管理员玩家
        ItemStack buttonOP = mock(ItemStack.class);
        when(nbtManager.isButtonItem(buttonOP)).thenReturn(true);
        when(nbtManager.getButtonPermission(buttonOP)).thenReturn("true");
        assertTrue(permissionManager.hasButtonPermission(adminPlayer, buttonOP), "OP玩家应能使用要求OP权限的按钮");
        
        // 测试按钮物品，要求OP权限，普通玩家
        assertFalse(permissionManager.hasButtonPermission(normalPlayer, buttonOP), "普通玩家不应能使用要求OP权限的按钮");
        
        // 测试按钮物品，要求OP权限，无权限玩家
        assertFalse(permissionManager.hasButtonPermission(noPermPlayer, buttonOP), "无权限玩家不应能使用要求OP权限的按钮");
        
        // 测试按钮物品，不要求OP权限
        ItemStack buttonNoOP = mock(ItemStack.class);
        when(nbtManager.isButtonItem(buttonNoOP)).thenReturn(true);
        when(nbtManager.getButtonPermission(buttonNoOP)).thenReturn("false");
        assertTrue(permissionManager.hasButtonPermission(normalPlayer, buttonNoOP), "所有玩家应能使用不要求OP权限的按钮");
    }
    
    @Test
    void testHasNPCPermission() {
        // 测试非NPC物品
        ItemStack nonNPCItem = mock(ItemStack.class);
        when(nbtManager.isNPCItem(nonNPCItem)).thenReturn(false);
        assertTrue(permissionManager.hasNPCPermission(normalPlayer, nonNPCItem), "非NPC物品应始终有权限");
        
        // 测试NPC物品，无权限要求
        ItemStack npcNoPerm = mock(ItemStack.class);
        when(nbtManager.isNPCItem(npcNoPerm)).thenReturn(true);
        when(nbtManager.getNPCPermission(npcNoPerm)).thenReturn("");
        assertTrue(permissionManager.hasNPCPermission(normalPlayer, npcNoPerm), "无权限要求的NPC应允许所有玩家使用");
        
        // 测试NPC物品，要求OP权限，管理员玩家
        ItemStack npcOP = mock(ItemStack.class);
        when(nbtManager.isNPCItem(npcOP)).thenReturn(true);
        when(nbtManager.getNPCPermission(npcOP)).thenReturn("true");
        assertTrue(permissionManager.hasNPCPermission(adminPlayer, npcOP), "OP玩家应能使用要求OP权限的NPC");
        
        // 测试NPC物品，要求OP权限，普通玩家
        assertFalse(permissionManager.hasNPCPermission(normalPlayer, npcOP), "普通玩家不应能使用要求OP权限的NPC");
        
        // 测试NPC物品，不要求OP权限
        ItemStack npcNoOP = mock(ItemStack.class);
        when(nbtManager.isNPCItem(npcNoOP)).thenReturn(true);
        when(nbtManager.getNPCPermission(npcNoOP)).thenReturn("false");
        assertTrue(permissionManager.hasNPCPermission(normalPlayer, npcNoOP), "所有玩家应能使用不要求OP权限的NPC");
    }
    
    @Test
    void testHasNPCPermissionEntity() {
        // 测试NPC实体，无权限要求
        org.bukkit.entity.Entity npcNoPerm = mock(org.bukkit.entity.Entity.class);
        when(nbtManager.getNPCPermission(npcNoPerm)).thenReturn("");
        assertTrue(permissionManager.hasNPCPermission(normalPlayer, npcNoPerm), "无权限要求的NPC实体应允许所有玩家使用");
        
        // 测试NPC实体，要求OP权限，管理员玩家
        org.bukkit.entity.Entity npcOP = mock(org.bukkit.entity.Entity.class);
        when(nbtManager.getNPCPermission(npcOP)).thenReturn("true");
        assertTrue(permissionManager.hasNPCPermission(adminPlayer, npcOP), "OP玩家应能使用要求OP权限的NPC实体");
        
        // 测试NPC实体，要求OP权限，普通玩家
        assertFalse(permissionManager.hasNPCPermission(normalPlayer, npcOP), "普通玩家不应能使用要求OP权限的NPC实体");
        
        // 测试NPC实体，不要求OP权限
        org.bukkit.entity.Entity npcNoOP = mock(org.bukkit.entity.Entity.class);
        when(nbtManager.getNPCPermission(npcNoOP)).thenReturn("false");
        assertTrue(permissionManager.hasNPCPermission(normalPlayer, npcNoOP), "所有玩家应能使用不要求OP权限的NPC实体");
    }
    
    @Test
    void testHasButtonItemPermission() {
        // 测试非ButtonItem物品
        ItemStack nonButtonItem = mock(ItemStack.class);
        when(nbtManager.isButtonItem(nonButtonItem)).thenReturn(false);
        assertTrue(permissionManager.hasButtonItemPermission(normalPlayer, nonButtonItem), "非ButtonItem物品应始终有权限");
        
        // 测试ButtonItem物品，无权限要求
        ItemStack buttonItemNoPerm = mock(ItemStack.class);
        when(nbtManager.isButtonItem(buttonItemNoPerm)).thenReturn(true);
        when(nbtManager.getButtonItemPermission(buttonItemNoPerm)).thenReturn("");
        assertTrue(permissionManager.hasButtonItemPermission(normalPlayer, buttonItemNoPerm), "无权限要求的ButtonItem应允许所有玩家使用");
        
        // 测试ButtonItem物品，要求OP权限，管理员玩家
        ItemStack buttonItemOP = mock(ItemStack.class);
        when(nbtManager.isButtonItem(buttonItemOP)).thenReturn(true);
        when(nbtManager.getButtonItemPermission(buttonItemOP)).thenReturn("true");
        assertTrue(permissionManager.hasButtonItemPermission(adminPlayer, buttonItemOP), "OP玩家应能使用要求OP权限的ButtonItem");
        
        // 测试ButtonItem物品，要求OP权限，普通玩家
        assertFalse(permissionManager.hasButtonItemPermission(normalPlayer, buttonItemOP), "普通玩家不应能使用要求OP权限的ButtonItem");
        
        // 测试ButtonItem物品，不要求OP权限
        ItemStack buttonItemNoOP = mock(ItemStack.class);
        when(nbtManager.isButtonItem(buttonItemNoOP)).thenReturn(true);
        when(nbtManager.getButtonItemPermission(buttonItemNoOP)).thenReturn("false");
        assertTrue(permissionManager.hasButtonItemPermission(normalPlayer, buttonItemNoOP), "所有玩家应能使用不要求OP权限的ButtonItem");
    }
    
    @Test
    void testSendNoPermissionMessage() {
        // 测试OP权限不足消息
        permissionManager.sendNoPermissionMessage(normalPlayer, "true");
        verify(normalPlayer).sendMessage(languageManager.getString("permission.no_permission_op"));
        verify(normalPlayer).sendMessage(languageManager.getString("permission.current_status", 
                java.util.Map.of("status", languageManager.getString("permission.player_status"))));
        
        // 测试普通权限不足消息
        permissionManager.sendNoPermissionMessage(normalPlayer, "fastgui.use");
        verify(normalPlayer).sendMessage(languageManager.getString("permission.no_permission_general", 
                java.util.Map.of("permission", "fastgui.use")));
        
        // 测试OP玩家状态消息
        permissionManager.sendNoPermissionMessage(adminPlayer, "true");
        verify(adminPlayer).sendMessage(languageManager.getString("permission.current_status", 
                java.util.Map.of("status", languageManager.getString("permission.op_status"))));
    }
}