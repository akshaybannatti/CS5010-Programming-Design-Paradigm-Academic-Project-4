package calendar;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.EnumSet;

/** Recurrence definition for a series (days + uniform start time, allDay flag). */
public final class RecurrenceRule {
  /** Weekdays included in this recurrence. */
  private final EnumSet<Weekday> days;
  /** Uniform start time-shared by the series. */
  private final LocalTime seriesStartTime;
  /** True if all-day; used for on-date series creation. */
  private final boolean allDay;

  /** Create a rule; days must be non-empty. */
  public RecurrenceRule(EnumSet<Weekday> days, LocalTime seriesStartTime, boolean allDay) {
    if (days == null || days.isEmpty()) {
      throw new IllegalArgumentException("Recurrence weekdays must be non-empty.");
    }
    this.days = EnumSet.copyOf(days);
    this.seriesStartTime = seriesStartTime;
    this.allDay = allDay;
  }

  /** Weekdays included by this rule. */
  public EnumSet<Weekday> days() {
    return EnumSet.copyOf(days);
  }

  /** Start time-shared by all occurrences. */
  public LocalTime seriesStartTime() {
    return seriesStartTime;
  }

  /** True if this rule represents an all-day series. */
  public boolean isAllDay() {
    return allDay;
  }

  /** Returns true if the datetime matches both weekday and series start time. */
  public boolean matches(LocalDateTime dt) {
    return days.contains(Weekday.fromJava(dt.getDayOfWeek()))
        && dt.toLocalTime().equals(seriesStartTime);
  }

  /** True if the given date’s weekday is included in this rule. */
  public boolean matches(LocalDate date) {
    return days.contains(Weekday.fromJava(date.getDayOfWeek()));
  }
}
