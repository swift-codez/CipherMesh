# 2. The server is a zero-knowledge relay

- Status: accepted
- Date: 2026-07-01

## Context

CipherMesh is end-to-end encrypted. The threat model assumes the server (and
anyone who compromises it) is untrusted: a breach of the database, the message
broker, or a running service must not expose message contents.

## Decision

No server component ever handles a private key, a derived shared secret, or
plaintext. Specifically:

- The identity service stores only **public** key material (identity keys,
  signed pre-keys, one-time pre-keys) and the signature over the signed pre-key.
- Clients generate all key pairs and keep every private key in the browser's
  IndexedDB vault.
- The delivery service, Kafka topics, and the mailbox carry only opaque
  ciphertext produced by the sender's device.
- X3DH and the message ratchet run entirely client-side.

## Consequences

- A full database or broker compromise yields public keys and ciphertext only —
  never readable messages.
- The server cannot implement server-side search, content moderation, or
  push-notification previews over message bodies; these must be client-side.
- Key custody and recovery are the client's responsibility (lost device = lost
  keys), which the client design must account for (backup/transfer flows).
- Metadata (who talks to whom, when) is still visible to the server; reducing
  it (sealed sender, etc.) is future work and out of scope for this decision.
