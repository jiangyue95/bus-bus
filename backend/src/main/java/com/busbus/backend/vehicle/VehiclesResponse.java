package com.busbus.backend.vehicle;

import java.time.Instant;
import java.util.List;

/**
 * Body of {@code GET /api/v1/vehicles}. Both timestamps are null until the first successful poll.
 *
 * @param feedTimestamp when NTA generated the data
 * @param fetchedAt     when this server last fetched it successfully
 */
public record VehiclesResponse(Instant feedTimestamp, Instant fetchedAt, int count, List<Vehicle> vehicles) {

	static VehiclesResponse from(VehicleSnapshot snapshot) {
		return new VehiclesResponse(snapshot.feedTimestamp(), snapshot.fetchedAt(), snapshot.vehicles().size(),
				snapshot.vehicles());
	}
}
