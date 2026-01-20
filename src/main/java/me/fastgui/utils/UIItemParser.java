package me.fastgui.utils;

/**
 * 用于UI物品的数据结构类
 * <p>简化版本，移除权限系统</p>
 */
public class UIItemParser {
    
    /**
     * UIItem接口，表示所有UI组件的基类
     */
    public interface UIItem {
        String getDisplayName();
    }
    
    /**
     * UIButton类，代表可交互的按钮
     */
    public static class UIButton implements UIItem {
        private final String displayName;
        private final String command;
        private final boolean closeOnClick;
        
        public UIButton(String displayName, String command, boolean closeOnClick) {
            this.displayName = displayName;
            this.command = command;
            this.closeOnClick = closeOnClick;
        }
        
        public String getCommand() {
            return command;
        }
        
        @Override
        public String getDisplayName() {
            return displayName;
        }
        
        public boolean shouldCloseOnClick() {
            return closeOnClick;
        }
    }
}