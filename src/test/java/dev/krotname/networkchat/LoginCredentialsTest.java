package dev.krotname.networkchat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import dev.krotname.networkchat.network.LoginCredentials;
import org.junit.jupiter.api.Test;

class LoginCredentialsTest {

  @Test
  void keepsPlainUserNameCompatibleWhenTokenIsBlank() {
    LoginCredentials credentials = LoginCredentials.parse(LoginCredentials.encode("alice", ""));

    assertEquals("alice", credentials.userName());
    assertNull(credentials.token());
  }

  @Test
  void roundTripsTokenWithoutLeakingDelimiterCharacters() {
    LoginCredentials credentials =
        LoginCredentials.parse(LoginCredentials.encode("alice", "secret|with:chars"));

    assertEquals("alice", credentials.userName());
    assertEquals("secret|with:chars", credentials.token());
  }

  @Test
  void parsesNullAndInvalidEncodedTokenSafely() {
    LoginCredentials empty = LoginCredentials.parse(null);
    LoginCredentials invalid = LoginCredentials.parse("alice|not base64");

    assertEquals("", empty.userName());
    assertNull(empty.token());
    assertEquals("alice", invalid.userName());
    assertEquals("", invalid.token());
  }
}
