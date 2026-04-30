package com.ecommerce.microservices.order_service.order.repository;

import com.ecommerce.microservices.order_service.order.entity.OrderEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<OrderEntity, String> {

	Page<OrderEntity> findByCustomerIdOrderByCreatedAtDesc(String customerId, Pageable pageable);

}
