from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from routers import decide, health

app = FastAPI(title="GuardianEye-IIoT Agent Dispatcher", version="0.1.0")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(decide.router, prefix="/decide", tags=["decision"])
app.include_router(health.router, tags=["health"])


@app.on_event("startup")
async def startup():
    print("[Agent调度器] 启动完成，等待Java后端调用...")
