import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import calendar.CalendarEvent;
import calendar.CalendarModel;
import calendar.CalendarModelImpl;
import calendar.EditScope;
import calendar.PrivacyStatus;
import calendar.RecurrenceRule;
import calendar.Weekday;
import java.io.BufferedReader;
import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.junit.Test;

/**
 * Targeted tests to kill PIT survivors for CalendarModelImpl and related CSV/export paths.
 */
public class CalendarModelImplPitKillerTest {

  private CalendarModel newModel() {
    return new CalendarModelImpl();
  }

  @Test
  public void createSingle_returnsNonNull_andIsRetrievable() {
    CalendarModel m = newModel();
    CalendarEvent ev = m.createSingle("A",
        LocalDateTime.of(2025, 11, 3, 9, 0),
        LocalDateTime.of(2025, 11, 3, 10, 0),
        "", "", PrivacyStatus.PUBLIC);
    assertNotNull(ev);
    List<CalendarEvent> got = m.eventsBetween(
        LocalDateTime.of(2025, 11, 3, 0, 0),
        LocalDateTime.of(2025, 11, 3, 23, 59));
    assertEquals(1, got.size());
    assertEquals("A", got.get(0).subject());
  }

  @Test
  public void createAllDay_returnsNonNull_andIsOnDate() {
    CalendarModel m = newModel();
    CalendarEvent ev = m.createAllDay("AllDay",
        LocalDate.of(2025, 11, 4),
        "", "", PrivacyStatus.PUBLIC);
    assertNotNull(ev);
    List<CalendarEvent> on = m.eventsOn(LocalDate.of(2025, 11, 4));
    assertEquals(1, on.size());
    assertEquals("AllDay", on.get(0).subject());
  }

  @Test
  public void duplicateTimedEventIsRejected() {
    CalendarModel m = newModel();
    LocalDateTime s = LocalDateTime.of(2025, 11, 5, 9, 0);
    LocalDateTime e = LocalDateTime.of(2025, 11, 5, 9, 30);
    m.createSingle("Dup", s, e, "", "", PrivacyStatus.PUBLIC);
    try {
      m.createSingle("Dup", s, e, "", "", PrivacyStatus.PUBLIC);
      fail("Expected an exception for duplicate event");
    } catch (IllegalArgumentException | IllegalStateException ex) {
      assertTrue("Caught expected duplicate event exception", true);
    }
  }

  @Test
  public void eventsRemainSortedByStartAfterInsertAndEdit() {
    CalendarModel m = newModel();
    CalendarEvent b = m.createSingle("B",
        LocalDateTime.of(2025, 11, 3, 11, 0),
        LocalDateTime.of(2025, 11, 3, 12, 0),
        "", "", PrivacyStatus.PUBLIC);
    CalendarEvent a = m.createSingle("A",
        LocalDateTime.of(2025, 11, 3, 9, 0),
        LocalDateTime.of(2025, 11, 3, 10, 0),
        "", "", PrivacyStatus.PUBLIC);

    List<CalendarEvent> day = m.eventsBetween(
        LocalDateTime.of(2025, 11, 3, 0, 0),
        LocalDateTime.of(2025, 11, 3, 23, 59));
    assertEquals(List.of("A", "B"),
        day.stream().map(CalendarEvent::subject).collect(Collectors.toList()));

    m.editStart("B", b.start(),
        LocalDateTime.of(2025, 11, 3, 11, 30), EditScope.ONE);

    day = m.eventsBetween(
        LocalDateTime.of(2025, 11, 3, 0, 0),
        LocalDateTime.of(2025, 11, 3, 23, 59));
    assertEquals(List.of("A", "B"),
        day.stream().map(CalendarEvent::subject).collect(Collectors.toList()));
  }

  @Test
  public void editEntireSeries_affectsOnlyThatSeries() {
    CalendarModel m = newModel();
    m.createSeriesCount("S1",
        LocalDateTime.of(2025, 11, 3, 9, 0),
        LocalDateTime.of(2025, 11, 3, 9, 15),
        new RecurrenceRule(EnumSet.of(Weekday.M, Weekday.W, Weekday.F), LocalTime.of(9, 0), false),
        3, "", "", PrivacyStatus.PUBLIC);

    m.createSeriesCount("S2",
        LocalDateTime.of(2025, 11, 4, 10, 0),
        LocalDateTime.of(2025, 11, 4, 10, 30),
        new RecurrenceRule(EnumSet.of(Weekday.T, Weekday.R), LocalTime.of(10, 0), false),
        3, "", "", PrivacyStatus.PUBLIC);

    m.editSubject("S1", LocalDateTime.of(2025, 11, 3, 9, 0), "S1*", EditScope.ENTIRE_SERIES);

    long s1count = m.eventsBetween(
            LocalDateTime.of(2025, 11, 3, 0, 0),
            LocalDateTime.of(2025, 11, 14, 23, 59))
        .stream().filter(e -> e.subject().startsWith("S1*")).count();

    long s2affected = m.eventsBetween(
            LocalDateTime.of(2025, 11, 3, 0, 0),
            LocalDateTime.of(2025, 11, 14, 23, 59))
        .stream().filter(e -> e.subject().startsWith("S2*")).count();

    assertTrue("All S1 series items renamed", s1count >= 3);
    assertEquals("S2 must not be touched", 0, s2affected);
  }

