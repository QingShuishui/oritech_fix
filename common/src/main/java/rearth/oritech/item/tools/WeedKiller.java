package rearth.oritech.item.tools;

import net.minecraft.block.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import rearth.oritech.client.init.ParticleContent;
import rearth.oritech.util.ChunkProtectionHelper;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;

public class WeedKiller extends Item {
    public WeedKiller(Settings settings) {
        super(settings);
    }
    
    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        if (context.getWorld().isClient())
            return super.useOnBlock(context);

        var startPos = context.getBlockPos();
        var player = context.getPlayer();

        new Thread(() -> doWeedKilling(context.getWorld(), startPos, player)).start();

        context.getStack().decrementUnlessCreative(1, context.getPlayer());

        return ActionResult.SUCCESS;
    }
    
    private void doWeedKilling(World world, BlockPos startPos, net.minecraft.entity.player.PlayerEntity player) {

        var maxRange = 20;
        var spreadRange = 3;
        var visited = new HashSet<BlockPos>();
        var open = new ArrayDeque<BlockPos>();
        open.add(startPos);

        while (!open.isEmpty()) {
            var candidate = open.pop();

            for (int x = -spreadRange; x <= spreadRange; x++) {
                for (int y = -1; y <= 1; y++) {
                    for (int z = -spreadRange; z <= spreadRange; z++) {

                        var target = new BlockPos(candidate.add(x,y,z));

                        if (visited.contains(target)) continue;
                        var distance = target.getManhattanDistance(startPos);

                        if (isWeedBlock(target, world) && distance < maxRange) {
                            // 检查区块保护权限
                            if (player != null && !ChunkProtectionHelper.canBreakBlock(world, target, player)) {
                                // 如果没有权限，跳过这个方块但继续处理其他方块
                                visited.add(target);
                                continue;
                            }

                            open.add(target);
                            world.setBlockState(target, Blocks.AIR.getDefaultState());

                            ParticleContent.WEED_KILLER.spawn(world, target.toCenterPos(), new ParticleContent.LineData(candidate.toCenterPos(), target.toCenterPos()));

                            try {
                                Thread.sleep(50);
                            } catch (InterruptedException e) {
                                throw new RuntimeException(e);
                            }

                        }

                        visited.add(target);

                    }
                }
            }

        }

    }
    
    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        super.appendTooltip(stack, context, tooltip, type);
        tooltip.add(Text.translatable("tooltip.oritech.weed_killer").formatted(Formatting.GRAY));
    }
    
    private boolean isWeedBlock(BlockPos pos, World world) {
        var state = world.getBlockState(pos);
        if (state.isAir() || state.getFluidState().isStill()) return false;
        return state.isReplaceable() || state.isIn(BlockTags.FLOWERS);
    }
}
