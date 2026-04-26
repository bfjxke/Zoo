<template>
  <div class="log-stream">
    <div class="log-header">实时日志</div>
    <div class="log-container" ref="container">
      <div v-for="(log, index) in logs" :key="index" :class="'log-entry log-' + log.level">
        <span class="log-tick">[Tick #{{ log.tick }}]</span>
        <span class="log-agent">{{ log.agent }}</span>
        <span class="log-action">{{ log.action }}</span>
        <span class="log-result">{{ log.result }}</span>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'LogStream',
  props: {
    logs: {
      type: Array,
      default: () => []
    }
  },
  watch: {
    logs() {
      this.$nextTick(() => {
        this.$refs.container.scrollTop = this.$refs.container.scrollHeight;
      });
    }
  }
}
</script>

<style scoped>
.log-stream {
  border: 1px solid #ddd;
  border-radius: 8px;
  padding: 12px;
  background: #1a1a1a;
  color: #0f0;
  font-family: monospace;
  font-size: 12px;
  height: 300px;
  overflow: hidden;
}

.log-container {
  height: 100%;
  overflow-y: auto;
}

.log-entry {
  margin: 4px 0;
  padding: 4px;
  border-radius: 4px;
}

.log-entry:hover {
  background: #2a2a2a;
}
</style>
