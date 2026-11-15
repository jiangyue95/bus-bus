package com.busbus.backend.gtfs;

/** Number of rows imported into each table. */
public record GtfsImportResult(int agencies, int routes, int trips, int stops) {
}
