package com.busbus.backend.gtfs;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.input.BOMInputStream;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Replaces the GTFS Static tables with the content of a GTFS zip file.
 *
 * <p>Everything runs in one transaction: either all four tables hold the new data, or the import
 * rolls back and the old data stays.
 */
@Component
public class GtfsStaticImporter {

	/** Rows sent to the database per round trip. */
	private static final int BATCH_SIZE = 5_000;

	private static final CSVFormat CSV_FORMAT = CSVFormat.DEFAULT.builder()
			.setHeader()
			.setSkipHeaderRecord(true)
			.setIgnoreSurroundingSpaces(true)
			.get();

	private final JdbcTemplate jdbcTemplate;
	private final TransactionTemplate transactionTemplate;

	public GtfsStaticImporter(JdbcTemplate jdbcTemplate, TransactionTemplate transactionTemplate) {
		this.jdbcTemplate = jdbcTemplate;
		this.transactionTemplate = transactionTemplate;
	}

	public boolean hasData() {
		return Boolean.TRUE.equals(jdbcTemplate.queryForObject("SELECT EXISTS (SELECT 1 FROM routes)", Boolean.class));
	}

	/**
	 * @throws GtfsImportException if the zip cannot be read or a required file, column or value is missing
	 * @throws org.springframework.dao.DataAccessException if the database rejects the data
	 */
	public GtfsImportResult importFrom(Path zipPath) {
		try (ZipFile zip = new ZipFile(zipPath.toFile())) {
			return transactionTemplate.execute(status -> {
				// One statement, so the foreign keys between these tables do not block the truncate.
				jdbcTemplate.execute("TRUNCATE TABLE trips, routes, agency, stops");

				// Parents before children, because of the foreign keys.
				int agencies = load(zip, "agency.txt",
						"INSERT INTO agency (agency_id, agency_name, agency_url, agency_timezone) VALUES (?, ?, ?, ?)",
						row -> new Object[] { row.required("agency_id"), row.required("agency_name"),
								row.optional("agency_url"), row.optional("agency_timezone") });
				int routes = load(zip, "routes.txt",
						"INSERT INTO routes (route_id, agency_id, route_short_name, route_long_name, route_type)"
								+ " VALUES (?, ?, ?, ?, ?)",
						row -> new Object[] { row.required("route_id"), row.required("agency_id"),
								row.optional("route_short_name"), row.optional("route_long_name"),
								Integer.valueOf(row.required("route_type")) });
				int trips = load(zip, "trips.txt",
						"INSERT INTO trips (trip_id, route_id, service_id, trip_headsign, direction_id)"
								+ " VALUES (?, ?, ?, ?, ?)",
						row -> new Object[] { row.required("trip_id"), row.required("route_id"),
								row.required("service_id"), row.optional("trip_headsign"),
								row.optionalInteger("direction_id") });
				int stops = load(zip, "stops.txt",
						"INSERT INTO stops (stop_id, stop_code, stop_name, stop_lat, stop_lon) VALUES (?, ?, ?, ?, ?)",
						row -> new Object[] { row.required("stop_id"), row.optional("stop_code"),
								row.required("stop_name"), Double.valueOf(row.required("stop_lat")),
								Double.valueOf(row.required("stop_lon")) });
				return new GtfsImportResult(agencies, routes, trips, stops);
			});
		}
		catch (IOException ex) {
			throw new GtfsImportException("Cannot read GTFS zip " + zipPath, ex);
		}
	}

	/** Streams one CSV file from the zip into the database in batches, so memory use stays flat. */
	private int load(ZipFile zip, String fileName, String insertSql, Function<Row, Object[]> toParameters) {
		ZipEntry entry = zip.getEntry(fileName);
		if (entry == null) {
			throw new GtfsImportException("GTFS zip has no " + fileName);
		}
		// GTFS files are UTF-8 and may start with a byte order mark, which would otherwise become
		// part of the first column name.
		try (Reader reader = new InputStreamReader(
				BOMInputStream.builder().setInputStream(zip.getInputStream(entry)).get(), StandardCharsets.UTF_8);
				CSVParser parser = CSV_FORMAT.parse(reader)) {
			int count = 0;
			List<Object[]> batch = new ArrayList<>(BATCH_SIZE);
			for (CSVRecord record : parser) {
				batch.add(toParameters.apply(new Row(fileName, record)));
				if (batch.size() == BATCH_SIZE) {
					jdbcTemplate.batchUpdate(insertSql, batch);
					count += batch.size();
					batch.clear();
				}
			}
			if (!batch.isEmpty()) {
				jdbcTemplate.batchUpdate(insertSql, batch);
				count += batch.size();
			}
			return count;
		}
		catch (IOException ex) {
			throw new GtfsImportException("Cannot read " + fileName + " from the GTFS zip", ex);
		}
		catch (NumberFormatException ex) {
			throw new GtfsImportException(fileName + " has a value that is not a number: " + ex.getMessage(), ex);
		}
	}

	/** One CSV line, with the GTFS rule that an empty value means "not given". */
	private record Row(String fileName, CSVRecord record) {

		String optional(String column) {
			if (!record.isMapped(column) || !record.isSet(column)) {
				return null;
			}
			String value = record.get(column);
			return value.isEmpty() ? null : value;
		}

		String required(String column) {
			String value = optional(column);
			if (value == null) {
				throw new GtfsImportException(
						fileName + " line " + record.getRecordNumber() + " has no value for " + column);
			}
			return value;
		}

		Integer optionalInteger(String column) {
			String value = optional(column);
			return value == null ? null : Integer.valueOf(value);
		}
	}
}
