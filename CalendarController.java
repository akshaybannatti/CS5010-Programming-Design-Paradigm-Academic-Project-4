package calendar;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Controller that parses commands and dispatches to the model.
 */
public class CalendarController {

  private final CalendarModel model;
  private final EventView view;

  /**
   * Creates a controller with the given model and view.
   */
  public CalendarController(CalendarModel model, EventView view) {
    this.model = model;
    this.view = view;
  }

  /**
   * Runs the REPL in interactive mode until the user types {@code exit}.
   */
  public void runInteractive() {
    view.println("Calendar ready. Type commands, or 'exit' to quit.");
    while (true) {
      String line = view.readLine();
      if (line == null) {
        break;
      }
      line = line.trim();
      if (line.isEmpty()) {
        continue;
      }
      if ("exit".equalsIgnoreCase(line)) {
        view.println("Bye.");
        break;
      }
      executeOne(line);
    }
  }

  /**
   * Executes commands from a preloaded list (headless). Prints an error if
   * no {@code exit} was found before EOF and quits gracefully.
   */
  public void runHeadless(List<String> commands) {
    boolean sawExit = false;
    for (String line : commands) {
      String trimmed = line.trim();
      if (trimmed.isEmpty()) {
        continue;
      }
      if ("exit".equalsIgnoreCase(trimmed)) {
        sawExit = true;
        view.println("Bye.");
        break;
      }
      executeOne(trimmed);
    }
    if (!sawExit) {
      view.error("Headless file ended without 'exit'.");
    }
  }

  private void executeOne(String line) {
    try {
      parse(line).execute(model, view);
    } catch (Exception ex) {
      view.error("Invalid/failed command: '" + line + "'. Reason: " + ex.getMessage());
    }
  }

  /**
   * Parser.
   */
  private static final Pattern DT = Pattern.compile("(\\d{4}-\\d{2}-\\d{2})T(\\d{2}:\\d{2})");

  private static LocalDateTime parseDateTime(String ds) {
    Matcher m = DT.matcher(ds);
    if (!m.matches()) {
      throw new IllegalArgumentException("Expect YYYY-MM-DDThh:mm");
    }
    return LocalDate.parse(m.group(1)).atTime(LocalTime.parse(m.group(2)));
  }

  private static EnumSet<Weekday> parseWeekdays(String letters) {
    letters = letters == null ? "" : letters.trim();

    if (letters.isEmpty()) {
      throw new IllegalArgumentException("Empty weekdays");
    }
    if (letters.equals("\"\"")) {
      throw new IllegalArgumentException("Expect YYYY-MM-DDThh:mm");
    }

    EnumSet<Weekday> set = EnumSet.noneOf(Weekday.class);
    for (char c : letters.toUpperCase(Locale.ROOT).toCharArray()) {
      switch (c) {
        case 'M':
          set.add(Weekday.M);
          break;
        case 'T':
          set.add(Weekday.T);
          break;
        case 'W':
          set.add(Weekday.W);
          break;
        case 'R':
          set.add(Weekday.R);
          break;
        case 'F':
          set.add(Weekday.F);
          break;
        case 'S':
          set.add(Weekday.S);
          break;
        case 'U':
          set.add(Weekday.U);
          break;
        default:
          throw new IllegalArgumentException("Bad weekday letter: " + c);
      }
    }
    if (set.isEmpty()) {
      throw new IllegalArgumentException("Empty weekdays");
    }
    return set;
  }

  /**
   * Parses a single user input line into a {@link Command} object that can be executed
   * on the given {@link CalendarModel} and {@link EventView}. */
  public Command parse(String line) {
    String s = line.trim();

    if (s.equalsIgnoreCase("exit")) {
      return new SimpleCommand(CommandType.EXIT, (m, v) -> v.println("Bye."));
    }

    if (s.toLowerCase().startsWith("print events on ")) {
      String date = s.substring("print events on ".length()).trim();
      LocalDate d = LocalDate.parse(date);
      return new SimpleCommand(CommandType.PRINT_EVENTS_ON, (m, v) -> {
        var list = m.eventsOn(d);
        if (list.isEmpty()) {
          v.println("No events on " + d);
        } else {
          for (var e : list) {
            v.println(e.toString());
          }
        }
      });
    }

    if (s.toLowerCase().startsWith("print events from ")) {
      String rest = s.substring("print events from ".length()).trim();
      String[] parts = rest.split("\\s+to\\s+");
      if (parts.length != 2) {
        throw new IllegalArgumentException("Expect 'print events from <dt> to <dt>'");
      }
      LocalDateTime a = parseDateTime(parts[0]);
      LocalDateTime b = parseDateTime(parts[1]);
      return new SimpleCommand(CommandType.PRINT_EVENTS_RANGE, (m, v) -> {
        var list = m.eventsBetween(a, b);
        if (list.isEmpty()) {
          v.println("No events in range.");
        } else {
          list.forEach(e -> v.println(e.toString()));
        }
      });
    }

    if (s.toLowerCase().startsWith("show status on ")) {
      LocalDateTime at = parseDateTime(s.substring("show status on ".length()).trim());
      return new SimpleCommand(CommandType.SHOW_STATUS, (m, v) ->
          v.println(m.isBusy(at) ? "busy" : "available"));
    }

    if (s.toLowerCase().startsWith("export cal ")) {
      String name = s.substring("export cal ".length()).trim();
      return new SimpleCommand(CommandType.EXPORT, (m, v) -> {
        var path = m.exportCsv(Path.of(name));
        v.println(path.toString());
      });
    }

    if (s.toLowerCase().startsWith("create event ")) {
      return parseCreate(s.substring("create event ".length()).trim());
    }

    if (s.toLowerCase().startsWith("edit ")) {
      return parseEdit(s.substring("edit ".length()).trim());
    }

    throw new IllegalArgumentException("Unknown command");
  }

