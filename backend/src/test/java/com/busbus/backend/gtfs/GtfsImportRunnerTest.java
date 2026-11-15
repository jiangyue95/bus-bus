package com.busbus.backend.gtfs;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;

import com.busbus.backend.gtfs.GtfsProperties.ImportMode;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class GtfsImportRunnerTest {

	private static final URI SOURCE = URI.create("https://gtfs.test/feed.zip");

	private final GtfsStaticDownloader downloader = mock(GtfsStaticDownloader.class);
	private final GtfsStaticImporter importer = mock(GtfsStaticImporter.class);

	@Test
	void neverModeDoesNothing() {
		runner(ImportMode.NEVER).run(null);

		verifyNoInteractions(downloader, importer);
	}

	@Test
	void ifEmptyModeSkipsTheImportWhenThereIsData() {
		when(importer.hasData()).thenReturn(true);

		runner(ImportMode.IF_EMPTY).run(null);

		verifyNoInteractions(downloader);
		verify(importer, never()).importFrom(any());
	}

	@Test
	void ifEmptyModeImportsWhenTheTablesAreEmpty() {
		when(importer.hasData()).thenReturn(false);

		runner(ImportMode.IF_EMPTY).run(null);

		verify(downloader).download(eq(SOURCE), any());
		verify(importer).importFrom(any());
	}

	@Test
	void alwaysModeImportsEvenWhenThereIsData() {
		when(importer.hasData()).thenReturn(true);

		runner(ImportMode.ALWAYS).run(null);

		verify(importer).importFrom(any());
	}

	@Test
	void importsTheDownloadedFileAndDeletesItAfterwards() {
		runner(ImportMode.ALWAYS).run(null);

		ArgumentCaptor<Path> downloaded = ArgumentCaptor.forClass(Path.class);
		ArgumentCaptor<Path> imported = ArgumentCaptor.forClass(Path.class);
		verify(downloader).download(eq(SOURCE), downloaded.capture());
		verify(importer).importFrom(imported.capture());
		assertThat(imported.getValue()).isEqualTo(downloaded.getValue());
		assertThat(Files.exists(downloaded.getValue())).isFalse();
	}

	@Test
	void aFailedDownloadDoesNotStopTheApplication() {
		doThrow(new GtfsImportException("HTTP 503")).when(downloader).download(any(), any());

		assertThatCode(() -> runner(ImportMode.ALWAYS).run(null)).doesNotThrowAnyException();

		verify(importer, never()).importFrom(any());
	}

	private GtfsImportRunner runner(ImportMode mode) {
		return new GtfsImportRunner(new GtfsProperties(SOURCE, mode), downloader, importer);
	}
}
