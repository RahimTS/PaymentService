package rahim.learning.paymentservices.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * DTO for creating a new payment.
 * Includes comprehensive validation rules.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = { "customerPhone", "customerEmail" }) // Exclude PII from logs
public class PaymentRequestDto {

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    @DecimalMax(value = "999999.99", message = "Amount exceeds maximum limit")
    @Digits(integer = 6, fraction = 2, message = "Invalid amount format")
    private BigDecimal amount;

    @NotBlank(message = "Order ID is required")
    @Size(min = 3, max = 100, message = "Order ID must be between 3 and 100 characters")
    @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "Order ID can only contain alphanumeric characters, hyphens, and underscores")
    private String orderId;

    @NotBlank(message = "Customer name is required")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    private String customerName;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String customerEmail;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^[+]?[0-9]{10,15}$", message = "Invalid phone number format")
    private String customerPhone;

    @Pattern(regexp = "INR|USD|EUR|GBP", message = "Unsupported currency. Supported: INR, USD, EUR, GBP")
    @Builder.Default
    private String currency = "INR";

    @Size(max = 500, message = "Description too long")
    private String description;

    /**
     * Optional idempotency key provided by client.
     * If not provided, will be generated from orderId
     */
    @JsonProperty(access = JsonProperty.Access.READ_WRITE)
    private String idempotencyKey;

    /**
     * Optional gateway preference (STRIPE, RAZORPAY)
     * If not provided, uses default gateway
     */
    private String gatewayType;
}
