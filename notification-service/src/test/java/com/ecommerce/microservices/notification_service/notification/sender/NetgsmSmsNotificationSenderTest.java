package com.ecommerce.microservices.notification_service.notification.sender;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class NetgsmSmsNotificationSenderTest {

	@Test
	void sendCallsNetgsmWithExpectedQueryParameters() {
		RestTemplate restTemplate = new RestTemplate();
		MockRestServiceServer server = MockRestServiceServer.createServer(restTemplate);
		NetgsmSmsNotificationSender sender = new NetgsmSmsNotificationSender(
				restTemplate,
				"https://api.netgsm.example/send",
				"user1",
				"pass1",
				"header1"
		);

		server.expect(requestTo(
				"https://api.netgsm.example/send?usercode=user1&password=pass1&gsmno=905551112233&message=Order%20paid&msgheader=header1"
		))
				.andExpect(method(HttpMethod.GET))
				.andRespond(withSuccess("00", MediaType.TEXT_PLAIN));

		sender.send("905551112233", "Order paid");

		server.verify();
	}

	@Test
	void sendRejectsMissingCredentials() {
		NetgsmSmsNotificationSender sender = new NetgsmSmsNotificationSender(
				new RestTemplate(),
				"https://api.netgsm.example/send",
				"",
				"pass1",
				"header1"
		);

		assertThatThrownBy(() -> sender.send("905551112233", "Order paid"))
				.isInstanceOf(IllegalStateException.class)
				.hasMessage("Netgsm credentials are not configured");
	}

	@Test
	void sendRejectsUnexpectedProviderResponse() {
		RestTemplate restTemplate = new RestTemplate();
		MockRestServiceServer server = MockRestServiceServer.createServer(restTemplate);
		NetgsmSmsNotificationSender sender = new NetgsmSmsNotificationSender(
				restTemplate,
				"https://api.netgsm.example/send",
				"user1",
				"pass1",
				"header1"
		);

		server.expect(requestTo(
				"https://api.netgsm.example/send?usercode=user1&password=pass1&gsmno=905551112233&message=Order%20paid&msgheader=header1"
		))
				.andExpect(method(HttpMethod.GET))
				.andRespond(withSuccess("99", MediaType.TEXT_PLAIN));

		assertThatThrownBy(() -> sender.send("905551112233", "Order paid"))
				.isInstanceOf(IllegalStateException.class)
				.hasMessage("Netgsm SMS send failed with response: 99");

		server.verify();
	}

}
