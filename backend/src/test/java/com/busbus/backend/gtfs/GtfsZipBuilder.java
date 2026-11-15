package com.busbus.backend.gtfs;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/** Builds a small GTFS zip for tests. Starts from a valid feed; tests replace or remove files. */
final class GtfsZipBuilder {

	private final Map<String, String> files = new LinkedHashMap<>();

	private GtfsZipBuilder() {
	}

	/** Two agencies, two routes, three trips and two stops. */
	static GtfsZipBuilder validFeed() {
		GtfsZipBuilder builder = new GtfsZipBuilder();
		// The byte order mark and the extra agency_lang column are both things real feeds contain.
		builder.files.put("agency.txt", """
				﻿agency_id,agency_name,agency_url,agency_timezone,agency_lang
				7778019,Dublin Bus,https://www.dublinbus.ie,Europe/Dublin,en
				7778020,Go-Ahead Ireland,,Europe/Dublin,en
				""");
		builder.files.put("routes.txt", """
				route_id,agency_id,route_short_name,route_long_name,route_type
				r-46A,7778019,46A,"Phoenix Park - Dún Laoghaire, via City Centre",3
				r-175,7778020,175,,3
				""");
		builder.files.put("trips.txt", """
				route_id,service_id,trip_id,trip_headsign,direction_id
				r-46A,weekday,t-1,Dún Laoghaire,0
				r-46A,weekday,t-2,Phoenix Park,1
				r-175,weekday,t-3,,
				""");
		builder.files.put("stops.txt", """
				stop_id,stop_code,stop_name,stop_lat,stop_lon
				s-1,2039,"O'Connell Street Upper, stop 2039",53.3521,-6.2607
				s-2,,Heuston Station,53.3464,-6.2921
				""");
		return builder;
	}

	GtfsZipBuilder with(String fileName, String content) {
		files.put(fileName, content);
		return this;
	}

	GtfsZipBuilder without(String fileName) {
		files.remove(fileName);
		return this;
	}

	Path writeTo(Path directory) throws IOException {
		Path zip = Files.createTempFile(directory, "gtfs-", ".zip");
		try (OutputStream out = Files.newOutputStream(zip); ZipOutputStream zipOut = new ZipOutputStream(out)) {
			for (Map.Entry<String, String> file : files.entrySet()) {
				zipOut.putNextEntry(new ZipEntry(file.getKey()));
				zipOut.write(file.getValue().getBytes(StandardCharsets.UTF_8));
				zipOut.closeEntry();
			}
		}
		return zip;
	}
}
