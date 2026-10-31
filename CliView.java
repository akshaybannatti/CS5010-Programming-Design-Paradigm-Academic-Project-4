package calendar;

import java.util.Scanner;

/** Simple console view implementing EventView. */
public class CliView implements EventView {
  private final Scanner in;

  /** Constructs a CLI view reading from the given InputStream. */
  public CliView(java.io.InputStream is) {
    this.in = new Scanner(is);
  }

  @Override
  public void println(String s) {
    System.out.println(s);
  }

  @Override
  public void error(String s) {
    System.err.println(s);
  }

  @Override
  public String readLine() {
    return in.hasNextLine() ? in.nextLine() : null;
  }
}
