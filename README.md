# SecureShield API

A Spring Boot backend built around the security patterns that enterprise and government API audits actually check for — not just authentication, but tamper-evident logging, active threat response, and resilience under infrastructure failure.

The scenario it's designed for: a fiscal reporting API that has to keep working during load-shedding, survive credential-stuffing attacks without exposing the real database, and produce audit logs that hold up in a compliance review.

## What it does

**RS256 JWT authentication**

Tokens are signed with a 2048-bit RSA private key and verified with the public key. Unlike HMAC (HS256), the signing key never needs to leave the authorization server — a compromised downstream service can't forge tokens.

**Object-level authorization (BOLA defense)**

`@PreAuthorize` checks don't just verify role membership. The `tin_number` claim inside the JWT is compared against the requested resource — so a logged-in user with `ROLE_INTERN` can't access another taxpayer's records even if they know the TIN.

**Hash-chained audit logs**

Every repository write is intercepted by a Spring AOP aspect. Each audit row is signed with HMAC-SHA256 over its own fields plus the previous row's hash — the same structure as a blockchain block. Editing any historical record breaks every hash that follows it, making silent tampering detectable.

**Redis threat detection + honeypot**

Failed requests are counted in Redis with a 1-minute rolling window. After 5 failures, the IP is blocked for 24 hours. Blocked clients aren't rejected with a 401 — their requests are silently forwarded to a honeypot controller that returns convincing fake taxpayer data while logging a CEF-formatted alert for Wazuh or Splunk ingestion. The attacker sees a working API. The real database is never touched.

**Transactional outbox**

Outbound calls to external gateways (e.g. a fiscal device management system) are written to a `fiscal_outbox` table within the same database transaction as the business operation. A scheduler retries failed deliveries with exponential backoff. A dropped network connection can't cause a lost transaction.

**Multi-currency audit capture**

Every audit row records the currency code and interbank exchange rate at the time of the write — relevant for environments where the functional currency changes frequently.

## Stack

- Java 17, Spring Boot 3.3
- Spring Security (OAuth2 Resource Server, method security)
- Spring Data JPA + Hibernate
- Spring AOP
- PostgreSQL 15
- Redis 7
- Docker + Docker Compose
- Lombok, Flyway

## Running it locally

You need Docker Desktop running.

```bash
git clone https://github.com/witnesskaukonde222-blip/shield-api.git
cd shield-api
cp .env.example .env
```

Edit `.env` and set `POSTGRES_PASSWORD` to something real. Then:

```bash
docker compose up -d db-postgres cache-redis
```

Wait a few seconds for Postgres to initialize, then run `ShieldApiApplication` from your IDE (IntelliJ or VS Code with Java extensions). The app comes up on port 8080.

If you get an SSL connection error on first run, add `?sslmode=disable` to the datasource URL in `application.yml`. The alpine Postgres image doesn't configure SSL by default.

## API

**Register**
```
POST /api/v1/auth/register
Content-Type: application/json

{ "username": "alice", "email": "alice@example.com", "password": "SecurePass123!" }
```

**Login**
```
POST /api/v1/auth/login
Content-Type: application/json

{ "username": "alice", "password": "SecurePass123!" }
```

Returns an `accessToken`. Use it as a Bearer token on subsequent requests.

**Protected endpoint**
```
GET /api/v1/taxpayers/{tin}/returns
Authorization: Bearer <token>
```

Returns 403 unless the caller has `ROLE_ADMIN` or their JWT `tin_number` claim matches the requested TIN.

**Triggering the honeypot**

Send 6+ requests with a wrong password within 60 seconds. From the 6th request onward, responses come from the honeypot controller instead of the real auth logic.

## Generating new RSA keys

The keys in `src/main/resources/keys/` are for local development only. For any real deployment, generate a fresh pair:

```bash
openssl genrsa -out private.pem 2048
openssl rsa -in private.pem -pubout -out public.pem
openssl pkcs8 -topk8 -inform PEM -outform PEM -nocrypt -in private.pem -out private_key.pem
```

Replace the files under `src/main/resources/keys/`. Never commit `private_key.pem` to version control.

## Project structure

```
src/main/java/com/enterprise/shield/
├── aspect/         # CryptographicAuditAspect — AOP-based HMAC log chaining
├── config/         # SecurityConfiguration, RsaKeyProperties, RedisConfig
├── controller/     # AuthController, TaxpayerController, HoneypotDecoyController
├── dto/            # Request/response DTOs
├── model/          # JPA entities (User, Role, AuditLog, FiscalOutbox)
├── repository/     # Spring Data repositories
├── scheduler/      # OutboxSyncScheduler — retry loop with exponential backoff
├── security/       # ThreatDetectionEngine, ThreatDetectionFilter
└── service/        # AuthService, TaxpayerService, ShieldUserDetailsService
```

## Production checklist

- Replace `SHIELD_AUDIT_SECRET` with a value from a secrets manager
- Generate fresh RSA keys and store the private key outside the codebase
- Put the API behind TLS termination
- Point `shield.fiscal-gateway.url` at the real endpoint
- Replace the honeypot synthetic payload with data that doesn't reveal your real schema
