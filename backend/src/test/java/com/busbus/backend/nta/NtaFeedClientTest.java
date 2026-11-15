package com.busbus.backend.nta;

import java.time.Duration;

import com.busbus.backend.vehicle.FeedFixtures;
import com.google.transit.realtime.GtfsRealtime.FeedMessage;
import org.junit.jupiter.api.Test;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class NtaFeedClientTest {

	private final RestClient.Builder builder = RestClient.builder();
	private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
	private final NtaFeedClient client = new NtaFeedClient(builder,
			new NtaProperties("https://nta.test/gtfsr/v2", "test-key", Duration.ofSeconds(60)));

	@Test
	void sendsTheApiKeyAndDecodesTheProtobufBody() {
		FeedMessage feed = FeedFixtures.feedOf(FeedFixtures.fullVehicle());
		server.expect(requestTo("https://nta.test/gtfsr/v2/Vehicles"))
				.andExpect(header("x-api-key", "test-key"))
				.andRespond(withSuccess(feed.toByteArray(), MediaType.APPLICATION_OCTET_STREAM));

		assertThat(client.fetchVehiclePositions()).isEqualTo(feed);
		server.verify();
	}

	@Test
	void throwsWhenNtaRejectsTheKey() {
		server.expect(requestTo("https://nta.test/gtfsr/v2/Vehicles")).andRespond(withStatus(HttpStatus.UNAUTHORIZED));

		assertThatThrownBy(client::fetchVehiclePositions).isInstanceOf(HttpClientErrorException.Unauthorized.class);
	}

	@Test
	void throwsWhenTheBodyIsNotProtobuf() {
		server.expect(requestTo("https://nta.test/gtfsr/v2/Vehicles"))
				.andRespond(withSuccess("<html>maintenance</html>", MediaType.TEXT_HTML));

		assertThatThrownBy(client::fetchVehiclePositions).isInstanceOf(NtaFeedException.class);
	}

	@Test
	void throwsWhenTheBodyIsEmpty() {
		server.expect(requestTo("https://nta.test/gtfsr/v2/Vehicles"))
				.andRespond(withSuccess(new byte[0], MediaType.APPLICATION_OCTET_STREAM));

		assertThatThrownBy(client::fetchVehiclePositions).isInstanceOf(NtaFeedException.class);
	}
}
