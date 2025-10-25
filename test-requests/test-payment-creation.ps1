# PowerShell script to test Payment Service API
# Usage: .\test-payment-creation.ps1

Write-Host "Testing Payment Service API" -ForegroundColor Cyan
Write-Host "==============================" -ForegroundColor Cyan
Write-Host ""

# Check if service is running
Write-Host "Checking if service is running..." -ForegroundColor Yellow
try {
    $healthCheck = Invoke-RestMethod -Uri "http://localhost:8080/actuator/health" -Method Get -ErrorAction Stop
    Write-Host "[OK] Service is running" -ForegroundColor Green
} catch {
    Write-Host "[FAIL] Service not running. Start it with: .\mvnw.cmd spring-boot:run" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "Creating test payment..." -ForegroundColor Yellow

$paymentRequest = @{
    orderId = "TEST-$(Get-Random -Maximum 9999)"
    amount = 99.99
    currency = "USD"
    customerName = "Test User"
    customerEmail = "test@example.com"
    customerPhone = "+919876543210"
    description = "Test payment from PowerShell script"
} | ConvertTo-Json

try {
    $response = Invoke-RestMethod -Uri "http://localhost:8080/api/v1/payments" -Method Post -ContentType "application/json" -Body $paymentRequest -ErrorAction Stop
    
    Write-Host "[OK] Payment created successfully!" -ForegroundColor Green
    Write-Host ""
    Write-Host "Payment Details:" -ForegroundColor Cyan
    Write-Host "  Payment ID: $($response.paymentId)" -ForegroundColor White
    Write-Host "  Order ID: $($response.orderId)" -ForegroundColor White
    Write-Host "  Amount: $($response.currency) $($response.amount)" -ForegroundColor White
    Write-Host "  Status: $($response.status)" -ForegroundColor White
    Write-Host "  Gateway: $($response.gatewayType)" -ForegroundColor White
    Write-Host ""
    Write-Host "  Payment Link: $($response.paymentLink)" -ForegroundColor Green
    
} catch {
    Write-Host "[FAIL] Payment creation failed" -ForegroundColor Red
    Write-Host "Error: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}
