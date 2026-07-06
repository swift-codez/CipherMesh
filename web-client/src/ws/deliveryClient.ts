// STOMP-over-WebSocket client for the Message Delivery service.

import { Client, type IMessage } from '@stomp/stompjs';

export interface IncomingMessage {
  messageId: string;
  senderId: string;
  ciphertext: string;
  sentAt: string;
}

export class DeliveryClient {
  private readonly client: Client;

  constructor(
    brokerUrl: string,
    token: string,
    private readonly onMessage: (message: IncomingMessage) => void,
    private readonly onStatus: (connected: boolean) => void = () => {},
  ) {
    this.client = new Client({
      brokerURL: brokerUrl,
      connectHeaders: { Authorization: `Bearer ${token}` },
      reconnectDelay: 3000,
      onConnect: () => {
        this.onStatus(true);
        this.client.subscribe('/user/queue/messages', (frame: IMessage) => {
          const incoming = JSON.parse(frame.body) as IncomingMessage;
          this.onMessage(incoming);
        });
      },
      onDisconnect: () => this.onStatus(false),
      onWebSocketClose: () => this.onStatus(false),
    });
  }

  activate(): void {
    this.client.activate();
  }

  async deactivate(): Promise<void> {
    await this.client.deactivate();
  }

  send(recipientId: string, recipientDeviceId: number, ciphertext: string): void {
    this.client.publish({
      destination: '/app/send',
      body: JSON.stringify({ recipientId, recipientDeviceId, ciphertext }),
    });
  }

  ack(messageId: string): void {
    this.client.publish({
      destination: '/app/ack',
      body: JSON.stringify({ messageId }),
    });
  }
}
