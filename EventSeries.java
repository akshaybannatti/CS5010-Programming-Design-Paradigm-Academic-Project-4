package calendar;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

/**
 * Pure value object used only at creation time to generate occurrences.
 */
final class EventSeries {
  final UUID id = UUID.randomUUID();
  final String subject;
  final LocalDateTime start;
  final LocalDateTime end;
  final RecurrenceRule rule;

  EventSeries(String subject, LocalDateTime start, LocalDateTime end, RecurrenceRule rule) {
    if (!start.toLocalDate().equals(end.toLocalDate())) {
      throw new IllegalArgumentException("Series event must start and end the same day.");
    }
    if (!start.toLocalTime().equals(rule.seriesStartTime())) {
      throw new IllegalArgumentException("All events in a series must share the same start time.");
    }
    this.subject = subject;
    this.start = start;
    this.end = end;
    this.rule = rule;
  }

  List<CalendarEvent> materializeCount(int count, String desc, String loc, PrivacyStatus status) {
    List<CalendarEvent> out = new ArrayList<>();
    LocalDate cursor = start.toLocalDate();
    int made = 0;
    while (made < count) {
      if (rule.matches(cursor)) {
        LocalDateTime s = cursor.atTime(start.toLocalTime());
        LocalDateTime e = cursor.atTime(end.toLocalTime());
        out.add(new CalendarEvent(subject, s, e, desc, loc, status, id));
        made++;
      }
      cursor = cursor.plusDays(1);
    }
    return out;
  }

  List<CalendarEvent> materializeUntil(LocalDate untilInclusive, String desc, String loc,
                                       PrivacyStatus status) {
    List<CalendarEvent> out = new ArrayList<>();
    LocalDate cursor = start.toLocalDate();
    while (!cursor.isAfter(untilInclusive)) {
      if (rule.matches(cursor)) {
        LocalDateTime s = cursor.atTime(start.toLocalTime());
        LocalDateTime e = cursor.atTime(end.toLocalTime());
        out.add(new CalendarEvent(subject, s, e, desc, loc, status, id));
      }
      cursor = cursor.plusDays(1);
    }
    return out;
  }
}
