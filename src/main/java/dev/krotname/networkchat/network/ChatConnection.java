package dev.krotname.networkchat.network;

import dev.krotname.networkchat.protocol.ChatMessage;
import dev.krotname.networkchat.protocol.ChatProtocol;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/** A small socket abstraction that serializes protocol messages over text frames. */
public final class ChatConnection implements Closeable {
  private final Socket socket;
  private final BufferedWriter writer;
  private final BufferedReader reader;

  @SuppressFBWarnings(
      value = "EI_EXPOSE_REP2",
      justification =
          "Socket is stored for its lifetime to preserve streaming lifecycle semantics.")
  public ChatConnection(Socket socket) throws IOException {
    this.socket = socket;
    this.writer =
        new BufferedWriter(
            new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
    this.reader =
        new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
  }

  public void send(ChatMessage message) throws IOException {
    synchronized (writer) {
      writer.write(ChatProtocol.encode(message));
      writer.newLine();
      writer.flush();
    }
  }

  /**
   * Reads exactly one protocol frame from the socket. If the remote endpoint closed the connection,
   * EOF is translated into IOException so all callers follow one error-handling path.
   */
  public ChatMessage receive() throws IOException {
    synchronized (reader) {
      String line = reader.readLine();
      if (line == null) {
        throw new EOFException("Connection closed by peer");
      }
      return ChatProtocol.decode(line);
    }
  }

  @Override
  public void close() throws IOException {
    IOException first = null;
    try {
      writer.close();
    } catch (IOException ex) {
      first = ex;
    }
    try {
      reader.close();
    } catch (IOException ex) {
      if (first == null) {
        first = ex;
      }
    }
    socket.close();
    if (first != null) {
      throw first;
    }
  }
}
