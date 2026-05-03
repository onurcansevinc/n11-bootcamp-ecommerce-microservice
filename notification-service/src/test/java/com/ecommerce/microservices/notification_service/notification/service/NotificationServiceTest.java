package com.ecommerce.microservices.notification_service.notification.service;

import com.ecommerce.microservices.common.events.payment.PaymentEventTypes;
import com.ecommerce.microservices.common.events.payment.PaymentFailedEventPayload;
import com.ecommerce.microservices.common.events.payment.PaymentSucceededEventPayload;
import com.ecommerce.microservices.notification_service.notification.entity.NotificationChannel;
import com.ecommerce.microservices.notification_service.notification.entity.NotificationEntity;
import com.ecommerce.microservices.notification_service.notification.entity.NotificationProvider;
import com.ecommerce.microservices.notification_service.notification.entity.NotificationStatus;
import com.ecommerce.microservices.notification_service.notification.entity.ProcessedNotificationEventEntity;
import com.ecommerce.microservices.notification_service.notification.repository.NotificationRepository;
import com.ecommerce.microservices.notification_service.notification.repository.ProcessedNotificationEventRepository;
import com.ecommerce.microservices.notification_service.notification.sender.EmailNotificationSender;
import com.ecommerce.microservices.notification_service.notification.sender.SmsNotificationSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

	@Mock
	private NotificationRepository notificationRepository;

	@Mock
	private ProcessedNotificationEventRepository processedNotificationEventRepository;

	@Mock
	private EmailNotificationSender emailNotificationSender;

	@Mock
	private SmsNotificationSender smsNotificationSender;

	private NotificationService notificationService;

	@BeforeEach
	void setUp() {
		notificationService = new NotificationService(
				notificationRepository,
				processedNotificationEventRepository,
				emailNotificationSender,
				smsNotificationSender
		);
	}

	@Test
	void handlePaymentSucceededSendsBothNotificationsAndRecordsProcessedEvent() {
		when(processedNotificationEventRepository.existsById("event-1")).thenReturn(false);
		when(notificationRepository.save(any(NotificationEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(processedNotificationEventRepository.save(any(ProcessedNotificationEventEntity.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		notificationService.handlePaymentSucceeded("event-1", succeededPayload());

		verify(emailNotificationSender).send(
				org.mockito.Mockito.eq("onur@example.com"),
				org.mockito.Mockito.eq("Odemeniz basariyla alindi"),
				org.mockito.ArgumentMatchers.argThat(content -> content.contains("Merhaba Onur Sevinc,")
						&& content.contains("order-1 numarali siparisiniz")
						&& content.contains("Odeme saglayicisi: IYZICO")
						&& content.contains("Tutar: 249.9"))
		);
		verify(smsNotificationSender).send(
				"905551112233",
				"Siparis order-1 icin odemeniz alindi. Tutar: 249.9"
		);

		ArgumentCaptor<NotificationEntity> notificationCaptor = ArgumentCaptor.forClass(NotificationEntity.class);
		verify(notificationRepository, org.mockito.Mockito.times(2)).save(notificationCaptor.capture());
		List<NotificationEntity> notifications = notificationCaptor.getAllValues();

		assertThat(notifications).hasSize(2);
		assertNotification(
				notifications,
				NotificationChannel.EMAIL,
				NotificationProvider.SMTP,
				"onur@example.com",
				"Odemeniz basariyla alindi",
				NotificationStatus.SENT,
				null
		);
		assertNotification(
				notifications,
				NotificationChannel.SMS,
				NotificationProvider.NETGSM,
				"905551112233",
				"SMS",
				NotificationStatus.SENT,
				null
		);

		ArgumentCaptor<ProcessedNotificationEventEntity> processedEventCaptor =
				ArgumentCaptor.forClass(ProcessedNotificationEventEntity.class);
		verify(processedNotificationEventRepository).save(processedEventCaptor.capture());
		assertThat(ReflectionTestUtils.getField(processedEventCaptor.getValue(), "eventId")).isEqualTo("event-1");
		assertThat(ReflectionTestUtils.getField(processedEventCaptor.getValue(), "eventType"))
				.isEqualTo(PaymentEventTypes.SUCCEEDED);
		assertThat(ReflectionTestUtils.getField(processedEventCaptor.getValue(), "orderId")).isEqualTo("order-1");
	}

	@Test
	void handlePaymentSucceededSkipsAlreadyProcessedEvents() {
		when(processedNotificationEventRepository.existsById("event-1")).thenReturn(true);

		notificationService.handlePaymentSucceeded("event-1", succeededPayload());

		verify(emailNotificationSender, never()).send(any(), any(), any());
		verify(smsNotificationSender, never()).send(any(), any());
		verify(notificationRepository, never()).save(any(NotificationEntity.class));
		verify(processedNotificationEventRepository, never()).save(any(ProcessedNotificationEventEntity.class));
	}

	@Test
	void handlePaymentFailedMarksMissingRecipientsAsFailedAndUsesDefaults() {
		when(processedNotificationEventRepository.existsById("event-2")).thenReturn(false);
		when(notificationRepository.save(any(NotificationEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(processedNotificationEventRepository.save(any(ProcessedNotificationEventEntity.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		notificationService.handlePaymentFailed("event-2", failedPayload(null, " ", null, null, null));

		verify(emailNotificationSender, never()).send(any(), any(), any());
		verify(smsNotificationSender, never()).send(any(), any());

		ArgumentCaptor<NotificationEntity> notificationCaptor = ArgumentCaptor.forClass(NotificationEntity.class);
		verify(notificationRepository, org.mockito.Mockito.times(2)).save(notificationCaptor.capture());
		List<NotificationEntity> notifications = notificationCaptor.getAllValues();

		assertThat(notifications).hasSize(2);
		assertNotification(
				notifications,
				NotificationChannel.EMAIL,
				NotificationProvider.SMTP,
				"unknown",
				"Odemeniz basarisiz oldu",
				NotificationStatus.FAILED,
				"Email recipient is missing"
		);
		assertNotification(
				notifications,
				NotificationChannel.SMS,
				NotificationProvider.NETGSM,
				"unknown",
				"SMS",
				NotificationStatus.FAILED,
				"SMS recipient is missing"
		);

		NotificationEntity emailNotification = findNotification(notifications, NotificationChannel.EMAIL);
		assertThat((String) ReflectionTestUtils.getField(emailNotification, "content"))
				.contains("Merhaba Musteri,")
				.contains("Sebep: Belirtilmedi");
	}

	@Test
	void handlePaymentFailedContinuesWhenEmailDeliveryFails() {
		when(processedNotificationEventRepository.existsById("event-3")).thenReturn(false);
		when(notificationRepository.save(any(NotificationEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(processedNotificationEventRepository.save(any(ProcessedNotificationEventEntity.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));
		doThrow(new IllegalStateException("SMTP is down"))
				.when(emailNotificationSender)
				.send(any(), any(), any());

		notificationService.handlePaymentFailed(
				"event-3",
				failedPayload("ayse@example.com", "905551112233", "Ayse", "Kara", "Kart reddedildi")
		);

		verify(emailNotificationSender).send(any(), any(), any());
		verify(smsNotificationSender).send("905551112233", "Siparis order-1 icin odeme basarisiz. Sebep: Kart reddedildi");

		ArgumentCaptor<NotificationEntity> notificationCaptor = ArgumentCaptor.forClass(NotificationEntity.class);
		verify(notificationRepository, org.mockito.Mockito.times(2)).save(notificationCaptor.capture());
		List<NotificationEntity> notifications = notificationCaptor.getAllValues();

		assertNotification(
				notifications,
				NotificationChannel.EMAIL,
				NotificationProvider.SMTP,
				"ayse@example.com",
				"Odemeniz basarisiz oldu",
				NotificationStatus.FAILED,
				"SMTP is down"
		);
		assertNotification(
				notifications,
				NotificationChannel.SMS,
				NotificationProvider.NETGSM,
				"905551112233",
				"SMS",
				NotificationStatus.SENT,
				null
		);
	}

	private void assertNotification(
			List<NotificationEntity> notifications,
			NotificationChannel channel,
			NotificationProvider provider,
			String recipient,
			String subject,
			NotificationStatus status,
			String failureReason
	) {
		NotificationEntity notification = findNotification(notifications, channel);
		assertThat(ReflectionTestUtils.getField(notification, "eventType"))
				.isIn(PaymentEventTypes.SUCCEEDED, PaymentEventTypes.FAILED);
		assertThat(ReflectionTestUtils.getField(notification, "orderId")).isEqualTo("order-1");
		assertThat(ReflectionTestUtils.getField(notification, "customerId")).isEqualTo("customer-1");
		assertThat(ReflectionTestUtils.getField(notification, "channel")).isEqualTo(channel);
		assertThat(ReflectionTestUtils.getField(notification, "provider")).isEqualTo(provider);
		assertThat(ReflectionTestUtils.getField(notification, "recipient")).isEqualTo(recipient);
		assertThat(ReflectionTestUtils.getField(notification, "subject")).isEqualTo(subject);
		assertThat(ReflectionTestUtils.getField(notification, "status")).isEqualTo(status);
		assertThat(ReflectionTestUtils.getField(notification, "failureReason")).isEqualTo(failureReason);

		LocalDateTime sentAt = (LocalDateTime) ReflectionTestUtils.getField(notification, "sentAt");
		if (status == NotificationStatus.SENT) {
			assertThat(sentAt).isNotNull();
		} else {
			assertThat(sentAt).isNull();
		}
	}

	private NotificationEntity findNotification(List<NotificationEntity> notifications, NotificationChannel channel) {
		return notifications.stream()
				.filter(notification -> ReflectionTestUtils.getField(notification, "channel") == channel)
				.findFirst()
				.orElseThrow();
	}

	private PaymentSucceededEventPayload succeededPayload() {
		return new PaymentSucceededEventPayload(
				"payment-1",
				"order-1",
				"customer-1",
				"Onur",
				"Sevinc",
				"onur@example.com",
				"905551112233",
				"IYZICO",
				BigDecimal.valueOf(249.90)
		);
	}

	private PaymentFailedEventPayload failedPayload(
			String email,
			String gsm,
			String name,
			String surname,
			String failureReason
	) {
		return new PaymentFailedEventPayload(
				"payment-1",
				"order-1",
				"customer-1",
				name,
				surname,
				email,
				gsm,
				"IYZICO",
				BigDecimal.valueOf(249.90),
				failureReason
		);
	}

}
