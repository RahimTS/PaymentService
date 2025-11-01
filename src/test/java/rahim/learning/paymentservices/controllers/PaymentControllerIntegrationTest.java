package rahim.learning.paymentservices.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import rahim.learning.paymentservices.dtos.PaymentRequestDto;
import rahim.learning.paymentservices.repositories.PaymentRepository;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for PaymentController
 * Tests with real database and service layer
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Payment Controller Integration Tests")
class PaymentControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PaymentRepository paymentRepository;

    private static String createdPaymentId;
    private static String createdOrderId;

    @BeforeEach
    void setUp() {
        // Clean database before each test (optional)
        // paymentRepository.deleteAll();
    }

    @Test
    @Order(1)
    @DisplayName("End-to-end: Create payment and verify database")
    void testCreatePayment_EndToEnd() throws Exception {
        createdOrderId = "INT-TEST-" + System.currentTimeMillis();
        
        PaymentRequestDto request = PaymentRequestDto.builder()
            .orderId(createdOrderId)
            .amount(new BigDecimal("149.99"))
            .currency("USD")
            .customerName("Integration Test User")
            .customerEmail("integration@test.com")
            .customerPhone("+919876543210")
            .description("Integration test payment")
            .build();

        String response = mockMvc.perform(post("/api/v1/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.paymentId").exists())
            .andExpect(jsonPath("$.orderId").value(createdOrderId))
            .andExpect(jsonPath("$.status").value("PENDING"))
            .andExpect(jsonPath("$.paymentLink").exists())
            .andReturn()
            .getResponse()
            .getContentAsString();

        // Extract payment ID for subsequent tests
        createdPaymentId = objectMapper.readTree(response).get("paymentId").asText();
        
        // Verify in database
        Assertions.assertTrue(paymentRepository.existsByOrderId(createdOrderId));
    }

    @Test
    @Order(2)
    @DisplayName("End-to-end: Retrieve created payment")
    void testGetPayment_EndToEnd() throws Exception {
        Assertions.assertNotNull(createdPaymentId, "Payment must be created first");

        mockMvc.perform(get("/api/v1/payments/{paymentId}", createdPaymentId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.paymentId").value(createdPaymentId))
            .andExpect(jsonPath("$.orderId").value(createdOrderId));
    }

    @Test
    @Order(3)
    @DisplayName("End-to-end: Test idempotency")
    void testIdempotency_EndToEnd() throws Exception {
        PaymentRequestDto request = PaymentRequestDto.builder()
            .orderId(createdOrderId)  // Same order ID
            .amount(new BigDecimal("149.99"))
            .currency("USD")
            .customerName("Integration Test User")
            .customerEmail("integration@test.com")
            .customerPhone("+919876543210")
            .build();

        // Second request with same order should return 409
        mockMvc.perform(post("/api/v1/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.errorCode").value("DUPLICATE_PAYMENT"));
    }

    @Test
    @Order(4)
    @DisplayName("End-to-end: Cancel payment workflow")
    @Transactional
    void testCancelPayment_EndToEnd() throws Exception {
        Assertions.assertNotNull(createdPaymentId, "Payment must be created first");

        mockMvc.perform(post("/api/v1/payments/{paymentId}/cancel", createdPaymentId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("CANCELLED"));

        // Verify status in database
        mockMvc.perform(get("/api/v1/payments/{paymentId}", createdPaymentId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("CANCELLED"));
    }
}