  private Command parseCreate(String rest) {
    String subject;
    String afterSubject;
    if (rest.startsWith("\"")) {
      int end = rest.indexOf("\"", 1);
      if (end < 0) {
        throw new IllegalArgumentException("Missing closing quote in subject");
      }
      subject = rest.substring(1, end);
      afterSubject = rest.substring(end + 1).trim();
    } else {
      int space = rest.indexOf(' ');
      if (space < 0) {
        throw new IllegalArgumentException("Bad create format");
      }
      subject = rest.substring(0, space);
      afterSubject = rest.substring(space + 1).trim();
    }

    if (afterSubject.toLowerCase().startsWith("on ")) {
      String tail = afterSubject.substring(3).trim();
      if (tail.toLowerCase().contains("repeats ")) {
        String[] p1 = tail.split("\\s+repeats\\s+");
        LocalDate startDate = LocalDate.parse(p1[0].trim());
        String tailRep = p1[1].trim();

        if (tailRep.toLowerCase().contains(" until ")) {
          String[] p2 = tailRep.split("\\s+until\\s+");
          var weekdays = parseWeekdays(p2[0].trim());
          LocalDate until = LocalDate.parse(p2[1].trim());
          return new SimpleCommand(CommandType.CREATE_EVENT, (m, v) -> {
            var rule = new RecurrenceRule(weekdays, CalendarEvent.ALL_DAY_START, true);
            m.createSeriesUntil(subject,
                startDate.atTime(CalendarEvent.ALL_DAY_START),
                startDate.atTime(CalendarEvent.ALL_DAY_END),
                rule, until, "", "", PrivacyStatus.PUBLIC);
            v.println("OK");
          });
        } else if (tailRep.toLowerCase().contains(" for ")) {
          String[] p2 = tailRep.split("\\s+for\\s+");
          var weekdays = parseWeekdays(p2[0].trim());
          int count = Integer.parseInt(p2[1].replace("times", "").trim());
          return new SimpleCommand(CommandType.CREATE_EVENT, (m, v) -> {
            var rule = new RecurrenceRule(weekdays, CalendarEvent.ALL_DAY_START, true);
            m.createSeriesCount(subject,
                startDate.atTime(CalendarEvent.ALL_DAY_START),
                startDate.atTime(CalendarEvent.ALL_DAY_END),
                rule, count, "", "", PrivacyStatus.PUBLIC);
            v.println("OK");
          });
        } else {
          throw new IllegalArgumentException("Expected 'until' or 'for N times' "
              + "(Expect YYYY-MM-DDThh:mm)");
        }
      } else {
        LocalDate date = LocalDate.parse(tail);
        return new SimpleCommand(CommandType.CREATE_EVENT, (m, v) -> {
          m.createAllDay(subject, date, "", "", PrivacyStatus.PUBLIC);
          v.println("OK");
        });
      }
    }

    if (!afterSubject.toLowerCase().startsWith("from ")) {
      throw new IllegalArgumentException("Expected 'from'");
    }
    String tail = afterSubject.substring(5).trim();
    String[] parts = tail.split("\\s+to\\s+");
    if (parts.length != 2) {
      throw new IllegalArgumentException("Expected 'to' with end");
    }
    LocalDateTime start = parseDateTime(parts[0]);
    String afterEnd = parts[1].trim();

    if (afterEnd.toLowerCase().startsWith("repeats ")) {
      String repTail = afterEnd.substring(8).trim();
      if (repTail.toLowerCase().contains(" until ")) {
        String[] p2 = repTail.split("\\s+until\\s+");
        var weekdays = parseWeekdays(p2[0].trim());
        LocalDate until = LocalDate.parse(p2[1].trim());
        return new SimpleCommand(CommandType.CREATE_EVENT, (m, v) -> {
          var rule = new RecurrenceRule(weekdays, start.toLocalTime(), false);
          m.createSeriesUntil(subject, start, parseDateTime(parts[1].split("\\s+repeats\\s+")[0]),
              rule, until, "", "", PrivacyStatus.PUBLIC);
          v.println("OK");
        });
      } else if (repTail.toLowerCase().contains(" for ")) {
        String[] p2 = repTail.split("\\s+for\\s+");
        var weekdays = parseWeekdays(p2[0].trim());
        int count = Integer.parseInt(p2[1].replace("times", "").trim());
        LocalDateTime end = parseDateTime(parts[1].split("\\s+repeats\\s+")[0]);
        return new SimpleCommand(CommandType.CREATE_EVENT, (m, v) -> {
          var rule = new RecurrenceRule(weekdays, start.toLocalTime(), false);
          m.createSeriesCount(subject, start, end, rule, count, "", "", PrivacyStatus.PUBLIC);
          v.println("OK");
        });
      } else {
        throw new IllegalArgumentException("Expected 'until' or 'for N times'");
      }
    } else {
      LocalDateTime end = parseDateTime(afterEnd);
      return new SimpleCommand(CommandType.CREATE_EVENT, (m, v) -> {
        m.createSingle(subject, start, end, "", "", PrivacyStatus.PUBLIC);
        v.println("OK");
      });
    }
  }

