# CipherMesh

A production-grade, end-to-end encrypted (E2EE) messaging backend built as a
distributed system with Java 21 and Spring Boot. CipherMesh implements the
foundational primitives of the [Signal Protocol](https://signal.org/docs/) —
X3DH for asynchronous key agreement and the Double Ratchet for message
encryption — while keeping the server a **zero-knowledge relay**: it stores and
forwards only public keys and ciphertext, and never sees a private key, a
derived shared secret, or a single byte of plaintext.

## Why this design

Most "chat app" projects are monoliths that encrypt nothing meaningful.
CipherMesh is deliberately the opposite: an event-driven, polyglot-persistence
system where the hard problems — asynchronous key distribution, stateful
WebSocket routing, and offline store-and-forward — are solved explicitly.

## Architecture

| Service | Responsibility | Datastore |
| --- | --- | --- |
| `identity-service` | User/device registration and the X3DH public pre-key registry | PostgreSQL |
| `delivery-service` | Real-time message routing over WebSocket/STOMP, session registry, offline mailbox | Redis + durable store |
| `common-events` | Shared Kafka event contracts used across services | — |
| `web-client` | React + Vite + TypeScript client doing all client-side cryptography | IndexedDB |

Apache Kafka decouples the services for fan-out, the offline mailbox, and
delivery receipts. Pre-key bundle retrieval stays **synchronous REST** by
design — a client cannot encrypt until it holds the recipient's keys, so making
that path asynchronous would be an anti-pattern (see `docs/adr/`).

### How Alice sends an encrypted message to Bob

1. Bob publishes his public pre-key bundle to `identity-service` (Postgres).
2. Alice fetches Bob's bundle via `GET /v1/keys/{userId}/bundle`.
3. The registry hands out one one-time pre-key **atomically and once**.
4. Alice runs X3DH locally and derives a shared secret — the server never sees it.
5. Alice encrypts with the Double Ratchet; plaintext never leaves her browser.
6. Ciphertext is sent to `delivery-service` over a STOMP WebSocket.
7. If Bob is offline, the ciphertext is stored in his durable mailbox (Kafka + store).
8. When Bob connects, the server pushes queued ciphertext; Bob decrypts and ACKs.

## Tech stack

- Java 21, Spring Boot 3.4, multi-module Maven
- Spring Data JPA + PostgreSQL, Spring Data Redis
- Spring for Apache Kafka
- Spring WebSocket (STOMP)
- Testcontainers for integration tests
- React + Vite + TypeScript with libsignal on the client

## Repository layout

```
ciphermesh/
├── pom.xml                 # parent / aggregator
├── docker-compose.yml      # postgres, redis, kafka (local dev)
├── common-events/          # shared Kafka event contracts
├── identity-service/       # X3DH public-key registry (Postgres)
├── delivery-service/       # WebSocket delivery + mailbox (planned)
└── web-client/             # React + Vite + TS client (planned)
```

## Local development

Start the backing services:

```bash
docker compose up -d
```

Build and test everything (requires Maven 3.9+ and JDK 21+):

```bash
mvn clean verify
```

> The repository test suite uses Testcontainers, so a running Docker daemon is
> required for `verify`.

## Documentation

- [Architecture & sequence diagrams](docs/architecture.md)
- [Threat model](docs/threat-model.md)
- Architecture decisions: [ADR 0001](docs/adr/0001-synchronous-prekey-bundle-fetch.md),
  [ADR 0002](docs/adr/0002-zero-knowledge-relay.md)
- Per-service: [identity-service](identity-service/README.md),
  [delivery-service](delivery-service/README.md), [web-client](web-client/README.md)

## Status

Feature-complete reference implementation (v1.0.0): both backend services, the
shared event contracts, and the web client are in place with unit and
Testcontainers-backed integration tests and a CI pipeline. See the
[CHANGELOG](CHANGELOG.md).
