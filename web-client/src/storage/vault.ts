// IndexedDB-backed private-key vault. CryptoKey objects are stored directly via
// structured clone, so raw private key bytes never surface in JavaScript. This
// is the browser equivalent of a keystore: the private halves of the identity,
// signed pre-key, and one-time pre-keys live here and never leave the device.

import type { DeviceSecrets } from '../crypto/keys';

const DB_NAME = 'ciphermesh';
const STORE = 'vault';
const DEVICE_KEY = 'device-secrets';
const VERSION = 1;

function openDb(): Promise<IDBDatabase> {
  return new Promise((resolve, reject) => {
    const request = indexedDB.open(DB_NAME, VERSION);
    request.onupgradeneeded = () => {
      if (!request.result.objectStoreNames.contains(STORE)) {
        request.result.createObjectStore(STORE);
      }
    };
    request.onsuccess = () => resolve(request.result);
    request.onerror = () => reject(request.error);
  });
}

async function put<T>(key: string, value: T): Promise<void> {
  const db = await openDb();
  await new Promise<void>((resolve, reject) => {
    const tx = db.transaction(STORE, 'readwrite');
    tx.objectStore(STORE).put(value, key);
    tx.oncomplete = () => resolve();
    tx.onerror = () => reject(tx.error);
  });
  db.close();
}

async function get<T>(key: string): Promise<T | undefined> {
  const db = await openDb();
  const value = await new Promise<T | undefined>((resolve, reject) => {
    const tx = db.transaction(STORE, 'readonly');
    const request = tx.objectStore(STORE).get(key);
    request.onsuccess = () => resolve(request.result as T | undefined);
    request.onerror = () => reject(request.error);
  });
  db.close();
  return value;
}

export function saveDeviceSecrets(secrets: DeviceSecrets): Promise<void> {
  return put(DEVICE_KEY, secrets);
}

export function loadDeviceSecrets(): Promise<DeviceSecrets | undefined> {
  return get<DeviceSecrets>(DEVICE_KEY);
}
