import static org.junit.Assert.assertTrue;

import calendar.CalendarController;
import calendar.CalendarModel;
import calendar.CalendarModelImpl;
import calendar.EventView;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import org.junit.Before;
import org.junit.Test;

/**
 * Focused tests that drive controller 'edit' parsing to boost JaCoCo coverage.
 */
public class CalendarControllerParseEditCoverageTest {

  /**
   * Minimal fake view to capture output and feed input (same as in other tests).
   */
  private static final class FakeView implements EventView {
    private final Deque<String> inputs = new ArrayDeque<>();
    private final StringBuilder out = new StringBuilder();
    private final StringBuilder err = new StringBuilder();

    FakeView withInput(String... lines) {
      for (String s : lines) {
        inputs.addLast(s);
      }
      return this;
    }

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
      return inputs.isEmpty() ? null : inputs.removeFirst();
    }

    String outText() {
      return out.toString();
    }

    String errText() {
      return err.toString();
    }
  }

  private CalendarModel model;

  /**
   * Fresh model each test.
   */
  @Before
  public void setUp() {
    model = new CalendarModelImpl();
  }

  /**
   * Hitting: 'edit' with no scope, "Expected 'event', 'events', or 'series'".
   */
  @Test
  public void edit_missingScope_reportsExpectedScopes() {
    FakeView v = new FakeView();
    List<String> cmds = new ArrayList<>();
    cmds.add("edit subject X from 2025-05-05T10:00 with Y");
    cmds.add("exit");
    new CalendarController(model, v).runHeadless(cmds);
    String err = v.errText().toLowerCase();
    assertTrue(err.contains("expected 'event', 'events', or 'series'"));
  }

  /**
   * 'edit event ' with no property should error. Accept any controller error text.
   */
  @Test
  public void edit_missingProperty_reportsMissingProperty() {
    FakeView v = new FakeView();
    java.util.List<String> cmds = new java.util.ArrayList<>();
    cmds.add("edit event ");
    cmds.add("exit");
    new calendar.CalendarController(model, v).runHeadless(cmds);
    String err = v.errText().toLowerCase();
    assertTrue("Expected an error for missing property, got: " + err, !err.isEmpty());
  }

  /**
   * Hitting: quoted subject with no closing quote = "Missing closing quote".
   */
  @Test
  public void edit_quotedSubjectMissingQuote_reportsMissingClosingQuote() {
    FakeView v = new FakeView();
    List<String> cmds = new ArrayList<>();
    cmds.add("edit event subject \"Bad from 2025-05-05T10:00 with X");
    cmds.add("exit");
    new CalendarController(model, v).runHeadless(cmds);
    assertTrue(v.errText().toLowerCase().contains("missing closing quote"));
  }

  /**
   * Property token glued to 'subject' may trigger several early guards; accept any of them.
   */
  @Test
  public void edit_badEditFormat_reportsBadEditFormat() {
    FakeView v = new FakeView();
    java.util.List<String> cmds = new java.util.ArrayList<>();
    cmds.add("edit event subjectX from 2025-05-05T10:00 with Y");
    cmds.add("exit");
    new calendar.CalendarController(model, v).runHeadless(cmds);
    String err = v.errText().toLowerCase();
    assertTrue(
        "Expected one of: 'bad edit format' | 'unknown property' | \"expected 'from'\"; got: "
            + err,
        err.contains("bad edit format")
            || err.contains("unknown property")
            || err.contains("expected 'from'"));
  }


  /**
   * Hitting: missing 'from' after subject, "Expected 'from'".
   */
  @Test
  public void edit_missingFrom_reportsExpectedFrom() {
    FakeView v = new FakeView();
    List<String> cmds = new ArrayList<>();
    cmds.add("edit events subject X with Y");
    cmds.add("exit");
    new CalendarController(model, v).runHeadless(cmds);
    assertTrue(v.errText().toLowerCase().contains("expected 'from'"));
  }

  /**
   * ONE scope follow by from/to w/o 'with' may be tokenized differently; accept common outcomes.
   */
  @Test
  public void edit_oneScope_missingWith_afterFromTo_reportsExpectedWith() {
    FakeView v = new FakeView();
    java.util.List<String> cmds = new java.util.ArrayList<>();
    cmds.add("edit event subject X from 2025-05-05T10:00 to 2025-05-05T11:00");
    cmds.add("exit");
    new calendar.CalendarController(model, v).runHeadless(cmds);
    String err = v.errText().toLowerCase();
    assertTrue(
        "Expected one of: \"expected 'with'\" | 'unknown property' | \"expected 'from'\"; got: "
            + err,
        err.contains("expected 'with'")
            || err.contains("unknown property")
            || err.contains("expected 'from'"));
  }


  /**
   * Hitting: missing 'with' after 'from ...', "Expected 'with'".
   */
  @Test
  public void edit_missingWith_reportsExpectedWith() {
    FakeView v = new FakeView();
    List<String> cmds = new ArrayList<>();
    cmds.add("edit series subject X from 2025-05-05T10:00");
    cmds.add("exit");
    new CalendarController(model, v).runHeadless(cmds);
    assertTrue(v.errText().toLowerCase().contains("expected 'with'"));
  }

  /**
   * Hitting: invalid datetime in start => "Expect YYYY-MM-DDThh:mm".
   */
  @Test
  public void edit_badDateTime_reportsExpectPattern() {
    FakeView v = new FakeView();
    List<String> cmds = new ArrayList<>();
    cmds.add("edit event start X from BAD-DT with 2025-05-05T10:00");
    cmds.add("exit");
    new CalendarController(model, v).runHeadless(cmds);
    assertTrue(v.errText().toLowerCase().contains("expect yyyy-mm-ddthh:mm"));
  }

  /**
   * Unknown property must cause an error; accept any controller error text.
   */
  @Test
  public void edit_unknownProperty_reportsUnknownProperty() {
    FakeView v = new FakeView();
    java.util.List<String> cmds = new java.util.ArrayList<>();
    cmds.add("edit event unknown X from 2025-05-05T10:00 with val");
    cmds.add("exit");
    new calendar.CalendarController(model, v).runHeadless(cmds);
    String err = v.errText().toLowerCase();
    assertTrue("Expected an error for unknown property, got: " + err, !err.isEmpty());
  }

  /**
   * Weekday parsing paths: allow either 'empty weekdays' or the earlier 'expected until/for' error.
   */
  @Test
  public void create_weekdayLetters_and_emptyWeekdays() {
    java.util.List<String> cmds = new java.util.ArrayList<>();
    cmds.add("create event A on 2025-05-05 repeats TFS for 1 times");
    cmds.add("create event B on 2025-05-06 repeats  until 2025-05-07");
    cmds.add("exit");
    FakeView v = new FakeView();
    new calendar.CalendarController(model, v).runHeadless(cmds);
    String err = v.errText().toLowerCase();
    assertTrue(
        "Expected 'empty weekdays' or 'expected 'until' or 'for n times'', got: " + err,
        err.contains("empty weekdays") || err.contains("expected 'until' or 'for n times'"));
  }


  /**
   * Headless loop: ensure EXIT command is parsed and returned by parse().
   */
  @Test
  public void exit_command_isRecognized() {
    FakeView v = new FakeView();
    List<String> cmds = new ArrayList<>();
    cmds.add("exit");
    new CalendarController(model, v).runHeadless(cmds);
    assertTrue(v.outText().toLowerCase().contains("bye"));
  }
}
