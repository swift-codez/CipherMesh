// Client-side key generation for the CipherMesh X3DH bundle.
//
// All private keys stay on the device; only the public halves are uploaded.
// This reference client uses Web Crypto ECDH (P-256) for key agreement and
// ECDSA (P-256) for signing the signed pre-key. A production client would use
// libsignal (X25519 + XEdDSA) — the shapes here mirror that protocol.

import { bufferToBase64 } from './base64';

const ECDH = { name: 'ECDH', namedCurve: 'P-256' } as const;
const ECDSA = { name: 'ECDSA', namedCurve: 'P-256' } as const;

export interface SignedPreKeySecret {
  keyId: number;
  keyPair: CryptoKeyPair;
  signature: string;
}

export interface OneTimePreKeySecret {
  keyId: number;
  keyPair: CryptoKeyPair;
}

export interface DeviceSecrets {
  identityKeyPair: CryptoKeyPair;
  signingKeyPair: CryptoKeyPair;
  signedPreKey: SignedPreKeySecret;
  oneTimePreKeys: OneTimePreKeySecret[];
}

export interface PreKeyBundleUpload {
  userId: string;
  deviceId: number;
  registrationId: number;
  identityKey: string;
  signedPreKey: { keyId: number; publicKey: string; signature: string };
  oneTimePreKeys: { keyId: number; publicKey: string }[];
}

export interface GeneratedIdentity {
  secrets: DeviceSecrets;
  upload: PreKeyBundleUpload;
}

export function generateEcdhKeyPair(): Promise<CryptoKeyPair> {
  return crypto.subtle.generateKey(ECDH, true, ['deriveBits']) as Promise<CryptoKeyPair>;
}

function generateSigningKeyPair(): Promise<CryptoKeyPair> {
  return crypto.subtle.generateKey(ECDSA, true, ['sign', 'verify']) as Promise<CryptoKeyPair>;
}

export async function exportPublicKey(key: CryptoKey): Promise<string> {
  return bufferToBase64(await crypto.subtle.exportKey('raw', key));
}

async function signPublicKey(signingPrivateKey: CryptoKey, publicKey: CryptoKey): Promise<string> {
  const raw = await crypto.subtle.exportKey('raw', publicKey);
  const signature = await crypto.subtle.sign({ name: 'ECDSA', hash: 'SHA-256' }, signingPrivateKey, raw);
  return bufferToBase64(signature);
}

/** Generates a full identity: the on-device secrets plus the public bundle to upload. */
export async function generateIdentity(
  userId: string,
  deviceId: number,
  oneTimeKeyCount = 20,
): Promise<GeneratedIdentity> {
  const identityKeyPair = await generateEcdhKeyPair();
  const signingKeyPair = await generateSigningKeyPair();

  const signedKeyPair = await generateEcdhKeyPair();
  const signedKeyId = 1;
  const signature = await signPublicKey(signingKeyPair.privateKey, signedKeyPair.publicKey);
  const signedPreKey: SignedPreKeySecret = { keyId: signedKeyId, keyPair: signedKeyPair, signature };

  const oneTimePreKeys: OneTimePreKeySecret[] = [];
  for (let i = 0; i < oneTimeKeyCount; i++) {
    oneTimePreKeys.push({ keyId: i + 1, keyPair: await generateEcdhKeyPair() });
  }

  const upload: PreKeyBundleUpload = {
    userId,
    deviceId,
    registrationId: crypto.getRandomValues(new Uint32Array(1))[0] & 0x3fff,
    identityKey: await exportPublicKey(identityKeyPair.publicKey),
    signedPreKey: {
      keyId: signedPreKey.keyId,
      publicKey: await exportPublicKey(signedKeyPair.publicKey),
      signature,
    },
    oneTimePreKeys: await Promise.all(
      oneTimePreKeys.map(async (otpk) => ({
        keyId: otpk.keyId,
        publicKey: await exportPublicKey(otpk.keyPair.publicKey),
      })),
    ),
  };

  return { secrets: { identityKeyPair, signingKeyPair, signedPreKey, oneTimePreKeys }, upload };
}

export function importEcdhPublicKey(base64: string): Promise<CryptoKey> {
  const bytes = new Uint8Array(atob(base64).split('').map((c) => c.charCodeAt(0)));
  return crypto.subtle.importKey('raw', bytes, ECDH, true, []);
}
