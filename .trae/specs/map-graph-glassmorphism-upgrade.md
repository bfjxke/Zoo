# 地图图结构与玻璃态UI升级规格说明

## Why

当前地图系统存在以下问题：

1. **数据结构混乱**：前端和后端使用硬编码的节点数组和连接数组，无法动态扩展
2. **缺乏真正的图语义**：只有简单的坐标和连线，没有图的概念（节点、边、邻接关系）
3. **UI视觉效果过时**：现有的多层圆形设计不够现代，缺少现代UI的玻璃态质感
4. **前后端不一致**：后端用Map<String, Set<String>>定义邻接，前端用独立的links数组

## What Changes

### 前端改进
- 将地图数据重构为**真正的图数据结构**（Graph）
- 引入**玻璃态（Glassmorphism）设计**：透明背景、模糊效果、边框光泽
- 节点使用**圆角矩形卡片**替代圆形，具有玻璃质感
- 连接线使用**渐变发光**效果
- Agent使用**悬浮气泡**显示，带有光晕效果

### 后端改进
- 创建**GraphNode模型类**，替代硬编码的Map
- 创建**GraphEdge模型类**，表示节点间的连接
- 统一前后端数据结构，确保完全一致
- 支持动态加载地图配置（可选）

## Impact

### 受影响的规格
- 前端Dashboard组件显示
- Agent位置追踪系统
- 地图初始化和更新逻辑

### 受影响的代码
- **前端**：`frontend/src/components/GameMap.vue`
- **后端**：`backend/src/main/java/com/guardianeye/iiot/model/GameConstants.java`
- **新增**：`backend/src/main/java/com/guardianeye/iiot/model/GraphNode.java`
- **新增**：`backend/src/main/java/com/guardianeye/iiot/model/GraphEdge.java`
- **新增**：`backend/src/main/java/com/guardianeye/iiot/model/GameGraph.java`

---

## ADDED Requirements

### Requirement: 图数据结构系统

系统**必须**提供真正的图数据结构，包含节点和边的概念。

#### Scenario: 图节点定义
- **WHEN** 定义地图节点时
- **THEN** 系统创建GraphNode实例，包含唯一ID、类型、阵营、坐标、标签

#### Scenario: 图边定义
- **WHEN** 定义节点间连接时
- **THEN** 系统创建GraphEdge实例，包含源节点、目标节点、权重

#### Scenario: 图遍历
- **WHEN** 需要获取某节点的相邻节点时
- **THEN** 系统根据GraphEdge快速返回邻接节点列表

### Requirement: 玻璃态UI设计

地图界面**必须**采用现代玻璃态设计语言。

#### Scenario: 玻璃态节点卡片
- **WHEN** 渲染地图节点时
- **THEN** 节点显示为半透明圆角矩形
- **AND** 背景使用模糊效果（backdrop-filter: blur）
- **AND** 边框使用渐变色，具有光泽感
- **AND** 具有阴影和内发光效果

#### Scenario: 玻璃态连接线
- **WHEN** 渲染节点间连接时
- **THEN** 连接线使用渐变色，从起点阵营色渐变到终点阵营色
- **AND** 连接线具有发光效果
- **AND** 连接线粗细为2-3px，带有透明度

#### Scenario: 玻璃态Agent标记
- **WHEN** 渲染Agent位置时
- **THEN** Agent显示为悬浮气泡
- **AND** 带有阵营色光晕
- **AND** 具有呼吸动画效果

### Requirement: 统一前后端数据结构

前端和后端**必须**使用完全一致的图数据结构。

#### Scenario: 后端图配置
- **WHEN** 后端初始化时
- **THEN** 创建GraphNode和GraphEdge实例
- **AND** 通过API暴露图结构给前端

#### Scenario: 前端图渲染
- **WHEN** 前端获取图数据时
- **THEN** 使用后端提供的图结构渲染
- **AND** 不再使用硬编码的节点数组

---

## MODIFIED Requirements

### Requirement: GameMap组件渲染逻辑

**当前**：使用独立的bases、centers、wildernesses数组硬编码

**修改为**：
- 接收统一的graph对象
- 动态渲染所有节点和边
- 根据节点类型应用不同的样式类

### Requirement: Agent位置追踪

**当前**：通过getAgentPos()方法计算Agent位置

**修改为**：
- 直接从graph节点获取Agent坐标
- 支持多Agent在同节点的堆叠显示
- Agent位置相对于节点中心偏移

---

## REMOVED Requirements

### Requirement: 旧地图数据结构
**Reason**: 硬编码的bases、centers、wildernesses数组无法动态扩展，不符合图数据结构语义

**Migration**: 迁移到统一的GraphNode/GraphEdge系统

### Requirement: 旧UI渲染方式
**Reason**: 多层圆形设计视觉效果过时，无法实现现代玻璃态效果

**Migration**: 重构为圆角矩形玻璃卡片设计

---

## Technical Design

### 前端数据结构

```javascript
// 新的图数据结构
{
  nodes: [
    {
      id: 'base_lawful',
      type: 'base', // base/center/wilderness
      faction: 'lawful', // lawful/aggressive/neutral/none
      label: '守序基地',
      icon: '🏛',
      x: 100, // 百分比位置
      y: 400
    }
  ],
  edges: [
    {
      source: 'base_lawful',
      target: 'center',
      weight: 1
    }
  ]
}
```

### 后端数据结构

```java
// GraphNode.java
public class GraphNode {
    private String id;
    private NodeType type; // BASE, CENTER, WILDERNESS
    private String faction;
    private String label;
    private String icon;
    private int x;
    private int y;
}

// GraphEdge.java
public class GraphEdge {
    private String sourceId;
    private String targetId;
    private int weight;
}

// GameGraph.java
public class GameGraph {
    private Map<String, GraphNode> nodes;
    private List<GraphEdge> edges;
    
    public List<String> getAdjacentNodes(String nodeId);
    public GraphNode getNode(String nodeId);
    public boolean areAdjacent(String node1, String node2);
}
```

### 玻璃态CSS设计

```css
/* 节点玻璃卡片 */
.glass-node {
  background: rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 16px;
  box-shadow: 
    0 8px 32px rgba(0, 0, 0, 0.3),
    inset 0 0 20px rgba(255, 255, 255, 0.1);
}

/* 连接线发光 */
.glass-edge {
  background: linear-gradient(90deg, var(--source-color), var(--target-color));
  filter: drop-shadow(0 0 6px var(--glow-color));
  opacity: 0.7;
}

/* Agent气泡 */
.agent-bubble {
  background: rgba(var(--faction-color), 0.8);
  backdrop-filter: blur(10px);
  border: 2px solid rgba(255, 255, 255, 0.5);
  animation: breathe 2s ease-in-out infinite;
}
```
