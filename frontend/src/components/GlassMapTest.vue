<template>
  <div class="test-container">
    <h1>🧪 图结构与玻璃态UI测试页面</h1>
    
    <div class="test-section">
      <h2>1. 图数据结构加载测试</h2>
      <div v-if="loading" class="loading">加载中...</div>
      <div v-else-if="error" class="error">{{ error }}</div>
      <div v-else class="success">
        <p>✅ 图数据加载成功！</p>
        <p>节点数量: {{ graphData.nodeCount }}</p>
        <p>边数量: {{ graphData.edgeCount }}</p>
      </div>
    </div>
    
    <div class="test-section" v-if="graphData.nodes">
      <h2>2. 节点列表</h2>
      <div class="nodes-list">
        <div 
          v-for="node in graphData.nodes" 
          :key="node.id"
          class="glass-node"
          :class="`type-${node.type.toLowerCase()}`"
        >
          <span class="node-icon">{{ getNodeIcon(node) }}</span>
          <span class="node-label">{{ getNodeLabel(node) }}</span>
          <span class="node-id">{{ node.id }}</span>
        </div>
      </div>
    </div>
    
    <div class="test-section" v-if="graphData.edges">
      <h2>3. 边列表</h2>
      <div class="edges-list">
        <div 
          v-for="edge in graphData.edges" 
          :key="`${edge.sourceId}-${edge.targetId}`"
          class="edge-item"
        >
          {{ edge.sourceId }} ↔ {{ edge.targetId }}
        </div>
      </div>
    </div>
    
    <div class="test-section">
      <h2>4. 组件引用检查</h2>
      <p>GraphNode.vue: {{ componentCheck.graphNode ? '✅ 已引用' : '❌ 未引用' }}</p>
      <p>GraphEdge.vue: {{ componentCheck.graphEdge ? '✅ 已引用' : '❌ 未引用' }}</p>
    </div>
    
    <div class="test-section">
      <h2>5. CSS样式检查</h2>
      <div class="css-test">
        <div class="glass-box">
          这是玻璃态测试
        </div>
      </div>
    </div>
    
    <div class="actions">
      <button @click="loadGraph">🔄 重新加载图数据</button>
      <button @click="openMainPage">📱 打开主页面</button>
    </div>
  </div>
</template>

<script>
import { sandboxApi } from '../api/index.js'
import GraphNode from './GraphNode.vue'
import GraphEdge from './GraphEdge.vue'

export default {
  name: 'GlassMapTest',
  components: {
    GraphNode,
    GraphEdge
  },
  data() {
    return {
      loading: true,
      error: null,
      graphData: {},
      componentCheck: {
        graphNode: !!GraphNode,
        graphEdge: !!GraphEdge
      }
    }
  },
  mounted() {
    this.loadGraph()
  },
  methods: {
    async loadGraph() {
      this.loading = true
      this.error = null
      try {
        const response = await sandboxApi.getGraph()
        this.graphData = response.data
        this.loading = false
      } catch (error) {
        this.error = '加载失败: ' + error.message
        this.loading = false
      }
    },
    getNodeIcon(node) {
      if (node.type === 'CENTER') return '⭐'
      if (node.type === 'WILDERNESS') {
        switch (node.id) {
          case 'forest': return '🌲'
          case 'mountain': return '⛰'
          case 'river': return '🌊'
          default: return '🌲'
        }
      }
      if (node.type === 'BASE') {
        switch (node.faction) {
          case 'lawful': return '🏛'
          case 'aggressive': return '⚔'
          case 'neutral': return '⚖'
          default: return '🏛'
        }
      }
      return '📍'
    },
    getNodeLabel(node) {
      if (node.label) return node.label
      if (node.type === 'CENTER') return '中心区域'
      if (node.type === 'WILDERNESS') {
        switch (node.id) {
          case 'forest': return '森林'
          case 'mountain': return '山地'
          case 'river': return '河流'
          default: return '野外'
        }
      }
      if (node.type === 'BASE') {
        switch (node.faction) {
          case 'lawful': return '守序基地'
          case 'aggressive': return '激进基地'
          case 'neutral': return '中立基地'
          default: return '基地'
        }
      }
      return node.id
    },
    openMainPage() {
      window.open('/', '_blank')
    }
  }
}
</script>

<style scoped>
.test-container {
  padding: 20px;
  background: #1a1a2e;
  color: white;
  min-height: 100vh;
}

h1 {
  text-align: center;
  color: #ffd700;
  margin-bottom: 30px;
}

h2 {
  color: #3b82f6;
  border-bottom: 2px solid #3b82f6;
  padding-bottom: 10px;
  margin-top: 30px;
}

.test-section {
  background: rgba(255, 255, 255, 0.05);
  border-radius: 12px;
  padding: 20px;
  margin: 20px 0;
}

.loading {
  color: #ffd700;
  font-size: 18px;
}

.error {
  color: #ef4444;
  background: rgba(239, 68, 68, 0.2);
  padding: 15px;
  border-radius: 8px;
}

.success {
  color: #22c55e;
  background: rgba(34, 197, 94, 0.2);
  padding: 15px;
  border-radius: 8px;
}

.nodes-list {
  display: flex;
  flex-wrap: wrap;
  gap: 15px;
}

.glass-node {
  background: rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 12px;
  padding: 15px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  min-width: 120px;
  transition: all 0.3s ease;
}

.glass-node:hover {
  transform: scale(1.05);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.3);
}

.glass-node.type-base {
  border-color: rgba(59, 130, 246, 0.5);
}

.glass-node.type-center {
  border-color: rgba(255, 215, 0, 0.5);
}

.glass-node.type-wilderness {
  border-color: rgba(34, 197, 94, 0.5);
}

.node-icon {
  font-size: 32px;
}

.node-label {
  font-size: 14px;
  font-weight: 600;
  color: white;
}

.node-id {
  font-size: 10px;
  color: rgba(255, 255, 255, 0.6);
  font-family: monospace;
}

.edges-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(150px, 1fr));
  gap: 10px;
}

.edge-item {
  background: rgba(255, 255, 255, 0.05);
  padding: 10px;
  border-radius: 8px;
  text-align: center;
  font-family: monospace;
}

.actions {
  display: flex;
  gap: 15px;
  justify-content: center;
  margin-top: 30px;
}

button {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  padding: 12px 24px;
  border-radius: 8px;
  cursor: pointer;
  font-size: 16px;
  transition: all 0.3s ease;
}

button:hover {
  transform: scale(1.05);
  box-shadow: 0 8px 32px rgba(102, 126, 234, 0.4);
}

.css-test {
  padding: 20px;
}

.glass-box {
  background: rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 16px;
  padding: 30px;
  text-align: center;
  font-size: 18px;
  color: white;
  box-shadow: 
    0 8px 32px rgba(0, 0, 0, 0.3),
    inset 0 0 20px rgba(255, 255, 255, 0.1);
}
</style>
