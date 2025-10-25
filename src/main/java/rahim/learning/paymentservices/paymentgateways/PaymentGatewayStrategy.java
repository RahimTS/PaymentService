package rahim.learning.paymentservices.paymentgateways;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import rahim.learning.paymentservices.entities.PaymentGatewayType;

import java.util.ArrayList;
import java.util.List;

/**
 * Strategy pattern implementation for payment gateway selection.
 * Supports multiple gateways with runtime switching capability.
 * 
 * Gateways can be conditionally loaded based on configuration,
 * allowing flexible deployment scenarios.
 */
@Component
@Slf4j
public class PaymentGatewayStrategy {

    @Autowired(required = false)
    private RazorPaymentGateway razorPaymentGateway;

    @Autowired
    private StripePaymentGateway stripePaymentGateway;
    
    @Value("${payment.gateway.default:STRIPE}")
    private String defaultGateway;

    /**
     * Get the default configured payment gateway
     * 
     * @return IPaymentGateway implementation
     */
    public IPaymentGateway getPaymentGateway() {
        return getPaymentGateway(defaultGateway);
    }
    
    /**
     * Get specific payment gateway by type
     * 
     * @param gatewayType Gateway type string (STRIPE, RAZORPAY) - can be null
     * @return IPaymentGateway implementation
     * @throws UnsupportedOperationException if gateway is not available
     */
    public IPaymentGateway getPaymentGateway(String gatewayType) {
        // If gatewayType is null or empty, use default
        if (gatewayType == null || gatewayType.isBlank()) {
            log.debug("No gateway type specified, using default: {}", defaultGateway);
            gatewayType = defaultGateway;
        }
        
        log.debug("Requesting payment gateway: {}", gatewayType);
        
        return switch (gatewayType.toUpperCase()) {
            case "RAZORPAY" -> {
                if (razorPaymentGateway == null) {
                    log.error("Razorpay gateway requested but not configured");
                    throw new UnsupportedOperationException(
                        "Razorpay gateway is not available. Enable it by setting razorpay.enabled=true"
                    );
                }
                yield razorPaymentGateway;
            }
            case "STRIPE" -> stripePaymentGateway;
            default -> {
                log.error("Unknown payment gateway requested: {}", gatewayType);
                throw new UnsupportedOperationException(
                    "Payment gateway not supported: " + gatewayType
                );
            }
        };
    }
    
    /**
     * Get the PaymentGatewayType enum for the default gateway
     * 
     * @return PaymentGatewayType enum
     */
    public PaymentGatewayType getPaymentGatewayType() {
        return getPaymentGatewayType(defaultGateway);
    }
    
    /**
     * Get PaymentGatewayType enum for specific gateway
     * 
     * @param gatewayType Gateway type string (can be null)
     * @return PaymentGatewayType enum
     */
    public PaymentGatewayType getPaymentGatewayType(String gatewayType) {
        // If gatewayType is null or empty, use default
        if (gatewayType == null || gatewayType.isBlank()) {
            gatewayType = defaultGateway;
        }
        
        try {
            return PaymentGatewayType.valueOf(gatewayType.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("Invalid gateway type: {}, defaulting to STRIPE", gatewayType);
            return PaymentGatewayType.STRIPE;
        }
    }
    
    /**
     * Check if a specific gateway is available
     * 
     * @param gatewayType Gateway type to check
     * @return true if gateway is available and configured
     */
    public boolean isGatewayAvailable(String gatewayType) {
        if (gatewayType == null || gatewayType.isBlank()) {
            return false;
        }
        
        return switch (gatewayType.toUpperCase()) {
            case "RAZORPAY" -> razorPaymentGateway != null;
            case "STRIPE" -> stripePaymentGateway != null;
            default -> false;
        };
    }
    
    /**
     * Get list of available gateway types
     * 
     * @return List of available gateway names
     */
    public List<String> getAvailableGateways() {
        List<String> gateways = new ArrayList<>();
        
        if (stripePaymentGateway != null) {
            gateways.add("STRIPE");
        }
        if (razorPaymentGateway != null) {
            gateways.add("RAZORPAY");
        }
        
        log.info("Available payment gateways: {}", gateways);
        return gateways;
    }
}
