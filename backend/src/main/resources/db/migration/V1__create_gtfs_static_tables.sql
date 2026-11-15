-- GTFS Static tables for the MVP: agency, routes, trips and stops.
-- Table and column names follow the GTFS specification, so they match the CSV headers.
-- Ids are text because GTFS ids are opaque strings (for example "03C_120_e_a").

CREATE TABLE agency (
    agency_id       text PRIMARY KEY,
    agency_name     text NOT NULL,
    agency_url      text,
    agency_timezone text
);

CREATE TABLE routes (
    route_id         text PRIMARY KEY,
    agency_id        text NOT NULL REFERENCES agency (agency_id),
    route_short_name text,
    route_long_name  text,
    route_type       integer NOT NULL
);

CREATE TABLE trips (
    trip_id       text PRIMARY KEY,
    route_id      text NOT NULL REFERENCES routes (route_id),
    service_id    text NOT NULL,
    trip_headsign text,
    direction_id  smallint
);

-- PostgreSQL does not index foreign key columns automatically.
CREATE INDEX trips_route_id_idx ON trips (route_id);

CREATE TABLE stops (
    stop_id   text PRIMARY KEY,
    stop_code text,
    stop_name text NOT NULL,
    stop_lat  double precision NOT NULL,
    stop_lon  double precision NOT NULL
);
