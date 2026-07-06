// REST client for the Identity & Key Registry service.

import type { PreKeyBundleUpload } from '../crypto/keys';

export interface RemotePreKeyBundle {
  userId: string;
  deviceId: number;
  registrationId: number;
  identityKey: string;
  signedPreKey: { keyId: number; publicKey: string; signature: string };
  oneTimePreKey: { keyId: number; publicKey: string } | null;
}

export interface RegisterResult {
  id: string;
  oneTimePreKeyCount: number;
}

export interface AccessToken {
  token: string;
  expiresInSeconds: number;
}

export class IdentityClient {
  constructor(private readonly baseUrl: string) {}

  async registerBundle(upload: PreKeyBundleUpload): Promise<RegisterResult> {
    const response = await fetch(`${this.baseUrl}/v1/keys`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(upload),
    });
    if (!response.ok) {
      throw new Error(`Registration failed: ${response.status}`);
    }
    return response.json() as Promise<RegisterResult>;
  }

  async fetchBundle(userId: string): Promise<RemotePreKeyBundle> {
    const response = await fetch(`${this.baseUrl}/v1/keys/${encodeURIComponent(userId)}/bundle`);
    if (!response.ok) {
      throw new Error(`Bundle fetch failed: ${response.status}`);
    }
    return response.json() as Promise<RemotePreKeyBundle>;
  }

  async requestToken(userId: string, deviceId: number): Promise<AccessToken> {
    const response = await fetch(`${this.baseUrl}/v1/auth/token`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ userId, deviceId }),
    });
    if (!response.ok) {
      throw new Error(`Token request failed: ${response.status}`);
    }
    return response.json() as Promise<AccessToken>;
  }
}
