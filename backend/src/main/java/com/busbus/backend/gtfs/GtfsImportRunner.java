package com.busbus.backend.gtfs;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import com.busbus.backend.gtfs.GtfsProperties.ImportMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/** Decides at startup whether to import GTFS Static, based on {@code gtfs.import-mode}. */
@Component
public class GtfsImportRunner implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(GtfsImportRunner.class);

	private final GtfsProperties properties;
	private final GtfsStaticDownloader downloader;
	private final GtfsStaticImporter importer;

	public GtfsImportRunner(GtfsProperties properties, GtfsStaticDownloader downloader, GtfsStaticImporter importer) {
		this.properties = properties;
		this.downloader = downloader;
		this.importer = importer;
	}

	@Override
	public void run(ApplicationArguments args) {
		if (!shouldImport()) {
			return;
		}
		Path zip = null;
		try {
			long start = System.nanoTime();
			zip = Files.createTempFile("gtfs-static-", ".zip");
			log.info("Downloading GTFS Static from {}", properties.staticUrl());
			downloader.download(properties.staticUrl(), zip);
			GtfsImportResult result = importer.importFrom(zip);
			log.info("Imported GTFS Static in {} s: {}", Duration.ofNanos(System.nanoTime() - start).toSeconds(), result);
		}
		catch (IOException | RuntimeException ex) {
			// Do not stop the application: live vehicles still work without the static data, and a
			// failed import rolls back, so any data from an earlier import is still there.
			log.error("GTFS Static import failed", ex);
		}
		finally {
			deleteQuietly(zip);
		}
	}

	private boolean shouldImport() {
		ImportMode mode = properties.importMode();
		if (mode == ImportMode.NEVER) {
			return false;
		}
		if (mode == ImportMode.IF_EMPTY && importer.hasData()) {
			log.info("GTFS Static tables already have data, so the import is skipped");
			return false;
		}
		return true;
	}

	private static void deleteQuietly(Path file) {
		if (file == null) {
			return;
		}
		try {
			Files.deleteIfExists(file);
		}
		catch (IOException ex) {
			log.warn("Could not delete temporary file {}", file);
		}
	}
}
