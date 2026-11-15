package com.busbus.backend.gtfs;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Uses the JDK's built-in HTTP server on a random local port, so no real network is involved. */
class GtfsStaticDownloaderTest {

	private final GtfsStaticDownloader downloader = new GtfsStaticDownloader();

	private HttpServer server;

	@TempDir
	private Path tempDir;

	@BeforeEach
	void startServer() throws IOException {
		server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
		server.createContext("/feed.zip", exchange -> {
			byte[] body = "zip-bytes".getBytes(StandardCharsets.UTF_8);
			exchange.sendResponseHeaders(200, body.length);
			exchange.getResponseBody().write(body);
			exchange.close();
		});
		server.createContext("/missing.zip", exchange -> {
			exchange.sendResponseHeaders(404, -1);
			exchange.close();
		});
		server.start();
	}

	@AfterEach
	void stopServer() {
		server.stop(0);
	}

	@Test
	void writesTheResponseBodyToTheTargetFile() throws IOException {
		Path target = tempDir.resolve("feed.zip");

		downloader.download(url("/feed.zip"), target);

		assertThat(Files.readString(target)).isEqualTo("zip-bytes");
	}

	@Test
	void failsWhenTheServerDoesNotAnswer200() {
		Path target = tempDir.resolve("feed.zip");

		assertThatThrownBy(() -> downloader.download(url("/missing.zip"), target))
				.isInstanceOf(GtfsImportException.class)
				.hasMessageContaining("404");
		assertThat(Files.exists(target)).isFalse();
	}

	private URI url(String path) {
		return URI.create("http://localhost:" + server.getAddress().getPort() + path);
	}
}
