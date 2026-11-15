package com.busbus.backend.vehicle;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/vehicles")
public class VehicleController {

	private final VehicleSnapshotStore store;

	public VehicleController(VehicleSnapshotStore store) {
		this.store = store;
	}

	/** Reads from memory only; a request never triggers a call to NTA. */
	@GetMapping
	public VehiclesResponse vehicles() {
		return VehiclesResponse.from(store.current());
	}
}
