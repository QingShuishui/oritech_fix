package rearth.oritech.util;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import rearth.oritech.Oritech;

/**
 * 用于检查区块保护权限的工具类
 * 支持FTBChunks等区块保护模组的权限检查
 */
public class ChunkProtectionHelper {
    
    /**
     * 检查玩家是否有权限在指定位置破坏方块
     * 
     * @param world 世界
     * @param pos 方块位置
     * @param player 玩家（可以是真实玩家或假玩家）
     * @return true如果有权限，false如果被保护
     */
    public static boolean canBreakBlock(World world, BlockPos pos, PlayerEntity player) {
        if (world.isClient()) return true;

        // 注意：Enderic Railgun现在使用Minecraft原生权限系统
        // 这个方法主要用于其他Oritech工具和机器

        try {
            // 使用Minecraft原生权限检查
            return world.canPlayerModifyAt(player, pos);
        } catch (Exception e) {
            // 如果检查失败，采用保守策略
            Oritech.LOGGER.warn("Permission check failed at {}: {}", pos, e.getMessage());
            return false;
        }
    }
    
    /**
     * 检查机器是否有权限在指定位置破坏方块
     * 
     * @param world 世界
     * @param pos 方块位置
     * @param machinePos 机器位置
     * @param machineName 机器名称（用于日志）
     * @return true如果有权限，false如果被保护
     */
    public static boolean canMachineBreakBlock(World world, BlockPos pos, BlockPos machinePos, String machineName) {
        if (world.isClient()) return true;

        // 简化的机器权限检查：只允许在同一区块内操作
        int machineChunkX = machinePos.getX() >> 4;
        int machineChunkZ = machinePos.getZ() >> 4;
        int targetChunkX = pos.getX() >> 4;
        int targetChunkZ = pos.getZ() >> 4;

        if (machineChunkX == targetChunkX && machineChunkZ == targetChunkZ) {
            // 同一区块内，允许操作
            return true;
        } else {
            // 跨区块操作，拒绝
            Oritech.LOGGER.info("Machine {} at {} denied cross-chunk operation to {}", machineName, machinePos, pos);
            return false;
        }
    }


    
    /**
     * 检查是否安装了FTBChunks
     */
    public static boolean isFTBChunksInstalled() {
        try {
            Class.forName("dev.ftb.mods.ftbchunks.api.FTBChunksAPI");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
    
    /**
     * 向玩家发送权限被拒绝的消息
     */
    public static void sendPermissionDeniedMessage(PlayerEntity player, BlockPos pos) {
        if (player instanceof ServerPlayerEntity serverPlayer) {
            // 发送硬编码的中文消息，确保服务端能正确显示
            var message = net.minecraft.text.Text.literal("§c[MinePixel]该区域受到保护，无法破坏方块！坐标: " + 
                pos.getX() + ", " + pos.getY() + ", " + pos.getZ());
            serverPlayer.sendMessage(message, true); // true表示发送到ActionBar
        }
    }
}