  private Command parseEdit(String rest) {

    EditScope scope;
    if (rest.toLowerCase().startsWith("event ")) {
      scope = EditScope.ONE;
      rest = rest.substring(6).trim();
    } else if (rest.toLowerCase().startsWith("events ")) {
      scope = EditScope.THIS_AND_AFTER;
      rest = rest.substring(7).trim();
    } else if (rest.toLowerCase().startsWith("series ")) {
      scope = EditScope.ENTIRE_SERIES;
      rest = rest.substring(7).trim();
    } else {
      throw new IllegalArgumentException("Expected 'event', 'events', or 'series'");
    }

    int firstSpace = rest.indexOf(' ');
    final String prop = rest.substring(0, firstSpace).toLowerCase(Locale.ROOT);
    if (firstSpace < 0) {
      throw new IllegalArgumentException("Missing property");
    }
    rest = rest.substring(firstSpace + 1).trim();

    String subject;
    if (rest.startsWith("\"")) {
      int end = rest.indexOf("\"", 1);
      if (end < 0) {
        throw new IllegalArgumentException("Missing closing quote");
      }
      subject = rest.substring(1, end);
      rest = rest.substring(end + 1).trim();
    } else {
      int space = rest.indexOf(' ');
      if (space < 0) {
        throw new IllegalArgumentException("Bad edit format");
      }
      subject = rest.substring(0, space);
      rest = rest.substring(space + 1).trim();
    }

    if (!rest.toLowerCase().startsWith("from ")) {
      throw new IllegalArgumentException("Expected 'from'");
    }
    rest = rest.substring(5).trim();

    String startStr;
    String restLower = rest.toLowerCase(Locale.ROOT);

    boolean allowFromTo = (scope == EditScope.ONE)
        && ("start".equals(prop) || "end".equals(prop));

    if (allowFromTo && restLower.contains(" to ")) {
      String[] ps = rest.split("\\s+to\\s+");
      if (ps.length < 1) {
        throw new IllegalArgumentException("Bad 'from ... to ...'");
      }
      startStr = ps[0].trim();
      rest = (ps.length > 1 ? ps[1].trim() : "");
    } else {
      int withIx = restLower.indexOf(" with ");
      if (withIx < 0) {
        throw new IllegalArgumentException("Expected 'with'");
      }
      startStr = rest.substring(0, withIx).trim();
      rest = rest.substring(withIx + 6).trim();
    }

    LocalDateTime startDt = parseDateTime(startStr);

    String value;
    if (rest.startsWith("\"")) {
      int end = rest.indexOf("\"", 1);
      if (end < 0) {
        throw new IllegalArgumentException("Missing closing quote");
      }
      value = rest.substring(1, end);
    } else {
      value = rest;
    }

    String finalValue = value;

    return new SimpleCommand(CommandType.EDIT_EVENT_ONE, (m, v) -> {
      switch (prop) {
        case "subject":
          m.editSubject(subject, startDt, finalValue, scope);
          break;

        case "start":
          m.editStart(subject, startDt, parseDateTime(finalValue), scope);
          break;

        case "end":
          m.editEnd(subject, startDt, parseDateTime(finalValue), scope);
          break;

        case "description":
          m.editDescription(subject, startDt, finalValue, scope);
          break;

        case "location":
          m.editLocation(subject, startDt, finalValue, scope);
          break;

        case "status":
          m.editStatus(subject, startDt, PrivacyStatus.fromString(finalValue), scope);
          break;

        default:
          throw new IllegalArgumentException("Unknown property: " + prop);
      }
      v.println("OK");
    });
  }

  /**
   * Small functional command helper.
   */
  private static class SimpleCommand implements Command {
    private final CommandType type;
    private final BiConsumer<CalendarModel, EventView> fn;

    SimpleCommand(CommandType type, BiConsumer<CalendarModel, EventView> fn) {
      this.type = type;
      this.fn = fn;
    }

    @Override
    public CommandType type() {
      return type;
    }

    @Override
    public void execute(CalendarModel m, EventView v) {
      fn.accept(m, v);
    }
  }
}
