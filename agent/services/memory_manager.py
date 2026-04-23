from typing import List, Dict, Any

class MemoryItem:
    """记忆条目类，用于存储单个记忆"""
    
    def __init__(self, turn: int, content: str, is_important: bool = False):
        self.turn = turn  # 回合数
        self.content = content  # 记忆内容
        self.is_important = is_important  # 是否为重要记忆

class AgentMemory:
    """Agent记忆管理器，管理常规记忆和重要记忆"""
    
    def __init__(self, max_recent_turns: int = 12):
        self.max_recent_turns = max_recent_turns  # 最大保留回合数
        self.recent = []  # 常规记忆列表（最近12回合）
        self.important = []  # 重要记忆列表（永久保留）
    
    def add(self, turn: int, content: str, is_important: bool = False):
        """添加记忆到相应的列表"""
        item = MemoryItem(turn, content, is_important)  # 创建记忆条目
        
        if is_important:  # 如果是重要记忆
            self.important.append(item)  # 添加到重要记忆列表
        else:
            self.recent.append(item)  # 否则添加到常规记忆列表
    
    def cleanup(self, current_turn: int):
        """清理超过12回合的常规记忆"""
        cutoff = current_turn - self.max_recent_turns  # 计算截止回合
        self.recent = [m for m in self.recent if m.turn > cutoff]  # 保留最近的记忆
    
    def build_context(self) -> str:
        """构建发送给AI的上下文字符串"""
        parts = []  # 初始化部分列表
        
        if self.important:  # 如果有重要记忆
            parts.append("[重要记忆]")  # 添加标题
            for m in self.important:  # 遍历所有重要记忆
                parts.append(f"- {m.content}")  # 添加到列表
        
        if self.recent:  # 如果有常规记忆
            parts.append("\n[近期记忆]")  # 添加标题
            for m in self.recent[-10:]:  # 只显示最近10条
                parts.append(f"- {m.content}")  # 添加到列表
        
        return "\n".join(parts)  # 返回拼接后的字符串
