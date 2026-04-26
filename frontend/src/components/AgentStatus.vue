<template>
  <div class="agent-card">
    <div class="agent-header">
      <span class="agent-name">{{ agent.name }}</span>
      <span :class="'faction-badge faction-' + agent.faction">{{ agent.faction }}</span>
    </div>
    <div class="personality-tag" v-if="agent.personality">
      <span class="personality-icon">{{ getPersonalityEmoji(agent.personality) }}</span>
      <span class="personality-name">{{ getPersonalityName(agent.personality) }}</span>
    </div>
    <div class="agent-stats">
      <div class="stat">
        <label>耐力</label>
        <div class="progress-bar">
          <div class="progress-fill stamina" :style="{ width: agent.stamina + '%' }"></div>
        </div>
        <span class="stat-value">{{ agent.stamina }}/100</span>
      </div>
      <div class="stat">
        <label>饱食</label>
        <div class="progress-bar">
          <div class="progress-fill satiety" :style="{ width: agent.satiety + '%' }"></div>
        </div>
        <span class="stat-value">{{ agent.satiety }}/140</span>
      </div>
      <div class="stat">
        <label>生命</label>
        <div class="progress-bar">
          <div class="progress-fill health" :style="{ width: (agent.health / 90 * 100) + '%' }"></div>
        <div class="progress-fill health-buff" v-if="agent.satiety > 100" :style="{ width: '0%' }"></div>
        </div>
        <span class="stat-value">{{ agent.health }}/90</span>
      </div>
    </div>
    <div class="agent-node">
      位置: {{ agent.currentNode }}
    </div>
  </div>
</template>

<script>
const personalityData = {
  brave: { name: '勇敢', emoji: '⚔️' },
  cautious: { name: '谨慎', emoji: '🛡' },
  cunning: { name: '狡诈', emoji: '🎭' },
  loyal: { name: '忠诚', emoji: '🛡️' },
  rebellious: { name: '叛逆', emoji: '🔥' },
  greedy: { name: '贪婪', emoji: '💰' },
  peaceful: { name: '和平', emoji: '☮️' },
  adventurous: { name: '冒险', emoji: '🗺' },
  strategic: { name: '战略', emoji: '♟️' },
  charismatic: { name: '魅力', emoji: '🗣️' },
  feral: { name: '野性', emoji: '🐺' },
  wise: { name: '睿智', emoji: '📚' }
}

export default {
  name: 'AgentStatus',
  props: {
    agent: Object
  },
  methods: {
    getPersonalityName(id) {
      return personalityData[id]?.name || '普通'
    },
    getPersonalityEmoji(id) {
      return personalityData[id]?.emoji || '❓'
    }
  }
}
</script>

<style scoped>
.agent-card {
  border: 1px solid #ddd;
  border-radius: 8px;
  padding: 12px;
  margin: 8px;
  background: #f9f9f9;
}

.agent-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
}

.personality-tag {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  padding: 4px 10px;
  border-radius: 12px;
  font-size: 11px;
  margin-bottom: 8px;
}

.personality-icon {
  font-size: 14px;
}

.personality-name {
  font-weight: 600;
}

.faction-badge {
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
  color: white;
}

.faction-lawful { background: #1890ff; }
.faction-aggressive { background: #f5222d; }
.faction-neutral { background: #52c41a; }

.stat {
  margin: 4px 0;
}

.progress-bar {
  height: 8px;
  background: #e8e8e8;
  border-radius: 4px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  transition: width 0.3s;
}

.progress-fill.stamina { background: #1890ff; }
.progress-fill.satiety { background: #faad14; }
.progress-fill.health { background: #52c41a; }
</style>
