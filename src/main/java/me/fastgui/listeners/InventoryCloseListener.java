package me.fastgui.listeners;

import me.fastgui.managers.UIOpener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;

/**
 * 容器关闭监听器，用于清理FastGUI的UI记录
 */
public class InventoryCloseListener implements Listener {
    private final UIOpener uiOpener;
    
    public InventoryCloseListener(UIOpener uiOpener) {
        this.uiOpener = uiOpener;
    }
    
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        Inventory inventory = event.getInventory();
        
        // 检查是否是FastGUI的UI界面
        if (uiOpener.isFastGUI(inventory)) {
            // 通知UIOpener清理记录
            uiOpener.onInventoryClose(inventory);
        }
    }
}