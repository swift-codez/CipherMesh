// A symmetric-key ratchet in the spirit of the Double Ratchet's sending/receiving
// chains: each message advances a chain key via HKDF and derives a fresh,
// single-use message key for AES-GCM. Two parties seeded with the same X3DH root
// key stay in lockstep. (The DH ratchet that rotates the root key per round trip
// is where libsignal would slot in; omitted here for brevity.)

import { base64ToBuffer, bufferToBase64 } from './base64';

const EMPTY_SALT = new Uint8Array(32);
const MESSAGE_INFO = new TextEncoder().encode('CipherMesh-msg-key');
const CHAIN_INFO = new TextEncoder().encode('CipherMesh-chain-key');

export interface RatchetMessage {
  iv: string;
  ciphertext: string;
}

export class SymmetricRatchet {
  private chainKey: ArrayBuffer;

  constructor(rootKey: ArrayBuffer) {
    this.chainKey = rootKey;
  }

  private async step(): Promise<CryptoKey> {
    const base = await crypto.subtle.importKey('raw', this.chainKey, 'HKDF', false, ['deriveBits']);
    const messageKeyBits = await crypto.subtle.deriveBits(
      { name: 'HKDF', hash: 'SHA-256', salt: EMPTY_SALT, info: MESSAGE_INFO },
      base,
      256,
    );
    this.chainKey = await crypto.subtle.deriveBits(
      { name: 'HKDF', hash: 'SHA-256', salt: EMPTY_SALT, info: CHAIN_INFO },
      base,
      256,
    );
    return crypto.subtle.importKey('raw', messageKeyBits, { name: 'AES-GCM' }, false, ['encrypt', 'decrypt']);
  }

  async encrypt(plaintext: string): Promise<RatchetMessage> {
    const key = await this.step();
    const iv = crypto.getRandomValues(new Uint8Array(12));
    const ciphertext = await crypto.subtle.encrypt(
      { name: 'AES-GCM', iv },
      key,
      new TextEncoder().encode(plaintext),
    );
    return { iv: bufferToBase64(iv.buffer), ciphertext: bufferToBase64(ciphertext) };
  }

  async decrypt(message: RatchetMessage): Promise<string> {
    const key = await this.step();
    const plaintext = await crypto.subtle.decrypt(
      { name: 'AES-GCM', iv: new Uint8Array(base64ToBuffer(message.iv)) },
      key,
      base64ToBuffer(message.ciphertext),
    );
    return new TextDecoder().decode(plaintext);
  }
}
