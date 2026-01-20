package me.fastgui.managers;

import me.fastgui.FastGUI;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * 权限验证管理器
 * 负责检查按钮和NPC的权限要求
 */
public class PermissionManager {
    
    private final FastGUI plugin;
    private final NBTManager nbtManager;
    private final LanguageManager languageManager;
    
    public PermissionManager(FastGUI plugin, NBTManager nbtManager, LanguageManager languageManager) {
        this.plugin = plugin;
        this.nbtManager = nbtManager;
        this.languageManager = languageManager;
    }
    
    /**
     * 检查玩家是否有权限使用按钮物品
     * @param player 玩家
     * @param item 物品
     * @return 是否有权限
     */
    public boolean hasButtonPermission(Player player, ItemStack item) {
        // 如果物品不是按钮，返回true
        if (!nbtManager.isButtonItem(item)) {
            return true;
        }
        
        // 获取按钮权限要求
        String requiredPermission = nbtManager.getButtonPermission(item);
        if (requiredPermission == null || requiredPermission.isEmpty()) {
            return true; // 没有权限要求，所有人都可以使用
        }
        
        // 检查权限
        boolean hasPermission = true;
        if (requiredPermission.equalsIgnoreCase("op")) {
            // 如果要求OP权限，检查玩家是否是OP
            hasPermission = player.isOp();
        }
        // 如果是np或空，所有人都可以使用
        
        return hasPermission;
    }
    
    /**
     * 检查玩家是否有权限使用NPC
     * @param player 玩家
     * @param item NPC刷怪蛋
     * @return 是否有权限
     */
    public boolean hasNPCPermission(Player player, ItemStack item) {
        // 如果物品不是NPC，返回true
        if (!nbtManager.isNPCItem(item)) {
            return true;
        }
        
        // 获取NPC权限要求
        String requiredPermission = nbtManager.getNPCPermission(item);
        if (requiredPermission == null || requiredPermission.isEmpty()) {
            return true; // 没有权限要求，所有人都可以使用
        }
        
        // 检查权限
        boolean hasPermission = true;
        if (requiredPermission.equalsIgnoreCase("op")) {
            // 如果要求OP权限，检查玩家是否是OP
            hasPermission = player.isOp();
        }
        // 如果是np或空，所有人都可以使用
        
        return hasPermission;
    }
    
    /**
     * 检查玩家是否有权限使用NPC实体
     * @param player 玩家
     * @param entity NPC实体
     * @return 是否有权限
     */
    public boolean hasNPCPermission(Player player, org.bukkit.entity.Entity entity) {
        // 获取NPC权限要求
        String requiredPermission = nbtManager.getNPCPermission(entity);
        if (requiredPermission == null || requiredPermission.isEmpty()) {
            return true; // 没有权限要求，所有人都可以使用
        }
        
        // 添加调试日志
        if (FastGUI.getInstance().getConfigManager().isDebugModeEnabled()) {
            plugin.getLogger().info("NPC权限检查: 玩家=" + player.getName() + ", 要求权限=" + requiredPermission + ", 玩家是否为OP=" + player.isOp());
        }
        
        // 检查权限
        boolean hasPermission = true;
        if (requiredPermission.equalsIgnoreCase("op")) {
            // 如果要求OP权限，检查玩家是否是OP
            hasPermission = player.isOp();
        }
        // 如果是np或空，所有人都可以使用
        
        if (FastGUI.getInstance().getConfigManager().isDebugModeEnabled()) {
            plugin.getLogger().info("NPC权限检查结果: " + hasPermission);
        }
        
        return hasPermission;
    }
    
