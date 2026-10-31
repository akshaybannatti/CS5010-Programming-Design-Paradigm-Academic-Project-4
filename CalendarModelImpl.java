package calendar;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Default in-memory implementation of the CalendarModel.
 */
public class CalendarModelImpl implements CalendarModel {
  private final List<CalendarEvent> events = new ArrayList<>();

  private void ensureNoDuplicate(CalendarEvent candidate, CalendarEvent ignore) {
    boolean dup = events.stream()
        .filter(e -> e != ignore)
        .anyMatch(e -> e.sameKey(candidate));
    if (dup) {
      throw new IllegalStateException("Duplicate event key (subject+start+end) not allowed: "
          + candidate.subject() + " @ " + candidate.start());
    }
  }

  @Override
  public CalendarEvent createSingle(String subject, LocalDateTime start, LocalDateTime end,
                                    String d, String l, PrivacyStatus s) {
    CalendarEvent ev = new CalendarEvent(subject, start, end, d, l, s, null);
    ensureNoDuplicate(ev, null);
    events.add(ev);
    return ev;
  }

  @Override
  public CalendarEvent createAllDay(String subject, LocalDate date,
                                    String d, String l, PrivacyStatus s) {
    CalendarEvent ev = CalendarEvent.allDay(subject, date, d, l, s, null);
    ensureNoDuplicate(ev, null);
    events.add(ev);
    return ev;
  }

  @Override
  public List<CalendarEvent> createSeriesCount(String subject, LocalDateTime start,
                                               LocalDateTime end,
                                               RecurrenceRule rule, int count, String d, String l,
                                               PrivacyStatus s) {
    if (count <= 0) {
      throw new IllegalArgumentException("Count must be positive");
    }
    EventSeries ser = new EventSeries(subject, start, end, rule);
    List<CalendarEvent> mat = ser.materializeCount(count, d, l, s);
    for (CalendarEvent e : mat) {
      ensureNoDuplicate(e, null);
    }
    events.addAll(mat);
    events.sort(Comparator.comparing(CalendarEvent::start));
    return mat;
  }

  @Override
  public List<CalendarEvent> createSeriesUntil(String subject, LocalDateTime start,
                                               LocalDateTime end,
                                               RecurrenceRule rule, LocalDate until, String d,
                                               String l, PrivacyStatus s) {
    EventSeries ser = new EventSeries(subject, start, end, rule);
    List<CalendarEvent> mat = ser.materializeUntil(until, d, l, s);
    for (CalendarEvent e : mat) {
      ensureNoDuplicate(e, null);
    }
    events.addAll(mat);
    events.sort(Comparator.comparing(CalendarEvent::start));
    return mat;
  }

  private List<CalendarEvent> pickByScope(String subject, LocalDateTime startsAt, EditScope scope) {
    List<CalendarEvent> matches = events.stream()
        .filter(e -> e.subject().equalsIgnoreCase(subject) && e.start().equals(startsAt))
        .collect(Collectors.toList());
    if (matches.isEmpty()) {
      throw new NoSuchElementException("No event with subject '" + subject + "' at " + startsAt);
    }
    if (matches.size() > 1) {
      throw new IllegalStateException(
          "Ambiguous: multiple events match. Provide more precise identifiers.");
    }
    CalendarEvent anchor = matches.get(0);

    if (scope == EditScope.ONE || anchor.seriesId().isEmpty()) {
      return List.of(anchor);
    }
    UUID sid = anchor.seriesId().get();
    if (scope == EditScope.ENTIRE_SERIES) {
      return events.stream()
          .filter(e -> e.seriesId().orElse(null) != null && e.seriesId().get().equals(sid))
          .collect(Collectors.toList());
    } else {
      return events.stream()
          .filter(e -> e.seriesId().orElse(null) != null && e.seriesId().get().equals(sid))
          .filter(e -> !e.start().isBefore(anchor.start()))
          .collect(Collectors.toList());
    }
  }

  private void applyAndCheck(List<CalendarEvent> targets,
                             java.util.function.Consumer<CalendarEvent> mutator,
                             boolean detachOnStartChange) {
    for (CalendarEvent e : targets) {
      CalendarEvent snapshot =
          new CalendarEvent(e.subject(), e.start(), e.end(), e.description(), e.location(),
              e.status(), e.seriesId().orElse(null));
      mutator.accept(e);
      ensureNoDuplicate(e, snapshot == e ? null : e);
      if (detachOnStartChange) {
        e.detachFromSeries();
      }
    }
    events.sort(Comparator.comparing(CalendarEvent::start));
  }

