# 1. Pre-key bundle fetch is synchronous REST, not an asynchronous event

- Status: accepted
- Date: 2026-07-01

## Context

CipherMesh is an event-driven system: services are decoupled with Apache Kafka,
and message delivery is asynchronous by nature (store-and-forward, fan-out to
multiple devices, delivery receipts). A reasonable instinct is to make *every*
inter-service and client-service interaction asynchronous for consistency.

Fetching a recipient's X3DH pre-key bundle does not fit that pattern. A sender
cannot begin encrypting until it holds the recipient's identity key, signed
pre-key, and (optionally) a one-time pre-key. The operation is a request that
must return a value before the caller can make any progress — a classic
request/response interaction.

Making it asynchronous would mean the client publishes a "bundle requested"
event, then waits for a "bundle returned" event correlated by some id, blocking
the send flow anyway while adding a broker round-trip, correlation bookkeeping,
and a new failure mode. It would be complexity in service of consistency for its
own sake.

There is also a correctness constraint: issuing a one-time pre-key must be
atomic and once-only (`FOR UPDATE SKIP LOCKED`, then delete). That transaction
is trivial over a synchronous database call and awkward to reason about spread
across asynchronous events.

## Decision

The Identity & Key Registry service exposes pre-key bundle retrieval as a
synchronous REST endpoint:

```
GET /v1/keys/{userId}/bundle
```

Kafka is reserved for interactions that are genuinely asynchronous:
fan-out and store-and-forward of encrypted messages, delivery/read receipts,
and cross-service notifications such as `identity.user-registered` and
`identity.prekeys-low`.

## Consequences

- The send path stays simple: fetch bundle, run X3DH, encrypt, send.
- One-time pre-key consumption is a single atomic transaction with no
  distributed coordination.
- The identity service must be reachable for a new conversation to start; this
  is acceptable because a client cannot encrypt without it regardless of
  transport. Availability is addressed with standard REST techniques (caching
  of the immutable identity key, retries, horizontal scaling), not by forcing
  the call through the broker.
- "Event-driven" in this system means *messages and cross-service facts* flow as
  events — not that every call is fire-and-forget.
