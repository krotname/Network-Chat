package dev.krotname.networkchat.network;

import java.util.Map;
import java.util.StringJoiner;

/** Minimal JSON-line formatter for operator-readable runtime events. */
final class StructuredLog {
  private StructuredLog() {}

  static String event(String name, Map<String, ?> fields) {
    StringJoiner joiner = new StringJoiner(",", "{", "}");
    joiner.add("\"event\":" + json(name));
    for (Map.Entry<String, ?> entry : fields.entrySet()) {
      joiner.add(json(entry.getKey()) + ":" + jsonValue(entry.getValue()));
    }
    return joiner.toString();
  }

  private static String jsonValue(Object value) {
    if (value == null) {
      return "null";
    }
    if (value instanceof Number || value instanceof Boolean) {
      return value.toString();
    }
    return json(value.toString());
  }

  private static String json(String value) {
    return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
  }
}
