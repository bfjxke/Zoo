from typing import List, Dict, Any

class MemoryItem:
    """记忆条目类，用于存储单个记忆"""
    
    def __init__(self, turn: int, content: str, is_important: bool = False, memory_type: str = "general"):
        self.turn = turn  # 回合数
        self.content = content  # 记忆内容
        self.is_important = is_important  # 是否为重要记忆
        self.memory_type = memory_type  # 记忆类型：alliance/enemy/item/status/relationship/general
    
    def to_dict(self) -> Dict[str, Any]:
        """转换为字典"""
        return {
            "turn": self.turn,
            "content": self.content,
            "is_important": self.is_important,
            "memory_type": self.memory_type
        }


class AgentMemory:
    """Agent记忆管理器，管理常规记忆和重要记忆"""
    
    # 记忆类型常量
    TYPE_ALLIANCE = "alliance"      # 联盟关系
    TYPE_ENEMY = "enemy"            # 敌对关系
    TYPE_ITEM = "item"              # 物品信息（如秩序之剑）
    TYPE_STATUS = "status"          # 状态信息
    TYPE_RELATIONSHIP = "relationship"  # 人际关系
    TYPE_GENERAL = "general"        # 普通记忆
    
    # 秩序之剑的详细描述（模板）
    ORDER_SWORD_INFO = """秩序之剑：
    1. 生成条件：游戏第40回合在随机位置(center/forest/river/mountain)生成
    2. 拾取方式：Agent移动到剑所在位置即可拾取
    3. 剑跟随：持有者移动时，剑位置同步更新
    4. 死亡掉落：持有者死亡后，剑掉落在当前位置
    5. 持有者增益：守序阵营持有时，全阵营获得10%属性加成
    6. 宣言效果：守序阵营持有剑可发布"秩序宣言"
    7. 宣言条件：游戏≥40回合，所有阵营存活，发布后激进阵营投票
    8. 和平结局：激进阵营过半数同意则触发和平结局，守序阵营获胜
    9. 唯一性：整局游戏只有一把剑"""
    
    def __init__(self, max_recent_turns: int = 16):
        self.max_recent_turns = max_recent_turns  # 最大保留回合数（默认16回合）
        self.recent = []  # 常规记忆列表（最近16回合）
        self.important = []  # 重要记忆列表（永久保留，可修改）
    
    def add(self, turn: int, content: str, is_important: bool = False, memory_type: str = "general"):
        """添加记忆到相应的列表"""
        item = MemoryItem(turn, content, is_important, memory_type)  # 创建记忆条目
        
        if is_important:
            # 重要记忆检查是否需要更新（如盟友变敌人）
            self._update_or_add_important(item)
        else:
            self.recent.append(item)  # 添加到常规记忆
    
    def _update_or_add_important(self, new_item: MemoryItem):
        """更新或添加重要记忆（处理盟友变敌人等情况）"""
        # 根据记忆类型查找是否已存在
        existing_idx = None
        for i, item in enumerate(self.important):
            # 简单匹配：检查类型和内容前缀
            if item.memory_type == new_item.memory_type:
                if new_item.memory_type in [self.TYPE_ALLIANCE, self.TYPE_ENEMY]:
                    # 联盟/敌对记忆：检查名字是否相同
                    if ":" in item.content and ":" in new_item.content:
                        old_name = item.content.split("（")[0] if "（" in item.content else item.content
                        new_name = new_item.content.split("（")[0] if "（" in new_item.content else new_item.content
                        if old_name == new_name:
                            existing_idx = i
                            break
                else:
                    existing_idx = i
                    break
        
        if existing_idx is not None:
            # 找到同类记忆，判断是否需要更新
            existing = self.important[existing_idx]
            # 如果内容不同（如盟友变敌人），更新
            if existing.content != new_item.content:
                self.important[existing_idx] = new_item
                print(f"[记忆更新] {existing.content} → {new_item.content}")
            # 如果内容相同，不更新（避免重复）
        else:
            # 新增重要记忆
            self.important.append(new_item)
    
    def add_alliance(self, turn: int, agent_name: str, faction: str, status: str = "盟友"):
        """添加联盟关系记忆
        status: 盟友/敌人/中立"""
        content = f"{agent_name}（{faction}）：{status}"
        memory_type = self.TYPE_ALLIANCE if status == "盟友" else self.TYPE_ENEMY
        self.add(turn, content, is_important=True, memory_type=memory_type)
    
    def add_order_sword_info(self, turn: int):
        """添加强制记忆：秩序之剑详细信息"""
        self.add(turn, self.ORDER_SWORD_INFO, is_important=True, memory_type=self.TYPE_ITEM)
    
    def cleanup(self, current_turn: int):
        """清理超过16回合的常规记忆"""
        cutoff = current_turn - self.max_recent_turns  # 计算截止回合
        self.recent = [m for m in self.recent if m.turn > cutoff]  # 保留最近16回合
    
    def build_context(self) -> str:
        """构建发送给AI的上下文字符串"""
        parts = []  # 初始化部分列表
        
        # 1. 秩序之剑信息放最前面（最重要）
        order_sword_found = False
        for m in self.important:
            if m.memory_type == self.TYPE_ITEM:
                parts.append(f"[物品信息]\n{m.content}")
                order_sword_found = True
                break
        
        if not order_sword_found:
            # 没有找到秩序之剑信息，添加默认
            parts.append("[物品信息]\n" + self.ORDER_SWORD_INFO)
        
        # 2. 重要记忆（联盟/敌对/关系）
        important_parts = []
        for m in self.important:
            if m.memory_type in [self.TYPE_ALLIANCE, self.TYPE_ENEMY, self.TYPE_RELATIONSHIP]:
                important_parts.append(f"- [{m.memory_type}] {m.content}")
        
        if important_parts:
            parts.append("\n[重要关系]")
            parts.extend(important_parts)
        
        # 3. 常规记忆（最近16回合，只显示最后10条）
        if self.recent:
            parts.append("\n[近期记忆]")
            for m in self.recent[-10:]:  # 只显示最近10条
                parts.append(f"- {m.content}")
        
        return "\n".join(parts)
    
    def to_dict(self) -> Dict[str, Any]:
        """导出为字典（供序列化）"""
        return {
            "recent": [m.to_dict() for m in self.recent],
            "important": [m.to_dict() for m in self.important]
        }
    
    @classmethod
    def from_dict(cls, data: Dict[str, Any]) -> "AgentMemory":
        """从字典恢复（供反序列化）"""
        memory = cls()
        memory.recent = [MemoryItem(**m) for m in data.get("recent", [])]
        memory.important = [MemoryItem(**m) for m in data.get("important", [])]
        return memory
