import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import calendar.CalendarEvent;
import calendar.CalendarModel;
import calendar.CalendarModelImpl;
import calendar.EditScope;
import calendar.PrivacyStatus;
import calendar.RecurrenceRule;
import calendar.Weekday;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.List;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests for the calendar model.
 */
public class CalendarTest {

  /**
   * Model under test.
   */
  private CalendarModel model;

  /**
   * Fresh model per test.
   */
  @Before
  public void setup() {
    model = new CalendarModelImpl();
  }

  /**
   * Helper to assert an exception in without rules.
   */
  private static <T extends Throwable> void expectThrows(Class<T> cls, Runnable r) {
    try {
      r.run();
      fail("Expected " + cls.getSimpleName());
    } catch (Throwable t) {
      if (!cls.isInstance(t)) {
        t.printStackTrace();
        fail("Expected " + cls.getSimpleName()
            + " but got " + t.getClass().getSimpleName());
      }
    }
  }

  /** Create single event. */
  @Test
  public void createSingle_ok() {
    LocalDateTime start = LocalDate.of(2025, 5, 5).atTime(10, 0);
    LocalDateTime end = LocalDate.of(2025, 5, 5).atTime(11, 0);
    model.createSingle("Meeting", start, end, "desc", "Zoom", PrivacyStatus.PUBLIC);
    assertEquals(1, model.eventsOn(LocalDate.of(2025, 5, 5)).size());
  }

  /** All-day event (8-17). */
  @Test
  public void createAllDay_setsDefaultTimes() {
    model.createAllDay("Holiday", LocalDate.of(2025, 5, 6), "", "", PrivacyStatus.PRIVATE);
    CalendarEvent ev = model.eventsOn(LocalDate.of(2025, 5, 6)).get(0);
    assertEquals(LocalTime.of(8, 0), ev.start().toLocalTime());
    assertEquals(LocalTime.of(17, 0), ev.end().toLocalTime());
    assertEquals(PrivacyStatus.PRIVATE, ev.status());
  }

  /** Duplicate prevention (same subject+start+end). */
  @Test
  public void duplicateKeyRejected() {
    LocalDate d = LocalDate.of(2025, 5, 5);
    LocalDateTime start = d.atTime(10, 0);
    LocalDateTime end = d.atTime(11, 0);
    model.createSingle("Meeting", start, end, "", "", PrivacyStatus.PUBLIC);
    expectThrows(IllegalStateException.class, () ->
        model.createSingle("Meeting", start, end, "", "", PrivacyStatus.PUBLIC));
  }

  /** Series with count on MRU. */
  @Test
  public void seriesCount_materializesCorrectly() {
    LocalDate d = LocalDate.of(2025, 5, 5);
    LocalDateTime start = d.atTime(10, 0);
    LocalDateTime end = d.atTime(11, 0);
    RecurrenceRule rule =
        new RecurrenceRule(EnumSet.of(Weekday.M, Weekday.R, Weekday.U), LocalTime.of(10, 0), false);
    List<CalendarEvent> list =
        model.createSeriesCount("First", start, end, rule, 6, "", "", PrivacyStatus.PUBLIC);
    assertEquals(6, list.size());
  }

  /** Series until date (inclusive). */
  @Test
  public void seriesUntil_inclusive() {
    LocalDate base = LocalDate.of(2025, 5, 5);
    LocalDateTime start = base.atTime(10, 0);
    LocalDateTime end = base.atTime(11, 0);
    RecurrenceRule rule =
        new RecurrenceRule(EnumSet.of(Weekday.M, Weekday.W), LocalTime.of(10, 0), false);
    List<CalendarEvent> list =
        model.createSeriesUntil("Lectures", start, end, rule, base.plusDays(10),
            "", "", PrivacyStatus.PUBLIC);
    assertEquals(4, list.size());
  }

