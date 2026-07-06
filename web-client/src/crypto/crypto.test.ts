import { describe, expect, test } from 'vitest';
import { base64ToBuffer, bufferToBase64 } from './base64';
import { generateEcdhKeyPair, generateIdentity } from './keys';
import { deriveRootKeyAsReceiver, deriveRootKeyAsSender } from './x3dh';
import { SymmetricRatchet } from './doubleRatchet';

describe('base64', () => {
  test('round-trips arbitrary bytes', () => {
    const bytes = new Uint8Array([0, 1, 2, 250, 128, 255]).buffer;
    const restored = new Uint8Array(base64ToBuffer(bufferToBase64(bytes)));
    expect(restored).toEqual(new Uint8Array(bytes));
  });
});

describe('X3DH', () => {
  test('sender and receiver derive the same root key', async () => {
    const aliceIdentity = await generateEcdhKeyPair();
    const aliceEphemeral = await generateEcdhKeyPair();
    const bobIdentity = await generateEcdhKeyPair();
    const bobSigned = await generateEcdhKeyPair();

    const senderRoot = await deriveRootKeyAsSender({
      identityPrivate: aliceIdentity.privateKey,
      ephemeralPrivate: aliceEphemeral.privateKey,
      recipientIdentityPublic: bobIdentity.publicKey,
      recipientSignedPreKeyPublic: bobSigned.publicKey,
    });
    const receiverRoot = await deriveRootKeyAsReceiver({
      identityPrivate: bobIdentity.privateKey,
      signedPreKeyPrivate: bobSigned.privateKey,
      senderIdentityPublic: aliceIdentity.publicKey,
      senderEphemeralPublic: aliceEphemeral.publicKey,
    });

    expect(bufferToBase64(senderRoot)).toEqual(bufferToBase64(receiverRoot));
  });
});

describe('SymmetricRatchet', () => {
  test('two parties seeded with the same root decrypt in lockstep', async () => {
    const root = crypto.getRandomValues(new Uint8Array(32)).buffer;
    const alice = new SymmetricRatchet(root.slice(0));
    const bob = new SymmetricRatchet(root.slice(0));

    const first = await alice.encrypt('hello');
    expect(await bob.decrypt(first)).toEqual('hello');

    const second = await alice.encrypt('again');
    expect(await bob.decrypt(second)).toEqual('again');
  });
});

describe('identity generation', () => {
  test('produces an uploadable public bundle', async () => {
    const { upload } = await generateIdentity('alice', 1, 3);
    expect(upload.identityKey).not.toHaveLength(0);
    expect(upload.signedPreKey.signature).not.toHaveLength(0);
    expect(upload.oneTimePreKeys).toHaveLength(3);
  });
});
