package calendar;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Abstraction of the calendar model (no I/O, pure logic). */
public interface CalendarModel {
  /** Create a single event. Throws if duplicate key (subject+start+end). */
  CalendarEvent createSingle(String subject, LocalDateTime start, LocalDateTime end,
                             String description, String location, PrivacyStatus status);

  /** Create an all-day event (8:00–17:00). */
  CalendarEvent createAllDay(String subject, LocalDate date,
                             String description, String location, PrivacyStatus status);

  /** Create a series for a fixed count. Each occurrence is single-day and same start time. */
  List<CalendarEvent> createSeriesCount(String subject, LocalDateTime start, LocalDateTime end,
                                        RecurrenceRule rule, int count,
                                        String description, String location, PrivacyStatus status);

  /** Create a recurring series until (inclusive) the given date. */
  List<CalendarEvent> createSeriesUntil(String subject, LocalDateTime start, LocalDateTime end,
                                        RecurrenceRule rule, LocalDate untilInclusive,
                                        String description, String location, PrivacyStatus status);

  /** Edit subject on one or more events determined by {@code scope}. */
  void editSubject(String subject, LocalDateTime startsAt, String newSubject, EditScope scope);

  /** Edit start time on one or more events determined by {@code scope}. */
  void editStart(String subject, LocalDateTime startsAt, LocalDateTime newStart, EditScope scope);

  /** Edit end time on one or more events determined by {@code scope}. */
  void editEnd(String subject, LocalDateTime startsAt, LocalDateTime newEnd, EditScope scope);

  /** Edit description on one or more events determined by {@code scope}. */
  void editDescription(String subject, LocalDateTime startsAt, String newDesc, EditScope scope);

  /** Edit location on one or more events determined by {@code scope}. */
  void editLocation(String subject, LocalDateTime startsAt, String newLoc, EditScope scope);

  /** Edit visibility status on one or more events determined by {@code scope}. */
  void editStatus(String subject, LocalDateTime startsAt, PrivacyStatus newStatus, EditScope scope);

  /** All events on a given date (including multi-day overlaps). */
  List<CalendarEvent> eventsOn(LocalDate date);

  /** All events that intersect the interval [from,to]. */
  List<CalendarEvent> eventsBetween(LocalDateTime from, LocalDateTime to);

  /** Whether any event covers the given moment. */
  boolean isBusy(LocalDateTime moment);

  /**
   * Export all events as a Google Calendar-compatible CSV written to {@code file}.
   *
   * @return absolute path of the file written
   */
  Path exportCsv(Path file);

  /** Find exactly one event by subject and start time, if unique. */
  Optional<CalendarEvent> findUnique(String subject, LocalDateTime startsAt);
}
