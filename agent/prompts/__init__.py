from .leader_prompt import get_leader_prompt, get_leader_system_prompt
from .soldier_prompt import get_soldier_prompt, get_soldier_system_prompt
from .judge_prompt import get_judge_prompt, get_judge_system_prompt

__all__ = [
    "get_leader_prompt", "get_leader_system_prompt",
    "get_soldier_prompt", "get_soldier_system_prompt",
    "get_judge_prompt", "get_judge_system_prompt"
]