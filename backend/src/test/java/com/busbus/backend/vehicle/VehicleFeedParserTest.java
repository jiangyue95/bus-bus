package com.busbus.backend.vehicle;

import java.time.Instant;

import com.google.transit.realtime.GtfsRealtime.FeedHeader;
import com.google.transit.realtime.GtfsRealtime.FeedMessage;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class VehicleFeedParserTest {

	private static final Instant FETCHED_AT = Instant.parse("2026-11-15T10:00:05Z");

	private final VehicleFeedParser parser = new VehicleFeedParser();

	@Test
	void mapsEveryFieldOfAFullVehicle() {
		VehicleSnapshot snapshot = parser.parse(FeedFixtures.feedOf(FeedFixtures.fullVehicle()), FETCHED_AT);

		assertThat(snapshot.vehicles()).containsExactly(new Vehicle("bus-42", "trip-1", "route-46A", 1, 53.3498,
				-6.2603, 90f, Instant.ofEpochSecond(FeedFixtures.VEHICLE_TIMESTAMP)));
	}

	@Test
	void usesTheEntityIdAndNullsWhenOptionalFieldsAreMissing() {
		VehicleSnapshot snapshot = parser.parse(FeedFixtures.feedOf(FeedFixtures.minimalVehicle()), FETCHED_AT);

		assertThat(snapshot.vehicles()).containsExactly(new Vehicle("entity-2", null, null, null, 53.35, -6.26,
				null, null));
	}

	@Test
	void skipsVehiclesWithoutAPosition() {
		VehicleSnapshot snapshot = parser.parse(
				FeedFixtures.feedOf(FeedFixtures.vehicleWithoutPosition(), FeedFixtures.fullVehicle()), FETCHED_AT);

		assertThat(snapshot.vehicles()).extracting(Vehicle::id).containsExactly("bus-42");
	}

	@Test
	void skipsVehiclesReportedAtZeroZero() {
		VehicleSnapshot snapshot = parser.parse(
				FeedFixtures.feedOf(FeedFixtures.vehicleAtZeroZero(), FeedFixtures.fullVehicle()), FETCHED_AT);

		assertThat(snapshot.vehicles()).extracting(Vehicle::id).containsExactly("bus-42");
	}

	@Test
	void keepsTheFeedTimestampAndTheFetchTime() {
		VehicleSnapshot snapshot = parser.parse(FeedFixtures.feedOf(), FETCHED_AT);

		assertThat(snapshot.vehicles()).isEmpty();
		assertThat(snapshot.feedTimestamp()).isEqualTo(Instant.ofEpochSecond(FeedFixtures.FEED_TIMESTAMP));
		assertThat(snapshot.fetchedAt()).isEqualTo(FETCHED_AT);
	}

	@Test
	void feedTimestampIsNullWhenTheHeaderHasNone() {
		FeedMessage feed = FeedMessage.newBuilder()
				.setHeader(FeedHeader.newBuilder().setGtfsRealtimeVersion("2.0"))
				.build();

		assertThat(parser.parse(feed, FETCHED_AT).feedTimestamp()).isNull();
	}
}
