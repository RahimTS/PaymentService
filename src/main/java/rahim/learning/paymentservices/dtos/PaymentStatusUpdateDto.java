package rahim.learning.paymentservices.dtos;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import rahim.learning.paymentservices.entities.PaymentStatus;

/**
 * DTO for updating payment status (used internally or by webhooks)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentStatusUpdateDto {
    
    @NotNull(message = "Status is required")
    private PaymentStatus status;
    
    private String gatewayPaymentId;
    private String errorMessage;
    private String errorCode;
    private String metadata;
}
