package rahim.learning.paymentservices.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import rahim.learning.paymentservices.dtos.PaymentRequestDto;
import rahim.learning.paymentservices.dtos.PaymentResponseDto;
import rahim.learning.paymentservices.dtos.PaymentStatusUpdateDto;
import rahim.learning.paymentservices.entities.PaymentGatewayType;
import rahim.learning.paymentservices.entities.PaymentStatus;
import rahim.learning.paymentservices.exceptions.DuplicatePaymentException;
import rahim.learning.paymentservices.exceptions.PaymentNotFoundException;
import rahim.learning.paymentservices.exceptions.PaymentProcessingException;
import rahim.learning.paymentservices.services.IPaymentService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for PaymentController
 * Tests controller layer in isolation using mocked service
 */
@WebMvcTest(PaymentController.class)
@DisplayName("Payment Controller Tests")
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private IPaymentService paymentService;

    private PaymentRequestDto validRequest;
    private PaymentResponseDto mockResponse;
    private UUID testPaymentId;
    private String testOrderId;

    @BeforeEach
    void setUp() {
        testPaymentId = UUID.randomUUID();
        testOrderId = "TEST-ORDER-001";

        // Setup valid request
        validRequest = PaymentRequestDto.builder()
            .orderId(testOrderId)
            .amount(new BigDecimal("99.99"))
            .currency("USD")
            .customerName("John Doe")
            .customerEmail("john@example.com")
            .customerPhone("+919876543210")
            .description("Test payment")
            .build();

        // Setup mock response
        mockResponse = PaymentResponseDto.builder()
            .paymentId(testPaymentId)
            .orderId(testOrderId)
            .amount(new BigDecimal("99.99"))
            .currency("USD")
            .status(PaymentStatus.PENDING)
            .gatewayType(PaymentGatewayType.STRIPE)
            .paymentLink("https://checkout.stripe.com/test")
            .customerName("John Doe")
            .customerEmail("jo***@example.com")
            .createdAt(LocalDateTime.now())
            .message("Payment link is ready. Awaiting customer action")
            .canRetry(false)
            .isExpired(false)
            .isSuccessful(false)
            .build();
    }

    // ========== CREATE PAYMENT TESTS ==========

    @Test
    @DisplayName("Should create payment successfully with valid request")
    void testCreatePayment_Success() throws Exception {
        when(paymentService.createPayment(any(PaymentRequestDto.class)))
            .thenReturn(mockResponse);

        mockMvc.perform(post("/api/v1/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.paymentId").value(testPaymentId.toString()))
            .andExpect(jsonPath("$.orderId").value(testOrderId))
            .andExpect(jsonPath("$.status").value("PENDING"))
            .andExpect(jsonPath("$.paymentLink").exists());
    }

    @Test
    @DisplayName("Should create payment with idempotency key header")
    void testCreatePayment_WithIdempotencyHeader() throws Exception {
        String idempotencyKey = "test-idempotency-key";
        
        when(paymentService.createPayment(any(PaymentRequestDto.class)))
            .thenReturn(mockResponse);

        mockMvc.perform(post("/api/v1/payments")
                .header("Idempotency-Key", idempotencyKey)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.paymentId").exists());
    }

    @Test
    @DisplayName("Should return 400 for invalid amount")
    void testCreatePayment_InvalidAmount() throws Exception {
        PaymentRequestDto invalidRequest = PaymentRequestDto.builder()
            .orderId(testOrderId)
            .amount(new BigDecimal("-10.00")) // Invalid: negative amount
            .currency("USD")
            .customerName("John Doe")
            .customerEmail("john@example.com")
            .customerPhone("+919876543210")
            .build();

        mockMvc.perform(post("/api/v1/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 400 for invalid email format")
    void testCreatePayment_InvalidEmail() throws Exception {
        PaymentRequestDto invalidRequest = PaymentRequestDto.builder()
            .orderId(testOrderId)
            .amount(new BigDecimal("99.99"))
            .currency("USD")
            .customerName("John Doe")
            .customerEmail("invalid-email") // Invalid email
            .customerPhone("+919876543210")
            .build();

        mockMvc.perform(post("/api/v1/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("Should return 409 for duplicate payment")
    void testCreatePayment_Duplicate() throws Exception {
        when(paymentService.createPayment(any(PaymentRequestDto.class)))
            .thenThrow(new DuplicatePaymentException(
                "Payment already exists for order: " + testOrderId,
                testPaymentId.toString()
            ));

        mockMvc.perform(post("/api/v1/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.errorCode").value("DUPLICATE_PAYMENT"));
    }

    @Test
    @DisplayName("Should return 502 for gateway error")
    void testCreatePayment_GatewayError() throws Exception {
        when(paymentService.createPayment(any(PaymentRequestDto.class)))
            .thenThrow(new PaymentProcessingException("Gateway timeout", true));

        mockMvc.perform(post("/api/v1/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
            .andExpect(status().isBadGateway())
            .andExpect(jsonPath("$.errorCode").value("PAYMENT_PROCESSING_ERROR"));
    }

    // ========== GET PAYMENT BY ID TESTS ==========

    @Test
    @DisplayName("Should get payment by ID successfully")
    void testGetPaymentById_Success() throws Exception {
        when(paymentService.getPaymentById(testPaymentId))
            .thenReturn(mockResponse);

        mockMvc.perform(get("/api/v1/payments/{paymentId}", testPaymentId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.paymentId").value(testPaymentId.toString()))
            .andExpect(jsonPath("$.orderId").value(testOrderId));
    }

    @Test
    @DisplayName("Should return 404 for non-existent payment ID")
    void testGetPaymentById_NotFound() throws Exception {
        UUID nonExistentId = UUID.randomUUID();
        
        when(paymentService.getPaymentById(nonExistentId))
            .thenThrow(new PaymentNotFoundException(nonExistentId.toString(), "id"));

        mockMvc.perform(get("/api/v1/payments/{paymentId}", nonExistentId))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.errorCode").value("PAYMENT_NOT_FOUND"));
    }

    // ========== GET PAYMENT BY ORDER ID TESTS ==========

    @Test
    @DisplayName("Should get payment by order ID successfully")
    void testGetPaymentByOrderId_Success() throws Exception {
        when(paymentService.getPaymentByOrderId(testOrderId))
            .thenReturn(mockResponse);

        mockMvc.perform(get("/api/v1/payments/order/{orderId}", testOrderId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.orderId").value(testOrderId));
    }

    @Test
    @DisplayName("Should return 404 for non-existent order ID")
    void testGetPaymentByOrderId_NotFound() throws Exception {
        String nonExistentOrderId = "NON-EXISTENT";
        
        when(paymentService.getPaymentByOrderId(nonExistentOrderId))
            .thenThrow(new PaymentNotFoundException(nonExistentOrderId, "orderId"));

        mockMvc.perform(get("/api/v1/payments/order/{orderId}", nonExistentOrderId))
            .andExpect(status().isNotFound());
    }

    // ========== GET CUSTOMER PAYMENTS TESTS ==========

    @Test
    @DisplayName("Should get all payments for customer")
    void testGetPaymentsByCustomer_Success() throws Exception {
        List<PaymentResponseDto> payments = Arrays.asList(mockResponse);
        
        when(paymentService.getPaymentsByCustomer("john@example.com"))
            .thenReturn(payments);

        mockMvc.perform(get("/api/v1/payments/customer/{email}", "john@example.com"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$[0].customerEmail").exists());
    }

    @Test
    @DisplayName("Should return empty list for customer with no payments")
    void testGetPaymentsByCustomer_Empty() throws Exception {
        when(paymentService.getPaymentsByCustomer("nopayments@example.com"))
            .thenReturn(Arrays.asList());

        mockMvc.perform(get("/api/v1/payments/customer/{email}", "nopayments@example.com"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$").isEmpty());
    }

    // ========== UPDATE PAYMENT STATUS TESTS ==========

    @Test
    @DisplayName("Should update payment status successfully")
    void testUpdatePaymentStatus_Success() throws Exception {
        PaymentStatusUpdateDto statusUpdate = PaymentStatusUpdateDto.builder()
            .status(PaymentStatus.SUCCESS)
            .build();

        PaymentResponseDto updatedResponse = PaymentResponseDto.builder()
            .paymentId(testPaymentId)
            .status(PaymentStatus.SUCCESS)
            .build();

        when(paymentService.updatePaymentStatus(eq(testPaymentId), any()))
            .thenReturn(updatedResponse);

        mockMvc.perform(patch("/api/v1/payments/{paymentId}/status", testPaymentId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(statusUpdate)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("SUCCESS"));
    }

    // ========== RETRY PAYMENT TESTS ==========

    @Test
    @DisplayName("Should retry failed payment successfully")
    void testRetryPayment_Success() throws Exception {
        when(paymentService.retryPayment(testPaymentId))
            .thenReturn(mockResponse);

        mockMvc.perform(post("/api/v1/payments/{paymentId}/retry", testPaymentId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.paymentId").value(testPaymentId.toString()));
    }

    // ========== CANCEL PAYMENT TESTS ==========

    @Test
    @DisplayName("Should cancel pending payment successfully")
    void testCancelPayment_Success() throws Exception {
        PaymentResponseDto cancelledResponse = PaymentResponseDto.builder()
            .paymentId(testPaymentId)
            .status(PaymentStatus.CANCELLED)
            .build();

        when(paymentService.cancelPayment(testPaymentId))
            .thenReturn(cancelledResponse);

        mockMvc.perform(post("/api/v1/payments/{paymentId}/cancel", testPaymentId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("CANCELLED"));
    }
}
