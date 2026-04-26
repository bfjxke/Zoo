# 地图图结构与玻璃态UI升级任务清单

## 阶段1：后端图数据结构重构

- [x] Task 1.1: 创建GraphNode模型类 ✅
  - 定义节点属性（id, type, faction, label, icon, x, y）
  - 创建NodeType枚举（BASE, CENTER, WILDERNESS）
  - 实现getter/setter和toString方法

- [x] Task 1.2: 创建GraphEdge模型类 ✅
  - 定义边属性（sourceId, targetId, weight）
  - 实现构造函数和验证逻辑

- [x] Task 1.3: 创建GameGraph图管理器类 ✅
  - 实现节点和边的存储结构
  - 实现getAdjacentNodes()方法获取邻接节点
  - 实现areAdjacent()方法检查连通性
  - 实现getNode()和getAllNodes()方法

- [x] Task 1.4: 重构GameConstants为图数据结构 ✅
  - 将ADJACENT_NODES转换为GraphNode和GraphEdge
  - 创建GameGraph单例或工厂方法
  - 保持向后兼容的常量定义

- [x] Task 1.5: 创建图结构API端点 ✅
  - 添加GET /api/graph端点返回完整图结构
  - 确保JSON序列化正确

## 阶段2：前端图数据结构实现

- [x] Task 2.1: 创建图数据结构定义 ✅
  - 定义nodes数组和edges数组结构
  - 创建节点类型和阵营类型常量
  - 添加API调用方法

- [x] Task 2.2: 创建图组件GraphNode.vue ✅
  - 实现玻璃态节点卡片设计
  - 支持不同节点类型（base/center/wilderness）
  - 实现hover和active状态

- [x] Task 2.3: 创建边组件GraphEdge.vue ✅
  - 实现渐变发光连接线
  - 支持动态计算起点终点坐标
  - 实现线段裁剪（不进入节点内部）

- [x] Task 2.4: 重构GameMap.vue为图渲染器 ✅
  - 移除硬编码的bases/centers/wildernesses
  - 使用统一的graph数据渲染
  - 实现动态图布局
  - Agent堆叠显示

- [ ] Task 2.5: 创建玻璃态样式系统
  - 定义CSS变量（颜色、透明度、模糊度）
  - 实现.glass-card通用玻璃卡片样式
  - 实现.glow-line发光线条样式

## 阶段3：UI视觉效果增强

- [ ] Task 3.1: 实现节点玻璃态效果
  - 半透明背景（rgba）
  - backdrop-filter模糊效果
  - 渐变边框和内发光
  - 阴影效果

- [ ] Task 3.2: 实现连接线渐变发光
  - 从起点色渐变到终点色
  - 添加drop-shadow发光滤镜
  - 调整透明度和粗细

- [ ] Task 3.3: 实现Agent悬浮气泡
  - 阵营色光晕
  - 呼吸动画（scale pulse）
  - 堆叠显示（同节点多Agent）

- [ ] Task 3.4: 添加交互效果
  - 节点hover高亮
  - 连接线hover增强发光
  - Agent点击显示详情

## 阶段4：集成与测试

- [ ] Task 4.1: 后端API集成测试
  - 测试图结构API返回正确数据
  - 验证JSON格式正确性
  - 测试邻接关系查询

- [ ] Task 4.2: 前端渲染测试
  - 测试图结构正确渲染
  - 验证所有7个节点显示
  - 验证所有8条边显示

- [ ] Task 4.3: Agent位置追踪测试
  - 测试Agent在节点上的位置
  - 测试多Agent堆叠显示
  - 测试Agent移动时的位置更新

- [ ] Task 4.4: 玻璃态效果测试
  - 验证模糊效果正常显示
  - 测试透明背景
  - 验证动画效果流畅

## 任务依赖关系

```
Task 1.1 ─┬─> Task 1.2 ─> Task 1.3 ─> Task 1.4 ─> Task 1.5 ✅
          │                                       │
          └───────────────────────────────────────┘
                              │
                              ▼
Task 2.1 ─┬─> Task 2.2 ─┬─> Task 2.3 ─> Task 2.4 ✅
          │             │
          └─────────────┘
                 │
                 ▼
            Task 2.5 ⏳ ─> Task 3.1 ─> Task 3.2 ─> Task 3.3 ─> Task 3.4
                 │
                 └───────────────────────────────────>
                                │
                                ▼
                      Task 4.1 ⏳ ─> Task 4.2 ─> Task 4.3 ─> Task 4.4
```

## 优先级排序

1. ✅ **已完成**：后端图数据结构（Task 1.1 - 1.5）
2. ✅ **已完成**：前端图渲染器核心（Task 2.1 - 2.4）
3. ⏳ **待完成**：玻璃态样式系统（Task 2.5 - 3.4）
4. ⏳ **待完成**：集成测试（Task 4.1 - 4.4）

## 完成状态

**核心功能已完成**：
- ✅ 后端图数据结构（GraphNode, GraphEdge, GameGraph）
- ✅ 图API端点（GET /api/graph）
- ✅ 前端图渲染器（GameMap.vue）
- ✅ 玻璃态节点组件（GraphNode.vue）
- ✅ 渐变边组件（GraphEdge.vue）
- ✅ Agent堆叠显示
- ✅ Agent呼吸动画

**待优化**：
- ⏳ 高级CSS样式细节
- ⏳ 完整交互功能
- ⏳ 集成测试
