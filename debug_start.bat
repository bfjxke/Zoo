@echo off
chcp 65001 > nul
echo ========================================
echo   AI沙箱动物园 - 调试模式启动脚本
echo ========================================
echo.

REM 创建日志目录
if not exist "debug_logs" mkdir debug_logs
echo [启动] 日志目录: debug_logs

REM 清理旧日志
echo [启动] 清理旧日志文件...
del /q debug_logs\*.log 2>nul

REM 启动后端服务
echo.
echo [启动] 启动后端服务 (端口 8080)...
echo [启动] 日志输出到: debug_logs\backend.log
start "Backend-Sandbox" cmd /k "cd backend ^&^& mvn spring-boot:run > ..\debug_logs\backend.log 2>^&1"

REM 等待后端启动
echo [启动] 等待后端服务启动 (约30秒)...
timeout /t 30 /nobreak > nul

REM 检查后端是否启动成功
powershell -Command "try { Invoke-WebRequest -Uri 'http://localhost:8080/api/state' -Method Get -TimeoutSec 5 | Out-Null; Write-Host '[成功] 后端服务已启动' } catch { Write-Host '[警告] 后端服务可能未启动' }"

REM 启动前端服务
echo.
echo [启动] 启动前端服务 (端口 3000)...
echo [启动] 日志输出到: debug_logs\frontend.log
start "Frontend-Sandbox" cmd /k "cd frontend ^&^& npm run dev > ..\debug_logs\frontend.log 2>^&1"

REM 等待前端启动
echo [启动] 等待前端服务启动...
timeout /t 10 /nobreak > nul

REM 打开浏览器
echo.
echo [启动] 打开浏览器访问游戏...
start http://localhost:3000/

REM 显示日志查看提示
echo.
echo ========================================
echo   调试日志已启动
echo ========================================
echo.
echo 日志文件位置: debug_logs\
echo   - backend.log  - 后端日志 (包含Agent决策)
echo   - frontend.log - 前端日志
echo.
echo 实时查看Agent日志命令:
echo   powershell -Command "Get-Content debug_logs\backend.log -Wait -Tail 50"
echo.
echo 过滤特定Agent日志:
echo   findstr /C:"守序领袖" debug_logs\backend.log
echo   findstr /C:"强势兵-Alpha" debug_logs\backend.log
echo.
echo 按任意键打开日志查看器...
pause > nul

REM 打开日志文件
start notepad debug_logs\backend.log
start notepad debug_logs\frontend.log
