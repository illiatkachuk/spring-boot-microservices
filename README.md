# Spring Boot microservices with PostgreSQL and Docker

Two Spring Boot microservices and PostgreSQL, started together with Docker Compose.

## Architecture

```
                 ┌─────────────────────── docker network "backend" ───────────────────────┐
                 │                                                                         │
  client ──────► │  auth-api :8080 ──── POST /api/transform ────►  data-api :8081           │
  (curl)         │      │               X-Internal-Token: <secret>   (no DB, reverses text) │
                 │      │                                                                  │
                 │      └──── JDBC ────► postgres :5432  (users, processing_log)           │
                 └─────────────────────────────────────────────────────────────────────────┘
```

**auth-api** (Service A) is the service clients talk to. It registers users (passwords stored as
BCrypt hashes in Postgres), logs them in by issuing a signed JWT, and exposes the protected
`POST /api/process`. For every `/process` call it verifies the JWT, forwards the text to **data-api**
(Service B) over the internal Docker network at `http://data-api:8081` — the service name is resolved
by Docker's DNS, no IPs — saves the input and output to `processing_log`, and returns the result.
**data-api** is a small stateless service without a database: it transforms the text and accepts a
call only if it carries the shared secret in the `X-Internal-Token` header. B checks that secret itself
instead of trusting the network, so even though port 8081 is published, nobody without the secret can use it.

**Transformation:** data-api **reverses** the text — `"hello"` → `"olleh"`.

**Stack:** Java 17 · Spring Boot 3.5 (Web, Security, Data JPA, Validation) · jjwt 0.13 (HS256) ·
Flyway · PostgreSQL 16 · Docker Compose.

## Project structure

```
auth-api/                          Service A
  src/main/java/travel/winwin/authapi/
    controller/   AuthController, ProcessController
    service/      AuthService, ProcessService
    security/     SecurityConfig, JwtAuthFilter, JwtService, AuthenticatedUser
    client/       DataApiClient              -> calls data-api
    entity/       User, ProcessingLog
    repository/   UserRepository, ProcessingLogRepository
    dto/          request/response records
    exception/    GlobalExceptionHandler + exceptions
  src/main/resources/
    application.yml
    db/migration/V1__init.sql     -> Flyway creates the tables on startup
  Dockerfile
data-api/                          Service B
  src/main/java/travel/winwin/dataapi/
    InternalTokenFilter            -> 403 unless X-Internal-Token is correct
    TransformController            -> POST /api/transform
  Dockerfile
docker-compose.yml
.env.example
```

## Prerequisites

- Docker with Docker Compose v2
- Only for the local `mvn` build: JDK 17+ and Maven 3.6.3+.
  The Docker images don't need them — each Dockerfile compiles the jar inside a Maven container.

## Configuration

Everything is configured through environment variables; no secrets are in the code or in
`docker-compose.yml`. Compose reads the values from a `.env` file:

```bash
cp .env.example .env
# The placeholder values are enough for a local try-out.
# For anything shared, replace the secrets, e.g. with the output of:
openssl rand -hex 32
```

| Variable            | Used by             | Purpose                                                   |
|---------------------|---------------------|-----------------------------------------------------------|
| `POSTGRES_DB`       | postgres            | database name                                             |
| `POSTGRES_USER`     | postgres, auth-api  | database user                                             |
| `POSTGRES_PASSWORD` | postgres, auth-api  | database password                                         |
| `POSTGRES_URL`      | auth-api            | JDBC URL, e.g. `jdbc:postgresql://postgres:5432/winwin`   |
| `JWT_SECRET`        | auth-api            | HS256 signing key, **at least 32 characters**             |
| `INTERNAL_TOKEN`    | auth-api, data-api  | shared secret sent/checked in `X-Internal-Token`          |

If a variable is missing, `docker compose` refuses to start and names the variable.
`.env` is git-ignored; only `.env.example` is committed.

## Build and run

```bash
# 1. create the local config (skip if you already have a .env)
cp .env.example .env

# 2. (optional) build both jars locally
mvn -f auth-api/pom.xml clean package -DskipTests
mvn -f data-api/pom.xml clean package -DskipTests

# 3. build the images and start postgres, data-api and auth-api
docker compose up -d --build

# 4. check status; auth-api is ready once its log shows "Started AuthApiApplication"
docker compose ps
docker compose logs -f auth-api
```

