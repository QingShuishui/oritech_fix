# Oritech FTBChunks 兼容性实现

## 概述
本实现为Oritech MOD添加了完整的FTBChunks区块保护兼容性，确保所有Oritech物品和机器都无法破坏受保护区域的方块。

## 已实现的保护功能

### 1. 工具和武器保护
- **Enderic Railgun (PortableLaserItem)** ✅
  - 挖矿模式权限检查
  - 爆炸模式不受影响（只伤害实体）
  
- **Promethium Pickaxe** ✅
  - 3x3区域挖掘权限检查
  - 每个方块单独验证权限
  
- **Promethium Axe** ✅
  - 树木砍伐权限检查
  - 延迟处理的方块也会检查权限
  
- **Chainsaw** ✅
  - 使用PromethiumAxeItem的权限检查系统
  
- **Drill (基础钻头)** ✅
  - 通过ChunkProtectionEventHandler保护
  
- **WeedKiller (除草剂)** ✅
  - 添加了权限检查到除草逻辑

### 2. 机器保护
- **Destroyer Block Entity** ✅
  - 双重权限检查（hasWorkAvailable + finishBlockWork）
  
- **Deep Drill Entity** ✅
  - 资源节点访问权限检查
  
- **Laser Arm Block Entity** ✅
  - 方块破坏权限检查
  
- **Treefeller Block Entity** ✅
  - 树木砍伐权限检查
  
- **Black Hole Block Entity** ✅
  - 方块吸取权限检查
  
- **Nuclear Explosion Entity** ✅
  - 爆炸范围权限检查

### 3. 事件处理器
- **ChunkProtectionEventHandler** ✅
  - 最后防线，捕获所有Oritech工具的方块破坏
  - 支持Fabric和NeoForge平台
  - 自动识别Oritech工具

## 技术实现

### 权限检查系统
```java
// 玩家权限检查
ChunkProtectionHelper.canBreakBlock(world, pos, player)

// 机器权限检查  
ChunkProtectionHelper.canMachineBreakBlock(world, pos, machinePos, machineName)
```

### FTBChunks API集成
- 使用反射调用FTBChunks API
- 支持玩家和假玩家权限检查
- 优雅降级（FTBChunks未安装时允许操作）

### 事件注册
- **Fabric**: PlayerBlockBreakEvents.BEFORE
- **NeoForge**: BlockEvent.BreakEvent

## 保护范围

### 完全保护的操作
1. 所有Oritech工具的方块破坏
2. 所有Oritech机器的方块破坏
3. 除草剂的杂草清除
4. 黑洞的方块吸取
5. 核爆炸的方块破坏

### 不受影响的操作
1. Enderic Railgun的爆炸伤害（只影响实体）
2. 能量传输到方块实体
3. 粒子效果和声音

## 用户体验

### 权限被拒绝时
- 显示中文提示消息："该区域受到保护，无法破坏方块！"
- 消息显示在ActionBar中
- 包含被保护方块的坐标信息
- 记录到服务器日志

### 性能优化
- 权限检查只在服务端执行
- 使用缓存减少API调用
- 优雅的错误处理

## 测试建议

1. **基础测试**
   - 在FTBChunks保护区域使用各种Oritech工具
   - 验证权限拒绝消息正确显示

2. **机器测试**
   - 在保护区域边界放置Oritech机器
   - 验证机器无法破坏保护区域内的方块

3. **边界测试**
   - 测试保护区域边界的精确性
   - 验证跨区块操作的正确性

## 兼容性

- **Minecraft版本**: 1.21+
- **Fabric**: 完全支持
- **NeoForge**: 完全支持  
- **FTBChunks**: 自动检测，可选依赖

## 日志记录

所有权限拒绝操作都会记录到服务器日志：
```
[Oritech] Blocked Oritech block breaking at [x, y, z] by player PlayerName due to chunk protection
[Oritech] Oritech MachineName at [x, y, z] was blocked from breaking block at [x, y, z] due to chunk protection
```

## 实现完成状态

✅ **完全实现** - 所有Oritech物品和机器都已添加FTBChunks权限检查
✅ **事件注册** - Fabric和NeoForge平台都已正确注册方块破坏事件
✅ **权限检查** - 使用FTBChunks API进行精确的权限验证
✅ **用户反馈** - 权限被拒绝时显示中文提示消息
✅ **日志记录** - 所有权限拒绝操作都会记录到服务器日志
✅ **性能优化** - 只在服务端执行检查，优雅的错误处理

## 总结

现在Oritech MOD已经完全兼容FTBChunks区块保护系统。无论玩家使用任何Oritech工具或机器，都无法破坏其他玩家受保护领地中的方块。这确保了服务器的安全性和玩家之间的公平性。
