// X3DH-style asynchronous key agreement over Web Crypto ECDH (P-256).
//
// Sender and receiver combine the same three Diffie-Hellman outputs in the same
// order and run them through HKDF to arrive at an identical 32-byte root key,
// which seeds the message ratchet. (A one-time pre-key DH is omitted here for
// brevity; libsignal adds it as a fourth term in production.)

const HKDF_INFO = new TextEncoder().encode('CipherMesh-X3DH-root');
const ZERO_SALT = new Uint8Array(32);

async function dh(privateKey: CryptoKey, publicKey: CryptoKey): Promise<ArrayBuffer> {
  return crypto.subtle.deriveBits({ name: 'ECDH', public: publicKey }, privateKey, 256);
}

function concat(buffers: ArrayBuffer[]): ArrayBuffer {
  const total = buffers.reduce((sum, b) => sum + b.byteLength, 0);
  const out = new Uint8Array(total);
  let offset = 0;
  for (const b of buffers) {
    out.set(new Uint8Array(b), offset);
    offset += b.byteLength;
  }
  return out.buffer;
}

async function hkdfRoot(ikm: ArrayBuffer): Promise<ArrayBuffer> {
  const base = await crypto.subtle.importKey('raw', ikm, 'HKDF', false, ['deriveBits']);
  return crypto.subtle.deriveBits(
    { name: 'HKDF', hash: 'SHA-256', salt: ZERO_SALT, info: HKDF_INFO },
    base,
    256,
  );
}

export interface SenderInputs {
  identityPrivate: CryptoKey;
  ephemeralPrivate: CryptoKey;
  recipientIdentityPublic: CryptoKey;
  recipientSignedPreKeyPublic: CryptoKey;
}

export interface ReceiverInputs {
  identityPrivate: CryptoKey;
  signedPreKeyPrivate: CryptoKey;
  senderIdentityPublic: CryptoKey;
  senderEphemeralPublic: CryptoKey;
}

/** Sender (Alice) derives the root key from her identity + ephemeral keys and Bob's bundle. */
export async function deriveRootKeyAsSender(inputs: SenderInputs): Promise<ArrayBuffer> {
  const dh1 = await dh(inputs.identityPrivate, inputs.recipientSignedPreKeyPublic);
  const dh2 = await dh(inputs.ephemeralPrivate, inputs.recipientIdentityPublic);
  const dh3 = await dh(inputs.ephemeralPrivate, inputs.recipientSignedPreKeyPublic);
  return hkdfRoot(concat([dh1, dh2, dh3]));
}

/** Receiver (Bob) derives the same root key from his private keys and Alice's public keys. */
export async function deriveRootKeyAsReceiver(inputs: ReceiverInputs): Promise<ArrayBuffer> {
  const dh1 = await dh(inputs.signedPreKeyPrivate, inputs.senderIdentityPublic);
  const dh2 = await dh(inputs.identityPrivate, inputs.senderEphemeralPublic);
  const dh3 = await dh(inputs.signedPreKeyPrivate, inputs.senderEphemeralPublic);
  return hkdfRoot(concat([dh1, dh2, dh3]));
}
