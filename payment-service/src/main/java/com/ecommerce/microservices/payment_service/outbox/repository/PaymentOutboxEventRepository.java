package com.ecommerce.microservices.payment_service.outbox.repository;

import com.ecommerce.microservices.payment_service.outbox.entity.PaymentOutboxEventEntity;
import com.ecommerce.microservices.payment_service.outbox.entity.PaymentOutboxEventStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaymentOutboxEventRepository extends JpaRepository<PaymentOutboxEventEntity, String> {

	List<PaymentOutboxEventEntity> findByStatusOrderByCreatedAtAsc(PaymentOutboxEventStatus status, Pageable pageable);

}
