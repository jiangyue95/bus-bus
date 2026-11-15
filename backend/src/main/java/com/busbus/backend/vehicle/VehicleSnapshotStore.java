package com.busbus.backend.vehicle;

import java.util.concurrent.atomic.AtomicReference;

import org.springframework.stereotype.Component;

/**
 * Holds the latest snapshot in memory. The polling thread replaces the whole snapshot in one
 * atomic step, so request threads never see a half-updated list.
 */
@Component
public class VehicleSnapshotStore {

	private final AtomicReference<VehicleSnapshot> current = new AtomicReference<>(VehicleSnapshot.EMPTY);

	public VehicleSnapshot current() {
		return current.get();
	}

	public void replace(VehicleSnapshot snapshot) {
		current.set(snapshot);
	}
}
