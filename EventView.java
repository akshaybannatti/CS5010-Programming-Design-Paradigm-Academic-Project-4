package calendar;

/** Small view abstraction to allow CLI or other UIs. */
public interface EventView {
  /** Print a normal line to the user. */
  void println(String s);

  /** Print an error line to the user. */
  void error(String s);

  /** Read one input line (interactive mode); may return null at EOF. */
  String readLine();
}
