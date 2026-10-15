# EventHub backend starter

Spring Boot 4.1.1 / Java 21 with PostgreSQL 17, Flyway migrations, JPA repositories, JWT security and Actuator. This is the database foundation for the [ticketing roadmap](docs/ROADMAP.md).

## Start the application and database in Docker

Open Docker Desktop and wait for its Linux engine to start. Run these commands from the project directory in PowerShell:

```powershell
./scripts/setup-env.ps1
docker compose up --build -d --wait
docker compose ps
Invoke-RestMethod http://localhost:8080/actuator/health
```

The setup script creates a private `.env` with random database and JWT secrets, and preserves an existing `.env`. With a database volume already initialized using the original starter password, use `./scripts/setup-env.ps1 -DatabasePassword '0000'` on first setup instead. The existing workspace `.env` uses that password to preserve its current database. `.env` is excluded from Git and the Docker build.

Compose starts PostgreSQL first and waits for its health check before starting the app. The app connects to `db:5432` inside Docker. Flyway applies migrations; Hibernate validates the resulting schema. Both services have health checks. A healthy response is `{"status":"UP"}` and includes the database health indicator internally.

The first Docker build downloads Java images and Maven dependencies and may take several minutes.

```powershell
docker compose logs --tail 100 app
docker compose stop
docker compose up -d --wait
```

The database persists in the existing `pgdata` volume. Changing `DB_USER`, `DB_NAME` or `DB_PASSWORD` in `.env` does not change credentials in an initialized PostgreSQL volume. Keep those values aligned with the actual database. `docker compose down` preserves the volume; adding `-v` deletes its data.

## Run Java locally with PostgreSQL in Docker

This is useful when developing repositories and services in IntelliJ or VS Code. Install Java 21 and Maven 3.6.3 or later, and run from the project directory:

```powershell
./scripts/setup-env.ps1
docker compose stop app
docker compose up -d db --wait
mvn spring-boot:run
```

Spring loads `.env` from the working directory, so you do not need to copy credentials into `application.properties` or IDE run configurations. The local app uses `localhost:5433`; the containerized app uses `db:5432`. Both use the same database volume. Stop the local Java process before restarting the Docker app on port 8080.

To connect from IntelliJ Database, DBeaver or pgAdmin:

| Setting | Value |
| --- | --- |
| Host | `localhost` |
| Port | `5433` (or `DB_PORT` in `.env`) |
| Database | `eventhub` (or `DB_NAME`) |
| User | `postgres` (or `DB_USER`) |
| Password | `DB_PASSWORD` in `.env` |
| JDBC URL | `jdbc:postgresql://localhost:5433/eventhub` |

Host port 5433 avoids the separate PostgreSQL instance on this machine's port 5432. Set `DB_PORT` to another unused port if necessary. App port is configured with `PORT`, default 8080. The published ports bind only to the local machine. Optional `DB_URL` overrides the local JDBC URL; Compose sets its container URL explicitly.

To inspect tables directly in the Docker database:

```powershell
docker compose exec db psql -U postgres -d eventhub
```

Then run:

```sql
SELECT version, description, success FROM flyway_schema_history;
SELECT id, email, display_name FROM app_users;
\dt
\q
```

## Verify database and repository integration

```powershell
mvn test
```

Docker must be running. `SchemaIntegrationTest` creates a separate temporary PostgreSQL container, applies Flyway V1, validates all entity mappings, and verifies user and role inserts, reloads and updates. The fixture transaction rolls back and Testcontainers removes the test database. It does not use the development database. Security tests verify current database roles and reject disabled users, revoked tokens and malformed claims. Docker image builds skip test execution; run Maven tests separately.

Follow `Controller -> Service -> Repository -> PostgreSQL`. Put business rules and `@Transactional` boundaries in services, extend `JpaRepository<Entity, UUID>` for persistence, and return DTOs from controllers. `AppUserRepository` supports `save`, `findById`, `findByEmail` and `existsByEmail`. Events and ticket types now have creation controllers, services and repositories. Reservations, orders, payments, promotions, tickets and check-ins remain future roadmap work. Java entities must populate required fields: SQL defaults do not replace explicit nulls sent by JPA.

Flyway owns the schema: add `V2__...sql`, `V3__...sql` migrations for changes and keep `ddl-auto=validate`. Do not edit a migration that has already been applied.

## Current API scope

`GET /actuator/health` is public. The admin role-grant endpoint exists and requires a valid bearer token from an enabled ADMIN user. The JWT filter reads current user roles and token version from PostgreSQL on every authenticated request.

`AuthController` is currently empty: registration, login and `/api/me` still need implementation. The security configuration permits the future registration/login routes, but those handlers are not available yet. The two creation APIs below require an existing enabled ORGANIZER account and its bearer token. Event update, publish, list/detail, purchasing and payment APIs remain roadmap work.

Optional `BOOTSTRAP_ADMIN_EMAIL` and `BOOTSTRAP_ADMIN_PASSWORD` in `.env` provision an administrator on startup. Set both or neither; the password requires at least 12 characters. An existing account with that email is never promoted automatically. Remove bootstrap credentials after provisioning.

## Create an event and its ticket types

Both endpoints return `201 Created` with a response DTO and a `Location` header. Send `Content-Type: application/json` and `Authorization: Bearer <token>`. The organizer ID comes from the token, and events start as `DRAFT`.

`POST /api/events`

```json
{
  "title": "Launch party",
  "description": "Our launch event",
  "venue": "Main hall",
  "startsAt": "2027-01-15T08:00:00Z",
  "endsAt": "2027-01-15T11:00:00Z"
}
```

Use the returned event `id` for `POST /api/events/{eventId}/ticket-types`:

```json
{
  "name": "General admission",
  "price": 25.50,
  "capacity": 100,
  "saleStartsAt": "2027-01-01T00:00:00Z",
  "saleEndsAt": "2027-01-15T08:00:00Z"
}
```

Only the owning organizer can add ticket types, and only to a draft event. Availability is initialized to capacity by the service. Title, venue and ticket name are required; description is optional. Event start must be in the future and end must follow start. Price must be nonnegative with at most two decimal places, capacity must be positive, and the sale end must follow the sale start without exceeding the event end. Ticket names are unique within an event after trimming whitespace (case-sensitive).

Missing or invalid tokens return `401`; insufficient role or wrong ownership returns `403`; missing events return `404`; invalid fields or date windows return `400`; duplicate names or a non-draft event return `409`. Validation responses include an `errors` map. Dates use ISO-8601 timestamps with a UTC offset.

`EventCreationIntegrationTest` exercises both endpoints over HTTP with signed JWTs and a temporary PostgreSQL container. It checks persistence, event linkage, initial inventory, ownership, duplicate names, input validation and authentication.

## Source control

This workspace currently has no `.git` directory. Docker and database development work independently of a Git remote. When adding a repository, keep `.env` out of commits and commit `.env.example`, migrations and source files instead.