  /** Edit ambiguous or missing. */
  @Test
  public void editAmbiguousOrMissingThrows() {
    LocalDate base = LocalDate.of(2025, 5, 5);
    LocalDateTime start = base.atTime(10, 0);
    LocalDateTime end = base.atTime(11, 0);
    expectThrows(java.util.NoSuchElementException.class,
        () -> model.editSubject("X", start, "Y", EditScope.ONE));
    model.createSingle("One", start, end, "", "", PrivacyStatus.PUBLIC);
    model.editSubject("One", start, "Two", EditScope.ONE);
    assertTrue(model.findUnique("Two", start).isPresent());
  }

  /** Edit subject for ONE. */
  @Test
  public void editOne_subjectOnlyOneInstanceChanges() {
    LocalDate base = LocalDate.of(2025, 5, 5);
    LocalDateTime s = base.atTime(10, 0);
    LocalDateTime e = base.atTime(11, 0);
    RecurrenceRule rule =
        new RecurrenceRule(EnumSet.of(Weekday.M, Weekday.W), LocalTime.of(10, 0), false);
    List<CalendarEvent> occ =
        model.createSeriesCount("First", s, e, rule, 4, "", "", PrivacyStatus.PUBLIC);
    LocalDateTime secondStart = occ.get(1).start();
    model.editSubject("First", secondStart, "Second", EditScope.ONE);
    assertTrue(model.findUnique("Second", secondStart).isPresent());
    assertTrue(model.findUnique("First", occ.get(0).start()).isPresent());
  }

  /** Edit subject THIS_AND_AFTER. */
  @Test
  public void editThisAndAfter_subject() {
    LocalDate base = LocalDate.of(2025, 5, 5);
    LocalDateTime s = base.atTime(10, 0);
    LocalDateTime e = base.atTime(11, 0);
    RecurrenceRule rule =
        new RecurrenceRule(EnumSet.of(Weekday.M, Weekday.W), LocalTime.of(10, 0), false);
    List<CalendarEvent> occ =
        model.createSeriesCount("First", s, e, rule, 6, "", "", PrivacyStatus.PUBLIC);
    model.editSubject("First", occ.get(2).start(), "Second", EditScope.THIS_AND_AFTER);
    assertTrue(model.findUnique("Second", occ.get(2).start()).isPresent());
    assertTrue(model.findUnique("First", occ.get(1).start()).isPresent());
  }

  /** Edit ENTIRE_SERIES subject. */
  @Test
  public void editEntireSeries_subject() {
    LocalDate base = LocalDate.of(2025, 5, 5);
    LocalDateTime s = base.atTime(10, 0);
    LocalDateTime e = base.atTime(11, 0);
    RecurrenceRule rule =
        new RecurrenceRule(EnumSet.of(Weekday.M, Weekday.W), LocalTime.of(10, 0), false);
    List<CalendarEvent> occ =
        model.createSeriesCount("First", s, e, rule, 6, "", "", PrivacyStatus.PUBLIC);
    model.editSubject("First", occ.get(0).start(), "Third", EditScope.ENTIRE_SERIES);
    for (CalendarEvent o : occ) {
      assertTrue(model.findUnique("Third", o.start()).isPresent());
    }
  }

  /** Start time change on THIS_AND_AFTER detaches from series. */
  @Test
  public void editStart_detachesSubsetFromSeries() {
    LocalDate base = LocalDate.of(2025, 5, 5);
    LocalDateTime s = base.atTime(10, 0);
    LocalDateTime e = base.atTime(11, 0);
    RecurrenceRule rule =
        new RecurrenceRule(EnumSet.of(Weekday.M, Weekday.W), LocalTime.of(10, 0), false);
    List<CalendarEvent> occ =
        model.createSeriesCount("First", s, e, rule, 6, "", "", PrivacyStatus.PUBLIC);
    LocalDateTime anchor = occ.get(2).start();
    model.editStart("First", anchor, anchor.plusMinutes(30), EditScope.THIS_AND_AFTER);
    model.editSubject("First", occ.get(0).start(), "Fourth", EditScope.ENTIRE_SERIES);
    assertTrue(model.findUnique("Fourth", occ.get(0).start()).isPresent());
    assertTrue(model.findUnique("Fourth", occ.get(1).start()).isPresent());
    assertFalse(model.findUnique("Fourth", anchor.plusMinutes(30)).isPresent());
  }

