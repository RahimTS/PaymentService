package rahim.learning.paymentservices.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import rahim.learning.paymentservices.entities.PaymentGatewayType;
import rahim.learning.paymentservices.entities.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for payment response.
 * Includes all relevant payment information for client consumption.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PaymentResponseDto {

    private UUID paymentId;
    private String orderId;
    private BigDecimal amount;
    private String currency;
    private PaymentStatus status;
    private PaymentGatewayType gatewayType;
    private String paymentLink;
    private String gatewayPaymentId;

    // Customer details (may be masked for security)
    private String customerName;
    private String customerEmail;

    // Timestamps
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime completedAt;
    private LocalDateTime expiresAt;

    // Status information
    private String message;
    private String errorMessage;
    private String errorCode;

    // Metadata
    private Integer retryCount;
    private Boolean canRetry;
    private Boolean isExpired;
    private Boolean isSuccessful;
}
