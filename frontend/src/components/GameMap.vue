<template>
  <div class="map-wrapper">
    <svg 
      class="game-map" 
      width="800" 
      height="500" 
      viewBox="0 0 800 500"
    >
      <defs>
        <radialGradient id="bgGradient" cx="50%" cy="50%" r="70%">
          <stop offset="0%" stop-color="#1a1a2e"/>
          <stop offset="100%" stop-color="#0f0f1a"/>
        </radialGradient>
        <radialGradient id="centerGlow" cx="50%" cy="50%" r="50%">
          <stop offset="0%" stop-color="#ffd700" stop-opacity="0.4"/>
          <stop offset="100%" stop-color="#ffd700" stop-opacity="0"/>
        </radialGradient>
      </defs>
      
      <rect width="800" height="500" fill="url(#bgGradient)"/>
      
      <circle 
        v-if="centerNode"
        :cx="centerNode.x" 
        :cy="centerNode.y" 
        r="180" 
        fill="url(#centerGlow)" 
        opacity="0.5"
      />
      
      <g class="edges">
        <GraphEdge
          v-for="edge in edges"
          :key="`${edge.sourceId}-${edge.targetId}`"
          :edge="edge"
          :source-node="getNode(edge.sourceId)"
          :target-node="getNode(edge.targetId)"
          :node-radius="50"
          @hover="handleEdgeHover"
          @hover-end="handleEdgeHoverEnd"
        />
      </g>
      
      <g class="nodes">
        <GraphNode
          v-for="node in nodes"
          :key="node.id"
          :node="node"
          @click="handleNodeClick"
          @hover="handleNodeHover"
          @hover-end="handleNodeHoverEnd"
        />
      </g>
      
      <g class="agents">
        <g 
          v-for="(agentGroup, nodeId) in agentsByNode"
          :key="`${nodeId}-${agentGroup.length}`"
          :transform="`translate(${getNodePosition(nodeId)})`"
        >
          <g 
            v-for="(agent, index) in agentGroup"
            :key="agent.id"
            :transform="`translate(${getAgentOffset(index, agentGroup.length)})`"
            class="agent-bubble"
            @click="handleAgentClick(agent)"
          >
            <circle
              :r="agentRadius"
              :fill="getFactionColor(agent.faction)"
              stroke="white"
              stroke-width="2"
            >
              <animate 
                attributeName="r" 
                :values="`${agentRadius};${agentRadius + 2};${agentRadius}`" 
                dur="2s" 
                repeatCount="indefinite"
              />
            </circle>
            <text 
                text-anchor="middle" 
                dy="4" 
                fill="white" 
                font-size="9" 
                font-weight="bold"
                style="pointer-events: none"
            >
                {{ agent.role === 'leader' ? '领' : (agent.name ? agent.name.charAt(0) : '?') }}
            </text>
          </g>
        </g>
      </g>
    </svg>
  </div>
</template>

<script>
import GraphNode from './GraphNode.vue'
import GraphEdge from './GraphEdge.vue'
import { sandboxApi } from '../api/index.js'
import { FactionColors } from '../graph.js'

