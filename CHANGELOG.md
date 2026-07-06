# Changelog

All notable changes to this project are documented here. The format follows
[Keep a Changelog](https://keepachangelog.com/) and the project uses
[Conventional Commits](https://www.conventionalcommits.org/).

## [1.0.0] - 2026-07-01

### Added

- **Identity & Key Registry service** (PostgreSQL): device registration
  (`POST /v1/keys`), X3DH pre-key bundle retrieval with atomic consume-once
  one-time keys (`GET /v1/keys/{userId}/bundle`), and JWT issuance
  (`POST /v1/auth/token`).
- **Message Delivery service** (Redis + WebSocket/STOMP): JWT-authenticated
  connections, a Redis session registry, Kafka-based ingestion and routing,
  cross-instance forwarding over Redis pub/sub, a durable offline mailbox with
  drain-on-reconnect, and delivery receipts.
- **Shared event contracts** (`common-events`): `user.registered`,
  `prekeys.low`, `messages.inbound`, `messages.receipts`.
- **Web client** (React + Vite + TypeScript): client-side key generation,
  IndexedDB private-key vault, X3DH key agreement, an AES-GCM message ratchet,
  and a STOMP delivery client.
- Testcontainers-backed integration tests, a GitHub Actions CI pipeline, ADRs,
  a threat model, and architecture diagrams.

[1.0.0]: https://example.com/ciphermesh/releases/tag/v1.0.0
