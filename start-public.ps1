# ── start-public.ps1 ──────────────────────────────────────────────────────────
# Kills any existing backend/frontend/ngrok processes,
# starts a fresh ngrok tunnel on the FRONTEND port (5173),
# reads the public URL, then launches backend + frontend with correct env vars.
#
# IMPORTANT: We tunnel the FRONTEND (5173), NOT the backend (8081).
#   - /share/:token is a React Router frontend route, not a backend endpoint.
#   - Vite's dev proxy forwards /api/* requests to the backend internally.
#   - Both APP_FRONTEND_URL and APP_PUBLIC_BACKEND_URL point to the SAME ngrok
#     URL because there is only one tunnel (on the frontend).

$ROOT   = $PSScriptRoot
$NGROK  = "$ROOT\ngrok.exe"

# ── 1. Stop any running backend/frontend/ngrok processes ─────────────────────
Write-Host "`n[1/5] Stopping backend, frontend, and ngrok processes..." -ForegroundColor Cyan
Get-Process -Name ngrok -ErrorAction SilentlyContinue | Stop-Process -Force
Get-Process -Name java -ErrorAction SilentlyContinue | Where-Object { $_.Path -like "*" -and $_.MainWindowTitle -eq "" } | Stop-Process -Force -ErrorAction SilentlyContinue
Get-Process -Name node -ErrorAction SilentlyContinue | Stop-Process -Force -ErrorAction SilentlyContinue
Start-Sleep 2

# ── 2. Start Docker services ──────────────────────────────────────────────────
Write-Host "[2/5] Starting Docker services (postgres + redis)..." -ForegroundColor Cyan
Push-Location $ROOT
docker-compose up -d
Pop-Location

# ── 3. Start ngrok tunnel on the FRONTEND port (5173) ────────────────────────
Write-Host "[3/5] Starting ngrok tunnel on port 5173 (frontend)..." -ForegroundColor Cyan
$ngrokProc = Start-Process -FilePath $NGROK -ArgumentList "http 5173" -PassThru -WindowStyle Minimized
Start-Sleep 5

# ── 4. Read public URL from ngrok API ────────────────────────────────────────
Write-Host "[4/5] Reading public URL from ngrok API..." -ForegroundColor Cyan
$tunnelUrl = $null
for ($i = 0; $i -lt 10; $i++) {
    try {
        $resp = Invoke-RestMethod -Uri "http://localhost:4040/api/tunnels" -ErrorAction Stop
        $tunnelUrl = ($resp.tunnels | Where-Object { $_.proto -eq "https" } | Select-Object -First 1).public_url
        if ($tunnelUrl) { break }
    } catch {}
    Start-Sleep 2
}

if (-not $tunnelUrl) {
    Write-Host "ERROR: Could not get ngrok URL. Is ngrok running?" -ForegroundColor Red
    exit 1
}

Write-Host "`n✅ Public frontend URL: $tunnelUrl" -ForegroundColor Green
Write-Host "   The app (and share links) will be accessible via this tunnel" -ForegroundColor Green

# Both env vars point to the SAME ngrok URL (single tunnel on the frontend)
$env:APP_FRONTEND_URL       = $tunnelUrl
$env:APP_PUBLIC_BACKEND_URL = $tunnelUrl
$env:APP_CORS_ALLOWED_ORIGINS = "$tunnelUrl,http://localhost:5173,http://localhost:3000"
$env:SERVER_PORT            = "8081"

Write-Host "`n[5/5] Starting backend, then frontend..." -ForegroundColor Cyan
Write-Host "   APP_FRONTEND_URL       = $env:APP_FRONTEND_URL"
Write-Host "   APP_PUBLIC_BACKEND_URL = $env:APP_PUBLIC_BACKEND_URL"
Write-Host "   SERVER_PORT            = $env:SERVER_PORT"
Write-Host ""
Write-Host ">>> Open the app at: $tunnelUrl" -ForegroundColor Yellow
Write-Host ">>> Local dev at: http://localhost:5173" -ForegroundColor Yellow
Write-Host ""

# ── Start backend (blocking) in a new window ─────────────────────────────────
Write-Host "Starting backend on port 8081..." -ForegroundColor Cyan
$backendProc = Start-Process -FilePath "powershell.exe" `
    -ArgumentList "-NoExit", "-Command", "Set-Location '$ROOT\backend'; `$env:APP_FRONTEND_URL='$tunnelUrl'; `$env:APP_PUBLIC_BACKEND_URL='$tunnelUrl'; `$env:APP_CORS_ALLOWED_ORIGINS='$tunnelUrl,http://localhost:5173,http://localhost:3000'; `$env:SERVER_PORT='8081'; .\maven\apache-maven-3.9.16\bin\mvn.cmd spring-boot:run" `
    -PassThru

Start-Sleep 8

# ── Start frontend (blocking) in a new window ────────────────────────────────
Write-Host "Starting frontend on port 5173..." -ForegroundColor Cyan
$frontendProc = Start-Process -FilePath "powershell.exe" `
    -ArgumentList "-NoExit", "-Command", "Set-Location '$ROOT\frontend'; npm run dev" `
    -PassThru

Write-Host "`n✅ Setup complete. ngrok URL: $tunnelUrl" -ForegroundColor Green
Write-Host "   Share links will use the ngrok https:// domain."
Write-Host ""
