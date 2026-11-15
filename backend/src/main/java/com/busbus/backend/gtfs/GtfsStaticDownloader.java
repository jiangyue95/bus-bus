package com.busbus.backend.gtfs;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;

import org.springframework.stereotype.Component;

/** Downloads the GTFS Static zip to a file. */
@Component
public class GtfsStaticDownloader {

	private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
	private static final Duration REQUEST_TIMEOUT = Duration.ofMinutes(5);

	private final HttpClient httpClient = HttpClient.newBuilder()
			.connectTimeout(CONNECT_TIMEOUT)
			.followRedirects(HttpClient.Redirect.NORMAL)
			.build();

	/**
	 * Streams the response body straight to {@code target}. The file is about 100 MB, so it is never
	 * held in memory.
	 *
	 * @throws GtfsImportException if the download fails or the server does not answer 200
	 */
	public void download(URI source, Path target) {
		HttpRequest request = HttpRequest.newBuilder(source).timeout(REQUEST_TIMEOUT).GET().build();
		try {
			HttpResponse<java.io.InputStream> response = httpClient.send(request,
					HttpResponse.BodyHandlers.ofInputStream());
			try (var body = response.body()) {
				if (response.statusCode() != 200) {
					throw new GtfsImportException(
							"GTFS Static download from " + source + " returned HTTP " + response.statusCode());
				}
				Files.copy(body, target, StandardCopyOption.REPLACE_EXISTING);
			}
		}
		catch (IOException ex) {
			throw new GtfsImportException("GTFS Static download from " + source + " failed", ex);
		}
		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			throw new GtfsImportException("GTFS Static download from " + source + " was interrupted", ex);
		}
	}
}
