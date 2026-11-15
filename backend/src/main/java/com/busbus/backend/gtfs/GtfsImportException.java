package com.busbus.backend.gtfs;

/** The GTFS Static file could not be downloaded or does not have the content we need. */
public class GtfsImportException extends RuntimeException {

	public GtfsImportException(String message) {
		super(message);
	}

	public GtfsImportException(String message, Throwable cause) {
		super(message, cause);
	}
}