  @Test
  public void editOneOccurrenceStart_detachesFromSeriesOnlyThatOne() {
    CalendarModel m = newModel();
    m.createSeriesCount("S",
        LocalDateTime.of(2025, 11, 3, 9, 0),
        LocalDateTime.of(2025, 11, 3, 9, 30),
        new RecurrenceRule(EnumSet.of(Weekday.M, Weekday.W, Weekday.F),
            LocalTime.of(9, 0), false),
        3, "", "", PrivacyStatus.PUBLIC);

    LocalDateTime firstStart = LocalDateTime.of(2025, 11, 3, 9, 0);
    m.editStart("S", firstStart, LocalDateTime.of(2025, 11, 3, 8, 0), EditScope.ONE);

    List<CalendarEvent> all = m.eventsBetween(
        LocalDateTime.of(2025, 11, 3, 0, 0),
        LocalDateTime.of(2025, 11, 7, 23, 59));

    CalendarEvent moved = all.stream()
        .filter(e -> e.subject().equals("S")
            && e.start().equals(LocalDateTime.of(2025, 11, 3, 8, 0)))
        .findFirst().orElseThrow();

    long seriesCount = all.stream().filter(e -> e.seriesId().isPresent()).count();

    boolean detached = !moved.seriesId().isPresent();
    boolean onlyOneMoved =
        all.stream().filter(e -> e.subject().equals("S"))
            .map(CalendarEvent::start)
            .filter(st -> st.equals(LocalDateTime.of(2025, 11, 3, 8, 0))).count() == 1;

    assertTrue("Edited instance must be detached OR the only one moved within series",
        detached || onlyOneMoved);

    assertTrue("Other occurrences remain in series", seriesCount >= 2);
  }

  @Test
  public void eventsOn_includesCrossMidnightIfAnyPortionOnDate() {
    CalendarModel m = newModel();
    m.createSingle("Overnight",
        LocalDateTime.of(2025, 11, 1, 23, 0),
        LocalDateTime.of(2025, 11, 2, 1, 0),
        "", "", PrivacyStatus.PUBLIC);

    List<CalendarEvent> onNov2 = m.eventsOn(LocalDate.of(2025, 11, 2));
    assertEquals(1, onNov2.size());
    assertEquals("Overnight", onNov2.get(0).subject());
  }

  @Test
  public void eventsBetween_includesBoundaryStartsAndEnds() {
    CalendarModel m = newModel();
    LocalDateTime s = LocalDateTime.of(2025, 11, 3, 10, 0);
    LocalDateTime e = LocalDateTime.of(2025, 11, 3, 11, 0);
    m.createSingle("Boundary", s, e, "", "", PrivacyStatus.PUBLIC);

    List<CalendarEvent> r1 = m.eventsBetween(s, e.plusMinutes(10));
    assertEquals(1, r1.size());

    List<CalendarEvent> r2 = m.eventsBetween(s.minusMinutes(10), e);
    assertEquals(1, r2.size());
  }

  @Test
  public void exportCsv_encodesAllDayAndPrivacyFlags() throws Exception {
    CalendarModel m = newModel();
    m.createAllDay("D1", LocalDate.of(2025, 11, 3), "", "", PrivacyStatus.PUBLIC);
    m.createSingle("D2",
        LocalDateTime.of(2025, 11, 3, 9, 0),
        LocalDateTime.of(2025, 11, 3, 10, 0),
        "", "", PrivacyStatus.PRIVATE);

    Path tmp = Files.createTempFile("cal-", ".csv");
    Path out = m.exportCsv(tmp);
    assertTrue(Files.exists(out));

    Pattern timePattern = Pattern.compile("\\b\\d{2}:\\d{2}(?::\\d{2})?\\b",
        Pattern.CASE_INSENSITIVE);

    try (BufferedReader br = new BufferedReader(new FileReader(out.toFile()))) {
      String header = br.readLine();
      String line1 = br.readLine();
      String line2 = br.readLine();
      assertNotNull(line1);
      assertNotNull(line2);

      String allday = (line1.contains("D1")) ? line1 : line2;
      String timed = (line1.contains("D2")) ? line1 : line2;

      assertTrue("all-day should have empty times and AllDay=True",
          allday.contains(",,") && allday.toLowerCase().contains("true"));

      long timeTokens = timePattern.matcher(timed).results().count();
      String lower = timed.toLowerCase();

      assertTrue("timed should contain subject D2", timed.contains("D2"));
      assertTrue("timed should have two time tokens", timeTokens >= 2);
      assertTrue("timed should include AllDay=False", lower.contains("false"));
      assertTrue("timed should mark Private=True", lower.contains("true"));
    }
  }
}
