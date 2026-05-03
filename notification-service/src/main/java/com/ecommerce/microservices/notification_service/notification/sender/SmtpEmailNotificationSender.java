package com.ecommerce.microservices.notification_service.notification.sender;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
public class SmtpEmailNotificationSender implements EmailNotificationSender {

	private final JavaMailSender javaMailSender;
	private final String fromAddress;

	public SmtpEmailNotificationSender(
			JavaMailSender javaMailSender,
			@Value("${notification.mail.from:no-reply@n11-bootcamp.local}") String fromAddress
	) {
		this.javaMailSender = javaMailSender;
		this.fromAddress = fromAddress;
	}

	@Override
	public void send(String recipient, String subject, String content) {
		SimpleMailMessage message = new SimpleMailMessage();
		message.setFrom(fromAddress);
		message.setTo(recipient);
		message.setSubject(subject);
		message.setText(content);
		javaMailSender.send(message);
	}

}
