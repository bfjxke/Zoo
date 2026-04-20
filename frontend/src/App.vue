<template>
  <div class="app">
    <header class="app-header">
      <h1>GuardianEye-IIoT 沙箱动物园</h1>
      <div class="header-info">
        <span class="tick-badge">Tick #{{ store.currentTick }}</span>
        <span :class="['status-badge', store.running ? 'running' : 'stopped']">
          {{ store.running ? '运行中' : '已停止' }}
        </span>
      </div>
    </header>

    <div class="controls">
      <button @click="store.initAgents()" :disabled="store.loading">初始化Agent</button>
      <button @click="store.startSimulation()" :disabled="store.running" class="btn-start">启动模拟</button>
      <button @click="store.stopSimulation()" :disabled="!store.running" class="btn-stop">停止模拟</button>
    </div>

    <div class="god-panel">
      <h3>上帝干预</h3>
      <div class="god-actions">
        <button @click="godAirdrop" class="btn-god">空投物资</button>
        <button @click="godPlague" class="btn-god">施加瘟疫</button>
        <button @click="godAmnesty" class="btn-god">赦免复活</button>
      </div>
    </div>

    <div class="map-container">
      <h3>上帝视角地图</h3>
      <div class="map-grid">
        <div v-for="node in mapNodes" :key="node.name" class="map-node" :class="node.type">
          <div class="node-name">{{ node.label }}</div>
          <div class="node-agents">
            <span v-for="agent in getAgentsAtNode(node.name)" :key="agent.id"
                  class="agent-dot" :class="agent.faction"
                  :title="`${agent.name} (${agent.faction}) E:${agent.stamina} S:${agent.satiety} H:${agent.health}`">
            </span>
          </div>
        </div>
      </div>
    </div>

    <div class="agents-panel">
      <h3>Agent 状态面板</h3>
      <div class="faction-tabs">
        <button @click="selectedFaction = 'lawful'" :class="{ active: selectedFaction === 'lawful' }">守序阵营</button>
        <button @click="selectedFaction = 'aggressive'" :class="{ active: selectedFaction === 'aggressive' }">强势阵营</button>
        <button @click="selectedFaction = 'neutral'" :class="{ active: selectedFaction === 'neutral' }">中立阵营</button>
      </div>
      <div class="agent-list">
        <div v-for="agent in filteredAgents" :key="agent.id" class="agent-card" :class="{ dead: !agent.alive }">
          <div class="agent-header">
            <span class="agent-name">{{ agent.name }}</span>
            <span :class="['agent-status', agent.alive ? 'alive' : 'dead']">
              {{ agent.alive ? '存活' : '死亡' }}
            </span>
          </div>
          <div class="agent-stats">
            <div class="stat">
              <label>耐力</label>
              <div class="stat-bar">
                <div class="stat-fill stamina" :style="{ width: agent.stamina + '%' }"></div>
              </div>
              <span>{{ agent.stamina }}</span>
            </div>
            <div class="stat">
              <label>饱食</label>
              <div class="stat-bar">
                <div class="stat-fill satiety" :style="{ width: agent.satiety + '%' }"></div>
              </div>
              <span>{{ agent.satiety }}</span>
            </div>
            <div class="stat">
              <label>健康</label>
              <div class="stat-bar">
                <div class="stat-fill health" :style="{ width: agent.health + '%' }"></div>
              </div>
              <span>{{ agent.health }}</span>
            </div>
          </div>
          <div class="agent-meta">
            <span>位置: {{ agent.currentNode }}</span>
            <span v-if="agent.fatigued" class="debuff">疲劳</span>
            <span v-if="agent.hungry" class="debuff">饥饿</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useSandboxStore } from './stores/sandbox'
import { godApi } from './api'

const store = useSandboxStore()
const selectedFaction = ref('lawful')

const mapNodes = [
  { name: 'base_lawful', label: '守序基地', type: 'base lawful' },
  { name: 'base_aggressive', label: '强势基地', type: 'base aggressive' },
  { name: 'base_neutral', label: '中立基地', type: 'base neutral' },
  { name: 'center', label: '中心广场', type: 'center' },
  { name: 'forest', label: '森林', type: 'resource' },
  { name: 'river', label: '河流', type: 'resource' },
  { name: 'mountain', label: '山脉', type: 'resource' },
]

const filteredAgents = computed(() => {
  return store.agents.filter(a => a.faction === selectedFaction.value)
})

function getAgentsAtNode(nodeName) {
  return store.agents.filter(a => a.currentNode === nodeName && a.alive)
}

async function godAirdrop() {
  await godApi.airdrop('center', 50)
}

async function godPlague() {
  await godApi.plague('all', 30)
}

async function godAmnesty() {
  await godApi.amnesty('all')
  await store.fetchAgents()
}

onMounted(async () => {
  await Promise.all([store.fetchState(), store.fetchAgents()])
})
</script>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body {
  font-family: 'Segoe UI', system-ui, sans-serif;
  background: #0a0e17;
  color: #e0e6ed;
  min-height: 100vh;
}

.app {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
}

.app-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 24px;
  background: linear-gradient(135deg, #1a1f2e, #2a3040);
  border-radius: 12px;
  margin-bottom: 20px;
}

