# web-client

React + Vite + TypeScript client. It does all client-side cryptography: it
generates the device's identity and pre-key material, keeps the private keys in
an IndexedDB vault, uploads only the public bundle, and encrypts messages before
they touch the network.

## What runs where

| Concern | Module |
| --- | --- |
| Key generation & bundle assembly | `src/crypto/keys.ts` |
| X3DH key agreement | `src/crypto/x3dh.ts` |
| Message ratchet (AES-GCM) | `src/crypto/doubleRatchet.ts` |
| Private-key vault (IndexedDB) | `src/storage/vault.ts` |
| Identity/Key REST client | `src/api/identityClient.ts` |
| Delivery WebSocket (STOMP) | `src/ws/deliveryClient.ts` |

## Cryptography note

This reference client implements the Signal-inspired flow with the Web Crypto
API — ECDH (P-256) for X3DH, HKDF for key derivation, and AES-GCM for message
encryption. It demonstrates the client's responsibilities (key custody, X3DH,
an authenticated-encryption ratchet) without a native dependency. A production
deployment would swap the crypto core for **libsignal** (X25519 + XEdDSA and the
full Double Ratchet, including the DH ratchet). Private keys never leave the
device in either case.

## Develop

```bash
npm install
npm run dev      # Vite dev server on :5173 (proxies /v1 to the identity service)
npm run build    # type-check + production build
npm test         # crypto unit tests (Vitest)
```

Point the client at the backend by running the identity service on `:8081` and
the delivery service on `:8082` (see the root README).
