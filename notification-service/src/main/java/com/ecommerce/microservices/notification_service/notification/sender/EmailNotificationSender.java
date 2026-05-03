package com.ecommerce.microservices.notification_service.notification.sender;

public interface EmailNotificationSender {

	void send(String recipient, String subject, String content);

}
