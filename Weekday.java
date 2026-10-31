package calendar;

/**
 * Days of week using letters M T W R F S U.
 */
public enum Weekday {
  M, T, W, R, F, S, U;

  /**
   * Convert {@link java.time.DayOfWeek} to our letter enum.
   */
  @SuppressWarnings("java:S1301")
  public static Weekday fromJava(java.time.DayOfWeek dow) {
    if (dow == null) {
      throw new IllegalArgumentException("Unexpected DayOfWeek: null");
    }
    switch (dow) {
      case MONDAY:
        return M;
      case TUESDAY:
        return T;
      case WEDNESDAY:
        return W;
      case THURSDAY:
        return R;
      case FRIDAY:
        return F;
      case SATURDAY:
        return S;
      case SUNDAY:
        return U;
      default:
        throw new IllegalArgumentException("Unexpected DayOfWeek: " + dow);
    }
  }
}
