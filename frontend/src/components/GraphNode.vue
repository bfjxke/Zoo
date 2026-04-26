<template>
  <g 
    class="glass-node"
    :class="[`node-${node.type.toLowerCase()}`, `faction-${node.faction || 'none'}`, { active: isActive, hovered: isHovered }]"
    :transform="`translate(${node.x}, ${node.y})`"
    @mouseenter="handleMouseEnter"
    @mouseleave="handleMouseLeave"
    @click="handleClick"
  >
    <defs>
      <linearGradient :id="`border-gradient-${node.id}`" x1="0%" y1="0%" x2="100%" y2="100%">
        <stop offset="0%" :stop-color="borderColorStart" stop-opacity="0.8"/>
        <stop offset="50%" :stop-color="borderColorEnd" stop-opacity="1"/>
        <stop offset="100%" :stop-color="borderColorStart" stop-opacity="0.8"/>
      </linearGradient>
      <filter :id="`glow-${node.id}`">
        <feGaussianBlur stdDeviation="6" result="coloredBlur"/>
        <feMerge>
          <feMergeNode in="coloredBlur"/>
          <feMergeNode in="SourceGraphic"/>
        </feMerge>
      </filter>
    </defs>
    
    <rect
      class="node-background"
      :x="-width / 2"
      :y="-height / 2"
      :width="width"
      :height="height"
      :rx="borderRadius"
      ry="borderRadius"
      :fill="backgroundColor"
      :stroke="`url(#border-gradient-${node.id})`"
      stroke-width="2"
      :filter="isHovered ? `url(#glow-${node.id})` : ''"
    />
    
    <circle
      class="inner-glow"
      :cx="0"
      :cy="0"
      :r="innerRadius"
      :fill="innerGlowColor"
      opacity="0.15"
    />
    
    <text
      class="node-icon"
      text-anchor="middle"
      dy="-12"
      :font-size="iconSize"
    >
      {{ icon }}
    </text>
    
    <text
      class="node-label"
      text-anchor="middle"
      dy="8"
      :font-size="labelSize"
      font-weight="600"
      :fill="textColor"
    >
      {{ displayLabel }}
    </text>
    
    <text
      class="node-id"
      text-anchor="middle"
      dy="22"
      :font-size="idSize"
      :fill="textColor"
      opacity="0.7"
    >
      {{ node.id.toUpperCase() }}
    </text>
  </g>
</template>

<script>
import { getNodeIcon, getNodeLabel, getNodeColor } from '../graph.js'

export default {
  name: 'GraphNode',
  props: {
    node: {
      type: Object,
      required: true
    },
    isActive: {
      type: Boolean,
      default: false
    }
  },
  data() {
    return {
      isHovered: false,
      width: 100,
      height: 80,
      borderRadius: 16,
      iconSize: 28,
      labelSize: 11,
      idSize: 9,
      innerRadius: 35
    }
  },
  computed: {
    icon() {
      return getNodeIcon(this.node)
    },
    displayLabel() {
      return getNodeLabel(this.node)
    },
    nodeColor() {
      return getNodeColor(this.node)
    },
    backgroundColor() {
      const color = this.nodeColor
      return `rgba(${this.hexToRgb(color)}, 0.12)`
    },
    borderColorStart() {
      const color = this.nodeColor
      return `rgba(${this.hexToRgb(color)}, 0.6)`
    },
    borderColorEnd() {
      const color = this.nodeColor
      return `rgba(${this.hexToRgb(color)}, 1)`
    },
    innerGlowColor() {
      return this.nodeColor
    },
    textColor() {
      return '#ffffff'
    }
  },
  methods: {
    hexToRgb(hex) {
      const result = /^#?([a-f\d]{2})([a-f\d]{2})([a-f\d]{2})$/i.exec(hex)
      if (!result) return '255, 255, 255'
      return `${parseInt(result[1], 16)}, ${parseInt(result[2], 16)}, ${parseInt(result[3], 16)}`
    },
    handleMouseEnter() {
      this.isHovered = true
      this.$emit('hover', this.node)
    },
    handleMouseLeave() {
      this.isHovered = false
      this.$emit('hover-end', this.node)
    },
    handleClick() {
      this.$emit('click', this.node)
    }
  }
}
</script>

<style scoped>
.glass-node {
  cursor: pointer;
  transition: all 0.3s ease;
}

.glass-node.hovered .node-background {
  backdrop-filter: blur(25px);
}

.node-icon {
  pointer-events: none;
  user-select: none;
}

.node-label {
  pointer-events: none;
  user-select: none;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.5);
}

.node-id {
  pointer-events: none;
  user-select: none;
  font-family: monospace;
}

.inner-glow {
  pointer-events: none;
}

.glass-node.active .node-background {
  stroke-width: 3;
  stroke-dasharray: 8, 4;
}
</style>
