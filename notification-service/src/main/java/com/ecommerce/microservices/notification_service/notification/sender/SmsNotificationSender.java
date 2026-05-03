package com.ecommerce.microservices.notification_service.notification.sender;

public interface SmsNotificationSender {

	void send(String recipient, String content);

}
