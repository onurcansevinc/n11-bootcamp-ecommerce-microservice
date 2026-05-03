package com.ecommerce.microservices.notification_service.notification.service;

import com.ecommerce.microservices.common.events.payment.PaymentEventTypes;
import com.ecommerce.microservices.common.events.payment.PaymentFailedEventPayload;
import com.ecommerce.microservices.common.events.payment.PaymentSucceededEventPayload;
import com.ecommerce.microservices.notification_service.notification.entity.NotificationChannel;
import com.ecommerce.microservices.notification_service.notification.entity.NotificationEntity;
import com.ecommerce.microservices.notification_service.notification.entity.NotificationProvider;
import com.ecommerce.microservices.notification_service.notification.entity.ProcessedNotificationEventEntity;
import com.ecommerce.microservices.notification_service.notification.repository.NotificationRepository;
import com.ecommerce.microservices.notification_service.notification.repository.ProcessedNotificationEventRepository;
import com.ecommerce.microservices.notification_service.notification.sender.EmailNotificationSender;
import com.ecommerce.microservices.notification_service.notification.sender.SmsNotificationSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {

	private final NotificationRepository notificationRepository;
	private final ProcessedNotificationEventRepository processedNotificationEventRepository;
	private final EmailNotificationSender emailNotificationSender;
	private final SmsNotificationSender smsNotificationSender;

	public NotificationService(
			NotificationRepository notificationRepository,
			ProcessedNotificationEventRepository processedNotificationEventRepository,
			EmailNotificationSender emailNotificationSender,
			SmsNotificationSender smsNotificationSender
	) {
		this.notificationRepository = notificationRepository;
		this.processedNotificationEventRepository = processedNotificationEventRepository;
		this.emailNotificationSender = emailNotificationSender;
		this.smsNotificationSender = smsNotificationSender;
	}

	@Transactional
	public void handlePaymentSucceeded(String eventId, PaymentSucceededEventPayload payload) {
		if (processedNotificationEventRepository.existsById(eventId)) {
			return;
		}

		String fullName = fullName(payload.customerName(), payload.customerSurname());
		String emailSubject = "Odemeniz basariyla alindi";
		String emailContent = """
				Merhaba %s,

				%s numarali siparisiniz icin odemeniz basariyla alinmistir.
				Odeme saglayicisi: %s
				Tutar: %s
				""".formatted(fullName, payload.orderId(), payload.provider(), payload.amount());
		String smsContent = "Siparis " + payload.orderId() + " icin odemeniz alindi. Tutar: " + payload.amount();

		deliverEmail(eventId, PaymentEventTypes.SUCCEEDED, payload.orderId(), payload.customerId(), payload.customerEmail(), emailSubject, emailContent);
		deliverSms(eventId, PaymentEventTypes.SUCCEEDED, payload.orderId(), payload.customerId(), payload.customerGsmNumber(), smsContent);
		recordProcessedEvent(eventId, PaymentEventTypes.SUCCEEDED, payload.orderId());
	}

	@Transactional
	public void handlePaymentFailed(String eventId, PaymentFailedEventPayload payload) {
		if (processedNotificationEventRepository.existsById(eventId)) {
			return;
		}

		String fullName = fullName(payload.customerName(), payload.customerSurname());
		String emailSubject = "Odemeniz basarisiz oldu";
		String emailContent = """
				Merhaba %s,

				%s numarali siparisiniz icin odeme islemi basarisiz oldu.
				Odeme saglayicisi: %s
				Tutar: %s
				Sebep: %s
				""".formatted(
					fullName,
					payload.orderId(),
					payload.provider(),
					payload.amount(),
					defaultFailureReason(payload.failureReason())
				);
		String smsContent = "Siparis " + payload.orderId() + " icin odeme basarisiz. Sebep: " + defaultFailureReason(payload.failureReason());

		deliverEmail(eventId, PaymentEventTypes.FAILED, payload.orderId(), payload.customerId(), payload.customerEmail(), emailSubject, emailContent);
		deliverSms(eventId, PaymentEventTypes.FAILED, payload.orderId(), payload.customerId(), payload.customerGsmNumber(), smsContent);
		recordProcessedEvent(eventId, PaymentEventTypes.FAILED, payload.orderId());
	}

	private void deliverEmail(
			String eventId,
			String eventType,
			String orderId,
			String customerId,
			String recipient,
			String subject,
			String content
	) {
		NotificationEntity notification = new NotificationEntity(
				eventId,
				eventType,
				orderId,
				customerId,
				NotificationChannel.EMAIL,
				NotificationProvider.SMTP,
				normalizeRecipient(recipient),
				subject,
				content
		);

		try {
			assertRecipient(recipient, "Email recipient is missing");
			emailNotificationSender.send(recipient, subject, content);
			notification.markSent();
		} catch (RuntimeException exception) {
			notification.markFailed(exception.getMessage());
		}

		notificationRepository.save(notification);
	}

	private void deliverSms(
			String eventId,
			String eventType,
			String orderId,
			String customerId,
			String recipient,
			String content
	) {
		NotificationEntity notification = new NotificationEntity(
				eventId,
				eventType,
				orderId,
				customerId,
				NotificationChannel.SMS,
				NotificationProvider.NETGSM,
				normalizeRecipient(recipient),
				"SMS",
				content
		);

		try {
			assertRecipient(recipient, "SMS recipient is missing");
			smsNotificationSender.send(recipient, content);
			notification.markSent();
		} catch (RuntimeException exception) {
			notification.markFailed(exception.getMessage());
		}

		notificationRepository.save(notification);
	}

	private void recordProcessedEvent(String eventId, String eventType, String orderId) {
		processedNotificationEventRepository.save(
				new ProcessedNotificationEventEntity(eventId, eventType, orderId)
		);
	}

	private String fullName(String customerName, String customerSurname) {
		String normalizedName = customerName == null ? "" : customerName.trim();
		String normalizedSurname = customerSurname == null ? "" : customerSurname.trim();
		String fullName = (normalizedName + " " + normalizedSurname).trim();
		return fullName.isBlank() ? "Musteri" : fullName;
	}

	private String defaultFailureReason(String failureReason) {
		if (failureReason == null || failureReason.isBlank()) {
			return "Belirtilmedi";
		}
		return failureReason;
	}

	private void assertRecipient(String recipient, String message) {
		if (recipient == null || recipient.isBlank()) {
			throw new IllegalStateException(message);
		}
	}

	private String normalizeRecipient(String recipient) {
		if (recipient == null || recipient.isBlank()) {
			return "unknown";
		}
		return recipient.trim();
	}

}
