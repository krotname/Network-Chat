package dev.krotname.networkchat.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/** Shared helpers for console prompts used by the command line client. */
public final class ConsoleInput {
  private static final BufferedReader READER =
      new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));

  private ConsoleInput() {}

  public static String readString(String message) throws IOException {
    System.out.print(message);
    String line = READER.readLine();
    while (line != null && line.isBlank()) {
      line = READER.readLine();
    }
    return line == null ? null : line.trim();
  }

  public static String readLine() throws IOException {
    return READER.readLine();
  }

  public static int readInt(String message) throws IOException {
    while (true) {
      String input = readString(message);
      if (input == null) {
        throw new IOException("Input stream closed");
      }
      try {
        return Integer.parseInt(input);
      } catch (NumberFormatException ex) {
        System.out.println("Введите корректное число.");
      }
    }
  }
}
