# Shield API — Role-Based Secure API & Threat-Logging Shield

A production-grade Spring Boot backend built for environments with strict
compliance requirements and unreliable infrastructure (e.g. ZIMRA-style
fiscal reporting under intermittent power/network conditions).

## What's inside

| Capability | Implementation |
|---|---|
| Stateless auth | RS256-signed JWTs (Spring Security OAuth2 Resource Server) |
| Object-level authorization | `@PreAuthorize` ownership checks — blocks BOLA/IDOR |
| Immutable audit trail | Spring AOP intercepts every repository write, chains a SHA-256 HMAC ledger |
| Active threat defense | Redis-backed rolling counter; abusive clients get silently rerouted to a honeypot |
| Offline resilience | Transactional Outbox + exponential backoff for the fiscal gateway sync |
| Multi-currency awareness | Every audit row captures currency code + interbank rate at time of write |

## Quick start

```bash
cp .env.example .env        # set POSTGRES_PASSWORD and SHIELD_AUDIT_SECRET
docker compose up --build
```

The API comes up on `http://localhost:8080`. PostgreSQL and Redis run as
sidecar containers; `schema.sql` applies automatically on first boot.

## Key endpoints

| Method | Path | Notes |
|---|---|---|
| POST | `/api/v1/auth/register` | Creates a user with `ROLE_INTERN` by default |
| POST | `/api/v1/auth/login` | Returns a bearer JWT (1 hour validity) |
| GET | `/api/v1/taxpayers/{tin}/returns` | Requires `ROLE_ADMIN` or matching `tin_number` claim |
| GET | `/api/v1/decoy/taxpayers/all` | Honeypot only — reached automatically after 5 suspicious requests/min |

## Local (non-Docker) development

1. Start PostgreSQL 15+ and Redis 7+ locally.
2. Export `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`,
   `SPRING_DATASOURCE_PASSWORD`, `SHIELD_AUDIT_SECRET`.
3. `mvn spring-boot:run`

## Regenerating the JWT signing keys

```bash
openssl genrsa -out private.pem 2048
openssl rsa -in private.pem -pubout -out public.pem
openssl pkcs8 -topk8 -inform PEM -outform PEM -nocrypt -in private.pem -out private_key.pem
```

Replace the files under `src/main/resources/keys/`. **Never commit
production keys** — the included pair is for local development only.

## Security notes before going to production

- Set `SHIELD_AUDIT_SECRET` to a long random value via your secrets manager —
  never the placeholder in `application.yml`.
- Swap the embedded honeypot synthetic payload for data that can't be used
  to fingerprint your real schema.
- Point `shield.fiscal-gateway.url` at the real government/bank endpoint.
- Put the API behind TLS termination (reverse proxy or load balancer).
