package me.fastgui.listeners;

import me.fastgui.FastGUI;
import me.fastgui.managers.NBTManager;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Objects;

/**
 * 实体生成监听器，用于将刷怪蛋的NPC属性传递给生成的实体
 */
public class EntitySpawnListener implements Listener {

    private final FastGUI plugin;
    private final NBTManager nbtManager;
    // 使用ThreadLocal存储最近使用的刷怪蛋信息
    private static final ThreadLocal<PlayerEggUseInfo> RECENT_EGG_USE = new ThreadLocal<>();
    // 有效时间阈值（毫秒）
    private static final long VALIDITY_THRESHOLD = 5000;

    public EntitySpawnListener(FastGUI plugin) {
        this.plugin = plugin;
        this.nbtManager = plugin.getNBTManager();
    }

    @EventHandler
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        try {
            // 检查是否是通过刷怪蛋生成的生物
            if (event.getSpawnReason() != CreatureSpawnEvent.SpawnReason.SPAWNER_EGG &&
                event.getSpawnReason() != CreatureSpawnEvent.SpawnReason.EGG) {
                return;
            }

            // 获取最近使用刷怪蛋的信息
            PlayerEggUseInfo eggUseInfo = RECENT_EGG_USE.get();
            if (eggUseInfo == null || !eggUseInfo.isValid()) {
                return;
            }

            // 检查刷怪蛋是否有NPC属性
            if (!nbtManager.isNPCItem(eggUseInfo.getItem())) {
                RECENT_EGG_USE.remove(); // 清理无效数据
                return;
            }

            // 获取NPC命令
            String command = nbtManager.getNPCCommand(eggUseInfo.getItem());
            if (command == null || command.isEmpty()) {
                plugin.getLogger().warning("刷怪蛋有NPC标签但缺少命令，忽略实体: " + event.getEntity().getType().name());
                RECENT_EGG_USE.remove();
                return;
            }

            // 获取生成的实体
            Entity entity = event.getEntity();

            // 获取权限、执行模式和执行体
            String permission = nbtManager.getNPCPermission(eggUseInfo.getItem());
            String executeMode = nbtManager.getNPCExecuteMode(eggUseInfo.getItem());
            String executor = nbtManager.getNPCExecutor(eggUseInfo.getItem());

            // 调试信息：刷怪蛋NBT读取
            if (FastGUI.getInstance().getConfigManager().isDebugModeEnabled()) {
                plugin.getLogger().info("NPC生成调试信息:");
                plugin.getLogger().info("  刷怪蛋来源: " + eggUseInfo.getPlayer().getName());
                plugin.getLogger().info("  实体类型: " + entity.getType().name());
                plugin.getLogger().info("  实体UUID: " + entity.getUniqueId().toString());
                plugin.getLogger().info("  命令: " + command);
                plugin.getLogger().info("  权限: " + (permission != null ? permission : "未设置"));
                plugin.getLogger().info("  执行模式: " + (executeMode != null ? executeMode : "未设置"));
                plugin.getLogger().info("  执行体: " + (executor != null ? executor : "未设置"));
                // 检查NBT读取结果
                plugin.getLogger().info("  NBT读取结果:");
                plugin.getLogger().info("    命令读取结果: " + (command != null ? "成功" : "失败"));
                plugin.getLogger().info("    权限读取结果: " + (permission != null ? "成功: " + permission : "失败"));
                plugin.getLogger().info("    执行模式读取结果: " + (executeMode != null ? "成功: " + executeMode : "失败"));
                plugin.getLogger().info("    执行体读取结果: " + (executor != null ? "成功: " + executor : "失败"));
                
                // 源物品全部属性
                plugin.getLogger().info("  源物品全部属性:");
                ItemStack item = eggUseInfo.getItem();
                plugin.getLogger().info("    物品类型: " + item.getType().name());
                plugin.getLogger().info("    物品数量: " + item.getAmount());
                plugin.getLogger().info("    物品名称: " + item.getItemMeta().getDisplayName());
                plugin.getLogger().info("    物品Lore: " + (item.getItemMeta().getLore() != null ? item.getItemMeta().getLore().toString() : "无"));
                
                // 检查NBTManager方法调用
                plugin.getLogger().info("  NBTManager方法调用:");
                plugin.getLogger().info("    getNPCCommand() 调用结果: " + (command != null ? "返回命令: " + command : "返回null"));
                plugin.getLogger().info("    getNPCPermission() 调用结果: " + (permission != null ? "返回权限: " + permission : "返回null"));
                plugin.getLogger().info("    getNPCExecuteMode() 调用结果: " + (executeMode != null ? "返回执行模式: " + executeMode : "返回null"));
                plugin.getLogger().info("    getNPCExecutor() 调用结果: " + (executor != null ? "返回执行体: " + executor : "返回null"));
            }

            // 设置实体的NPC属性
            boolean success = nbtManager.addNPCAttributeToEntity(entity, command, permission, executeMode, executor);

            if (success) {
                // 记录成功日志
                if (FastGUI.getInstance().getConfigManager().isDebugModeEnabled()) {
                    plugin.getLogger().info("成功将NPC属性应用到实体: " + entity.getType().name() + 
                        " (由玩家 " + eggUseInfo.getPlayer().getName() + " 生成)");
                }
            } else {
                // 记录失败日志
                plugin.getLogger().warning("无法将NPC属性应用到实体: " + entity.getType().name());
            }

        } catch (Exception e) {
            plugin.getLogger().severe("处理实体生成事件时出错: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // 确保总是清除ThreadLocal数据，避免内存泄漏
            RECENT_EGG_USE.remove();
        }
    }

    /**
     * 记录刷怪蛋使用信息
     */
    public static void recordEggUse(Player player, ItemStack item) {
        Objects.requireNonNull(player, "玩家对象不能为空");
        Objects.requireNonNull(item, "物品对象不能为空");
        
        RECENT_EGG_USE.set(new PlayerEggUseInfo(player, item));
    }

    /**
     * 玩家使用刷怪蛋的信息
     */
    private static class PlayerEggUseInfo {
        private final Player player;
        private final ItemStack item;
        private final long timestamp;

        public PlayerEggUseInfo(Player player, ItemStack item) {
            this.player = player;
            this.item = item;
            this.timestamp = System.currentTimeMillis();
        }

        public Player getPlayer() {
            return player;
        }

        public ItemStack getItem() {
            return item;
        }

        /**
         * 检查信息是否有效
         */
        public boolean isValid() {
            // 检查时间戳是否在有效范围内
            long timeDiff = System.currentTimeMillis() - timestamp;
            return timeDiff >= 0 && timeDiff < VALIDITY_THRESHOLD;
        }
    }
}