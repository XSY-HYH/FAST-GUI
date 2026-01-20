package me.fastgui.utils;

import me.fastgui.utils.UIItemParser.UIButton;
import me.fastgui.utils.UIItemParser.UIItem;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UIItemParserTest {
    
    @Test
    void testUIButtonCreation() {
        // 测试UIButton的创建和基本属性
        UIButton button = new UIButton("测试按钮", "say 你好", true);
        
        // 验证结果
        assertNotNull(button, "创建的按钮不应为null");
        assertEquals("测试按钮", button.getDisplayName(), "按钮显示名称应正确");
        assertEquals("say 你好", button.getCommand(), "按钮命令应正确");
        assertTrue(button.shouldCloseOnClick(), "closeOnClick属性应正确");
    }
    
    @Test
    void testUIButtonWithPermission() {
        // 测试UIButton创建（权限系统已移除）
        UIButton button = new UIButton("带权限按钮", "admin command", false);
        
        // 验证结果
        assertNotNull(button, "创建的按钮不应为null");
        assertEquals("带权限按钮", button.getDisplayName(), "按钮显示名称应正确");
        assertEquals("admin command", button.getCommand(), "按钮命令应正确");
        assertFalse(button.shouldCloseOnClick(), "closeOnClick属性应正确");
    }
    
    @Test
    void testUIButtonImplementsInterface() {
        // 测试UIButton实现UIItem接口
        UIButton button = new UIButton("测试按钮", "test command", true);
        
        // 验证结果
        assertTrue(button instanceof UIItem, "UIButton应实现UIItem接口");
        assertEquals("测试按钮", ((UIItem)button).getDisplayName(), "通过接口获取的显示名称应正确");
    }
}