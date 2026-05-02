package com.ecommerce.microservices.order_service.payment.processed.repository;

import com.ecommerce.microservices.order_service.payment.processed.entity.ProcessedPaymentEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedPaymentEventRepository extends JpaRepository<ProcessedPaymentEventEntity, String> {
}
