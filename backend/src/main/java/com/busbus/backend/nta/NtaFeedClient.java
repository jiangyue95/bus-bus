package com.busbus.backend.nta;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.transit.realtime.GtfsRealtime.FeedMessage;

import org.springframework.web.client.RestClient;

/** Calls the NTA GTFS-Realtime API and returns the decoded protobuf feed. */
public class NtaFeedClient {

	private static final String API_KEY_HEADER = "x-api-key";

	private final RestClient restClient;

	public NtaFeedClient(RestClient.Builder builder, NtaProperties properties) {
		this.restClient = builder
				.baseUrl(properties.baseUrl())
				.defaultHeader(API_KEY_HEADER, properties.apiKey())
				.build();
	}

	/**
	 * Fetches the current vehicle positions.
	 *
	 * @throws org.springframework.web.client.RestClientException if the call fails or returns an error status
	 * @throws NtaFeedException if the response body is not a valid GTFS-Realtime feed
	 */
	public FeedMessage fetchVehiclePositions() {
		byte[] body = restClient.get().uri("/Vehicles").retrieve().body(byte[].class);
		if (body == null || body.length == 0) {
			throw new NtaFeedException("NTA returned an empty vehicle feed", null);
		}
		try {
			return FeedMessage.parseFrom(body);
		}
		catch (InvalidProtocolBufferException ex) {
			throw new NtaFeedException("NTA vehicle feed is not valid protobuf", ex);
		}
	}
}
