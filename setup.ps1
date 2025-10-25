# Payment Service Setup Script
# Run this to initialize your local development environment

Write-Host "Payment Service Setup" -ForegroundColor Cyan
Write-Host "========================" -ForegroundColor Cyan
Write-Host ""

# Check if .env exists
if (Test-Path ".env") {
    Write-Host "[OK] .env file already exists" -ForegroundColor Green
} else {
    Write-Host "Creating .env file from template..." -ForegroundColor Yellow
    Copy-Item ".env.example" ".env"
    Write-Host "[OK] .env file created. Please edit it with your Stripe API key" -ForegroundColor Green
}

# Check Docker
Write-Host ""
Write-Host "Checking Docker..." -ForegroundColor Cyan
if (Get-Command docker -ErrorAction SilentlyContinue) {
    Write-Host "[OK] Docker is installed" -ForegroundColor Green
    
    Write-Host "Starting MySQL container..." -ForegroundColor Yellow
    docker compose up -d
    Write-Host "[OK] MySQL container started" -ForegroundColor Green
} else {
    Write-Host "[FAIL] Docker not found. Please install Docker Desktop" -ForegroundColor Red
}

# Check Java
Write-Host ""
Write-Host "Checking Java..." -ForegroundColor Cyan
if (Get-Command java -ErrorAction SilentlyContinue) {
    $javaVersion = java -version 2>&1 | Select-String -Pattern "version"
    Write-Host "[OK] Java is installed: $javaVersion" -ForegroundColor Green
} else {
    Write-Host "[FAIL] Java not found. Please install JDK 17+" -ForegroundColor Red
}

Write-Host ""
Write-Host "Setup complete!" -ForegroundColor Green
Write-Host ""
Write-Host "Next steps:" -ForegroundColor Cyan
Write-Host "1. Edit .env and add your Stripe API key" -ForegroundColor White
Write-Host "2. Run: .\mvnw.cmd spring-boot:run" -ForegroundColor White
Write-Host "3. Test: http://localhost:8080/actuator/health" -ForegroundColor White