  @Override
  public void editSubject(String subj, LocalDateTime at, String ns, EditScope s) {
    applyAndCheck(pickByScope(subj, at, s), ev -> ev.changeSubject(ns), false);
  }

  @Override
  public void editStart(String subj, LocalDateTime at, LocalDateTime ns, EditScope s) {
    boolean detach = (s != EditScope.ONE);
    applyAndCheck(pickByScope(subj, at, s), ev -> ev.changeStart(ns), detach);
  }

  @Override
  public void editEnd(String subj, LocalDateTime at, LocalDateTime ne, EditScope s) {
    applyAndCheck(pickByScope(subj, at, s), ev -> ev.changeEnd(ne), false);
  }

  @Override
  public void editDescription(String subj, LocalDateTime at, String nd, EditScope s) {
    applyAndCheck(pickByScope(subj, at, s), ev -> ev.changeDescription(nd), false);
  }

  @Override
  public void editLocation(String subj, LocalDateTime at, String nl, EditScope s) {
    applyAndCheck(pickByScope(subj, at, s), ev -> ev.changeLocation(nl), false);
  }

  @Override
  public void editStatus(String subj, LocalDateTime at, PrivacyStatus st, EditScope s) {
    applyAndCheck(pickByScope(subj, at, s), ev -> ev.changeStatus(st), false);
  }

  @Override
  public List<CalendarEvent> eventsOn(LocalDate date) {
    return events.stream()
        .filter(
            e -> !e.end().toLocalDate().isBefore(date) && !e.start().toLocalDate().isAfter(date))
        .sorted(Comparator.comparing(CalendarEvent::start))
        .collect(Collectors.toList());
  }

  @Override
  public List<CalendarEvent> eventsBetween(LocalDateTime from, LocalDateTime to) {
    return events.stream()
        .filter(e -> !(e.end().isBefore(from) || e.start().isAfter(to)))
        .sorted(Comparator.comparing(CalendarEvent::start))
        .collect(Collectors.toList());
  }

  @Override
  public boolean isBusy(LocalDateTime moment) {
    return events.stream().anyMatch(e -> !e.start().isAfter(moment) && !e.end().isBefore(moment));
  }

  @Override
  public Path exportCsv(Path file) {
    String header = "Subject,Start Date,Start Time,End Date,End Time,All Day Event"
        + ",Description,Location,Private\n";
    StringBuilder sb = new StringBuilder(header);
    for (CalendarEvent e : events) {
      boolean allDay = e.start().toLocalTime().equals(CalendarEvent.ALL_DAY_START)
          && e.end().toLocalTime().equals(CalendarEvent.ALL_DAY_END)
          && e.start().toLocalDate().equals(e.end().toLocalDate());
      String line = String.join(",",
          escape(e.subject()),
          e.start().toLocalDate().toString(),
          allDay ? "" : e.start().toLocalTime().toString(),
          e.end().toLocalDate().toString(),
          allDay ? "" : e.end().toLocalTime().toString(),
          allDay ? "True" : "False",
          escape(e.description()),
          escape(e.location()),
          e.status() == PrivacyStatus.PRIVATE ? "True" : "False"
      );
      sb.append(line).append("\n");
    }
    try {
      Files.createDirectories(file.getParent());
      Files.write(file, sb.toString().getBytes(StandardCharsets.UTF_8));
      return file.toAbsolutePath();
    } catch (Exception ex) {
      throw new RuntimeException("Failed to export CSV: " + ex.getMessage(), ex);
    }
  }

  private static String escape(String s) {
    if (s == null) {
      return "";
    }
    boolean need = s.contains(",") || s.contains("\"") || s.contains("\n") || s.contains("\r");
    String val = s.replace("\"", "\"\"");
    return need ? "\"" + val + "\"" : val;
  }

  @Override
  public Optional<CalendarEvent> findUnique(String subject, LocalDateTime startsAt) {
    List<CalendarEvent> matches = events.stream()
        .filter(e -> e.subject().equalsIgnoreCase(subject) && e.start().equals(startsAt))
        .collect(Collectors.toList());
    return matches.size() == 1 ? Optional.of(matches.get(0)) : Optional.empty();
  }
}
