package rearth.oritech.block.entity.pipes;

import com.google.common.collect.Streams;
import dev.architectury.fluid.FluidStack;
import dev.architectury.hooks.fluid.FluidStackHooks;
import net.minecraft.block.BlockState;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import rearth.oritech.Oritech;
import rearth.oritech.api.fluid.FluidApi;
import rearth.oritech.block.blocks.pipes.ExtractablePipeConnectionBlock;
import rearth.oritech.block.blocks.pipes.fluid.FluidPipeBlock;
import rearth.oritech.block.blocks.pipes.fluid.FluidPipeConnectionBlock;
import rearth.oritech.init.BlockEntitiesContent;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class FluidPipeInterfaceEntity extends ExtractablePipeInterfaceEntity {

    public static final int MAX_TRANSFER_RATE = (int) (FluidStackHooks.bucketAmount() * Oritech.CONFIG.fluidPipeExtractAmountBuckets());
    private static final int TRANSFER_PERIOD = Oritech.CONFIG.fluidPipeExtractIntervalDuration();

    private List<FluidApi.FluidStorage> filteredFluidTargetsCached;

    // 用于限制日志输出频率的字段
    private long lastWarningTime = 0;
    private static final long WARNING_COOLDOWN = 30000; // 30秒冷却时间
    
    public FluidPipeInterfaceEntity(BlockPos pos, BlockState state) {
        super(BlockEntitiesContent.FLUID_PIPE_ENTITY, pos, state);
    }
    
    @Override
    public void tick(World world, BlockPos pos, BlockState state, GenericPipeInterfaceEntity blockEntity) {
        var block = (ExtractablePipeConnectionBlock) state.getBlock();
        if (world.isClient || !block.isExtractable(state)) return;
        
        var boosted = isBoostAvailable();
        
        // boosted pipe works every tick, otherwise only every N tick
        if (world.getTime() % TRANSFER_PERIOD != 0 && !boosted)
            return;
        
        var data = FluidPipeBlock.FLUID_PIPE_DATA.getOrDefault(world.getRegistryKey().getValue(), new PipeNetworkData());
        var transferAmount = boosted ? MAX_TRANSFER_RATE * 100 : MAX_TRANSFER_RATE;
        
        // try to get fluid to transfer
        // one transaction for each side
        var stackToMove = FluidStack.empty();
        FluidApi.FluidStorage takenFrom = null;
        var sources = data.machineInterfaces.getOrDefault(pos, new HashSet<>());
        
        for (var sourcePos : sources) {
            try {
                var offset = pos.subtract(sourcePos);
                var direction = Direction.fromVector(offset.getX(), offset.getY(), offset.getZ());

                // 添加空指针检查
                if (direction == null) {
                    Oritech.LOGGER.warn("Invalid direction vector for fluid pipe at position {}, sourcePos: {}", pos, sourcePos);
                    continue;
                }

                if (!block.isSideExtractable(state, direction.getOpposite())) continue;
                var sourceContainer = FluidApi.BLOCK.find(world, sourcePos, direction);
                if (sourceContainer == null || !sourceContainer.supportsExtraction()) continue;

                var contents = sourceContainer.getContent();
                // 添加内容空指针检查
                if (contents == null) {
                    Oritech.LOGGER.warn("Fluid container returned null contents at position {}", sourcePos);
                    continue;
                }

                var extractionCandidate = Streams.stream(contents)
                                            .filter(candidate -> candidate != null && !candidate.isEmpty())
                                            .filter(candidate -> {
                                                try {
                                                    return sourceContainer.extract(candidate, true) > 0;
                                                } catch (Exception e) {
                                                    Oritech.LOGGER.warn("Error during fluid extraction simulation at {}: {}", sourcePos, e.getMessage());
                                                    return false;
                                                }
                                            })
                                            .findFirst();
            
                if (extractionCandidate.isPresent()) {
                    try {
                        var extractionTest = extractionCandidate.get().copyWithAmount(transferAmount);
                        var movedAmount = sourceContainer.extract(extractionTest, true);
                        stackToMove = extractionTest;
                        stackToMove.setAmount(movedAmount);
                        takenFrom = sourceContainer;
                        break;
                    } catch (Exception e) {
                        Oritech.LOGGER.warn("Error during fluid extraction test at {}: {}", sourcePos, e.getMessage());
                        continue;
                    }
                }
            } catch (Exception e) {
                Oritech.LOGGER.error("Critical error in fluid pipe processing at position {}, sourcePos {}: {}", pos, sourcePos, e.getMessage());
                // 如果出现严重错误，破坏管道并通知玩家
                world.breakBlock(pos, true);
                notifyNearbyPlayersOfFluidViolation(pos, world);
                return;
            }
        }
        
        // if one (or more) of connected blocks has fluid available (of first found type, only transfer one type per tick)
        // gather all connection targets supporting insertion
        // shuffle em
        // insert until no more fluid to output is available
        if (stackToMove.isEmpty() || takenFrom == null) return;
        
        var targets = findNetworkTargets(pos, data);
        
        if (targets == null) {
            System.err.println("Yeah your pipe network likely is too long. At: " + this.getPos());
            return;
        }
        
        var netHash = targets.hashCode();
        
        if (netHash != filteredTargetsNetHash || filteredFluidTargetsCached == null) {
            filteredFluidTargetsCached = targets.stream()
                                           .filter(target -> {
                                               var direction = target.getRight();
                                               var pipePos = target.getLeft().add(direction.getVector());
                                               var pipeState = world.getBlockState(pipePos);
                                               if (!(pipeState.getBlock() instanceof FluidPipeConnectionBlock fluidBlock))
                                                   return true;   // edge case, this should never happen
                                               var extracting = fluidBlock.isSideExtractable(pipeState, target.getRight().getOpposite());
                                               return !extracting;
                                           })
                                           .map(target -> FluidApi.BLOCK.find(world, target.getLeft(), target.getRight()))
                                           .filter(obj -> Objects.nonNull(obj) && obj.supportsInsertion())
                                           .collect(Collectors.toList());
            
            filteredTargetsNetHash = netHash;
        }
        
        // 额外的安全检查
        if (filteredFluidTargetsCached == null || filteredFluidTargetsCached.isEmpty()) {
            // 限制警告日志的输出频率，避免日志刷屏
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastWarningTime > WARNING_COOLDOWN) {
                Oritech.LOGGER.warn("No valid fluid targets found for pipe at position {} (this warning is rate-limited)", pos);
                lastWarningTime = currentTime;
            }
            return;
        }

        Collections.shuffle(filteredFluidTargetsCached);

        var availableFluid = stackToMove.getAmount();

        for (var targetStorage : filteredFluidTargetsCached) {
            if (targetStorage == null) {
                // 这种情况比较少见，可以保持正常的警告频率
                Oritech.LOGGER.warn("Null target storage found in fluid pipe at position {}", pos);
                continue;
            }

            try {
                var transferred = targetStorage.insert(stackToMove, false);
                stackToMove.shrink(transferred);
                targetStorage.update();

                if (stackToMove.getAmount() <= 0) break;
            } catch (Exception e) {
                Oritech.LOGGER.error("Error during fluid insertion at position {}: {}", pos, e.getMessage());
                // 继续处理下一个目标，而不是崩溃
                continue;
            }
        }
        
        var moved = availableFluid - stackToMove.getAmount();
        if (moved > 0 && takenFrom != null) {
            try {
                stackToMove.setAmount(moved);
                var actualExtracted = takenFrom.extract(stackToMove, false);

                // 验证实际提取是否成功
                if (actualExtracted <= 0) {
                    Oritech.LOGGER.error("CRITICAL: Fluid extraction failed but insertion succeeded at position {}. Destroying pipe.", pos);
                    world.breakBlock(pos, true);
                    notifyNearbyPlayersOfFluidViolation(pos, world);
                    return;
                }

                onBoostUsed();
                takenFrom.update();
            } catch (Exception e) {
                Oritech.LOGGER.error("Critical error during fluid extraction at position {}: {}", pos, e.getMessage());
                world.breakBlock(pos, true);
                notifyNearbyPlayersOfFluidViolation(pos, world);
                return;
            }
        }

    }

    /**
     * 向指定坐标附近的玩家发送流体传输违规警告
     * @param violationPos 违规发生的坐标
     * @param world 世界实例
     */
    private void notifyNearbyPlayersOfFluidViolation(BlockPos violationPos, World world) {
        if (world.isClient) return;

        // 创建32格范围的检测区域
        var notificationRange = 32.0;
        var box = new Box(
            violationPos.getX() - notificationRange, violationPos.getY() - notificationRange, violationPos.getZ() - notificationRange,
            violationPos.getX() + notificationRange, violationPos.getY() + notificationRange, violationPos.getZ() + notificationRange
        );

        // 获取范围内的所有玩家
        var nearbyPlayers = world.getEntitiesByClass(ServerPlayerEntity.class, box, player -> !player.isSpectator());

        // 向每个玩家发送警告消息
        for (var player : nearbyPlayers) {
            // 发送ActionBar消息（左下角显示）- 使用硬编码文本确保服务端能正确显示
            var warningMessage = Text.literal("§c§l[MinePixel]检测到流体传输异常！坐标: " +
                violationPos.getX() + ", " + violationPos.getY() + ", " + violationPos.getZ());

            player.sendMessage(warningMessage, true); // true表示发送到ActionBar

            // 同时发送聊天消息作为备份
            var chatMessage = Text.literal("§4[MinePixel]流体管道异常已记录，禁止使用该BUG");

            player.sendMessage(chatMessage, false); // false表示发送到聊天框
        }
    }
}
