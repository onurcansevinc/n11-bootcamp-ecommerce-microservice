package com.ecommerce.microservices.notification_service.notification.repository;

import com.ecommerce.microservices.notification_service.notification.entity.ProcessedNotificationEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedNotificationEventRepository extends JpaRepository<ProcessedNotificationEventEntity, String> {
}
