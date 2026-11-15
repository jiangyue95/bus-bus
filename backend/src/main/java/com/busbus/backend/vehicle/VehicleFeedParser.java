package com.busbus.backend.vehicle;

import java.time.Instant;
import java.util.List;

import com.google.transit.realtime.GtfsRealtime.FeedEntity;
import com.google.transit.realtime.GtfsRealtime.FeedMessage;
import com.google.transit.realtime.GtfsRealtime.Position;
import com.google.transit.realtime.GtfsRealtime.TripDescriptor;
import com.google.transit.realtime.GtfsRealtime.VehiclePosition;

import org.springframework.stereotype.Component;

/** Converts a GTFS-Realtime feed into our own {@link VehicleSnapshot}. No I/O, so it is easy to test. */
@Component
public class VehicleFeedParser {

	public VehicleSnapshot parse(FeedMessage feed, Instant fetchedAt) {
		List<Vehicle> vehicles = feed.getEntityList().stream()
				.filter(entity -> entity.hasVehicle() && hasUsablePosition(entity.getVehicle()))
				.map(this::toVehicle)
				.toList();
		Instant feedTimestamp = feed.getHeader().hasTimestamp()
				? Instant.ofEpochSecond(feed.getHeader().getTimestamp())
				: null;
		return new VehicleSnapshot(vehicles, feedTimestamp, fetchedAt);
	}

	/**
	 * The live NTA feed sometimes reports a vehicle at latitude 0, longitude 0 when it has no GPS
	 * fix. That point is in the Atlantic Ocean, so such a vehicle cannot be shown on the map.
	 */
	private static boolean hasUsablePosition(VehiclePosition vehiclePosition) {
		if (!vehiclePosition.hasPosition()) {
			return false;
		}
		Position position = vehiclePosition.getPosition();
		return position.getLatitude() != 0f || position.getLongitude() != 0f;
	}

	private Vehicle toVehicle(FeedEntity entity) {
		VehiclePosition vehiclePosition = entity.getVehicle();
		Position position = vehiclePosition.getPosition();
		TripDescriptor trip = vehiclePosition.getTrip();

		// The vehicle id is optional in the spec; the entity id is always present.
		String id = vehiclePosition.getVehicle().hasId() ? vehiclePosition.getVehicle().getId() : entity.getId();

		return new Vehicle(
				id,
				trip.hasTripId() ? trip.getTripId() : null,
				trip.hasRouteId() ? trip.getRouteId() : null,
				trip.hasDirectionId() ? trip.getDirectionId() : null,
				roundCoordinate(position.getLatitude()),
				roundCoordinate(position.getLongitude()),
				position.hasBearing() ? position.getBearing() : null,
				vehiclePosition.hasTimestamp() ? Instant.ofEpochSecond(vehiclePosition.getTimestamp()) : null);
	}

	/**
	 * The feed sends coordinates as 32-bit floats. Widening one to a double adds meaningless digits
	 * (53.35 becomes 53.349998474121094), so round to 5 decimal places. That is about 1 m, which is
	 * all the precision a float holds at Dublin's latitude.
	 */
	private static double roundCoordinate(float value) {
		return Math.round(value * 100_000d) / 100_000d;
	}
}
