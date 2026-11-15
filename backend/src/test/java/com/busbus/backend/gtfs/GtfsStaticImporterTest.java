package com.busbus.backend.gtfs;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Runs against the real busbus_test database, because the SQL and the constraints are what we test. */
@SpringBootTest
class GtfsStaticImporterTest {

	@Autowired
	private GtfsStaticImporter importer;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@TempDir
	private Path tempDir;

	@BeforeEach
	void emptyTables() {
		jdbcTemplate.execute("TRUNCATE TABLE trips, routes, agency, stops");
	}

	@Test
	void importsAllFourFiles() throws IOException {
		GtfsImportResult result = importer.importFrom(GtfsZipBuilder.validFeed().writeTo(tempDir));

		assertThat(result).isEqualTo(new GtfsImportResult(2, 2, 3, 2));
		assertThat(count("agency")).isEqualTo(2);
		assertThat(count("routes")).isEqualTo(2);
		assertThat(count("trips")).isEqualTo(3);
		assertThat(count("stops")).isEqualTo(2);
	}

	@Test
	void readsTheFirstColumnDespiteTheByteOrderMark() throws IOException {
		importer.importFrom(GtfsZipBuilder.validFeed().writeTo(tempDir));

		assertThat(jdbcTemplate.queryForObject("SELECT agency_name FROM agency WHERE agency_id = '7778019'",
				String.class)).isEqualTo("Dublin Bus");
	}

	@Test
	void keepsCommasInsideQuotedValues() throws IOException {
		importer.importFrom(GtfsZipBuilder.validFeed().writeTo(tempDir));

		assertThat(jdbcTemplate.queryForObject("SELECT route_long_name FROM routes WHERE route_id = 'r-46A'",
				String.class)).isEqualTo("Phoenix Park - Dún Laoghaire, via City Centre");
	}

	@Test
	void storesEmptyOptionalValuesAsNull() throws IOException {
		importer.importFrom(GtfsZipBuilder.validFeed().writeTo(tempDir));

		Map<String, Object> trip = jdbcTemplate.queryForMap("SELECT trip_headsign, direction_id FROM trips WHERE trip_id = 't-3'");
		assertThat(trip.get("trip_headsign")).isNull();
		assertThat(trip.get("direction_id")).isNull();
		assertThat(jdbcTemplate.queryForObject("SELECT stop_code FROM stops WHERE stop_id = 's-2'", String.class))
				.isNull();
	}

	@Test
	void storesCoordinatesAndNumbers() throws IOException {
		importer.importFrom(GtfsZipBuilder.validFeed().writeTo(tempDir));

		Map<String, Object> stop = jdbcTemplate.queryForMap("SELECT stop_lat, stop_lon FROM stops WHERE stop_id = 's-1'");
		assertThat(stop).containsEntry("stop_lat", 53.3521).containsEntry("stop_lon", -6.2607);
		assertThat(jdbcTemplate.queryForObject("SELECT direction_id FROM trips WHERE trip_id = 't-2'", Integer.class))
				.isEqualTo(1);
	}

	@Test
	void aSecondImportReplacesTheFirst() throws IOException {
		importer.importFrom(GtfsZipBuilder.validFeed().writeTo(tempDir));

		importer.importFrom(GtfsZipBuilder.validFeed()
				.with("stops.txt", """
						stop_id,stop_code,stop_name,stop_lat,stop_lon
						s-9,9,New Stop,53.0,-6.0
						""")
				.writeTo(tempDir));

		assertThat(jdbcTemplate.queryForList("SELECT stop_id FROM stops", String.class)).containsExactly("s-9");
	}

	@Test
	void keepsTheOldDataWhenTheNewFeedBreaksAForeignKey() throws IOException {
		importer.importFrom(GtfsZipBuilder.validFeed().writeTo(tempDir));
		Path broken = GtfsZipBuilder.validFeed()
				.with("trips.txt", """
						route_id,service_id,trip_id,trip_headsign,direction_id
						no-such-route,weekday,t-9,Nowhere,0
						""")
				.writeTo(tempDir);

		assertThatThrownBy(() -> importer.importFrom(broken)).isInstanceOf(DataIntegrityViolationException.class);

		// trips fails after agency and routes were already replaced, so this proves the rollback.
		assertThat(count("trips")).isEqualTo(3);
		assertThat(count("agency")).isEqualTo(2);
	}

	@Test
	void keepsTheOldDataWhenAFileIsMissing() throws IOException {
		importer.importFrom(GtfsZipBuilder.validFeed().writeTo(tempDir));
		Path incomplete = GtfsZipBuilder.validFeed().without("stops.txt").writeTo(tempDir);

		assertThatThrownBy(() -> importer.importFrom(incomplete))
				.isInstanceOf(GtfsImportException.class)
				.hasMessageContaining("stops.txt");

		assertThat(count("stops")).isEqualTo(2);
	}

	@Test
	void rejectsARowWithoutARequiredValue() throws IOException {
		Path missingName = GtfsZipBuilder.validFeed()
				.with("stops.txt", """
						stop_id,stop_code,stop_name,stop_lat,stop_lon
						s-1,2039,,53.3521,-6.2607
						""")
				.writeTo(tempDir);

		assertThatThrownBy(() -> importer.importFrom(missingName))
				.isInstanceOf(GtfsImportException.class)
				.hasMessageContaining("stop_name");
	}

	@Test
	void hasDataReflectsTheRoutesTable() throws IOException {
		assertThat(importer.hasData()).isFalse();

		importer.importFrom(GtfsZipBuilder.validFeed().writeTo(tempDir));

		assertThat(importer.hasData()).isTrue();
	}

	private int count(String table) {
		return jdbcTemplate.queryForObject("SELECT count(*) FROM " + table, Integer.class);
	}
}
