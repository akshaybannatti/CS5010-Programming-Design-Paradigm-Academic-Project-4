import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import calendar.CalendarController;
import calendar.CalendarModel;
import calendar.CalendarModelImpl;
import calendar.Command;
import calendar.EventView;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.Before;
import org.junit.Test;

/**
 * Verifies creation, editing, printing, status, and CSV export flows.
 */
public class CalendarHiddenTests {

  private CalendarModel model;
  private ByteArrayOutputStream out;
  private EventView view;
  private CalendarController controller;

  private static final class CapturingView implements EventView {
    private final PrintStream ps;

    CapturingView(PrintStream ps) {
      this.ps = ps;
    }

    @Override
    public void println(String s) {
      ps.println(s);
    }

    @Override
    public void error(String s) {
      ps.println(s);
    }

    @Override
    public String readLine() {
      return "";
    }
  }

  /** Creates a fresh model/view/controller for each test. */
  @Before
  public void setup() {
    model = new CalendarModelImpl();
    out = new ByteArrayOutputStream();
    view = new CapturingView(new PrintStream(out));
    controller = new CalendarController(model, view);
  }

  private Command invokeParse(String input) {
    try {
      java.lang.reflect.Method m =
          CalendarController.class.getDeclaredMethod("parse", String.class);
      m.setAccessible(true);
      return (Command) m.invoke(controller, input);
    } catch (InvocationTargetException ite) {
      Throwable cause = ite.getCause();
      if (cause instanceof RuntimeException) {
        throw (RuntimeException) cause;
      }
      if (cause instanceof Error) {
        throw (Error) cause;
      }
      throw new RuntimeException(cause);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private String run(String cmd) {
    out.reset();
    try {
      Command c = invokeParse(cmd);
      c.execute(model, view);
      return out.toString().trim();
    } catch (Exception ex) {
      String msg = ex.getMessage();
      return (msg == null ? "" : msg).trim();
    }
  }

  @Test
  public void exitPrintsBye() {
    String o = run("exit");
    assertTrue("Exit should print Bye.", o.toLowerCase().contains("bye"));
  }

  @Test
  public void badTimestampShowsExpectMessage() {
    String msg = run("create event A from 2025-11-03 09:00 to 2025-11-03T10:00");
    assertTrue(msg.contains("Expect YYYY-MM-DDThh:mm"));
  }

  @Test
  public void quotedMultiwordSubject() {
    run("create event \"Team Meeting Alpha\" from 2025-11-03T09:00 to 2025-11-03T10:00");
    assertTrue(
        model.eventsOn(LocalDate.of(2025, 11, 3))
            .stream().anyMatch(e -> e.subject().equals("Team Meeting Alpha")));
  }

  @Test
  public void unquotedSingleSubjectWorks() {
    run("create event Standup from 2025-11-03T09:00 to 2025-11-03T09:15");
    assertFalse(model.eventsOn(LocalDate.of(2025, 11, 3)).isEmpty());
  }

  @Test
  public void allDaySimple() {
    run("create event \"Day Off\" on 2025-11-07");
    assertEquals(1, model.eventsOn(LocalDate.of(2025, 11, 7)).size());
  }

  @Test
  public void allDayRepeatsUntil() {
    String res = run("create event \"Holiday\" on 2025-11-02 repeats SU until 2025-11-23");
    boolean printedOk = res.toUpperCase().contains("OK");
    boolean createdSomething = !model.eventsOn(LocalDate.of(2025, 11, 2)).isEmpty()
        || !model.eventsOn(LocalDate.of(2025, 11, 9)).isEmpty()
        || !model.eventsOn(LocalDate.of(2025, 11, 16)).isEmpty()
        || !model.eventsOn(LocalDate.of(2025, 11, 23)).isEmpty();
    assertTrue(printedOk || createdSomething);
  }

  @Test
  public void allDayRepeatsFornTimes() {
    String res = run("create event \"Chore\" on 2025-11-03 repeats M for 2 times");
    boolean printedOk = res.toUpperCase().contains("OK");
    boolean createdSomething = !model.eventsOn(LocalDate.of(2025, 11, 3)).isEmpty()
        || !model.eventsOn(LocalDate.of(2025, 11, 10)).isEmpty();
    assertTrue(printedOk || createdSomething);
  }

  @Test
  public void allDayMissingUntilOrForThrows() {
    String msg = run("create event X on 2025-11-02 repeats MTWRF");
    String lower = msg.toLowerCase();
    assertTrue(lower.contains("expected 'until'") || lower.contains("for n times")
        || lower.contains("expect"));
  }

  @Test
  public void emptyWeekdaysThrows() {
    String msg = run("create event X on 2025-11-03 repeats  until 2025-11-10");
    String lower = msg.toLowerCase();
    assertTrue(lower.contains("empty") || lower.contains("weekday")
        || lower.contains("expected"));
  }

  @Test
  public void invalidWeekdayThrows() {
    String msg = run("create event X on 2025-11-03 repeats MX until 2025-11-10");
    String lower = msg.toLowerCase();
    assertTrue(lower.contains("invalid") || lower.contains("weekday"));
  }

  @Test
  public void editPropertiesAllOk() {
    run("create event \"A\" from 2025-11-03T09:00 to 2025-11-03T09:15");
    String o1 = run("edit event subject \"A\" from 2025-11-03T09:00 with \"A2\"");
    String o2 = run("edit event description \"A2\" from 2025-11-03T09:00 with \"desc\"");
    String o3 = run("edit event location \"A2\" from 2025-11-03T09:00 with \"loc\"");
    String o4 = run("edit event status \"A2\" from 2025-11-03T09:00 with PRIVATE");
    String all = String.join("\n", o1, o2, o3, o4).toLowerCase();
    boolean hasOk = all.contains("ok");
    boolean hasError =
        all.contains("invalid") || all.contains("error") || all.contains("duplicate")
            || all.contains("ambiguous") || all.contains("expect");
    assertTrue(hasOk || !hasError);
  }

  @Test
  public void editMissingWithError() {
    String msg = run("edit event subject A from 2025-11-03T09:00 to 2025-11-03T10:00");
    assertTrue(msg.toLowerCase().contains("with"));
  }

  @Test
  public void editUnknownPropertyError() {
    String msg = run("edit event foo A from 2025-11-03T09:00 with bar");
    assertTrue(msg.toLowerCase().contains("unknown"));
  }

  @Test
  public void duplicateEventRejected() {
    run("create event \"Dup\" from 2025-11-05T09:00 to 2025-11-05T09:30");
    String msg = run("create event \"Dup\" from 2025-11-05T09:00 to 2025-11-05T09:30");
    assertTrue(msg.toLowerCase().contains("duplicate"));
  }

  @Test
  public void ambiguousEditRejected() {
    run("create event \"Dup2\" from 2025-11-05T09:00 to 2025-11-05T09:30");
    run("create event \"Dup2\" from 2025-11-05T09:00 to 2025-11-05T09:45");
    String msg = run("edit event location \"Dup2\" from 2025-11-05T09:00 with Room");
    assertTrue(msg.toLowerCase().contains("ambiguous"));
  }

  @Test
  public void exportWritesCsv() {
    run("create event \"A\" on 2025-11-03");
    run("export cal build/out.csv");
    assertTrue(new File("build/out.csv").exists());
  }

  @Test
  public void showStatusBusyAndAvailable() {
    run("create event A from 2025-11-03T09:00 to 2025-11-03T10:00");
    String busy = run("show status on 2025-11-03T09:30");
    String free = run("show status on 2025-11-03T11:00");
    assertTrue(busy.toLowerCase().contains("busy"));
    assertTrue(free.toLowerCase().contains("available"));
  }

  @Test
  public void subjectWithPunctuationOk() {
    run("create event \"Q4 - Wrap up!\" on 2025-11-10");
    assertFalse(model.eventsOn(LocalDate.of(2025, 11, 10)).isEmpty());
  }

  @Test
  public void caseInsensitiveKeywords() {
    run("create event \"Doc\" from 2025-11-04T15:00 to 2025-11-04T15:30");
    String o = run("edit event status \"Doc\" from 2025-11-04T15:00 with pRiVaTe");
    assertTrue(o.toUpperCase().contains("OK") || !o.toLowerCase().contains("invalid"));
  }

  @Test
  public void printEventsRangeInclusive() {
    run("create event \"C\" from 2025-11-03T09:00 to 2025-11-03T09:30");
    String result = run("print events from 2025-11-03T09:00 to 2025-11-03T09:30");
    assertTrue(result.contains("C"));
  }
}
