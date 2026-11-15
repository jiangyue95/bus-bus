package com.busbus.backend.vehicle;

import java.time.Instant;

/**
 * One bus at one moment. This is our own model, so the API contract does not depend on the
 * GTFS-Realtime classes.
 *
 * @param tripId      GTFS trip id, or null when the vehicle is not assigned to a trip
 * @param routeId     GTFS route id, or null when the feed does not give one
 * @param directionId 0 or 1, or null when the feed does not give one
 * @param bearing     degrees clockwise from north, or null when the feed does not give one
 * @param recordedAt  when the vehicle reported this position, or null when the feed does not say
 */
public record Vehicle(
		String id,
		String tripId,
		String routeId,
		Integer directionId,
		double latitude,
		double longitude,
		Float bearing,
		Instant recordedAt) {
}
