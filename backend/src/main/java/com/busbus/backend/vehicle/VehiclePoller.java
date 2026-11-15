package com.busbus.backend.vehicle;

import java.time.Clock;

import com.busbus.backend.nta.NtaFeedClient;
import com.busbus.backend.nta.NtaProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Polls the NTA feed on a fixed delay and replaces the in-memory snapshot. */
@Component
public class VehiclePoller {

	private static final Logger log = LoggerFactory.getLogger(VehiclePoller.class);

	private final NtaFeedClient feedClient;
	private final VehicleFeedParser parser;
	private final VehicleSnapshotStore store;
	private final NtaProperties properties;
	private final Clock clock;

	public VehiclePoller(NtaFeedClient feedClient, VehicleFeedParser parser, VehicleSnapshotStore store,
			NtaProperties properties, Clock clock) {
		this.feedClient = feedClient;
		this.parser = parser;
		this.store = store;
		this.properties = properties;
		this.clock = clock;
		if (!properties.hasApiKey()) {
			log.warn("NTA_API_KEY is not set, so vehicle polling is disabled");
		}
	}

	/**
	 * fixedDelay (not fixedRate) waits for the previous poll to finish, so slow responses never
	 * cause overlapping calls.
	 */
	@Scheduled(fixedDelayString = "${nta.poll-interval}")
	public void poll() {
		if (!properties.hasApiKey()) {
			return;
		}
		try {
			VehicleSnapshot snapshot = parser.parse(feedClient.fetchVehiclePositions(), clock.instant());
			store.replace(snapshot);
			log.debug("Polled {} vehicles", snapshot.vehicles().size());
		}
		catch (RuntimeException ex) {
			// Keep the previous snapshot: slightly old data is more useful than none, and the
			// response timestamps let clients see how old it is.
			log.warn("Vehicle poll failed, keeping the previous snapshot: {}", ex.toString());
		}
	}
}