export default {
  name: 'GameMap',
  components: {
    GraphNode,
    GraphEdge
  },
  props: {
    agents: {
      type: Array,
      default: () => []
    }
  },
  data() {
    return {
      nodes: [],
      edges: [],
      agentRadius: 10,
      agentOffsetRadius: 15
    }
  },
  computed: {
        centerNode() {
            return this.nodes.find(n => n.id === 'G')
        },
    nodesMap() {
      const map = {}
      this.nodes.forEach(node => {
        map[node.id] = node
      })
      return map
    },
    agentsByNode() {
      const grouped = {}
      this.agents.forEach(agent => {
        if (agent.currentNode) {
          if (!grouped[agent.currentNode]) {
            grouped[agent.currentNode] = []
          }
          grouped[agent.currentNode].push(agent)
        }
      })
      return grouped
    }
  },
  async mounted() {
    await this.loadGraph()
  },
  methods: {
    async loadGraph() {
      try {
        const response = await sandboxApi.getGraph()
        if (response.data) {
          this.nodes = response.data.nodes || []
          this.edges = response.data.edges || []
        }
      } catch (error) {
        console.error('Failed to load graph:', error)
        this.loadFallbackGraph()
      }
    },
    loadFallbackGraph() {
      this.nodes = [
        { id: 'A', type: 'BASE', faction: 'lawful', label: '守序', x: 100, y: 60 },
        { id: 'B', type: 'CENTER', faction: 'neutral', label: '中立', x: 400, y: 60 },
        { id: 'C', type: 'BASE', faction: 'aggressive', label: '激进', x: 700, y: 60 },
        { id: 'D', type: 'WILDERNESS', faction: null, label: '广场', x: 200, y: 250 },
        { id: 'E', type: 'WILDERNESS', faction: null, label: '广场', x: 600, y: 250 },
        { id: 'F', type: 'WILDERNESS', faction: 'lawful', label: '森林', x: 100, y: 440 },
        { id: 'G', type: 'CENTER', faction: null, label: '河流', x: 400, y: 440 },
        { id: 'H', type: 'WILDERNESS', faction: 'aggressive', label: '山地', x: 700, y: 440 }
      ]
      this.edges = [
        { sourceId: 'A', targetId: 'D', weight: 1 },
        { sourceId: 'A', targetId: 'F', weight: 1 },
        { sourceId: 'B', targetId: 'D', weight: 1 },
        { sourceId: 'B', targetId: 'E', weight: 1 },
        { sourceId: 'B', targetId: 'G', weight: 1 },
        { sourceId: 'C', targetId: 'E', weight: 1 },
        { sourceId: 'C', targetId: 'H', weight: 1 },
        { sourceId: 'D', targetId: 'E', weight: 1 },
        { sourceId: 'D', targetId: 'F', weight: 1 },
        { sourceId: 'D', targetId: 'G', weight: 1 },
        { sourceId: 'E', targetId: 'G', weight: 1 },
        { sourceId: 'E', targetId: 'H', weight: 1 },
        { sourceId: 'G', targetId: 'H', weight: 1 }
      ]
    },
    getNode(nodeId) {
      return this.nodesMap[nodeId]
    },
    getNodePosition(nodeId) {
      const node = this.nodesMap[nodeId]
      if (!node) return '0,0'
      return `${node.x + 15},${node.y + 15}`
    },
    getAgentOffset(index, total) {
      if (total === 1) return '0,0'
      const angle = (2 * Math.PI * index) / total
      const x = Math.cos(angle) * this.agentOffsetRadius
      const y = Math.sin(angle) * this.agentOffsetRadius
      return `${x},${y}`
    },
    getFactionColor(faction) {
      return FactionColors[faction] || '#666666'
    },
    handleNodeClick(node) {
      this.$emit('node-click', node)
    },
    handleNodeHover(node) {
      this.$emit('node-hover', node)
    },
    handleNodeHoverEnd(node) {
      this.$emit('node-hover-end', node)
    },
    handleEdgeHover(edge) {
      this.$emit('edge-hover', edge)
    },
    handleEdgeHoverEnd(edge) {
      this.$emit('edge-hover-end', edge)
    },
    handleAgentClick(agent) {
      this.$emit('agent-click', agent)
    }
  }
}
</script>

<style scoped>
.game-map {
  background: radial-gradient(ellipse at center, #1a1a2e 0%, #0f0f1a 100%);
  border-radius: 12px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.4);
}

.agent-bubble {
  cursor: pointer;
  transition: all 0.3s ease;
}

.agent-bubble:hover {
  filter: brightness(1.3);
  transform: scale(1.2);
}
</style>