    /**
     * 检查玩家是否有权限使用ButtonItem
     * @param player 玩家
     * @param item 物品
     * @return 是否有权限
     */
    public boolean hasButtonItemPermission(Player player, ItemStack item) {
        if (plugin.getConfigManager().isDebugModeEnabled()) {
            plugin.getLogger().info("PermissionManager.hasButtonItemPermission: 开始检查权限");
        }
        
        // 如果物品不是ButtonItem，返回true
        if (!nbtManager.isButtonItem(item)) {
            if (plugin.getConfigManager().isDebugModeEnabled()) {
                plugin.getLogger().info("PermissionManager.hasButtonItemPermission: 物品不是ButtonItem，返回true");
            }
            return true;
        }
        
        // 获取ButtonItem权限要求
        String requiredPermission = nbtManager.getButtonItemPermission(item);
        if (plugin.getConfigManager().isDebugModeEnabled()) {
            plugin.getLogger().info("PermissionManager.hasButtonItemPermission: 获取到权限要求: " + requiredPermission);
        }
        
        if (requiredPermission == null || requiredPermission.isEmpty()) {
            if (plugin.getConfigManager().isDebugModeEnabled()) {
                plugin.getLogger().info("PermissionManager.hasButtonItemPermission: 权限要求为空，返回true");
            }
            return true; // 没有权限要求，所有人都可以使用
        }
        
        // 添加调试日志
        plugin.getLogger().info("检测到ButtonItem使用，开始权限检查");
        plugin.getLogger().info("要求权限: " + requiredPermission);
        plugin.getLogger().info("玩家是否为OP: " + player.isOp());
        
        // 检查权限
        boolean hasPermission = true;
        if (requiredPermission.equalsIgnoreCase("op")) {
            // 如果要求OP权限，检查玩家是否是OP
            hasPermission = player.isOp();
            if (plugin.getConfigManager().isDebugModeEnabled()) {
                plugin.getLogger().info("PermissionManager.hasButtonItemPermission: 权限要求为op，玩家isOp=" + player.isOp() + ", 返回" + hasPermission);
            }
        } else {
            if (plugin.getConfigManager().isDebugModeEnabled()) {
                plugin.getLogger().info("PermissionManager.hasButtonItemPermission: 权限要求为np或其他，返回true");
            }
        }
        // 如果是np或空，所有人都可以使用
        
        plugin.getLogger().info("权限检查结果: " + (hasPermission ? "通过" : "拒绝"));
        
        if (plugin.getConfigManager().isDebugModeEnabled()) {
            plugin.getLogger().info("PermissionManager.hasButtonItemPermission: 最终返回: " + hasPermission);
        }
        
        return hasPermission;
    }
    
    /**
     * 检查玩家的OP等级是否满足要求
     * @param player 玩家
     * @param requiredLevel 要求的权限等级
     * @return 是否满足要求
     */
    private boolean checkOpLevel(Player player, String requiredLevel) {
        try {
            int required = Integer.parseInt(requiredLevel);
            
            // 权限等级说明：
            // 4 = 控制台（玩家永远无法达到）
            // 3 = 服务器OP（管理员）
            // 2 = 命令方块（玩家永远无法达到）
            // 1 = 常规玩家（所有玩家）
            // 0 = 无限制（所有玩家）
            
            // 如果要求4级（控制台权限），玩家永远无法满足
            if (required >= 4) {
                return false;
            }
            
            // 如果要求3级（OP权限），检查玩家是否是OP
            if (required >= 3) {
                return player.isOp();
            }
            
            // 如果要求2级（命令方块权限），玩家永远无法满足
            if (required >= 2) {
                return false;
            }
            
            // 如果要求1级或0级，所有玩家都可以使用
            return true;
        } catch (NumberFormatException e) {
            // 如果权限等级不是数字，默认允许
            return true;
        }
    }
    
    /**
     * 发送权限不足的消息
     * @param player 玩家
     * @param requiredLevel 需要的权限等级
     */
    public void sendNoPermissionMessage(Player player, String requiredPermission) {
        if (requiredPermission.equalsIgnoreCase("op")) {
            player.sendMessage(languageManager.getString("permission.no_permission_op"));
        } else {
            player.sendMessage(languageManager.getString("permission.no_permission_general", 
                    java.util.Map.of("permission", requiredPermission)));
        }
    }
}