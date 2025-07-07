package rearth.oritech.event;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import rearth.oritech.Oritech;
import rearth.oritech.util.ChunkProtectionHelper;

/**
 * 事件处理器，用于监听和阻止Oritech模组在受保护区域的方块破坏行为
 * 这是最后一道防线，确保即使其他检查失败也能保护受保护的区域
 */
public class ChunkProtectionEventHandler {
    
    /**
     * 检查方块破坏事件，如果是Oritech相关的破坏且没有权限则取消
     */
    public static boolean onBlockBreak(World world, BlockPos pos, PlayerEntity player) {
        if (world.isClient()) return true;
        
        // 检查是否是Oritech相关的操作
        if (isOritechRelatedBreaking(player)) {
            // 检查权限
            if (!ChunkProtectionHelper.canBreakBlock(world, pos, player)) {
                ChunkProtectionHelper.sendPermissionDeniedMessage(player, pos);
                Oritech.LOGGER.info("Blocked Oritech block breaking at {} by player {} due to chunk protection", 
                    pos, player.getName().getString());
                return false; // 取消事件
            }
        }
        
        return true; // 允许事件继续
    }
    
    /**
     * 检查是否是Oritech相关的方块破坏操作
     */
    private static boolean isOritechRelatedBreaking(PlayerEntity player) {
        if (player == null) return false;
        
        var mainHandStack = player.getMainHandStack();
        var offHandStack = player.getOffHandStack();
        
        // 检查主手和副手是否持有Oritech工具
        return isOritechTool(mainHandStack.getItem().toString()) || 
               isOritechTool(offHandStack.getItem().toString());
    }
    
    /**
     * 检查物品是否是Oritech工具
     */
    private static boolean isOritechTool(String itemName) {
        return itemName.contains("oritech:") && (
            itemName.contains("laser") ||
            itemName.contains("pickaxe") ||
            itemName.contains("axe") ||
            itemName.contains("chainsaw") ||
            itemName.contains("shovel") ||
            itemName.contains("hoe")
        );
    }
    
    /**
     * 检查假玩家的方块破坏权限
     */
    public static boolean checkFakePlayerPermission(World world, BlockPos pos, BlockPos sourcePos, String machineName) {
        if (world.isClient()) return true;
        
        boolean hasPermission = ChunkProtectionHelper.canMachineBreakBlock(world, pos, sourcePos, machineName);
        
        if (!hasPermission) {
            Oritech.LOGGER.info("Blocked {} at {} from breaking block at {} due to chunk protection", 
                machineName, sourcePos, pos);
        }
        
        return hasPermission;
    }
    
    /**
     * 初始化事件监听器
     * 这个方法应该在模组初始化时调用
     */
    public static void initialize() {
        Oritech.LOGGER.info("Chunk protection event handler initialized for Oritech");
        
        // 检查FTBChunks是否安装
        if (ChunkProtectionHelper.isFTBChunksInstalled()) {
            Oritech.LOGGER.info("FTBChunks detected, chunk protection is active");
        } else {
            Oritech.LOGGER.info("FTBChunks not detected, chunk protection is disabled");
        }
    }
}
