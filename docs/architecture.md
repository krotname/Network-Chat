# Network Chat Architecture

Network Chat is a small Java 21 TCP chat application with a deliberately simple runtime shape:

```text
clients <-> ChatConnection <-> ChatProtocol <-> ChatServer
```

## Runtime Flow

1. `ChatServer` opens a `ServerSocket` on the configured port.
2. Each accepted socket is handled by a bounded client executor.
3. The server sends `NAME_REQUEST`, waits for `USER_NAME`, validates uniqueness, then responds with
   `NAME_ACCEPTED`.
4. Existing users are sent to the new client as `USER_ADDED` events.
5. `TEXT` messages are broadcast to other clients with raw text in `data` and the author in
   `sender`.
6. Closing or failed connections are removed and announced with `USER_REMOVED`.

## Message Frames

Frames are one-line UTF-8 JSON objects serialized by `ChatProtocol`.

- `type` is required for every frame.
- `data` is optional for control frames, but required and non-blank for `TEXT`.
- `sender` carries the author for `TEXT`; clients own display formatting.
- `timestamp` and `messageId` are generated when a frame is created.
- `data` is limited by `ChatMessage.MAX_DATA_LENGTH`.

## Server Limits

`ChatServerConfig` centralizes runtime limits:

- `port` - TCP port, default `1500`.
- `maxClients` - maximum concurrent client handler threads, default `100`.
- `handshakeTimeout` - maximum time to complete username registration, default `10s`.
- `readTimeout` - idle socket read timeout after handshake, default `5m`.

The legacy `new ChatServer(int port)` constructor delegates to `ChatServerConfig.ofPort(port)`.

## Client Model

`ChatClient` owns the socket lifecycle and exposes hooks for console, bot, and Swing clients.
Connection status is updated through a final template method before client-specific UI or console
side effects run, so subclasses cannot skip the shared latch/status update.
