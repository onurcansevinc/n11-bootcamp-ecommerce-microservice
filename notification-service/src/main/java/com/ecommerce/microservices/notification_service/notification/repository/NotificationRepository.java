package com.ecommerce.microservices.notification_service.notification.repository;

import com.ecommerce.microservices.notification_service.notification.entity.NotificationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<NotificationEntity, String> {
}
