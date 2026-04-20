from fastapi import APIRouter

router = APIRouter()


@router.get("/health")
async def health_check():
    return {"status": "ok", "service": "GuardianEye-IIoT Agent Dispatcher"}
