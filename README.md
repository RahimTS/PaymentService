# Payment Service - Production-Ready Payment Processing System

## 🚀 Overview

A scalable, production-grade payment processing microservice built with Spring Boot 3, supporting multiple payment gateways (Stripe, Razorpay) with comprehensive features including:

- ✅ Multi-gateway support with strategy pattern
- ✅ Database persistence with MySQL
- ✅ Event sourcing for complete audit trail
- ✅ Idempotency to prevent duplicate payments
- ✅ Optimistic locking for concurrent safety
- ✅ Flyway migrations for version-controlled schema

## 🏗️ Architecture

### Current Implementation (Phase 1)

Payment Service API

Controllers │ Services │ Gateways               

JPA Repositories │ Entities                     

MySQL Database (8.0)                            
 - payments table                                
 - payment_events table (event sourcing)         


text

## 🛠️ Tech Stack

- **Java**: 17
- **Spring Boot**: 3.2.0
- **Database**: MySQL 8.0
- **Migration**: Flyway
- **Payment Gateways**: Stripe, Razorpay
- **Build Tool**: Maven
- **Containerization**: Docker & Docker Compose

## 📋 Prerequisites

- JDK 17 or higher
- Maven 3.8+
- Docker & Docker Compose
- Git

## 🚀 Getting Started

### 1. Clone the Repository

git clone https://github.com/RahimTS/PaymentService.git
cd PaymentService

### 2. Start MySQL with Docker Compose

    docker-compose up

This starts:
- MySQL 8.0 on port 3306
- phpMyAdmin on port 8081 (http://localhost:8081)

### 3. Configure Environment Variables

    cp .env.example .env

Edit .env with your actual credentials

### 4. Run Flyway Migrations

    mvn flyway:migrate

### 5. Build the Project

    mvn clean install

### 6. Run the Application

    mvn spring-boot:run

The application will start on `http://localhost:8080`

## 📊 Database Schema

### Payments Table
Stores main payment transaction records with:
- Payment lifecycle tracking
- Customer information
- Gateway integration details
- Retry and error handling fields
- Optimistic locking

### Payment Events Table
Event sourcing table capturing:
- All payment state changes
- Complete audit trail
- Event replay capability

## 🔍 Testing Database Connection

Access phpMyAdmin at http://localhost:8081
- Server: mysql
- Username: root
- Password: password

## 📈 Development Roadmap

### ✅ Phase 1: Database Foundation (Current)
- [x] JPA entities with proper relationships
- [x] Repository layer with custom queries
- [x] Flyway migrations
- [x] Docker Compose setup
- [x] Comprehensive README

### 🚧 Phase 2: Business Logic (Next)
- [ ] Payment service implementation
- [ ] Validation and error handling
- [ ] Retry logic with exponential backoff
- [ ] Custom exceptions

### 📅 Phase 3: Idempotency
- [ ] Redis integration
- [ ] Idempotency service
- [ ] Duplicate payment prevention

### 📅 Phase 4: Event Sourcing & Kafka
- [ ] Kafka integration
- [ ] Event publishing
- [ ] Event consumers

### 📅 Phase 5: Webhook Processing
- [ ] Async webhook handlers
- [ ] Signature verification
- [ ] Gateway-specific processors

### 📅 Phase 6: Observability
- [ ] Prometheus metrics
- [ ] Custom payment metrics
- [ ] Health checks
- [ ] Distributed tracing

## 🤝 Contributing

This is a learning project to demonstrate production-ready payment system development. Contributions and suggestions are welcome!

## 📝 License

MIT License

## 👤 Author

**Rahim T S**
- GitHub: [@RahimTS](https://github.com/RahimTS)
- LinkedIn: [rahim-t-s](https://linkedin.com/in/rahim-t-s-15910512b)

---

**Note**: This is a learning/portfolio project. Never use real payment credentials in development/testing. Always use sandbox/test API keys from payment gateways.