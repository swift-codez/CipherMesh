# identity-service

The Identity & Key Registry service. Stores the **public** halves of each
device's X3DH key material and hands them out so peers can start encrypted
sessions. It never sees a private key, a derived secret, or any plaintext.

## Responsibilities

- Register a device and its public pre-key bundle (`POST /v1/keys`)
- Serve a peer's pre-key bundle, consuming one one-time pre-key atomically
  (`GET /v1/keys/{userId}/bundle`)
- Issue signed access tokens for the delivery service (`POST /v1/auth/token`)
- Publish `identity.user-registered` and `identity.prekeys-low` to Kafka

## Data model (PostgreSQL)

| Table | Purpose |
| --- | --- |
| `device` | user/device identity + permanent public identity key |
| `signed_pre_key` | current signed pre-key (rotates via the `active` flag) |
| `one_time_pre_key` | pool of one-time keys; deleted on issue (consume-once) |

## Key endpoints

```
POST /v1/keys                     register a device bundle       -> 201
GET  /v1/keys/{userId}/bundle     fetch a bundle for X3DH        -> 200
POST /v1/auth/token               issue a delivery access token  -> 200
```

## Run locally

```bash
docker compose up -d postgres kafka
mvn -pl identity-service spring-boot:run
```

Listens on `:8081`. See [ADR 0001](../docs/adr/0001-synchronous-prekey-bundle-fetch.md)
for why bundle fetch is synchronous.
