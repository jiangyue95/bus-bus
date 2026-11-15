package com.busbus.backend.nta;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Settings for the NTA GTFS-Realtime API, bound from the {@code nta.*} properties. */
@ConfigurationProperties("nta")
public record NtaProperties(String baseUrl, String apiKey, Duration pollInterval) {

	public boolean hasApiKey() {
		return apiKey != null && !apiKey.isBlank();
	}
}
