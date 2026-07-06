# CipherMesh threat model

A short STRIDE-style summary of what CipherMesh defends against and what it
explicitly does not.

## Trust boundaries

- **Client device** — trusted. Holds all private keys and plaintext.
- **Network** — untrusted. TLS in transit; payloads are already E2E encrypted.
- **Server (services, PostgreSQL, Redis, Kafka)** — untrusted for confidentiality.
  Treated as an honest-but-curious relay that may be breached.

## What an attacker gains by compromising the server

| Component | Exposed | Not exposed |
| --- | --- | --- |
| PostgreSQL (identity) | public keys, user/device ids | any private key, any plaintext |
| Redis (sessions/mailbox) | who is online, ciphertext envelopes | plaintext |
| Kafka (messages/receipts) | ciphertext, routing metadata | plaintext |

Message confidentiality survives a full server compromise because encryption and
key custody are entirely client-side (see [ADR 0002](adr/0002-zero-knowledge-relay.md)).

## Threats and mitigations

- **Spoofing** — WebSocket CONNECT requires a signed JWT from the identity
  service; the sender identity comes from the token, not the payload.
- **Tampering** — messages are AES-GCM (authenticated encryption); the signed
  pre-key is signed by the device identity key.
- **Repudiation** — delivery receipts (`messages.receipts`) provide an audit trail
  of delivery events.
- **Information disclosure** — only public keys and ciphertext are persisted;
  private keys never leave the device's IndexedDB vault.
- **Denial of service** — per-recipient Kafka partitioning and horizontal scaling
  of stateless-ish delivery instances; not hardened against volumetric attacks here.
- **Elevation of privilege** — one-time pre-keys are consumed atomically
  (`FOR UPDATE SKIP LOCKED`) so a key cannot be replayed to weaken forward secrecy.

## Out of scope (known gaps)

- Metadata privacy (sealed sender), full XEdDSA verification of signed pre-keys,
  key backup/transfer, rate limiting, and abuse prevention. These are deliberate
  boundaries for a portfolio reference implementation.
