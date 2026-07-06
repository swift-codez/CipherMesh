import { useCallback, useRef, useState } from 'react';
import { IdentityClient } from './api/identityClient';
import {
  exportPublicKey,
  generateEcdhKeyPair,
  generateIdentity,
  importEcdhPublicKey,
} from './crypto/keys';
import { deriveRootKeyAsSender } from './crypto/x3dh';
import { SymmetricRatchet } from './crypto/doubleRatchet';
import { loadDeviceSecrets, saveDeviceSecrets } from './storage/vault';
import { DeliveryClient, type IncomingMessage } from './ws/deliveryClient';

const IDENTITY_URL = 'http://localhost:8081';
const DELIVERY_URL = 'ws://localhost:8082/ws';

export function App() {
  const [userId, setUserId] = useState('alice');
  const [deviceId, setDeviceId] = useState(1);
  const [recipient, setRecipient] = useState('bob');
  const [plaintext, setPlaintext] = useState('hello from CipherMesh');
  const [connected, setConnected] = useState(false);
  const [log, setLog] = useState<string[]>([]);
  const deliveryRef = useRef<DeliveryClient | null>(null);

  const append = useCallback((line: string) => {
    setLog((prev) => [`${new Date().toLocaleTimeString()}  ${line}`, ...prev].slice(0, 50));
  }, []);

  const onIncoming = useCallback(
    (message: IncomingMessage) => {
      append(`recv from ${message.senderId}: ${message.ciphertext.slice(0, 32)}…`);
      deliveryRef.current?.ack(message.messageId);
    },
    [append],
  );

  const initialize = useCallback(async () => {
    try {
      const identity = await generateIdentity(userId, deviceId);
      await saveDeviceSecrets(identity.secrets);
      const client = new IdentityClient(IDENTITY_URL);
      const result = await client.registerBundle(identity.upload);
      append(`registered device ${result.id} with ${result.oneTimePreKeyCount} one-time keys`);
      const token = await client.requestToken(userId, deviceId);
      const delivery = new DeliveryClient(DELIVERY_URL, token.token, onIncoming, setConnected);
      delivery.activate();
      deliveryRef.current = delivery;
      append('connecting to delivery service…');
    } catch (error) {
      append(`error: ${(error as Error).message}`);
    }
  }, [append, deviceId, onIncoming, userId]);

  const send = useCallback(async () => {
    try {
      const secrets = await loadDeviceSecrets();
      if (!secrets) {
        append('initialize your identity first');
        return;
      }
      const client = new IdentityClient(IDENTITY_URL);
      const bundle = await client.fetchBundle(recipient);
      const recipientIdentity = await importEcdhPublicKey(bundle.identityKey);
      const recipientSigned = await importEcdhPublicKey(bundle.signedPreKey.publicKey);
      const ephemeral = await generateEcdhKeyPair();
      const rootKey = await deriveRootKeyAsSender({
        identityPrivate: secrets.identityKeyPair.privateKey,
        ephemeralPrivate: ephemeral.privateKey,
        recipientIdentityPublic: recipientIdentity,
        recipientSignedPreKeyPublic: recipientSigned,
      });
      const ratchet = new SymmetricRatchet(rootKey);
      const message = await ratchet.encrypt(plaintext);
      const envelope = btoa(
        JSON.stringify({ ephemeralPublicKey: await exportPublicKey(ephemeral.publicKey), ...message }),
      );
      deliveryRef.current?.send(recipient, bundle.deviceId, envelope);
      append(`sent encrypted message to ${recipient}`);
    } catch (error) {
      append(`error: ${(error as Error).message}`);
    }
  }, [append, plaintext, recipient]);

  return (
    <main style={{ fontFamily: 'system-ui, sans-serif', maxWidth: 640, margin: '2rem auto', padding: '0 1rem' }}>
      <h1>CipherMesh</h1>
      <p>
        Status: <strong>{connected ? 'connected' : 'disconnected'}</strong>
      </p>

      <section style={{ display: 'grid', gap: '0.5rem', marginBottom: '1rem' }}>
        <label>
          User id <input value={userId} onChange={(e) => setUserId(e.target.value)} />
        </label>
        <label>
          Device id{' '}
          <input type="number" value={deviceId} onChange={(e) => setDeviceId(Number(e.target.value))} />
        </label>
        <button onClick={initialize}>Generate identity, register &amp; connect</button>
      </section>

      <section style={{ display: 'grid', gap: '0.5rem', marginBottom: '1rem' }}>
        <label>
          Recipient <input value={recipient} onChange={(e) => setRecipient(e.target.value)} />
        </label>
        <label>
          Message <input value={plaintext} onChange={(e) => setPlaintext(e.target.value)} />
        </label>
        <button onClick={send} disabled={!connected}>
          Encrypt &amp; send
        </button>
      </section>

      <h2>Activity</h2>
      <ul style={{ fontFamily: 'monospace', fontSize: 13, lineHeight: 1.6, listStyle: 'none', padding: 0 }}>
        {log.map((line, index) => (
          <li key={index}>{line}</li>
        ))}
      </ul>
    </main>
  );
}
