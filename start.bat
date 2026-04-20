@echo off
chcp 65001 >nul
echo ============================================
echo   GuardianEye-IIoT 沙箱动物园 一键启动
echo ============================================
echo.

echo [1/4] 检查环境...
where java >nul 2>nul
if %errorlevel% neq 0 (
    echo [错误] 未找到 Java，请安装 JDK 17+
    pause
    exit /b 1
)
where node >nul 2>nul
if %errorlevel% neq 0 (
    echo [错误] 未找到 Node.js，请安装 Node.js 18+
    pause
    exit /b 1
)
echo [OK] 环境检查通过

echo.
echo [2/4] 检查虚拟环境...
if not exist "%~dp0agent\venv\Scripts\activate.bat" (
    echo [2/4a] 创建 Python 虚拟环境...
    cd /d "%~dp0agent"
    python -m venv venv
    echo [2/4b] 在虚拟环境中安装 Python 依赖（使用豆瓣镜像源）...
    "%~dp0agent\venv\Scripts\python.exe" -m pip install --timeout 120 --retries 5 -i https://pypi.doubanio.com/simple/ --trusted-host pypi.doubanio.com -r requirements.txt
) else (
    echo [OK] Python 虚拟环境已存在
)

echo [2/4c] 安装前端依赖...
cd /d "%~dp0frontend"
call npm install

echo.
echo [3/4] 启动服务...
echo [3/4a] 启动 Python Agent 调度器 (端口 8000)...
cd /d "%~dp0agent"
start "Python-Agent" cmd /k "venv\Scripts\activate.bat" ^& python -m uvicorn main:app --host 0.0.0.0 --port 8000 --reload

echo [3/4b] 启动 Java 后端 (端口 8080)...
cd /d "%~dp0backend"
start "Java-Backend" cmd /k mvn spring-boot:run

echo [3/4c] 启动 Vue 前端 (端口 3000)...
cd /d "%~dp0frontend"
start "Vue-Frontend" cmd /k npm run dev

echo.
echo ============================================
echo   所有服务已启动！
echo   - 前端: http://localhost:3000
echo   - Java后端: http://localhost:8080
echo   - Python调度器: http://localhost:8000
echo ============================================
echo.
echo 等待服务就绪后，请在浏览器打开 http://localhost:3000
echo 先点击"初始化Agent"，再点击"启动模拟"
echo.
pause
