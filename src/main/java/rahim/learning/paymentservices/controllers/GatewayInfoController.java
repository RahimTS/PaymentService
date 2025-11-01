package rahim.learning.paymentservices.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import rahim.learning.paymentservices.paymentgateways.PaymentGatewayStrategy;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller for gateway information
 */
@RestController
@RequestMapping("/api/v1/gateways")
@Slf4j
@Tag(name = "Gateway Information", description = "Payment gateway availability and status")
public class GatewayInfoController {

    @Autowired
    private PaymentGatewayStrategy gatewayStrategy;

    @Operation(summary = "Get available gateways", description = "Returns list of configured payment gateways")
    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> getGatewayInfo() {
        log.debug("Gateway info requested");
        
        Map<String, Object> info = new HashMap<>();
        info.put("available_gateways", gatewayStrategy.getAvailableGateways());
        info.put("default_gateway", gatewayStrategy.getPaymentGatewayType());
        info.put("stripe_available", gatewayStrategy.isGatewayAvailable("STRIPE"));
        info.put("razorpay_available", gatewayStrategy.isGatewayAvailable("RAZORPAY"));
        
        return ResponseEntity.ok(info);
    }
}