On the first start Flyway creates the `users` and `processing_log` tables.

Stop everything with `docker compose down` (the database volume is kept) or
`docker compose down -v` (also deletes the database).

## Try it

### 1. Register

```bash
curl -i -X POST http://localhost:8080/api/auth/register -H "Content-Type: application/json" \
  -d '{"email":"a@a.com","password":"pass"}'
```

Expected: `HTTP/1.1 201` with an empty body.
The same email again → `409 {"error":"Email is already registered"}`.

### 2. Log in

```bash
curl -X POST http://localhost:8080/api/auth/login -H "Content-Type: application/json" \
  -d '{"email":"a@a.com","password":"pass"}'
```

Expected: `200 {"token":"eyJhbGciOiJIUzI1NiJ9...."}`.
Wrong email or password → `401 {"error":"Invalid email or password"}`.

To keep the token in a shell variable for the next step:

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login -H "Content-Type: application/json" \
  -d '{"email":"a@a.com","password":"pass"}' | sed -E 's/.*"token":"([^"]+)".*/\1/')
```

### 3. Process text

```bash
curl -X POST http://localhost:8080/api/process -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" -d '{"text":"hello"}'
```

Expected: `200 {"result":"olleh"}`, and a new row in `processing_log`.
Missing or invalid token → `401`.

### 4. Look at processing_log

```bash
docker compose exec postgres sh -c 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" \
  -c "SELECT user_id, input_text, output_text, created_at FROM processing_log ORDER BY created_at DESC LIMIT 5;"'
```

The command uses the credentials already set inside the container, so you don't type the password.

### 5. data-api rejects calls without the internal token

```bash
# no header -> 403
curl -i -X POST http://localhost:8081/api/transform -H "Content-Type: application/json" -d '{"text":"hello"}'

# wrong header -> 403
curl -i -X POST http://localhost:8081/api/transform -H "X-Internal-Token: wrong" \
  -H "Content-Type: application/json" -d '{"text":"hello"}'
```

## API summary

| Service  | Method & path               | Auth                | Body                  | Success          | Errors                                        |
|----------|-----------------------------|---------------------|-----------------------|------------------|-----------------------------------------------|
| auth-api | `POST /api/auth/register`   | —                   | `{ email, password }` | `201`            | `400` invalid input, `409` email taken        |
| auth-api | `POST /api/auth/login`      | —                   | `{ email, password }` | `200 { token }`  | `400` invalid input, `401` wrong credentials  |
| auth-api | `POST /api/process`         | `Bearer <JWT>`      | `{ text }`            | `200 { result }` | `400`, `401`, `502` data-api unavailable      |
| data-api | `POST /api/transform`       | `X-Internal-Token`  | `{ text }`            | `200 { result }` | `400` missing text, `403` missing/wrong token |

## Security notes

- **Passwords are hashed with BCrypt** before they are stored; the plain password is never saved or logged.
- **Secrets are never logged.** `JWT_SECRET`, `INTERNAL_TOKEN` and the database password are read only
  from environment variables, and neither they nor issued JWTs are written to the logs. A rejected
  internal call is logged only as "missing or invalid X-Internal-Token header". Spring Boot's default
  in-memory user (whose generated password it would print at startup) is disabled.
- **JWT:** HS256, `sub` = user id, `email` claim, valid for 60 minutes. No refresh tokens.
- **data-api protects itself:** it compares `X-Internal-Token` in constant time and fails to start if
  `INTERNAL_TOKEN` is empty.
- Login returns the same `401` for an unknown email and a wrong password, so the API doesn't reveal
  which emails are registered.

## Design decisions

- **Flyway instead of a Postgres init script.** Init scripts run only on an empty volume; Flyway runs on
  every auth-api start and applies only migrations that haven't run yet. Hibernate is set to
  `ddl-auto: validate`, so an entity/table mismatch fails at startup.
- **jjwt + a small `JwtAuthFilter`** instead of Spring's OAuth2 resource server, so every step of token
  validation is visible in one class.
- **`RestClient`** (synchronous, part of Spring Web) calls data-api with 2 s connect / 5 s read timeouts.
- **data-api is called before anything is saved**, so a failed call leaves no row in `processing_log`
  and the client gets `502`.
- **Postgres has no published port** — only auth-api needs it, inside the Docker network.
