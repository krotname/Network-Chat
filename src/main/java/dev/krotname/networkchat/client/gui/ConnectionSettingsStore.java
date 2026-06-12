package dev.krotname.networkchat.client.gui;

import dev.krotname.networkchat.network.ChatServerConfig;
import java.util.Objects;
import java.util.prefs.Preferences;

/** Stores the last successful GUI connection settings in user preferences. */
public final class ConnectionSettingsStore {
  private static final String NODE_NAME = "dev.krotname.networkchat.gui";
  private static final String KEY_SERVER_ADDRESS = "serverAddress";
  private static final String KEY_SERVER_PORT = "serverPort";
  private static final String KEY_USER_NAME = "userName";

  private final Preferences preferences;

  public ConnectionSettingsStore() {
    this(Preferences.userRoot().node(NODE_NAME));
  }

  public ConnectionSettingsStore(Preferences preferences) {
    this.preferences = Objects.requireNonNull(preferences, "preferences");
  }

  public ConnectionSettings load() {
    return new ConnectionSettings(
        preferences.get(KEY_SERVER_ADDRESS, ConnectionSettings.DEFAULT_SERVER_ADDRESS),
        preferences.getInt(KEY_SERVER_PORT, ChatServerConfig.DEFAULT_PORT),
        preferences.get(KEY_USER_NAME, ConnectionSettings.DEFAULT_USER_NAME),
        "");
  }

  public void save(ConnectionSettings settings) {
    Objects.requireNonNull(settings, "settings");
    preferences.put(KEY_SERVER_ADDRESS, settings.serverAddress());
    preferences.putInt(KEY_SERVER_PORT, settings.serverPort());
    preferences.put(KEY_USER_NAME, settings.userName());
  }
}
