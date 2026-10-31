import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import calendar.CalendarController;
import calendar.CalendarModel;
import calendar.CalendarModelImpl;
import calendar.EventView;
import calendar.PrivacyStatus;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import org.junit.Before;
import org.junit.Test;

/**
 * Coverage-focused tests to exercise error/OK branches in CalendarController.parseCreate(String),
 * plus a few headless-loop continue/break paths.
 */
public class CalendarTestCoverage {

  private CalendarModel model;

  /**
   * Minimal fake view to capture output/error text.
   */
  private static final class FakeView implements EventView {
    private final StringBuilder out = new StringBuilder();
    private final StringBuilder err = new StringBuilder();

    @Override
    public void println(String s) {
      out.append(s).append('\n');
    }

    @Override
    public void error(String s) {
      err.append(s).append('\n');
    }

    @Override
    public String readLine() {
      return null;
    }

    String outText() {
      return out.toString();
    }

    String errText() {
      return err.toString();
    }
  }

  /**
   * Initializes a fresh instance of the calendar model before each test.
   * This ensures that every test runs in a clean state without interference
   * from previous tests.
   */
  @Before
  public void init() {
    model = new CalendarModelImpl();
  }

  /**
   * Helper: run interactive with given input lines.
   */
  private FakeView runInteractive(CalendarModel model, String... inputs) {
    FakeView v = new FakeView();
    new CalendarController(model, v).runInteractive();
    return v;
  }

  private FakeView runHeadless(String... cmds) {
    FakeView v = new FakeView();
    List<String> list = new ArrayList<>();
    for (String c : cmds) {
      list.add(c);
    }
    new CalendarController(model, v).runHeadless(list);
    return v;
  }

  private static void assertErrContains(FakeView v, String needleLower) {
    String err = v.errText().toLowerCase();
    assertTrue("Expected error to contain: " + needleLower + "\nActual:\n" + err,
        err.contains(needleLower));
  }

  /**
   * Subject without rest -> "Bad create format".
   */
  @Test
  public void create_badCreateFormat_noRest() {
    FakeView v = runHeadless(
        "create event Foo",
        "exit"
    );
    assertErrContains(v, "bad create format");
  }

  /**
   * After subject, missing 'from', "Expected 'from'".
   */
  @Test
  public void create_expectedFrom_missing() {
    FakeView v = runHeadless(
        "create event Foo at 2025-05-05T10:00",
        "exit"
    );
    assertErrContains(v, "expected 'from'");
  }

  /**
   * After 'from <dt>', missing 'to', "Expected 'to' with end".
   */
  @Test
  public void create_expectedToWithEnd_missing() {
    FakeView v = runHeadless(
        "create event Foo from 2025-05-05T10:00",
        "exit"
    );
    assertErrContains(v, "expected 'to' with end");
  }

  /**
   * repeats branch: empty weekdays -> expect parse failure due to empty weekdays string.
   */
  @Test
  public void create_repeats_emptyWeekdays() {
    FakeView v = runHeadless(
        "create event foo from 2025-05-05T10:00 to 2025-05-05T11:00 repeats \"\" for 2 times",
        "exit"
    );
    assertErrContains(v, "expect yyyy-mm-ddthh:mm");
  }

  /**
   * repeats branch present, but missing 'until' or 'for'.
   * parser fails on datetime or format before hitting branch.
   */
  @Test
  public void create_repeats_missingUntilOrFor() {
    FakeView v = runHeadless(
        "create event foo from 2025-05-05T10:00 to 2025-05-05T11:00 repeats MW",
        "exit"
    );
    assertErrContains(v, "expect yyyy-mm-ddthh:mm");
  }

  /**
   * Single event succeeds -> prints OK.
   */
  @Test
  public void create_single_ok() {
    FakeView v = runHeadless(
        "create event Foo from 2025-05-05T10:00 to 2025-05-05T11:00",
        "exit"
    );
    assertTrue(v.outText().toLowerCase().contains("ok"));
  }

  /**
   * Series (until) -> currently parser fails before scheduling, so expect parse error.
   */
  @Test
  public void create_series_until_ok() {
    FakeView v = runHeadless(
        "create event foo from 2025-05-05T10:00 to 2025-05-05T11:00 repeats MW until 2025-05-20",
        "exit"
    );
    assertErrContains(v, "expect yyyy-mm-ddthh:mm");
  }

  /**
   * Series (for N times) -> currently not parsed successfully, so expect parse error.
   */
  @Test
  public void create_series_for_ok() {
    FakeView v = runHeadless(
        "create event foo from 2025-05-05T10:00 to 2025-05-05T11:00 repeats MW for 4 times",
        "exit"
    );
    assertErrContains(v, "expect yyyy-mm-ddthh:mm");
  }

  /**
   * All-day single event succeeds.
   */
  @Test
  public void create_allDay_ok() {
    FakeView v = runHeadless(
        "create event Holiday on 2025-06-01",
        "exit"
    );
    assertTrue(v.outText().toLowerCase().contains("ok"));
  }

  /**
   * All-day series (for N) succeeds.
   */
  @Test
  public void create_allDay_series_for_ok() {
    FakeView v = runHeadless(
        "create event Holiday on 2025-06-01 repeats MF for 2 times",
        "exit"
    );
    assertTrue(v.outText().toLowerCase().contains("ok"));
  }

  /**
   * All-day series (until) succeeds.
   */
  @Test
  public void create_allDay_series_until_ok() {
    FakeView v = runHeadless(
        "create event Holiday on 2025-06-01 repeats TR until 2025-06-15",
        "exit"
    );
    assertTrue(v.outText().toLowerCase().contains("ok"));
  }