  /** EventsOn(date) & between(range). */
  @Test
  public void queriesWork() {
    LocalDate d = LocalDate.of(2025, 5, 5);
    model.createAllDay("A", d, "", "", PrivacyStatus.PUBLIC);
    model.createSingle("B", d.atTime(7, 0), d.atTime(7, 30), "", "", PrivacyStatus.PUBLIC);
    assertEquals(2, model.eventsOn(d).size());
    long countInRangeForA = model.eventsBetween(d.atTime(8, 0), d.atTime(17, 0))
        .stream()
        .filter(e -> e.subject().equals("A"))
        .count();
    assertEquals(1L, countInRangeForA);
  }

  /** isBusy check. */
  @Test
  public void busyCheck() {
    LocalDate d = LocalDate.of(2025, 5, 6);
    model.createAllDay("X", d, "", "", PrivacyStatus.PUBLIC);
    assertTrue(model.isBusy(d.atTime(8, 1)));
    assertFalse(model.isBusy(d.atTime(7, 59)));
  }

  /** CSV export format basic. */
  @Test
  public void exportCsv_ok() throws Exception {
    LocalDate d = LocalDate.of(2025, 5, 7);
    model.createAllDay("Holiday", d, "Desc, with comma", "Home", PrivacyStatus.PRIVATE);
    Path tmp = Path.of("build/test-cal.csv");
    Path abs = model.exportCsv(tmp);
    assertTrue(Files.exists(abs));
    String text = new String(Files.readAllBytes(abs));
    assertTrue(text.startsWith("Subject,Start Date"));
    assertTrue(text.contains(",All Day Event,Description,Location,Private"));
    assertTrue(text.contains("Holiday"));
  }

  /** Series same-day constraint enforced. */
  @Test
  public void seriesSameDayGuard() {
    LocalDateTime start = LocalDate.of(2025, 5, 5).atTime(23, 30);
    LocalDateTime end = LocalDate.of(2025, 5, 6).atTime(0, 30);
    RecurrenceRule rule =
        new RecurrenceRule(EnumSet.of(Weekday.M), LocalTime.of(23, 30), false);
    expectThrows(IllegalArgumentException.class, () ->
        model.createSeriesCount("Late", start, end, rule, 2, "", "", PrivacyStatus.PUBLIC));
  }

  /** Series start time uniformity enforced. */
  @Test
  public void seriesStartUniformityGuard() {
    LocalDateTime start = LocalDate.of(2025, 5, 5).atTime(10, 0);
    LocalDateTime end = LocalDate.of(2025, 5, 5).atTime(11, 0);
    RecurrenceRule rule =
        new RecurrenceRule(EnumSet.of(Weekday.M), LocalTime.of(9, 30), false);
    expectThrows(IllegalArgumentException.class, () ->
        model.createSeriesCount("X", start, end, rule, 2, "", "", PrivacyStatus.PUBLIC));
  }

  /** Editing must not allow creating a duplicate key (subject+start+end). */
  @Test
  public void editCannotCreateDuplicateKey() {
    LocalDate d = LocalDate.of(2025, 5, 5);
    LocalDateTime s1 = d.atTime(10, 0);
    LocalDateTime e1 = d.atTime(11, 0);
    LocalDateTime s2 = d.atTime(12, 0);
    LocalDateTime e2 = d.atTime(13, 0);

    model.createSingle("A", s1, e1, "", "", PrivacyStatus.PUBLIC);
    model.createSingle("B", s2, e2, "", "", PrivacyStatus.PUBLIC);

    model.editStart("B", s2, s1, EditScope.ONE);

    model.editEnd("B", s1, e1, EditScope.ONE);

    expectThrows(IllegalStateException.class,
        () -> model.editSubject("B", s1, "A", EditScope.ONE));
  }
}
