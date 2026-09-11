# ── start-backend.ps1 ────────────────────────────────────────────────────────
# Sets the required environment variables and launches the Spring Boot backend
# using the bundled Maven. Run this in its own PowerShell window.

$ROOT = "C:\Users\Ansh\Desktop\file distributor system"

$env:SERVER_PORT            = "8081"
$env:APP_FRONTEND_URL       = "https://yogurt-grinch-everyone.ngrok-free.dev"
$env:APP_PUBLIC_BACKEND_URL = "https://yogurt-grinch-everyone.ngrok-free.dev"
$env:APP_CORS_ALLOWED_ORIGINS = "https://yogurt-grinch-everyone.ngrok-free.dev,http://localhost:5173"
$env:APP_MAIL_ENABLED         = "true"
$env:MAIL_USERNAME           = "prakul5555@gmail.com"
$env:MAIL_PASSWORD           = "slig ashg lekx biqg"

Write-Host "Starting backend on port 8081..." -ForegroundColor Cyan
Write-Host "  APP_FRONTEND_URL       = $env:APP_FRONTEND_URL"
Write-Host "  APP_PUBLIC_BACKEND_URL = $env:APP_PUBLIC_BACKEND_URL"
Write-Host "  APP_CORS_ALLOWED_ORIGINS = $env:APP_CORS_ALLOWED_ORIGINS"
Write-Host "  APP_MAIL_ENABLED        = $env:APP_MAIL_ENABLED"
Write-Host "  MAIL_USERNAME           = $env:MAIL_USERNAME"
Write-Host ""

Set-Location "$ROOT\backend"
& ".\maven\apache-maven-3.9.16\bin\mvn.cmd" spring-boot:run

