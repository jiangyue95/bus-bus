# bus-bus

A real-time map of Dublin buses.

bus-bus is a backend-focused project. The Spring Boot backend polls the NTA GTFS-Realtime feed, combines it with GTFS Static data, and exposes a versioned REST API. The web client is a thin client that only displays what the API returns. An iOS client is planned as a follow-up.

## MVP scope

- Live vehicle map
- Vehicle details
- Nearby stops
- User location
- Data freshness indicator

Out of scope: crowdsourced GPS, automatic boarding/alighting detection, journey planner, user accounts.

## Tech stack

| Part | Technology |
|---|---|
| Backend | Java 21, Spring Boot, Maven |
| Database | PostgreSQL (GTFS Static data) |
| Frontend | React, TypeScript, MapLibre |
| Data | NTA GTFS Static + GTFS-Realtime |

## Repository layout

```
backend/    Spring Boot API
frontend/   Web client (not started yet)
```

## Run the backend

The backend needs a local PostgreSQL server with two databases, one for development and one for tests:

```bash
createdb busbus
createdb busbus_test
```

By default it connects as a role named after your OS user with no password, which matches a Homebrew install. Set `DB_URL`, `DB_USER` and `DB_PASSWORD` in `.env` to override this. Flyway creates the tables when the app starts.

The backend also needs an NTA API key to fetch live data. Copy `.env.example` to `.env` and set `NTA_API_KEY`. Without a key the app still starts, but polling is disabled and the vehicle list stays empty.

```bash
cd backend
./mvnw spring-boot:run
```

Then check that it is up:

```bash
curl localhost:8080/actuator/health
```

## API

| Endpoint | Description |
|---|---|
| `GET /api/v1/vehicles` | Latest vehicle positions, with the feed timestamp and the time of the last successful fetch |
