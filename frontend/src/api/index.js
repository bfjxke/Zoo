import axios from 'axios'

const api = axios.create({
  baseURL: '',
  timeout: 10000,
})

export const sandboxApi = {
  getState: () => api.get('/api/state'),
  startSimulation: () => api.post('/api/start'),
  stopSimulation: () => api.post('/api/stop'),
  getAgents: () => api.get('/api/agents'),
  getAgent: (id) => api.get(`/api/agents/${id}`),
  executeAction: (id, action, target) => api.post(`/api/agents/${id}/action`, { action, target }),
  getLogs: (tick) => api.get('/api/logs', { params: { tick } }),
  initAgents: () => api.post('/api/init'),
  getGraph: () => api.get('/api/graph'),
  getNode: (nodeId) => api.get(`/api/graph/node/${nodeId}`),
  getAdjacentNodes: (nodeId) => api.get(`/api/graph/node/${nodeId}/adjacent`),
}

export const godApi = {
  airdrop: (targetNode, foodAmount) => api.post('/god/airdrop', { target_node: targetNode, food_amount: foodAmount }),
  plague: (targetFaction, staminaPenalty) => api.post('/god/plague', { target_faction: targetFaction, stamina_penalty: staminaPenalty }),
  amnesty: (targetAgent) => api.post('/god/amnesty', { target_agent: targetAgent }),
}

export default api
