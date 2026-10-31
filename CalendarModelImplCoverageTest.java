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
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Focused tests to raise coverage in CalendarModelImpl on the branches
 * highlighted by JaCoCo.
 */
public class CalendarModelImplCoverageTest {

  @Rule
  public TemporaryFolder tmp = new TemporaryFolder();

  private static CalendarModel newModel() {
    return new CalendarModelImpl();
  }

  @Test(expected = IllegalArgumentException.class)
  public void createSeriesCount_rejects_non_positive_count() {
    CalendarModel m = newModel();
    LocalDateTime s = LocalDateTime.of(2025, 1, 1, 9, 0);
    LocalDateTime e = s.plusHours(1);
    RecurrenceRule rule = new RecurrenceRule(EnumSet.of(Weekday.M), LocalTime.of(9, 0), false);
    m.createSeriesCount("Bad", s, e, rule, 0, "", "", PrivacyStatus.PUBLIC);
  }

  @Test
  public void editDescription_editLocation_editStatus_cover_applyAndCheck_true_path() {
    CalendarModel m = newModel();
    LocalDateTime s = LocalDateTime.of(2025, 2, 3, 9, 0);
    LocalDateTime e = s.plusHours(1);
    m.createSingle("Standup", s, e, "", "", PrivacyStatus.PUBLIC);

    m.editDescription("Standup", s, "Daily sync", EditScope.ONE);
    m.editLocation("Standup", s, "Room-101", EditScope.ONE);
    m.editStatus("Standup", s, PrivacyStatus.PRIVATE, EditScope.ONE);
  }

  @Test(expected = IllegalStateException.class)
  public void ambiguousPickThrowsIllegalStateException() {
    CalendarModel m = newModel();
    LocalDateTime s = LocalDateTime.of(2025, 3, 10, 10, 0);
    LocalDateTime e = s.plusHours(1);
    m.createSingle("Dup", s, e, "", "", PrivacyStatus.PUBLIC);
    m.createSingle("Dup", s, e, "", "", PrivacyStatus.PUBLIC);

    m.editDescription("Dup", s, "X", EditScope.ONE);
  }

  @Test
  public void csv_export_success_and_quote_variants_are_exercised() throws Exception {
    CalendarModel m = newModel();
    LocalDateTime s1 = LocalDateTime.of(2025, 4, 1, 12, 0);
    LocalDateTime e1 = s1.plusHours(1);
    String tricky = "has,comma \"and quote\"\nnewline";
    m.createSingle("CSV", s1, e1, null, tricky, PrivacyStatus.PUBLIC);

    Path out = tmp.newFile("ok.csv").toPath();
    Path ret = m.exportCsv(out);
    assertTrue("export should create a file", Files.exists(ret));
    String content = Files.readString(out);
    assertTrue(content.toLowerCase().contains("subject"));
    assertTrue(content.contains("CSV"));
  }

  @Test
  public void csv_export_failure_wraps_in_runtime_exception() throws Exception {
    CalendarModel m = newModel();
    Path dir = tmp.newFolder("as_dir").toPath();
    try {
      m.exportCsv(dir);
      fail("Expected RuntimeException wrapping export failure");
    } catch (RuntimeException ex) {
      assertTrue(ex.getMessage().toLowerCase().contains("failed to export csv"));
    }
  }

  @Test
  public void eventsOn_eventsBetween_isBusy_cover_predicates_and_all_day_checks() {
    CalendarModel m = newModel();

    LocalDateTime start = LocalDateTime.of(2025, 1, 5, 10, 0);
    LocalDateTime end = start.plusHours(1);
    m.createSingle("Timed", start, end, "", "", PrivacyStatus.PUBLIC);

    m.createAllDay("Holiday", LocalDate.of(2025, 1, 6), "", "", PrivacyStatus.PUBLIC);

    List<CalendarEvent> on5 = m.eventsOn(LocalDate.of(2025, 1, 5));
    assertFalse(on5.isEmpty());
    List<CalendarEvent> on6 = m.eventsOn(LocalDate.of(2025, 1, 6));
    assertFalse(on6.isEmpty());

    List<CalendarEvent> between = m.eventsBetween(
        LocalDateTime.of(2025, 1, 5, 9, 30),
        LocalDateTime.of(2025, 1, 5, 10, 30));
    assertFalse(between.isEmpty());

    assertTrue(m.isBusy(LocalDateTime.of(2025, 1, 5, 10, 15)));

    assertTrue(m.isBusy(LocalDateTime.of(2025, 1, 6, 12, 0)));
  }

  @Test
  public void series_edits_cover_scope_and_seriesId_filters() {
    CalendarModel m = newModel();

    LocalDateTime s = LocalDateTime.of(2025, 2, 3, 9, 0);
    LocalDateTime e = s.plusHours(1);
    RecurrenceRule rule =
        new RecurrenceRule(EnumSet.of(Weekday.M, Weekday.W), LocalTime.of(9, 0), false);

    m.createSeriesCount("Series", s, e, rule, 3, "", "", PrivacyStatus.PUBLIC);

    m.editDescription("Series", s, "entire", EditScope.ENTIRE_SERIES);

    m.editLocation("Series", s, "after", EditScope.THIS_AND_AFTER);

    m.editStatus("Series", s, PrivacyStatus.PRIVATE, EditScope.ONE);
  }

  @Test
  public void ensureNoDuplicate_path_is_exercised_by_editing_subject() {
    CalendarModel m = newModel();
    LocalDateTime s = LocalDateTime.of(2025, 7, 1, 8, 0);
    LocalDateTime e = s.plusHours(1);

    m.createSingle("A1", s, e, "", "", PrivacyStatus.PUBLIC);
    m.createSingle("B1", s.plusHours(2), e.plusHours(2), "", "", PrivacyStatus.PUBLIC);

    m.editSubject("A1", s, "A1-renamed", EditScope.ONE);
  }

  @Test
  public void csv_quote_null_and_need_flags_are_exercised() throws Exception {
    CalendarModel m = newModel();
    LocalDateTime s = LocalDateTime.of(2025, 8, 1, 9, 0);
    LocalDateTime e = s.plusHours(1);
    m.createSingle("Q", s, e, null, "x,\"y\",z", PrivacyStatus.PUBLIC);

    Path out = tmp.newFile("quote.csv").toPath();
    m.exportCsv(out);
    String content = Files.readString(out);
    assertTrue(content.contains("Q"));
    assertTrue(content.contains("\"x,\"\"y\"\",z\""));
  }
}
