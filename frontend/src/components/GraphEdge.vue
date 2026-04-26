<template>
  <g class="glass-edge" :class="{ hovered: isHovered }">
    <defs>
      <linearGradient :id="`edge-gradient-${edge.sourceId}-${edge.targetId}`" x1="0%" y1="0%" x2="100%" y2="0%">
        <stop offset="0%" :stop-color="sourceColor" stop-opacity="0.8"/>
        <stop offset="50%" :stop-color="middleColor" stop-opacity="0.6"/>
        <stop offset="100%" :stop-color="targetColor" stop-opacity="0.8"/>
      </linearGradient>
      <filter :id="`edge-glow-${edge.sourceId}-${edge.targetId}`">
        <feGaussianBlur stdDeviation="4" result="coloredBlur"/>
        <feMerge>
          <feMergeNode in="coloredBlur"/>
          <feMergeNode in="SourceGraphic"/>
        </feMerge>
      </filter>
    </defs>
    
    <line
      class="edge-line"
      :x1="startX"
      :y1="startY"
      :x2="endX"
      :y2="endY"
      :stroke="`url(#edge-gradient-${edge.sourceId}-${edge.targetId})`"
      stroke-width="2"
      :stroke-dasharray="isHovered ? 'none' : '8,4'"
      :filter="isHovered ? `url(#edge-glow-${edge.sourceId}-${edge.targetId})` : ''"
      stroke-opacity="0.6"
    />
    
    <line
      class="edge-line-hover"
      :x1="startX"
      :y1="startY"
      :x2="endX"
      :y2="endY"
      stroke="white"
      stroke-width="3"
      stroke-opacity="0"
      @mouseenter="handleMouseEnter"
      @mouseleave="handleMouseLeave"
    />
  </g>
</template>

<script>
import { getNodeColor } from '../graph.js'

export default {
  name: 'GraphEdge',
  props: {
    edge: {
      type: Object,
      required: true
    },
    sourceNode: {
      type: Object,
      required: true
    },
    targetNode: {
      type: Object,
      required: true
    },
    nodeRadius: {
      type: Number,
      default: 50
    }
  },
  data() {
    return {
      isHovered: false
    }
  },
  computed: {
    sourceColor() {
      return getNodeColor(this.sourceNode)
    },
    targetColor() {
      return getNodeColor(this.targetNode)
    },
    middleColor() {
      return '#888888'
    },
    startX() {
      return this.sourceNode.x
    },
    startY() {
      return this.sourceNode.y
    },
    endX() {
      return this.targetNode.x
    },
    endY() {
      return this.targetNode.y
    },
    dx() {
      return this.endX - this.startX
    },
    dy() {
      return this.endY - this.startY
    },
    distance() {
      return Math.sqrt(this.dx * this.dx + this.dy * this.dy)
    },
    adjustedStartX() {
      const ratio = this.nodeRadius / this.distance
      return this.startX + this.dx * ratio
    },
    adjustedStartY() {
      const ratio = this.nodeRadius / this.distance
      return this.startY + this.dy * ratio
    },
    adjustedEndX() {
      const ratio = this.nodeRadius / this.distance
      return this.endX - this.dx * ratio
    },
    adjustedEndY() {
      const ratio = this.nodeRadius / this.distance
      return this.endY - this.dy * ratio
    }
  },
  methods: {
    handleMouseEnter() {
      this.isHovered = true
      this.$emit('hover', this.edge)
    },
    handleMouseLeave() {
      this.isHovered = false
      this.$emit('hover-end', this.edge)
    }
  }
}
</script>

<style scoped>
.glass-edge {
  pointer-events: none;
}

.edge-line {
  pointer-events: none;
  transition: all 0.3s ease;
}

.edge-line-hover {
  pointer-events: stroke;
  cursor: pointer;
  stroke-opacity: 0;
}

.glass-edge:hover .edge-line {
  stroke-opacity: 1;
  stroke-width: 3;
}
</style>
