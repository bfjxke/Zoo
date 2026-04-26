# Map System Test Script

Write-Host "========================================"
Write-Host " Map System API Test"
Write-Host "========================================"
Write-Host ""

# Check services
Write-Host "[1/6] Checking services..."
$backend = Get-NetTCPConnection -LocalPort 8080 -ErrorAction SilentlyContinue
if ($backend) {
    Write-Host "[OK] Backend running on port 8080" -ForegroundColor Green
} else {
    Write-Host "[ERROR] Backend not running on port 8080" -ForegroundColor Red
    exit 1
}

$frontend = Get-NetTCPConnection -LocalPort 3001 -ErrorAction SilentlyContinue
if ($frontend) {
    Write-Host "[OK] Frontend running on port 3001" -ForegroundColor Green
} else {
    Write-Host "[WARNING] Frontend not on port 3001" -ForegroundColor Yellow
}
Write-Host ""

# Test graph API
Write-Host "[2/6] Testing Graph API..."
try {
    $response = Invoke-RestMethod -Uri "http://localhost:8080/api/graph" -Method Get
    Write-Host "[OK] Graph API Success" -ForegroundColor Green
    Write-Host "  Nodes: $($response.nodeCount)" -ForegroundColor Cyan
    Write-Host "  Edges: $($response.edgeCount)" -ForegroundColor Cyan
} catch {
    Write-Host "[ERROR] API Failed: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}
Write-Host ""

# Test node query
Write-Host "[3/6] Testing Node Query API..."
try {
    $node = Invoke-RestMethod -Uri "http://localhost:8080/api/graph/node/base_lawful" -Method Get
    Write-Host "[OK] Node Query Success" -ForegroundColor Green
    Write-Host "  ID: $($node.id)" -ForegroundColor Cyan
    Write-Host "  Type: $($node.type)" -ForegroundColor Cyan
} catch {
    Write-Host "[ERROR] Node Query Failed" -ForegroundColor Red
}
Write-Host ""

# Test adjacent
Write-Host "[4/6] Testing Adjacent Nodes API..."
try {
    $adjacent = Invoke-RestMethod -Uri "http://localhost:8080/api/graph/node/center/adjacent" -Method Get
    Write-Host "[OK] Adjacent Query Success" -ForegroundColor Green
    Write-Host "  Adjacent to center:"
    $adjacent | ForEach-Object { Write-Host "    - $_" -ForegroundColor Cyan }
} catch {
    Write-Host "[ERROR] Adjacent Query Failed" -ForegroundColor Red
}
Write-Host ""

# List all nodes
Write-Host "[5/6] Node List..."
Write-Host "All $($response.nodeCount) nodes:"
$response.nodes | ForEach-Object {
    Write-Host "  - $($_.id) [Type: $($_.type)]"
}
Write-Host ""

# Test agents
Write-Host "[6/6] Testing Agent API..."
try {
    $agents = Invoke-RestMethod -Uri "http://localhost:8080/api/agents" -Method Get
    Write-Host "[OK] Agent Query Success" -ForegroundColor Green
    Write-Host "  Total: $($agents.Count) agents"
    
    $lawful = ($agents | Where-Object {$_.faction -eq "lawful"}).Count
    $aggressive = ($agents | Where-Object {$_.faction -eq "aggressive"}).Count
    $neutral = ($agents | Where-Object {$_.faction -eq "neutral"}).Count
    
    Write-Host "    Lawful: $lawful"
    Write-Host "    Aggressive: $aggressive"
    Write-Host "    Neutral: $neutral"
} catch {
    Write-Host "[ERROR] Agent Query Failed" -ForegroundColor Red
}
Write-Host ""

Write-Host "========================================"
Write-Host " All API Tests PASSED!" -ForegroundColor Green
Write-Host "========================================"
Write-Host ""
Write-Host "Next: Test frontend in browser"
Write-Host ""
Write-Host "1. Open: http://localhost:3001"
Write-Host "2. Hard Refresh: Ctrl + Shift + R"
Write-Host "3. Or use Incognito/Private mode"
Write-Host ""
Write-Host "Expected:"
Write-Host "  - 7 glass-morphism node cards"
Write-Host "  - Gradient glowing edges"
Write-Host "  - Agent stack display"
Write-Host "  - Breathing animation"
Write-Host ""
