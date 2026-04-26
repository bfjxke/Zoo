<template>
  <div class="faction-stats">
    <div class="stat-card lawful">
      <div class="stat-title">守序阵营</div>
      <div class="stat-count">{{ stats.lawful.count }}</div>
      <div class="stat-detail">
        存活: {{ stats.lawful.alive }} | 死亡: {{ stats.lawful.dead }}
      </div>
    </div>
    <div class="stat-card aggressive">
      <div class="stat-title">激进阵营</div>
      <div class="stat-count">{{ stats.aggressive.count }}</div>
      <div class="stat-detail">
        存活: {{ stats.aggressive.alive }} | 死亡: {{ stats.aggressive.dead }}
      </div>
    </div>
    <div class="stat-card neutral">
      <div class="stat-title">中立阵营</div>
      <div class="stat-count">{{ stats.neutral.count }}</div>
      <div class="stat-detail">
        存活: {{ stats.neutral.alive }} | 死亡: {{ stats.neutral.dead }}
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'FactionStats',
  props: {
    stats: {
      type: Object,
      default: () => ({
        lawful: { count: 0, alive: 0, dead: 0 },
        aggressive: { count: 0, alive: 0, dead: 0 },
        neutral: { count: 0, alive: 0, dead: 0 }
      })
    }
  }
}
</script>

<style scoped>
.faction-stats {
  display: flex;
  gap: 12px;
  margin: 12px 0;
}

.stat-card {
  flex: 1;
  padding: 16px;
  border-radius: 8px;
  text-align: center;
  color: white;
}

.stat-card.lawful { background: #1890ff; }
.stat-card.aggressive { background: #f5222d; }
.stat-card.neutral { background: #52c41a; }

.stat-title { font-weight: bold; margin-bottom: 8px; }
.stat-count { font-size: 24px; font-weight: bold; }
.stat-detail { font-size: 12px; opacity: 0.8; }
</style>
