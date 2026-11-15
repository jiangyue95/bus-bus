package com.busbus.backend.vehicle;

import com.google.transit.realtime.GtfsRealtime.FeedEntity;
import com.google.transit.realtime.GtfsRealtime.FeedHeader;
import com.google.transit.realtime.GtfsRealtime.FeedMessage;
import com.google.transit.realtime.GtfsRealtime.Position;
import com.google.transit.realtime.GtfsRealtime.TripDescriptor;
import com.google.transit.realtime.GtfsRealtime.VehicleDescriptor;
import com.google.transit.realtime.GtfsRealtime.VehiclePosition;

/** Builds small GTFS-Realtime feeds for tests. */
public final class FeedFixtures {

	public static final long FEED_TIMESTAMP = 1_794_738_000L;
	public static final long VEHICLE_TIMESTAMP = 1_794_737_990L;

	private FeedFixtures() {
	}

	public static FeedMessage feedOf(FeedEntity... entities) {
		FeedMessage.Builder feed = FeedMessage.newBuilder()
				.setHeader(FeedHeader.newBuilder().setGtfsRealtimeVersion("2.0").setTimestamp(FEED_TIMESTAMP));
		for (FeedEntity entity : entities) {
			feed.addEntity(entity);
		}
		return feed.build();
	}

	/** A vehicle with every field we read. */
	public static FeedEntity fullVehicle() {
		return FeedEntity.newBuilder()
				.setId("entity-1")
				.setVehicle(VehiclePosition.newBuilder()
						.setVehicle(VehicleDescriptor.newBuilder().setId("bus-42"))
						.setTrip(TripDescriptor.newBuilder().setTripId("trip-1").setRouteId("route-46A").setDirectionId(1))
						.setPosition(Position.newBuilder().setLatitude(53.3498f).setLongitude(-6.2603f).setBearing(90f))
						.setTimestamp(VEHICLE_TIMESTAMP))
				.build();
	}

	/** A vehicle with only the position: no vehicle id, trip, bearing or timestamp. */
	public static FeedEntity minimalVehicle() {
		return FeedEntity.newBuilder()
				.setId("entity-2")
				.setVehicle(VehiclePosition.newBuilder()
						.setPosition(Position.newBuilder().setLatitude(53.35f).setLongitude(-6.26f)))
				.build();
	}

	/** A vehicle without a GPS fix, as seen in the live NTA feed: position present but 0, 0. */
	public static FeedEntity vehicleAtZeroZero() {
		return FeedEntity.newBuilder()
				.setId("entity-4")
				.setVehicle(VehiclePosition.newBuilder()
						.setVehicle(VehicleDescriptor.newBuilder().setId("bus-0"))
						.setPosition(Position.newBuilder().setLatitude(0f).setLongitude(0f)))
				.build();
	}

	/** A vehicle entity that has no position, so it cannot be shown on a map. */
	public static FeedEntity vehicleWithoutPosition() {
		return FeedEntity.newBuilder()
				.setId("entity-3")
				.setVehicle(VehiclePosition.newBuilder().setVehicle(VehicleDescriptor.newBuilder().setId("bus-99")))
				.build();
	}
}
