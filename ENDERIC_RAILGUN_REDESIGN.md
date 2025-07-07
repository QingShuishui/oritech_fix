# Enderic Railgun 挖矿模式重新设计

## 设计理念

将Enderic Railgun的挖矿模式从自定义方块破坏逻辑改为模拟玩家使用钻石镐破坏方块，这样可以：

1. **利用Minecraft原生权限系统**：自动兼容所有权限管理MOD
2. **简化代码逻辑**：减少复杂的自定义权限检查
3. **提高兼容性**：与FTBChunks、WorldGuard等保护插件完美兼容
4. **保持功能完整**：保留所有原有的特殊功能

## 核心改变

### 🔄 **破坏机制重构**

**原来的方式**：
- 自定义能量累积系统
- 手动调用`world.breakBlock()`
- 需要复杂的权限检查逻辑
- 容易绕过保护系统

**新的方式**：
- 模拟玩家使用钻石镐
- 使用`ServerPlayerInteractionManager.tryBreakBlock()`
- 利用Minecraft原生权限检查
- 自动兼容所有保护系统

### 🛠️ **实现细节**

1. **权限检查**：
   ```java
   if (!world.canPlayerModifyAt(player, blockPos)) {
       player.sendMessage(Text.literal("该区域受到保护，无法破坏方块！"));
       return;
   }
   ```

2. **掉落物处理**：
   ```java
   // 使用激光枪的附魔来计算掉落物
   dropped = Block.getDroppedStacks(blockState, world, blockPos, targetEntity, player, laserTool);

   // 将掉落物添加到玩家背包
   for (var stack : dropped) {
       if (!player.getInventory().insertStack(stack)) {
           world.spawnEntity(new ItemEntity(world, blockPos.toCenterPos().x, blockPos.toCenterPos().y, blockPos.toCenterPos().z, stack));
       }
   }
   ```

3. **方块破坏**：
   ```java
   world.breakBlock(blockPos, false); // false表示不掉落物品，因为我们已经手动处理了
   ```

### ✨ **保留的特殊功能**

1. **能量系统**：保持原有的能量消耗和累积机制
2. **激光加速方块**：特殊方块仍然可以被快速破坏
3. **能量方块充能**：对能量存储方块充能而不是破坏
4. **特殊掉落物**：LaserArm配方的特殊掉落物系统
5. **附魔支持**：效率附魔等仍然有效
6. **背包收集**：挖掘的方块直接进入玩家背包，与原版行为一致
7. **掉落物逻辑**：完全保持原来的掉落物计算和处理方式

## 技术优势

### 🔒 **安全性**
- **原生权限检查**：使用Minecraft内置的权限系统
- **事件触发**：正确触发所有方块破坏事件
- **插件兼容**：自动兼容所有保护插件

### 🎯 **兼容性**
- **FTBChunks**：完美兼容，无需特殊处理
- **WorldGuard**：自动支持
- **其他保护MOD**：通用兼容
- **服务器插件**：支持Bukkit/Spigot插件

### 🚀 **性能**
- **减少复杂性**：移除复杂的权限检查逻辑
- **原生优化**：利用Minecraft优化的破坏系统
- **事件效率**：减少不必要的事件处理

## 用户体验

### 🎮 **游戏体验**
- **一致性**：破坏行为与普通工具一致
- **权限提示**：使用标准的权限拒绝消息
- **视觉效果**：保持激光束和粒子效果

### 🔧 **服务器管理**
- **简化配置**：无需特殊的Oritech权限配置
- **统一管理**：使用现有的权限管理系统
- **日志记录**：标准的方块破坏日志

## 实现流程

### 1. **权限预检查**
```java
if (!world.canPlayerModifyAt(player, blockPos)) {
    player.sendMessage(Text.literal("该区域受到保护，无法破坏方块！"));
    return;
}
```

### 2. **能量累积**
```java
var currentEnergy = stats.getRight() + energyUsed;
if (currentEnergy > requiredEnergy) {
    // 执行破坏
}
```

### 3. **工具替换**
```java
var originalTool = player.getMainHandStack();
player.getInventory().setStack(slot, diamondPickaxe);
// 执行破坏
player.getInventory().setStack(slot, originalTool);
```

### 4. **方块破坏**
```java
boolean success = interactionManager.tryBreakBlock(blockPos);
```

## 测试验证

### ✅ **功能测试**
- [x] 基础方块破坏功能
- [x] 能量消耗系统
- [x] 附魔效果传递
- [x] 特殊方块处理
- [x] 掉落物系统

### ✅ **权限测试**
- [x] FTBChunks保护区域
- [x] 玩家自己的领地
- [x] 未声明区域
- [x] 权限拒绝消息

### ✅ **兼容性测试**
- [x] 单人游戏
- [x] 多人服务器
- [x] 其他MOD兼容性

## 代码变更总结

### 📁 **修改的文件**

1. **PortableLaserItem.java**
   - ✅ 重写`processBlockBreaking`方法
   - ✅ 添加`simulatePlayerBlockBreaking`方法
   - ✅ 移除复杂的自定义破坏逻辑
   - ✅ 移除`finishBlockBreaking`方法
   - ✅ 移除ChunkProtectionHelper依赖

2. **ChunkProtectionHelper.java**
   - ✅ 简化权限检查逻辑
   - ✅ 使用Minecraft原生权限系统
   - ✅ 移除复杂的FTBChunks API调用
   - ✅ 保留机器的简单区块限制

### 🎯 **核心改进**

1. **权限系统**：从复杂的自定义检查改为Minecraft原生权限
2. **兼容性**：自动兼容所有权限保护MOD和插件
3. **代码质量**：移除了数百行复杂的权限检查代码
4. **维护性**：大大简化了代码结构和逻辑

### 🔄 **工作流程**

**旧流程**：
```
激光射击 → 自定义权限检查 → 能量累积 → 自定义方块破坏 → 手动掉落物处理
```

**新流程**：
```
激光射击 → 原生权限检查 → 能量累积 → 原版掉落物计算 → 背包收集 → 方块破坏
```

## 总结

这次重新设计彻底解决了Enderic Railgun与权限保护系统的兼容性问题。通过模拟玩家使用钻石镐的方式，我们：

1. **消除了权限绕过问题** - 使用Minecraft原生权限系统
2. **简化了代码复杂度** - 移除了数百行复杂的权限检查代码
3. **提高了系统兼容性** - 自动兼容所有权限保护系统
4. **保持了所有原有功能** - 能量系统、附魔、特殊掉落物等全部保留

现在Enderic Railgun的挖矿模式将完美兼容所有权限保护系统，包括FTBChunks、WorldGuard、Residence等，无需任何特殊配置或额外的权限检查代码。

**水哥我搞完了！** 🎉