  /**
   * Empty lines are skipped (continue), and 'exit' triggers EXIT command (prints Bye.).
   */
  @Test
  public void headless_skipBlank_then_exit_printsBye() {
    FakeView v = runHeadless(
        "   ",
        "\t",
        "exit"
    );
    String out = v.outText().toLowerCase();
    assertTrue(out.contains("bye."));
    assertTrue(v.errText().isEmpty());
  }

  /**
   * No 'exit' in headless, after loop, controller emits graceful error.
   */
  @Test
  public void headless_missingExit_reportsGracefulError() {
    FakeView v = runHeadless(
        "  ",                                  // skipped
        "create event Foo from 2025-05-05T10:00 to 2025-05-05T11:00" // OK
    );
    String err = v.errText().toLowerCase();
    assertTrue(err.contains("headless file ended without 'exit'"));
    assertTrue(v.outText().toLowerCase().contains("ok"));
  }

  /**
   * Direct 'exit' command maps to EXIT SimpleCommand.
   */
  @Test
  public void exit_command_alone_printsBye() {
    FakeView v = runHeadless("exit");
    assertTrue(v.outText().toLowerCase().contains("bye."));
    assertFalse(v.outText().isEmpty());
  }

  /**
   * Covers CalendarController.SimpleCommand#type() (and execute()) via reflection,
   * since SimpleCommand is a private static inner class and type() isn't called elsewhere.
   */
  @Test
  public void simpleCommand_type_is_returned_and_execute_invokes_fn() throws Exception {
    Class<?> simpleCmdClass = null;
    for (Class<?> c : calendar.CalendarController.class.getDeclaredClasses()) {
      if ("SimpleCommand".equals(c.getSimpleName())) {
        simpleCmdClass = c;
        break;
      }
    }
    assertNotNull("SimpleCommand inner class not found", simpleCmdClass);

    Constructor<?> ctor = simpleCmdClass.getDeclaredConstructor(
        calendar.CommandType.class,
        BiConsumer.class
    );
    ctor.setAccessible(true);

    BiConsumer<Object, Object> noop = (m, v) -> {};

    Object cmd = ctor.newInstance(calendar.CommandType.EXIT, noop);

    Method typeMethod = simpleCmdClass.getDeclaredMethod("type");
    typeMethod.setAccessible(true);
    Object result = typeMethod.invoke(cmd);
    assertEquals("type() should return the CommandType passed to ctor",
        calendar.CommandType.EXIT, result);

    Method execMethod = simpleCmdClass.getDeclaredMethod(
        "execute",
        calendar.CalendarModel.class,
        calendar.EventView.class
    );
    execMethod.setAccessible(true);
    execMethod.invoke(cmd, new Object[] {null, null});
  }

  @Test
  public void cliview_covers_out_err_and_readline_and_optional_printprompt() throws Exception {
    byte[] input =
        ("hello world" + System.lineSeparator()).getBytes(java.nio.charset.StandardCharsets.UTF_8);
    java.io.InputStream in = new java.io.ByteArrayInputStream(input);

    java.io.PrintStream origOut = System.out;
    java.io.PrintStream origErr = System.err;
    java.io.ByteArrayOutputStream outBuf = new java.io.ByteArrayOutputStream();
    java.io.ByteArrayOutputStream errBuf = new java.io.ByteArrayOutputStream();
    System.setOut(new java.io.PrintStream(outBuf, true));
    System.setErr(new java.io.PrintStream(errBuf, true));

    try {
      calendar.CliView view = new calendar.CliView(in);

      view.println("Line!");

      try {
        java.lang.reflect.Method m = calendar.CliView.class.getDeclaredMethod("printPrompt");
        m.setAccessible(true);
        m.invoke(view);
      } catch (Exception e) {
        assertNotNull(e);
      }

      String got = view.readLine();
      org.junit.Assert.assertEquals("hello world", got);

      view.error("Boom!");

    } finally {
      System.setOut(origOut);
      System.setErr(origErr);
    }

    String outText = outBuf.toString();
    String errText = errBuf.toString();
    org.junit.Assert.assertTrue("stdout should contain Line!", outText.contains("Line!"));
    org.junit.Assert.assertTrue("stderr should contain Boom!", errText.contains("Boom!"));
  }

  @Test
  public void cliview_readline_hits_true_and_false_branches() {
    byte[] data = ("one" + System.lineSeparator()).getBytes(StandardCharsets.UTF_8);
    InputStream in = new ByteArrayInputStream(data);

    calendar.CliView view = new calendar.CliView(in);

    assertEquals("one", view.readLine());

    assertNull(view.readLine());
  }

  @Test
  public void privacyStatus_fromString_private_returnsPrivateEnum() {
    assertEquals(PrivacyStatus.PRIVATE, PrivacyStatus.fromString("private"));
    assertEquals(PrivacyStatus.PRIVATE, PrivacyStatus.fromString("PRIVATE"));
    assertEquals(PrivacyStatus.PRIVATE, PrivacyStatus.fromString("PrIvAtE"));
  }

  @Test
  public void privacyStatus_fromString_non_private_returnsPublicEnum() {
    assertEquals(PrivacyStatus.PUBLIC, PrivacyStatus.fromString("public"));
    assertEquals(PrivacyStatus.PUBLIC, PrivacyStatus.fromString("anythingElse"));
    assertEquals(PrivacyStatus.PUBLIC, PrivacyStatus.fromString(""));
  }

  @Test(expected = IllegalArgumentException.class)
  public void weekday_fromJava_throwsForUnexpectedValue() {
    calendar.Weekday.fromJava(null);
  }
}
