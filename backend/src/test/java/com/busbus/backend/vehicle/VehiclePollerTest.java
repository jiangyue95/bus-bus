package com.busbus.backend.vehicle;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import com.busbus.backend.nta.NtaFeedClient;
import com.busbus.backend.nta.NtaProperties;
import org.junit.jupiter.api.Test;

import org.springframework.web.client.ResourceAccessException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class VehiclePollerTest {

	private static final Instant NOW = Instant.parse("2026-11-15T10:00:05Z");

	private final NtaFeedClient feedClient = mock(NtaFeedClient.class);
	private final VehicleSnapshotStore store = new VehicleSnapshotStore();

	@Test
	void storesTheSnapshotAfterASuccessfulPoll() {
		when(feedClient.fetchVehiclePositions()).thenReturn(FeedFixtures.feedOf(FeedFixtures.fullVehicle()));

		pollerWithApiKey("test-key").poll();

		assertThat(store.current().vehicles()).extracting(Vehicle::id).containsExactly("bus-42");
		assertThat(store.current().fetchedAt()).isEqualTo(NOW);
	}

	@Test
	void keepsThePreviousSnapshotWhenAPollFails() {
		when(feedClient.fetchVehiclePositions())
				.thenReturn(FeedFixtures.feedOf(FeedFixtures.fullVehicle()))
				.thenThrow(new ResourceAccessException("timeout"));
		VehiclePoller poller = pollerWithApiKey("test-key");

		poller.poll();
		VehicleSnapshot afterFirstPoll = store.current();
		poller.poll();

		assertThat(store.current()).isSameAs(afterFirstPoll);
	}

	@Test
	void doesNotCallNtaWithoutAnApiKey() {
		pollerWithApiKey("").poll();

		verifyNoInteractions(feedClient);
		assertThat(store.current()).isSameAs(VehicleSnapshot.EMPTY);
	}

	private VehiclePoller pollerWithApiKey(String apiKey) {
		NtaProperties properties = new NtaProperties("https://nta.test", apiKey, Duration.ofSeconds(60));
		return new VehiclePoller(feedClient, new VehicleFeedParser(), store, properties,
				Clock.fixed(NOW, ZoneOffset.UTC));
	}
}
