package com.busbus.backend.gtfs;

import java.net.URI;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Settings for the GTFS Static import, bound from the {@code gtfs.*} properties. */
@ConfigurationProperties("gtfs")
public record GtfsProperties(URI staticUrl, ImportMode importMode) {

	public enum ImportMode {

		/** Import at startup only when the tables are empty. */
		IF_EMPTY,

		/** Re-import at every startup. */
		ALWAYS,

		/** Never import. */
		NEVER
	}
}
