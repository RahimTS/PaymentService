#!/usr/bin/env pwsh

# Normalize console encoding for consistent output on Windows PowerShell 5.1
try {
    [Console]::OutputEncoding = [System.Text.Encoding]::UTF8
    [Console]::InputEncoding  = [System.Text.Encoding]::UTF8
} catch {}

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Payment Service Comprehensive Test Suite" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

$baseUrl = "http://localhost:8080/api/v1/payments"
$testResults = @()

function Test-Endpoint {
    param(
        [string]$Name,
        [string]$Method,
        [string]$Url,
        [object]$Body,
        [Nullable[int]]$ExpectedStatus = $null
    )
    
    Write-Host "Testing: $Name" -ForegroundColor Yellow
    
    try {
        $params = @{
            Uri = $Url
            Method = $Method
            ContentType = "application/json"
        }
        
        if ($Body) {
            $params.Body = ($Body | ConvertTo-Json -Depth 6)
        }
        
        $response = Invoke-RestMethod @params -ErrorAction Stop
        
    Write-Host "PASS" -ForegroundColor Green
    $script:testResults += @{Test=$Name; Status="PASS"; Response=$response}
        return $response
        
    } catch {
        $statusCode = $null
        if ($_.Exception -and $_.Exception.Response) {
            try { $statusCode = $_.Exception.Response.StatusCode.value__ } catch {}
        }
        if ($statusCode -eq $ExpectedStatus) {
            Write-Host "PASS (Expected error $ExpectedStatus)" -ForegroundColor Green
            $script:testResults += @{Test=$Name; Status="PASS"}
        } else {
            $codeText = if ($statusCode) { $statusCode } else { 'N/A' }
            # Try to extract error response body for easier diagnosis
            $errMsg = $_.Exception.Message
            try {
                if ($_.ErrorDetails -and $_.ErrorDetails.Message) {
                    $errMsg = $_.ErrorDetails.Message
                } elseif ($_.Exception.Response) {
                    $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
                    $errBody = $reader.ReadToEnd()
                    if ($errBody) { $errMsg = $errBody }
                }
            } catch {}
            Write-Host "FAIL (Status: $codeText) - $errMsg" -ForegroundColor Red
            $script:testResults += @{Test=$Name; Status="FAIL"; Error=$errMsg}
        }
    }
    
    Write-Host ""
}

# Test 1: Create Payment
Write-Host "Test 1: Create Valid Payment" -ForegroundColor Cyan
$payment1 = Test-Endpoint `
    -Name "Create Payment" `
    -Method "POST" `
    -Url $baseUrl `
    -Body @{
        orderId="TEST-$(Get-Random)"
        amount=99.99
        currency="USD"
        customerName="Test User"
        customerEmail="test@example.com"
        customerPhone="+919876543210"
    }

$paymentId = $payment1.paymentId
$orderId = $payment1.orderId

# Test 2: Get Payment by ID
Write-Host "Test 2: Get Payment by ID" -ForegroundColor Cyan
Test-Endpoint `
    -Name "Get by ID" `
    -Method "GET" `
    -Url "$baseUrl/$paymentId"

# Test 3: Get Payment by Order ID
Write-Host "Test 3: Get Payment by Order ID" -ForegroundColor Cyan
Test-Endpoint `
    -Name "Get by Order ID" `
    -Method "GET" `
    -Url "$baseUrl/order/$orderId"

# Test 4: Get Customer Payments
Write-Host "Test 4: Get Customer Payments" -ForegroundColor Cyan
Test-Endpoint `
    -Name "Get Customer Payments" `
    -Method "GET" `
    -Url "$baseUrl/customer/test@example.com"

# Test 5: Duplicate Payment (should fail with 409)
Write-Host "Test 5: Test Idempotency (Duplicate Payment)" -ForegroundColor Cyan
Test-Endpoint `
    -Name "Duplicate Payment" `
    -Method "POST" `
    -Url $baseUrl `
    -Body @{
        orderId=$orderId
        amount=99.99
        currency="USD"
        customerName="Test User"
        customerEmail="test@example.com"
        customerPhone="+919876543210"
    } `
    -ExpectedStatus 409

# Test 6: Invalid Amount (should fail with 400)
Write-Host "Test 6: Invalid Amount Validation" -ForegroundColor Cyan
Test-Endpoint `
    -Name "Invalid Amount" `
    -Method "POST" `
    -Url $baseUrl `
    -Body @{
        orderId="INVALID-$(Get-Random)"
        amount=-10.00
        currency="USD"
        customerName="Test User"
        customerEmail="test@example.com"
        customerPhone="+919876543210"
    } `
    -ExpectedStatus 400

# Test 7: Cancel Payment
Write-Host "Test 7: Cancel Payment" -ForegroundColor Cyan
Test-Endpoint `
    -Name "Cancel Payment" `
    -Method "POST" `
    -Url "$baseUrl/$paymentId/cancel"

# Test 8: Get Non-Existent Payment (should fail with 404)
Write-Host "Test 8: Get Non-Existent Payment" -ForegroundColor Cyan
$fakeId = "550e8400-e29b-41d4-a716-446655440000"
Test-Endpoint `
    -Name "Non-Existent Payment" `
    -Method "GET" `
    -Url "$baseUrl/$fakeId" `
    -ExpectedStatus 404

# Summary
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Test Summary" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

$passed = ($testResults | Where-Object {$_.Status -eq "PASS"}).Count
$failed = ($testResults | Where-Object {$_.Status -eq "FAIL"}).Count
$total = $testResults.Count

Write-Host "Total Tests: $total" -ForegroundColor White
Write-Host "Passed: $passed" -ForegroundColor Green
Write-Host "Failed: $failed" -ForegroundColor Red

if ($failed -eq 0) {
    Write-Host "`nAll tests passed!" -ForegroundColor Green
} else {
    Write-Host "`nSome tests failed" -ForegroundColor Red
}
