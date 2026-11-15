package com.busbus.backend.nta;

/** The NTA API answered, but the body could not be used as a GTFS-Realtime feed. */
public class NtaFeedException extends RuntimeException {

	public NtaFeedException(String message, Throwable cause) {
		super(message, cause);
	}
}
