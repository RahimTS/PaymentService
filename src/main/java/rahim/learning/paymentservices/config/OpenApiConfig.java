package rahim.learning.paymentservices.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI 3.0 configuration for API documentation
 */
@Configuration
public class OpenApiConfig {

    @Value("${spring.application.name}")
    private String applicationName;

    @Bean
    public OpenAPI paymentServiceOpenAPI() {
        Server localServer = new Server();
        localServer.setUrl("http://localhost:8080");
        localServer.setDescription("Local Development Server");

        Contact contact = new Contact();
        contact.setName("Rahim T S");
        contact.setEmail("rahim@example.com");
        contact.setUrl("https://github.com/RahimTS");

        License license = new License()
                .name("MIT License")
                .url("https://opensource.org/licenses/MIT");

        Info info = new Info()
                .title("Payment Service API")
                .version("1.3.0")
                .description("""
                        Production-ready payment processing system with multi-gateway support.

                        ## Features
                        - Multi-gateway support (Stripe, Razorpay)
                        - Idempotency handling with Redis
                        - Event sourcing for audit trail
                        - Retry logic for failed payments
                        - Distributed caching
                        - Comprehensive metrics

                        ## Rate Limiting
                        100 requests per minute per IP
                        """)
                .contact(contact)
                .license(license);

        return new OpenAPI()
                .info(info)
                .servers(List.of(localServer));
    }
}
