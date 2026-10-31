package calendar;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** Immutable-ish calendar event entity used by the model. */
public class CalendarEvent {
  /** Start time for an all-day event (assignment default). */
  public static final LocalTime ALL_DAY_START = LocalTime.of(8, 0);
  /** End time for an all-day event (assignment default). */
  public static final LocalTime ALL_DAY_END = LocalTime.of(17, 0);

  private String subject;
  private LocalDateTime start;
  private LocalDateTime end;
  private String description;
  private String location;
  private PrivacyStatus status;

  private UUID seriesId;

  /** Constructs an event; end must not be before start. */
  public CalendarEvent(String subject, LocalDateTime start, LocalDateTime end,
                       String description, String location, PrivacyStatus status, UUID seriesId) {
    if (subject == null || subject.isBlank()) {
      throw new IllegalArgumentException("Subject required");
    }
    if (start == null) {
      throw new IllegalArgumentException("Start required");
    }
    if (end == null) {
      throw new IllegalArgumentException("End required");
    }
    if (end.isBefore(start)) {
      throw new IllegalArgumentException("End before start");
    }

    this.subject = subject.trim();
    this.start = start;
    this.end = end;
    this.description = description == null ? "" : description;
    this.location = location == null ? "" : location;
    this.status = status == null ? PrivacyStatus.PUBLIC : status;
    this.seriesId = seriesId;
  }

  /**
   * Factory for creating an all-day event on the given date using the assignment defaults
   * ({@code 08:00}–{@code 17:00}).
   *
   * @param subject     non-blank event subject
   * @param date        calendar date for the all-day event
   * @param description optional free-form description (may be empty)
   * @param location    optional location text (may be empty)
   * @param status      visibility flag (PUBLIC or PRIVATE)
   * @param seriesId    series identifier if this occ. belongs to a series; {@code null} otherwise
   *
   * @return a new {@link CalendarEvent} spanning 08:00–17:00 on {@code date}
   * @throws IllegalArgumentException if {@code subject} is blank
   */
  public static CalendarEvent allDay(String subject, LocalDate date,
                                     String description, String location, PrivacyStatus status,
                                     UUID seriesId) {
    LocalDateTime s = date.atTime(ALL_DAY_START);
    LocalDateTime e = date.atTime(ALL_DAY_END);
    return new CalendarEvent(subject, s, e, description, location, status, seriesId);
  }

  /** Subject (case-insensitive for equality checks). */
  public String subject() {
    return subject;
  }

  /** Subject (case-insensitive for equality checks). */
  public LocalDateTime start() {
    return start;
  }

  /** Subject (case-insensitive for equality checks). */
  public LocalDateTime end() {
    return end;
  }

  /** Subject (case-insensitive for equality checks). */
  public String description() {
    return description;
  }

  /** Subject (case-insensitive for equality checks). */
  public String location() {
    return location;
  }

  /** Subject (case-insensitive for equality checks). */
  public PrivacyStatus status() {
    return status;
  }

  /** Subject (case-insensitive for equality checks). */
  public Optional<UUID> seriesId() {
    return Optional.ofNullable(seriesId);
  }

  void changeSubject(String s) {
    if (s == null || s.isBlank()) {
      throw new IllegalArgumentException("Subject required");
    }
    this.subject = s.trim();
  }

  /**
   * Changes the start date and time of this calendar event.
   *
   * <p>The new start time must not be {@code null} and must not be after the current end time.
   * If the provided start is {@code null}, or if it results in an invalid time interval,
   * an {@link IllegalArgumentException} is thrown.
   *
   * @param s the new start date-time for this event
   * @throws IllegalArgumentException if {@code s} is null or if the end time is before,
   *        the new start time
   */
  public void changeStart(LocalDateTime s) {
    if (s == null) {
      throw new IllegalArgumentException("Start required");
    }
    if (end.isBefore(s)) {
      throw new IllegalArgumentException("End before new start");
    }
    this.start = s;
  }

  /**
   * Changes the end date and time of this calendar event.
   *
   * <p>The new end time must not be {@code null} and must not be before the current start time.
   * If the provided end is {@code null}, or if it results in an invalid time interval,
   * an {@link IllegalArgumentException} is thrown.
   *
   * @param e the new end date-time for this event
   * @throws IllegalArgumentException if {@code e} is null or if it is before the current start time
   */
  public void changeEnd(LocalDateTime e) {
    if (e == null) {
      throw new IllegalArgumentException("End required");
    }
    if (e.isBefore(start)) {
      throw new IllegalArgumentException("New end before start");
    }
    this.end = e;
  }

  void changeDescription(String d) {
    this.description = d == null ? "" : d;
  }

  void changeLocation(String l) {
    this.location = l == null ? "" : l;
  }

  void changeStatus(PrivacyStatus st) {
    this.status = st == null ? PrivacyStatus.PUBLIC : st;
  }

  void detachFromSeries() {
    this.seriesId = null;
  }

  /**
   * Two events are considered equal for de-duplication rule if same subject, start, end.
   */
  public boolean sameKey(CalendarEvent other) {
    return this.subject.equalsIgnoreCase(other.subject)
        && this.start.equals(other.start)
        && this.end.equals(other.end);
  }

  @Override
  public String toString() {
    return subject
        + " starting on " + start.toLocalDate()
        + " at " + start.toLocalTime()
        + ", ending on " + end.toLocalDate()
        + " at " + end.toLocalTime()
        + (location.isBlank() ? "" : " @ " + location);
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof CalendarEvent)) {
      return false;
    }
    CalendarEvent e = (CalendarEvent) o;
    return sameKey(e);
  }

  @Override
  public int hashCode() {
    return Objects.hash(subject.toLowerCase(), start, end);
  }
}
