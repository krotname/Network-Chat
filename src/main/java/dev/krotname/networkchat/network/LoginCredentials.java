package dev.krotname.networkchat.network;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/** Encodes optional account tokens into the existing USER_NAME handshake frame. */
public record LoginCredentials(String userName, String token) {
  private static final String TOKEN_SEPARATOR = "|";

  public LoginCredentials {
    if (userName != null) {
      userName = userName.trim();
    }
  }

  public static String encode(String userName, String token) {
    if (token == null || token.isBlank()) {
      return userName;
    }
    String encodedToken =
        Base64.getEncoder().encodeToString(token.getBytes(StandardCharsets.UTF_8));
    return userName + TOKEN_SEPARATOR + encodedToken;
  }

  public static LoginCredentials parse(String value) {
    if (value == null) {
      return new LoginCredentials("", null);
    }
    String trimmed = value.trim();
    int separator = trimmed.indexOf(TOKEN_SEPARATOR);
    if (separator < 0) {
      return new LoginCredentials(trimmed, null);
    }
    String encodedToken = trimmed.substring(separator + TOKEN_SEPARATOR.length());
    try {
      byte[] decoded = Base64.getDecoder().decode(encodedToken);
      return new LoginCredentials(
          trimmed.substring(0, separator), new String(decoded, StandardCharsets.UTF_8));
    } catch (IllegalArgumentException ex) {
      return new LoginCredentials(trimmed.substring(0, separator), "");
    }
  }
}
