from fastapi import APIRouter
from models.schemas import DecisionRequest, DecisionResponse, AgentDecision
from services.agent_scheduler import dispatch_all_agents

router = APIRouter()


@router.post("", response_model=DecisionResponse)
async def decide_actions(request: DecisionRequest):
    decisions_data = await dispatch_all_agents(request.agents)
    decisions = [AgentDecision(**d) for d in decisions_data]
    return DecisionResponse(tick=request.tick, decisions=decisions)
