package rahim.learning.paymentservices.services;

import rahim.learning.paymentservices.dtos.PaymentRequestDto;
import rahim.learning.paymentservices.dtos.PaymentResponseDto;
import rahim.learning.paymentservices.dtos.PaymentStatusUpdateDto;
import rahim.learning.paymentservices.entities.PaymentStatus;

import java.util.List;
import java.util.UUID;

/**
 * Service interface for payment operations.
 * Defines contract for payment processing, retrieval, and updates.
 */
public interface IPaymentService {

    /**
     * Create a new payment and generate payment link
     * 
     * @param requestDto Payment creation request
     * @return PaymentResponseDto with payment details and link
     */
    PaymentResponseDto createPayment(PaymentRequestDto requestDto);

    /**
     * Get payment by ID
     * 
     * @param paymentId Payment UUID
     * @return PaymentResponseDto
     */
    PaymentResponseDto getPaymentById(UUID paymentId);

    /**
     * Get payment by order ID
     * 
     * @param orderId Merchant order ID
     * @return PaymentResponseDto
     */
    PaymentResponseDto getPaymentByOrderId(String orderId);

    /**
     * Update payment status
     * 
     * @param paymentId    Payment UUID
     * @param statusUpdate Status update details
     * @return Updated PaymentResponseDto
     */
    PaymentResponseDto updatePaymentStatus(UUID paymentId, PaymentStatusUpdateDto statusUpdate);

    /**
     * Get all payments for a customer
     * 
     * @param customerEmail Customer email
     * @return List of payments
     */
    List<PaymentResponseDto> getPaymentsByCustomer(String customerEmail);

    /**
     * Retry failed payment
     * 
     * @param paymentId Payment UUID
     * @return Updated PaymentResponseDto
     */
    PaymentResponseDto retryPayment(UUID paymentId);

    /**
     * Cancel pending payment
     * 
     * @param paymentId Payment UUID
     * @return Updated PaymentResponseDto
     */
    PaymentResponseDto cancelPayment(UUID paymentId);
}
