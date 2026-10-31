import static org.junit.Assert.assertTrue;

import calendar.CalendarController;
import calendar.CalendarModel;
import calendar.CalendarModelImpl;
import calendar.CliView;
import calendar.EventView;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests to exercise controller parsing and run loops for JaCoCo coverage.
 */
public class CalendarControllerTest {

  /**
   * Minimal fake view to capture output and feed input.
   */
  private static final class FakeView implements EventView {
    private final Deque<String> inputs = new ArrayDeque<>();
    private final StringBuilder out = new StringBuilder();
    private final StringBuilder err = new StringBuilder();

    /**
     * Queue input lines for {@link #readLine()}.
     */
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

    /**
     * Captured stdout text.
     */
    String outText() {
      return out.toString();
    }

    /**
     * Captured error text.
     */
    String errText() {
      return err.toString();
    }
  }

  private CalendarModel model;

  /**
   * Fresh model per test.
   */
  @Before
  public void setUp() {
    model = new CalendarModelImpl();
  }

  /**
   * Covers the interactive loop: greeting, exit detection, bye.
   */
  @Test
  public void interactive_showsGreetingAndExits() {
    String input = "exit\n";
    ByteArrayInputStream is =
        new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8));
    EventView view = new CliView(is);
    CalendarController c = new CalendarController(model, view);
    c.runInteractive();
  }

  /**
   * Headless: ends without exit, prints graceful error.
   */
  @Test
  public void headless_withoutExit_printsError() {
    FakeView v = new FakeView();
    CalendarController c = new CalendarController(model, v);
    List<String> cmds = new ArrayList<>();
    cmds.add("create event \"A\" on 2025-05-05");
    c.runHeadless(cmds);
    assertTrue(v.errText().toLowerCase().contains("without 'exit'"));
  }

  /**
   * Create single, query, status, export: exercises parseCreate/dateTime and export path.
   */
  @Test
  public void headless_createSingle_thenQueryAndExport() throws Exception {
    FakeView v = new FakeView();
    List<String> cmds = new ArrayList<>();
    cmds.add("create event \"A\" from 2025-05-05T10:00 to 2025-05-05T11:00");
    cmds.add("print events on 2025-05-05");
    cmds.add("print events from 2025-05-05T09:00 to 2025-05-05T12:00");
    cmds.add("show status on 2025-05-05T10:30");
    cmds.add("export cal build/ctrl-cal.csv");
    cmds.add("exit");
    CalendarController c = new CalendarController(model, v);
    c.runHeadless(cmds);

    String out = v.outText();
    String[] lines = out.split("\\R");
    String lastPath = "";
    for (String s : lines) {
      if (s.endsWith(".csv")) {
        lastPath = s.trim();
      }
    }
    if (!lastPath.isEmpty()) {
      assertTrue(Files.exists(Path.of(lastPath)));
    }
    assertTrue(out.toLowerCase().contains("busy"));
  }

  /**
   * Create ALL-DAY with repeats until and range print: hits "on ... repeats ... until".
   */
  @Test
  public void headless_createAllDayRepeatsUntil_printRange() {
    FakeView v = new FakeView();
    List<String> cmds = new ArrayList<>();
    cmds.add("create event \"Lectures\" on 2025-05-05 repeats MW until 2025-05-14");
    cmds.add("print events from 2025-05-05T00:00 to 2025-05-15T00:00");
    cmds.add("exit");
    CalendarController c = new CalendarController(model, v);
    c.runHeadless(cmds);
    String out = v.outText();
    assertTrue(out.contains("starting on"));
  }

  /**
   * Create ALL-DAY series with 'repeats MRU for N times'; verify created via model.
   */
  @Test
  public void headless_seriesForN_allDay_ok() {
    CalendarControllerTest.FakeView v = new CalendarControllerTest.FakeView();
    java.util.List<String> cmds = new java.util.ArrayList<>();
    cmds.add("create event \"Standup\" on 2025-05-05 repeats MRU for 4 times");
    cmds.add("print events on 2025-05-05");
    cmds.add("exit");

    CalendarController c = new CalendarController(model, v);
    c.runHeadless(cmds);

    org.junit.Assert.assertTrue("Controller reported errors: " + v.errText(),
        v.errText().isEmpty());
    java.util.List<calendar.CalendarEvent> day =
        model.eventsOn(java.time.LocalDate.of(2025, 5, 5));
    org.junit.Assert.assertFalse("Expected at least one Standup on 2025-05-05", day.isEmpty());
    org.junit.Assert.assertTrue(day.stream().anyMatch(e -> e.subject().equals("Standup")));
  }

  /**
   * Create timed single, then print range and status.
   */
  @Test
  public void headless_timedSingle_printAndStatus() {
    CalendarControllerTest.FakeView v = new CalendarControllerTest.FakeView();
    java.util.List<String> cmds = new java.util.ArrayList<>();
    cmds.add("create event \"First\" from 2025-05-05T10:00 to 2025-05-05T11:00");
    cmds.add("print events from 2025-05-05T09:00 to 2025-05-05T12:00");
    cmds.add("show status on 2025-05-05T10:30");
    cmds.add("exit");

    CalendarController c = new CalendarController(model, v);
    c.runHeadless(cmds);

    org.junit.Assert.assertTrue("Controller reported errors: " + v.errText(),
        v.errText().isEmpty());
    String out = v.outText().toLowerCase();
    org.junit.Assert.assertTrue(out.contains("starting on"));
    org.junit.Assert.assertTrue("Expected busy during event", out.contains("busy"));
    java.util.Optional<calendar.CalendarEvent> ev =
        model.findUnique("First", java.time.LocalDateTime.of(2025, 5, 5, 10, 0));
    org.junit.Assert.assertTrue("Expected event present", ev.isPresent());
  }


  /**
   * Error paths: unknown command, bad weekdays letter, missing quote, wrong formats.
   */
  @Test
  public void headless_errorsAreReported() {
    FakeView v = new FakeView();
    List<String> cmds = new ArrayList<>();
    cmds.add("frobnicate stuff");
    cmds.add("create event X on 2025-05-05 repeats MQ until 2025-05-06");
    cmds.add("create event \"Bad on 2025-05-05");
    cmds.add("print events from 2025-05-05T10:00 2025-05-05T11:00");
    cmds.add("edit event subject X from 2025-05-05T10:00");
    cmds.add("exit");

    CalendarController c = new CalendarController(model, v);
    c.runHeadless(cmds);

    String err = v.errText().toLowerCase();
    assertTrue(err.contains("invalid/failed command"));
    assertTrue(err.contains("bad weekday letter"));
    assertTrue(err.contains("missing closing quote")
        || err.contains("bad create format"));
    assertTrue(err.contains("expect 'print events from <dt> to <dt>'")
        || err.contains("expected 'to' with end"));
    assertTrue(err.contains("expected 'with'"));
  }

  /**
   * Status available branch (no events at the moment).
   */
  @Test
  public void headless_statusAvailable() {
    FakeView v = new FakeView();
    List<String> cmds = new ArrayList<>();
    cmds.add("show status on " + LocalDateTime.of(2025, 5, 5, 1, 0));
    cmds.add("exit");

    CalendarController c = new CalendarController(model, v);
    c.runHeadless(cmds);

    assertTrue(v.outText().toLowerCase().contains("available"));
  }

  /**
   * Print 'on' with empty schedule branch shows a 'no events' message.
   */
  @Test
  public void headless_printOn_noEventsShowsMessage() {
    FakeView v = new FakeView();
    List<String> cmds = new ArrayList<>();
    cmds.add("print events on " + LocalDate.of(2025, 5, 5));
    cmds.add("exit");

    CalendarController c = new CalendarController(model, v);
    c.runHeadless(cmds);

    assertTrue(v.outText().toLowerCase().contains("no events on"));
  }

  /**
   * Print range with empty schedule branch shows a 'no events' message.
   */
  @Test
  public void headless_printRange_noEventsShowsMessage() {
    FakeView v = new FakeView();
    List<String> cmds = new ArrayList<>();
    cmds.add("print events from 2025-05-05T00:00 to 2025-05-05T01:00");
    cmds.add("exit");

    CalendarController c = new CalendarController(model, v);
    c.runHeadless(cmds);

    assertTrue(v.outText().toLowerCase().contains("no events in range"));
  }
}
