package calendar;

/** Visibility of an event. */
public enum PrivacyStatus {
  PUBLIC, PRIVATE;

  /** Parse a status string; any non-{@code "private"} becomes {@link #PUBLIC}. */
  public static PrivacyStatus fromString(String s) {
    return "private".equalsIgnoreCase(s) ? PRIVATE : PUBLIC;
  }
}
