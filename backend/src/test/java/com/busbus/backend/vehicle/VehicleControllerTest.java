package com.busbus.backend.vehicle;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(VehicleController.class)
@Import(VehicleSnapshotStore.class)
class VehicleControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private VehicleSnapshotStore store;

	@Test
	void returnsAnEmptyListBeforeTheFirstPoll() throws Exception {
		store.replace(VehicleSnapshot.EMPTY);

		mockMvc.perform(get("/api/v1/vehicles"))
				.andExpect(status().isOk())
				.andExpect(content().json("""
						{"feedTimestamp": null, "fetchedAt": null, "count": 0, "vehicles": []}
						""", JsonCompareMode.STRICT));
	}

	@Test
	void returnsTheCurrentSnapshotWithIsoTimestamps() throws Exception {
		Vehicle vehicle = new Vehicle("bus-42", "trip-1", "route-46A", 1, 53.3498, -6.2603, 90f,
				Instant.parse("2026-11-15T09:59:50Z"));
		store.replace(new VehicleSnapshot(List.of(vehicle), Instant.parse("2026-11-15T10:00:00Z"),
				Instant.parse("2026-11-15T10:00:05Z")));

		mockMvc.perform(get("/api/v1/vehicles"))
				.andExpect(status().isOk())
				.andExpect(content().json("""
						{
						  "feedTimestamp": "2026-11-15T10:00:00Z",
						  "fetchedAt": "2026-11-15T10:00:05Z",
						  "count": 1,
						  "vehicles": [{
						    "id": "bus-42",
						    "tripId": "trip-1",
						    "routeId": "route-46A",
						    "directionId": 1,
						    "latitude": 53.3498,
						    "longitude": -6.2603,
						    "bearing": 90.0,
						    "recordedAt": "2026-11-15T09:59:50Z"
						  }]
						}
						""", JsonCompareMode.STRICT));
	}
}
