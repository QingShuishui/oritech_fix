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
        
        try {
            // 尝试使用FTBChunks API检查权限
            return checkFTBChunksPermission(world, pos, player);
        } catch (Exception e) {
            // 如果FTBChunks不存在或出错，记录警告但允许操作
            Oritech.LOGGER.warn("Failed to check chunk protection at {}: {}", pos, e.getMessage());
            return true;
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
        
        try {
            // 尝试使用FTBChunks API检查机器权限
            boolean canBreak = checkFTBChunksMachinePermission(world, pos, machinePos);
            
            if (!canBreak) {
                Oritech.LOGGER.info("Oritech {} at {} was blocked from breaking block at {} due to chunk protection", 
                    machineName, machinePos, pos);
            }
            
            return canBreak;
        } catch (Exception e) {
            // 如果FTBChunks不存在或出错，记录警告但允许操作
            Oritech.LOGGER.warn("Failed to check chunk protection for {} at {}: {}", machineName, pos, e.getMessage());
            return true;
        }
    }
    
    /**
     * 使用反射检查FTBChunks权限
     */
    private static boolean checkFTBChunksPermission(World world, BlockPos pos, PlayerEntity player) {
        try {
            // 尝试加载FTBChunks的API类
            Class<?> ftbChunksAPI = Class.forName("dev.ftb.mods.ftbchunks.api.FTBChunksAPI");
            Class<?> claimResult = Class.forName("dev.ftb.mods.ftbchunks.api.ClaimResult");
            
            // 获取API实例
            Object apiInstance = ftbChunksAPI.getMethod("api").invoke(null);
            
            // 检查权限：canPlayerBreakBlock(ServerWorld world, BlockPos pos, ServerPlayerEntity player)
            Object result = apiInstance.getClass()
                .getMethod("canPlayerBreakBlock", ServerWorld.class, BlockPos.class, ServerPlayerEntity.class)
                .invoke(apiInstance, world, pos, player);
            
            // 检查结果是否为SUCCESS
            Object successValue = claimResult.getField("SUCCESS").get(null);
            return result.equals(successValue);
            
        } catch (ClassNotFoundException e) {
            // FTBChunks未安装，允许操作
            return true;
        } catch (Exception e) {
            // 其他错误，记录但允许操作
            Oritech.LOGGER.debug("Error checking FTBChunks permission: {}", e.getMessage());
            return true;
        }
    }
    
    /**
     * 检查机器在FTBChunks中的权限
     */
    private static boolean checkFTBChunksMachinePermission(World world, BlockPos pos, BlockPos machinePos) {
        try {
            // 尝试加载FTBChunks的API类
            Class<?> ftbChunksAPI = Class.forName("dev.ftb.mods.ftbchunks.api.FTBChunksAPI");
            Class<?> claimResult = Class.forName("dev.ftb.mods.ftbchunks.api.ClaimResult");
            
            // 获取API实例
            Object apiInstance = ftbChunksAPI.getMethod("api").invoke(null);
            
            // 检查权限：canFakePlayerBreakBlock(ServerWorld world, BlockPos pos, BlockPos sourcePos)
            Object result = apiInstance.getClass()
                .getMethod("canFakePlayerBreakBlock", ServerWorld.class, BlockPos.class, BlockPos.class)
                .invoke(apiInstance, world, pos, machinePos);
            
            // 检查结果是否为SUCCESS
            Object successValue = claimResult.getField("SUCCESS").get(null);
            return result.equals(successValue);
            
        } catch (ClassNotFoundException e) {
            // FTBChunks未安装，允许操作
            return true;
        } catch (Exception e) {
            // 其他错误，记录但允许操作
            Oritech.LOGGER.debug("Error checking FTBChunks machine permission: {}", e.getMessage());
            return true;
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
