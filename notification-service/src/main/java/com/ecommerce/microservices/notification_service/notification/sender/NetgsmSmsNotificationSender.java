package com.ecommerce.microservices.notification_service.notification.sender;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@Component
public class NetgsmSmsNotificationSender implements SmsNotificationSender {

	private final RestTemplate restTemplate;
	private final String endpoint;
	private final String userCode;
	private final String password;
	private final String msgHeader;

	public NetgsmSmsNotificationSender(
			RestTemplate restTemplate,
			@Value("${notification.netgsm.endpoint:https://api.netgsm.com.tr/sms/send/get/}") String endpoint,
			@Value("${notification.netgsm.user-code:}") String userCode,
			@Value("${notification.netgsm.password:}") String password,
			@Value("${notification.netgsm.msg-header:}") String msgHeader
	) {
		this.restTemplate = restTemplate;
		this.endpoint = endpoint;
		this.userCode = userCode;
		this.password = password;
		this.msgHeader = msgHeader;
	}

	@Override
	public void send(String recipient, String content) {
		if (userCode.isBlank() || password.isBlank() || msgHeader.isBlank()) {
			throw new IllegalStateException("Netgsm credentials are not configured");
		}

		String requestUrl = UriComponentsBuilder.fromUriString(endpoint)
				.queryParam("usercode", userCode)
				.queryParam("password", password)
				.queryParam("gsmno", recipient)
				.queryParam("message", content)
				.queryParam("msgheader", msgHeader)
				.encode()
				.build()
				.toUriString();

		try {
			String response = restTemplate.getForObject(URI.create(requestUrl), String.class);
			if (!isSuccessful(response)) {
				throw new IllegalStateException("Netgsm SMS send failed with response: " + response);
			}
		} catch (RestClientException exception) {
			throw new IllegalStateException("Netgsm SMS request failed", exception);
		}
	}

	private boolean isSuccessful(String response) {
		if (response == null) {
			return false;
		}
		String normalized = response.trim();
		return "00".equals(normalized) || normalized.length() > 2;
	}

}
