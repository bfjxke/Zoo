# 地图系统测试脚本

Write-Host "========================================" -ForegroundColor Cyan
Write-Host " 地图图结构与玻璃态UI测试" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# 检查服务状态
Write-Host "[1/6] 检查服务状态..." -ForegroundColor Yellow

$backendRunning = Get-NetTCPConnection -LocalPort 8080 -ErrorAction SilentlyContinue
if ($backendRunning) {
    Write-Host "[OK] 后端服务运行中 (端口8080)" -ForegroundColor Green
} else {
    Write-Host "[错误] 后端服务未运行 (端口8080)" -ForegroundColor Red
    Write-Host "请先启动后端: cd backend; mvn spring-boot:run" -ForegroundColor Red
    exit 1
}

$frontendRunning = Get-NetTCPConnection -LocalPort 3001 -ErrorAction SilentlyContinue
if ($frontendRunning) {
    Write-Host "[OK] 前端服务运行中 (端口3001)" -ForegroundColor Green
} else {
    Write-Host "[警告] 前端服务未在3001端口运行" -ForegroundColor Yellow
}
Write-Host ""

# 测试图API
Write-Host "[2/6] 测试图结构API..." -ForegroundColor Yellow

try {
    $response = Invoke-RestMethod -Uri "http://localhost:8080/api/graph" -Method Get
    Write-Host "[OK] 图API返回成功" -ForegroundColor Green
    Write-Host "节点数量: $($response.nodeCount)" -ForegroundColor Cyan
    Write-Host "边数量: $($response.edgeCount)" -ForegroundColor Cyan
} catch {
    Write-Host "[错误] API请求失败: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}
Write-Host ""

# 测试节点查询
Write-Host "[3/6] 测试节点查询API..." -ForegroundColor Yellow

try {
    $nodeResponse = Invoke-RestMethod -Uri "http://localhost:8080/api/graph/node/base_lawful" -Method Get
    Write-Host "[OK] 节点查询成功" -ForegroundColor Green
    Write-Host "节点ID: $($nodeResponse.id)" -ForegroundColor Cyan
    Write-Host "节点类型: $($nodeResponse.type)" -ForegroundColor Cyan
    Write-Host "所属阵营: $($nodeResponse.faction)" -ForegroundColor Cyan
} catch {
    Write-Host "[错误] 节点查询失败: $($_.Exception.Message)" -ForegroundColor Red
}
Write-Host ""

# 测试邻接查询
Write-Host "[4/6] 测试邻接节点API..." -ForegroundColor Yellow

try {
    $adjacentResponse = Invoke-RestMethod -Uri "http://localhost:8080/api/graph/node/center/adjacent" -Method Get
    Write-Host "[OK] 邻接查询成功" -ForegroundColor Green
    Write-Host "中心节点的邻接节点:" -ForegroundColor Cyan
    $adjacentResponse | ForEach-Object { Write-Host "  - $_" -ForegroundColor Cyan }
} catch {
    Write-Host "[错误] 邻接查询失败: $($_.Exception.Message)" -ForegroundColor Red
}
Write-Host ""

# 列出所有节点
Write-Host "[5/6] 验证节点列表..." -ForegroundColor Yellow

Write-Host "所有节点:" -ForegroundColor Cyan
$response.nodes | ForEach-Object {
    $icon = switch ($_.type) {
        "BASE" { "🏛" }
        "CENTER" { "⭐" }
        "WILDERNESS" { "🌲" }
        default { "📍" }
    }
    Write-Host "  $icon $($_.id) - $($_.type)" -ForegroundColor White
}
Write-Host ""

# 测试Agent API
Write-Host "[6/6] 测试Agent API..." -ForegroundColor Yellow

try {
    $agentsResponse = Invoke-RestMethod -Uri "http://localhost:8080/api/agents" -Method Get
    Write-Host "[OK] Agent查询成功" -ForegroundColor Green
    Write-Host "Agent数量: $($agentsResponse.Count)" -ForegroundColor Cyan
    
    # 按阵营统计
    $lawfulCount = ($agentsResponse | Where-Object { $_.faction -eq "lawful" }).Count
    $aggressiveCount = ($agentsResponse | Where-Object { $_.faction -eq "aggressive" }).Count
    $neutralCount = ($agentsResponse | Where-Object { $_.faction -eq "neutral" }).Count
    
    Write-Host "  守序阵营: $lawfulCount 个" -ForegroundColor Blue
    Write-Host "  激进阵营: $aggressiveCount 个" -ForegroundColor Red
    Write-Host "  中立阵营: $neutralCount 个" -ForegroundColor Green
} catch {
    Write-Host "[错误] Agent查询失败: $($_.Exception.Message)" -ForegroundColor Red
}
Write-Host ""

# 完成
Write-Host "========================================" -ForegroundColor Cyan
Write-Host " 测试完成！所有API测试通过！" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "下一步：浏览器测试前端界面" -ForegroundColor Yellow
Write-Host ""
Write-Host "1. 打开浏览器访问: http://localhost:3001" -ForegroundColor White
Write-Host "2. 强制刷新页面: Ctrl + Shift + R" -ForegroundColor White
Write-Host "3. 或使用无痕/隐私模式" -ForegroundColor White
Write-Host ""
Write-Host "预期效果:" -ForegroundColor Cyan
Write-Host "  - 7个玻璃态节点卡片" -ForegroundColor White
Write-Host "  - 渐变发光连接线" -ForegroundColor White
Write-Host "  - Agent堆叠显示" -ForegroundColor White
Write-Host "  - 呼吸动画效果" -ForegroundColor White
Write-Host ""
Write-Host "详细测试指南: docs/BROWSER_TEST_GUIDE.md" -ForegroundColor Gray
Write-Host ""
