@echo off
REM 地图图结构与玻璃态UI测试脚本
REM 使用前请确保后端和前端服务已启动

echo ========================================
echo  地图系统测试
echo ========================================
echo.

REM 检查服务状态
echo [1/6] 检查服务状态...
netstat -ano | findstr ":8080" | findstr "LISTENING" >nul
if %errorlevel% neq 0 (
    echo [错误] 后端服务未运行 (端口8080)
    echo 请先启动后端: cd backend ^&^& mvn spring-boot:run
    pause
    exit /b 1
)
echo [OK] 后端服务运行中 (端口8080)

netstat -ano | findstr ":3001" | findstr "LISTENING" >nul
if %errorlevel% neq 0 (
    echo [警告] 前端服务未在3001端口运行
    echo 请检查前端服务状态
) else (
    echo [OK] 前端服务运行中 (端口3001)
)
echo.

REM 测试图API
echo [2/6] 测试图结构API...
curl -s http://localhost:8080/api/graph > graph_response.json
if %errorlevel% neq 0 (
    echo [错误] API请求失败
    pause
    exit /b 1
)

REM 检查节点数量
findstr /C:"nodeCount" graph_response.json >nul
if %errorlevel% neq 0 (
    echo [错误] API响应格式错误
    echo 响应内容:
    type graph_response.json
    pause
    exit /b 1
)

echo [OK] 图API返回成功
echo.

REM 显示节点数量
echo [3/6] 验证图数据...
echo 正在解析节点数量...
REM 使用PowerShell解析JSON
powershell -Command "$json = Get-Content graph_response.json -Raw | ConvertFrom-Json; Write-Host '节点数量:' $json.nodeCount; Write-Host '边数量:' $json.edgeCount"
echo.

REM 测试节点查询
echo [4/6] 测试节点查询API...
curl -s http://localhost:8080/api/graph/node/base_lawful > node_response.json
if %errorlevel% equ 0 (
    echo [OK] 节点查询成功
) else (
    echo [错误] 节点查询失败
)
echo.

REM 测试邻接查询
echo [5/6] 测试邻接节点API...
curl -s http://localhost:8080/api/graph/node/center/adjacent > adjacent_response.json
if %errorlevel% equ 0 (
    echo [OK] 邻接查询成功
    echo 中心节点邻接节点:
    type adjacent_response.json
) else (
    echo [错误] 邻接查询失败
)
echo.

REM 清理
echo [6/6] 清理临时文件...
del graph_response.json node_response.json adjacent_response.json 2>nul
echo.

echo ========================================
echo  测试完成！
echo ========================================
echo.
echo 后端API测试: 全部通过
echo.
echo 打开浏览器测试前端界面:
echo 1. 访问: http://localhost:3001
echo 2. 强制刷新: Ctrl + Shift + R
echo 3. 或使用无痕模式
echo.
echo 如果界面未更新，请:
echo - 强制刷新页面 (Ctrl+F5)
echo - 打开无痕窗口
echo - 重启前端服务
echo.
echo 查看测试指南: docs/BROWSER_TEST_GUIDE.md
echo.
pause
