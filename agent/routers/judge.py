from fastapi import APIRouter
from pydantic import BaseModel
from typing import Optional, List
from services.judge_service import judge_action

router = APIRouter()

class JudgeRequest(BaseModel):
    agent_id: int
    agent_name: str
    agent_faction: str
    action: str
    target: Optional[str] = None
    context: str
    current_tick: int
    recent_logs: List[str] = []

class JudgeResponse(BaseModel):
    agent_id: int
    agent_name: str
    action: str
    approved: bool
    confinement_ticks: int
    reasoning: str
    timestamp: str

@router.post("/judge", response_model=JudgeResponse)
async def judge_agent_action(request: JudgeRequest):
    result = await judge_action(
        agent_id=request.agent_id,
        agent_name=request.agent_name,
        agent_faction=request.agent_faction,
        action=request.action,
        target=request.target,
        context=request.context,
        current_tick=request.current_tick,
        recent_logs=request.recent_logs
    )
    return JudgeResponse(**result)