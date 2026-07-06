# delivery-service

The Message Delivery service. Routes end-to-end encrypted envelopes between
clients over WebSocket/STOMP. It moves opaque ciphertext and never decrypts it.

## Responsibilities

- Terminate client WebSocket/STOMP connections (`/ws`), authenticated by the
  JWT issued by the identity service
- Track which instance each device is connected to (Redis session registry)
- Ingest sent messages to Kafka (`messages.inbound`) and route them to the
  recipient's live session
- Forward across instances via Redis pub/sub when the recipient is elsewhere
- Store-and-forward to a durable per-device mailbox when the recipient is offline
- Drain the mailbox on reconnect and emit delivery receipts (`messages.receipts`)

## Routing decision

For each inbound envelope the router looks up the recipient's session:

| Recipient state | Action |
| --- | --- |
| connected to this instance | deliver over the local WebSocket |
| connected to another instance | forward via `cm:instance:<id>` (Redis pub/sub) |
| offline | append to `cm:mailbox:<userId>:<deviceId>` (Redis list) |

## Client destinations (STOMP)

```
CONNECT           header: Authorization: Bearer <jwt>
SEND  /app/send   { recipientId, recipientDeviceId, ciphertext }
SEND  /app/ack    { messageId }
SUB   /user/queue/messages   receive delivered ciphertext
```

## Run locally

```bash
docker compose up -d redis kafka
mvn -pl delivery-service spring-boot:run
```

Listens on `:8082`.
