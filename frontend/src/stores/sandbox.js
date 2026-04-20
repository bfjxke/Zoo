import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { sandboxApi } from '../api'

export const useSandboxStore = defineStore('sandbox', () => {
  const currentTick = ref(0)
  const running = ref(false)
  const agents = ref([])
  const logs = ref([])
  const loading = ref(false)

  const aliveAgents = computed(() => agents.value.filter(a => a.alive))
  const deadAgents = computed(() => agents.value.filter(a => !a.alive))
  const lawfulAgents = computed(() => agents.value.filter(a => a.faction === 'lawful'))
  const aggressiveAgents = computed(() => agents.value.filter(a => a.faction === 'aggressive'))
  const neutralAgents = computed(() => agents.value.filter(a => a.faction === 'neutral'))

  async function fetchState() {
    try {
      const { data } = await sandboxApi.getState()
      currentTick.value = data.currentTick
      running.value = data.running
    } catch (e) {
      console.error('获取状态失败:', e)
    }
  }

  async function fetchAgents() {
    try {
      const { data } = await sandboxApi.getAgents()
      agents.value = data
    } catch (e) {
      console.error('获取Agent列表失败:', e)
    }
  }

  async function initAgents() {
    loading.value = true
    try {
      const { data } = await sandboxApi.initAgents()
      await fetchAgents()
      return data
    } finally {
      loading.value = false
    }
  }

  async function startSimulation() {
    try {
      const { data } = await sandboxApi.startSimulation()
      running.value = data.running
    } catch (e) {
      console.error('启动模拟失败:', e)
    }
  }

  async function stopSimulation() {
    try {
      const { data } = await sandboxApi.stopSimulation()
      running.value = data.running
    } catch (e) {
      console.error('停止模拟失败:', e)
    }
  }

  async function fetchLogs(tick) {
    try {
      const { data } = await sandboxApi.getLogs(tick)
      logs.value = data
    } catch (e) {
      console.error('获取日志失败:', e)
    }
  }

  function updateFromWebSocket(data) {
    currentTick.value = data.tick
    running.value = data.running
    if (data.agents) {
      agents.value = data.agents
    }
  }

  return {
    currentTick,
    running,
    agents,
    logs,
    loading,
    aliveAgents,
    deadAgents,
    lawfulAgents,
    aggressiveAgents,
    neutralAgents,
    fetchState,
    fetchAgents,
    initAgents,
    startSimulation,
    stopSimulation,
    fetchLogs,
    updateFromWebSocket,
  }
})