.app-header h1 {
  font-size: 1.5rem;
  background: linear-gradient(90deg, #00d4ff, #7b2ff7);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.header-info {
  display: flex;
  gap: 12px;
  align-items: center;
}

.tick-badge {
  background: #2a3040;
  padding: 6px 14px;
  border-radius: 20px;
  font-weight: 600;
  color: #00d4ff;
}

.status-badge {
  padding: 6px 14px;
  border-radius: 20px;
  font-weight: 600;
  font-size: 0.85rem;
}

.status-badge.running {
  background: rgba(0, 200, 83, 0.2);
  color: #00c853;
}

.status-badge.stopped {
  background: rgba(255, 82, 82, 0.2);
  color: #ff5252;
}

.controls {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
}

.controls button, .god-actions button {
  padding: 10px 20px;
  border: none;
  border-radius: 8px;
  font-size: 0.9rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
  background: #2a3040;
  color: #e0e6ed;
}

.controls button:hover:not(:disabled), .god-actions button:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
}

.controls button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.btn-start {
  background: linear-gradient(135deg, #00c853, #009624) !important;
  color: white !important;
}

.btn-stop {
  background: linear-gradient(135deg, #ff5252, #d50000) !important;
  color: white !important;
}

.btn-god {
  background: linear-gradient(135deg, #7b2ff7, #5500cc) !important;
  color: white !important;
}

.god-panel {
  background: #1a1f2e;
  border-radius: 12px;
  padding: 16px 20px;
  margin-bottom: 20px;
  border: 1px solid rgba(123, 47, 247, 0.3);
}

.god-panel h3 {
  margin-bottom: 12px;
  color: #b388ff;
}

.god-actions {
  display: flex;
  gap: 10px;
}

.map-container {
  background: #1a1f2e;
  border-radius: 12px;
  padding: 20px;
  margin-bottom: 20px;
}

.map-container h3 {
  margin-bottom: 16px;
  color: #00d4ff;
}

.map-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
}

.map-node {
  background: #2a3040;
  border-radius: 8px;
  padding: 12px;
  text-align: center;
  min-height: 80px;
  transition: all 0.2s;
}

.map-node.base {
  border: 2px solid rgba(255, 255, 255, 0.1);
}

.map-node.lawful {
  border-color: rgba(0, 200, 83, 0.4);
}

.map-node.aggressive {
  border-color: rgba(255, 82, 82, 0.4);
}

.map-node.neutral {
  border-color: rgba(255, 193, 7, 0.4);
}

.map-node.center {
  border-color: rgba(0, 212, 255, 0.4);
  grid-column: 2 / 4;
}

.map-node.resource {
  border-color: rgba(139, 195, 74, 0.4);
}

.node-name {
  font-size: 0.85rem;
  font-weight: 600;
  margin-bottom: 8px;
}

.node-agents {
  display: flex;
  gap: 4px;
  justify-content: center;
  flex-wrap: wrap;
}

.agent-dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  display: inline-block;
}

.agent-dot.lawful {
  background: #00c853;
}

.agent-dot.aggressive {
  background: #ff5252;
}

.agent-dot.neutral {
  background: #ffc107;
}

.agents-panel {
  background: #1a1f2e;
  border-radius: 12px;
  padding: 20px;
}

.agents-panel h3 {
  margin-bottom: 12px;
  color: #00d4ff;
}

.faction-tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
}

.faction-tabs button {
  padding: 8px 16px;
  border: 1px solid #3a4050;
  border-radius: 6px;
  background: transparent;
  color: #e0e6ed;
  cursor: pointer;
  font-size: 0.85rem;
}

.faction-tabs button.active {
  background: #2a3040;
  border-color: #00d4ff;
  color: #00d4ff;
}

.agent-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 12px;
}

.agent-card {
  background: #2a3040;
  border-radius: 8px;
  padding: 14px;
  border: 1px solid #3a4050;
  transition: all 0.2s;
}

.agent-card.dead {
  opacity: 0.5;
  border-color: #ff5252;
}

.agent-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.agent-name {
  font-weight: 600;
  font-size: 0.95rem;
}

.agent-status.alive {
  color: #00c853;
  font-size: 0.8rem;
}

.agent-status.dead {
  color: #ff5252;
  font-size: 0.8rem;
}

.agent-stats {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.stat {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 0.8rem;
}

.stat label {
  width: 32px;
  color: #8892a4;
}

.stat-bar {
  flex: 1;
  height: 6px;
  background: #1a1f2e;
  border-radius: 3px;
  overflow: hidden;
}

.stat-fill {
  height: 100%;
  border-radius: 3px;
  transition: width 0.3s;
}

.stat-fill.stamina {
  background: linear-gradient(90deg, #00d4ff, #0091ea);
}

.stat-fill.satiety {
  background: linear-gradient(90deg, #ffc107, #ff9800);
}

.stat-fill.health {
  background: linear-gradient(90deg, #00c853, #76ff03);
}

.stat span {
  width: 28px;
  text-align: right;
}

.agent-meta {
  margin-top: 8px;
  font-size: 0.75rem;
  color: #8892a4;
  display: flex;
  gap: 8px;
  align-items: center;
}

.debuff {
  background: rgba(255, 82, 82, 0.2);
  color: #ff5252;
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 0.7rem;
}
</style>
