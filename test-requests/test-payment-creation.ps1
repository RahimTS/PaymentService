#!/usr/bin/env pwsh

Write-Host "🧪 Testing Payment Service API" -ForegroundColor Cyan
Write-Host "==============================" -ForegroundColor Cyan
Write-Host ""

# Check if service is running
Write-Host "🔍 Checking if service is running..." -ForegroundColor Yellow
try {
    $healthCheck = Invoke-RestMethod -Uri "http://localhost:8080/api/v1/gateways/info" -Method Get
    Write-Host "✓ Service is running" -ForegroundColor Green
    Write-Host "Available gateways: $($healthCheck.available_gateways -join ', ')" -ForegroundColor White
} catch {
    Write-Host "✗ Service not running. Start it with: mvn spring-boot:run" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "💳 Creating test payment..." -ForegroundColor Yellow

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
    $response = Invoke-RestMethod -Uri "http://localhost:8080/api/v1/payments" `
        -Method Post `
        -ContentType "application/json" `
        -Body $paymentRequest
    
    Write-Host "✓ Payment created successfully!" -ForegroundColor Green
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
    Write-Host "✗ Payment creation failed" -ForegroundColor Red
    Write-Host "Error: $($_.Exception.Message)" -ForegroundColor Red
}
