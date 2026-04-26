<template>
  <div class="dashboard">
    <!-- 标题栏 -->
    <div class="header">
      <h1>GuardianEye-IIoT 沙箱动物园 - 监控大屏</h1>
      <div class="connection-status">
        <span :class="connected ? 'status-connected' : 'status-disconnected'">
          {{ connected ? '已连接' : '未连接' }}
        </span>
        <button @click="connect">连接</button>
        <button @click="initAgents" class="btn-init">初始化</button>
        <button @click="startSim" class="btn-start" :disabled="gameRunning">开始</button>
        <button @click="stopSim" class="btn-stop" :disabled="!gameRunning">停止</button>
        <span class="tick-info" v-if="currentTick > 0">Tick: {{ currentTick }}</span>
      </div>
    </div>

    <!-- 阵营统计 -->
    <div class="section">
      <FactionStats :stats="factionStats" />
    </div>

    <!-- 游戏地图 -->
    <div class="section">
      <h3>游戏地图</h3>
      <GameMap :agents="agents" @node-click="onNodeClick" />
    </div>

    <!-- Agent列表 -->
    <div class="section agents-grid">
      <AgentStatus v-for="agent in agents" :key="agent.id" :agent="agent" />
    </div>

    <!-- 实时日志 -->
    <div class="section logs-section">
      <LogStream :logs="logs" />
    </div>
  </div>
</template>

<script>
import FactionStats from '../components/FactionStats.vue';
import AgentStatus from '../components/AgentStatus.vue';
import LogStream from '../components/LogStream.vue';
import GameMap from '../components/GameMap.vue';

export default {
  name: 'Dashboard',
  components: { FactionStats, AgentStatus, LogStream, GameMap },
  data() {
    return {
      connected: false,
      stompClient: null,
      agents: [],
      logs: [],
      gameRunning: false,
      currentTick: 0,
      factionStats: {
        lawful: { count: 0, alive: 0, dead: 0 },
        aggressive: { count: 0, alive: 0, dead: 0 },
        neutral: { count: 0, alive: 0, dead: 0 }
      }
    };
  },
  mounted() {
    this.connect();
    this.fetchState();
  },
  beforeUnmount() {
    this.disconnect();
  },
  methods: {
    connect() {
      const socket = new SockJS('/ws');
      this.stompClient = Stomp.over(socket);
      
      this.stompClient.connect({}, (frame) => {
        this.connected = true;
        console.log('WebSocket连接成功');
        
        // 订阅游戏状态
        this.stompClient.subscribe('/topic/game/state', (message) => {
          const state = JSON.parse(message.body);
          this.updateAgents(state.agents);
        });
        
        // 订阅动作日志
        this.stompClient.subscribe('/topic/game/actions', (message) => {
          const actions = JSON.parse(message.body);
          this.addLogs(actions);
        });
      }, (error) => {
        console.error('WebSocket连接失败', error);
        this.connected = false;
      });
    },
    disconnect() {
      if (this.stompClient) {
        this.stompClient.disconnect();
        this.connected = false;
      }
    },
    async fetchState() {
      try {
        const res = await fetch('/api/state');
        const state = await res.json();
        this.gameRunning = state.running;
        this.currentTick = state.currentTick;
        this.fetchAgents();
      } catch (e) {
        console.error('获取状态失败', e);
      }
    },
    async fetchAgents() {
      try {
        const res = await fetch('/api/agents');
        this.agents = await res.json();
        this.calculateFactionStats();
      } catch (e) {
        console.error('获取Agent失败', e);
      }
    },
    async initAgents() {
      try {
        const res = await fetch('/api/init', { method: 'POST' });
        const msg = await res.text();
        alert(msg);
        this.fetchAgents();
      } catch (e) {
        console.error('初始化失败', e);
        alert('初始化失败');
      }
    },
    async startSim() {
      try {
        const res = await fetch('/api/start', { method: 'POST' });
        const state = await res.json();
        this.gameRunning = state.running;
        this.currentTick = state.currentTick;
        alert('模拟已开始！每30秒执行一个Tick');
      } catch (e) {
        console.error('启动失败', e);
        alert('启动失败');
      }
    },
    async stopSim() {
      try {
        const res = await fetch('/api/stop', { method: 'POST' });
        const state = await res.json();
        this.gameRunning = state.running;
      } catch (e) {
        console.error('停止失败', e);
      }
    },
    updateAgents(agentList) {
      this.agents = agentList || [];
      this.calculateFactionStats();
    },
    calculateFactionStats() {
      const stats = {
        lawful: { count: 0, alive: 0, dead: 0 },
        aggressive: { count: 0, alive: 0, dead: 0 },
        neutral: { count: 0, alive: 0, dead: 0 }
      };
      
      this.agents.forEach(agent => {
        const faction = agent.faction || 'neutral';
        if (!stats[faction]) stats[faction] = { count: 0, alive: 0, dead: 0 };
        stats[faction].count++;
        if (agent.alive) {
          stats[faction].alive++;
        } else {
          stats[faction].dead++;
        }
      });
      
      this.factionStats = stats;
    },
    addLogs(actions) {
      actions.forEach(action => {
        this.logs.push({
          tick: action.tickNumber,
          agent: action.agentName,
          action: action.action,
          result: action.result,
          level: action.violation ? 'warning' : 'info'
        });
        
        // 保持最近100条日志
        if (this.logs.length > 100) {
          this.logs.shift();
        }
      });
    },
    onNodeClick(node) {
      console.log('节点点击', node);
    }
  }
};
</script>

<style scoped>
.dashboard {
  padding: 20px;
  max-width: 1200px;
  margin: 0 auto;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.connection-status {
  display: flex;
  align-items: center;
  gap: 10px;
}

.status-connected { color: green; }
.status-disconnected { color: red; }

.btn-init { background: #6366f1; color: white; }
.btn-start { background: #22c55e; color: white; }
.btn-stop { background: #ef4444; color: white; }
.tick-info { margin-left: 10px; color: #ffd700; font-weight: bold; }

.section {
  margin-bottom: 20px;
}

.agents-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(250px, 1fr));
  gap: 10px;
}

.logs-section {
  height: 300px;
}
</style>
