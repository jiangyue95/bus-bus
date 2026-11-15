package com.busbus.backend.vehicle;

import java.time.Instant;
import java.util.List;

/**
 * All vehicles from one successful poll. Immutable, so readers always see a complete poll.
 *
 * @param feedTimestamp when NTA generated the feed, or null before the first successful poll
 * @param fetchedAt     when we fetched it, or null before the first successful poll
 */
public record VehicleSnapshot(List<Vehicle> vehicles, Instant feedTimestamp, Instant fetchedAt) {

	public static final VehicleSnapshot EMPTY = new VehicleSnapshot(List.of(), null, null);

	public VehicleSnapshot {
		vehicles = List.copyOf(vehicles);
	}
}
