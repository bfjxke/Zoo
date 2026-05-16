import asyncio
import random
from typing import Dict, List, Optional
from dataclasses import dataclass
from datetime import datetime

@dataclass
class AgentEvent:
    agent_id: int
    agent_name: str
    event_type: str  # "thinking", "action", "state_change"
    data: Dict
    timestamp: datetime

class AgentNotifier:
    def __init__(self):
        self._subscribers: List[callable] = []
    
    def subscribe(self, callback: callable):
        self._subscribers.append(callback)
    
    def unsubscribe(self, callback: callable):
        if callback in self._subscribers:
            self._subscribers.remove(callback)
    
    def notify(self, event: AgentEvent):
        for callback in self._subscribers:
            try:
                callback(event)
            except Exception as e:
                print(f"通知失败: {e}")

class AgentThinkingLoop:
    def __init__(self, agent_id: int, agent_data: Dict, notifier: AgentNotifier):
        self.agent_id = agent_id
        self.agent_data = agent_data
        self.notifier = notifier
        self.state = agent_data.copy()
        self.last_think_time = datetime.now()
        self.decision_count = 0
    
    def should_think(self) -> bool:
        """判断是否应该思考"""
        now = datetime.now()
        seconds_since_last = (now - self.last_think_time).total_seconds()
        
        if seconds_since_last < 0.5:
            return False
        
        if self.state.get("satiety", 100) < 30:
            return True
        
        if self.state.get("stamina", 100) < 20:
            return True
        
        return random.random() < 0.3
    
    async def think(self) -> Dict:
        """思考并生成决策"""
        self.notifier.notify(AgentEvent(
            agent_id=self.agent_id,
            agent_name=self.agent_data.get("name", "Unknown"),
            event_type="thinking",
            data={"state": self.state},
            timestamp=datetime.now()
        ))
        
        await asyncio.sleep(random.uniform(0.1, 0.5))
        
        decision = {
            "action": self._decide_action(),
            "target": self._decide_target(),
            "reasoning": "自主思考生成"
        }
        
        self.decision_count += 1
        self.last_think_time = datetime.now()
        
        self.notifier.notify(AgentEvent(
            agent_id=self.agent_id,
            agent_name=self.agent_data.get("name", "Unknown"),
            event_type="action",
            data=decision,
            timestamp=datetime.now()
        ))
        
        return decision
    
    def _decide_action(self) -> str:
        """基于状态决定动作"""
        if self.state.get("satiety", 100) < 30:
            return "eat"
        if self.state.get("stamina", 100) < 20:
            return "rest"
        return random.choice(["move", "move", "move", "talk"])
    
    def _decide_target(self) -> Optional[str]:
        """决定目标"""
        if self.state.get("satiety", 100) < 30:
            return None
        return random.choice(["center", "D", "E", "F"])
    
    def update_state(self, new_state: Dict):
        """更新状态"""
        self.state.update(new_state)

async def run_agent_loop(agent_loop: AgentThinkingLoop):
    """运行单个Agent的思考循环"""
    while True:
        if agent_loop.should_think():
            await agent_loop.think()
        
        wait_time = random.uniform(0.5, 3.0)
        await asyncio.sleep(wait_time)

async def run_all_agents(agents_data: List[Dict]) -> List[asyncio.Task]:
    """运行所有Agent的思考循环"""
    notifier = AgentNotifier()
    
    agent_loops = [
        AgentThinkingLoop(a["id"], a, notifier)
        for a in agents_data
    ]
    
    tasks = [
        asyncio.create_task(run_agent_loop(loop))
        for loop in agent_loops
    ]
    
    return tasks

if __name__ == "__main__":
    agents = [
        {"id": 1, "name": "Agent1", "stamina": 80, "satiety": 90},
        {"id": 2, "name": "Agent2", "stamina": 50, "satiety": 70},
        {"id": 3, "name": "Agent3", "stamina": 100, "satiety": 20},
    ]
    
    async def main():
        tasks = await run_all_agents(agents)
        await asyncio.gather(*tasks)
    
    asyncio.run(main())
