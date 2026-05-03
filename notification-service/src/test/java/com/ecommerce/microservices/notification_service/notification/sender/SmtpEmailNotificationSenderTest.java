package com.ecommerce.microservices.notification_service.notification.sender;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SmtpEmailNotificationSenderTest {

	@Mock
	private JavaMailSender javaMailSender;

	@Test
	void sendBuildsExpectedMailMessage() {
		SmtpEmailNotificationSender sender =
				new SmtpEmailNotificationSender(javaMailSender, "no-reply@example.com");

		sender.send("onur@example.com", "Payment received", "Order paid");

		ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
		verify(javaMailSender).send(messageCaptor.capture());
		SimpleMailMessage message = messageCaptor.getValue();
		assertThat(message.getFrom()).isEqualTo("no-reply@example.com");
		assertThat(message.getTo()).containsExactly("onur@example.com");
		assertThat(message.getSubject()).isEqualTo("Payment received");
		assertThat(message.getText()).isEqualTo("Order paid");
	}

}
