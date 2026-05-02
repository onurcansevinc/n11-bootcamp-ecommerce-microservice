package com.ecommerce.microservices.payment_service.payment.repository;

import com.ecommerce.microservices.payment_service.payment.entity.PaymentEntity;
import com.ecommerce.microservices.payment_service.payment.entity.PaymentProvider;
import com.ecommerce.microservices.payment_service.payment.entity.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<PaymentEntity, String> {

	boolean existsByOrderIdAndStatusIn(String orderId, Collection<PaymentStatus> statuses);

	Page<PaymentEntity> findByCustomerIdOrderByCreatedAtDesc(String customerId, Pageable pageable);

	Optional<PaymentEntity> findByExternalPaymentIdAndProvider(String externalPaymentId, PaymentProvider provider);

}
