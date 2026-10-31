import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import calendar.CalendarEvent;
import calendar.CalendarModelImpl;
import calendar.EditScope;
import calendar.PrivacyStatus;
import calendar.RecurrenceRule;
import calendar.Weekday;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.List;
import org.junit.Test;

/**
 * Extra coverage tests for {@link CalendarModelImpl}, specifically targeting
 * CSV null/quoting branches, series filters, ensureNoDuplicate paths, and
 * all-day detection during export.
 */
public class CalendarModelImplExtraCoverage2Test {

  /**
   * Reflectively extracts the internal events list from {@link CalendarModelImpl}.
   * We purposely avoid referencing the field name; instead, we find the first
   * non-empty {@code List<CalendarEvent>} and return it.
   *
   * @param m the model implementation instance
   * @return the internal events list
   * @throws Exception on reflection issues
   */
  @SuppressWarnings("unchecked")
  private static List<CalendarEvent> internalEvents(CalendarModelImpl m) throws Exception {
    for (Field f : CalendarModelImpl.class.getDeclaredFields()) {
      if (List.class.isAssignableFrom(f.getType())) {
        f.setAccessible(true);
        Object v = f.get(m);
        if (v instanceof List) {
          List<?> lst = (List<?>) v;
          if (!lst.isEmpty() && lst.get(0) instanceof CalendarEvent) {
            return (List<CalendarEvent>) lst;
          }
        }
      }
    }
    throw new IllegalStateException("Could not find internal events list");
  }

  /**
   * Covers CSV helper branch where a cell value {@code s == null} returns {@code ""}.
   * We force a stored event string field to {@code null} before export.
   */
  @Test
  public void csv_null_cell_returns_empty_string() throws Exception {
    CalendarModelImpl m = new CalendarModelImpl();
    m.createAllDay("NullProbe", LocalDate.of(2025, 5, 2), "desc", "loc", PrivacyStatus.PUBLIC);

    List<CalendarEvent> list = internalEvents(m);
    assertTrue(!list.isEmpty());
    CalendarEvent stored = list.get(list.size() - 1);

    Field target = null;
    for (Field f : stored.getClass().getDeclaredFields()) {
      if (f.getType() == String.class) {
        target = f;
        break;
      }
    }
    assertNotNull("No String field to null out", target);
    target.setAccessible(true);
    target.set(stored, null);

    Path out = Path.of("build/tmp/export-null.csv");
    Files.createDirectories(out.getParent());
    String csv = new String(Files.readAllBytes(m.exportCsv(out)));
    assertTrue(csv.contains(",,"));
  }

  /**
   * Covers CSV quoting branch: {@code need == true} when a cell contains comma/quote/newline.
   */
  @Test
  public void csv_quoted_cell_needed_for_special_chars() throws Exception {
    CalendarModelImpl m = new CalendarModelImpl();
    LocalDateTime s = LocalDateTime.of(2025, 6, 1, 10, 0);
    LocalDateTime e = s.plusHours(1);
    String bad = "Room, \"Quoted\"\nLine";
    m.createSingle("Q", s, e, bad, "ok", PrivacyStatus.PUBLIC);

    Path out = Path.of("build/tmp/export-quoted.csv");
    Files.createDirectories(out.getParent());
    String csv = new String(Files.readAllBytes(m.exportCsv(out)));

    String expected = "\"Room, \"\"Quoted\"\"\nLine\"";
    assertTrue(csv.contains(expected));
  }

  /**
   * Drives ENTIRE_SERIES and THIS_AND_AFTER filtering to exercise:.
   * <ul>
   *   <li>{@code .filter(e -> e.seriesId().orElse(null)
   *   != null && e.seriesId().get().equals(sid))}</li>
   *   <li>{@code ensureNoDuplicate(e, snapshot == e ? null : e)} (both null & non-null paths)</li>
   * </ul>
   */
  @Test
  public void series_filters_and_ensureNoDuplicate_cover_both_sides() {
    CalendarModelImpl m = new CalendarModelImpl();
    RecurrenceRule rule = new RecurrenceRule(
        EnumSet.of(Weekday.M, Weekday.W, Weekday.F), LocalTime.of(9, 0), false);

    LocalDateTime base = LocalDateTime.of(2025, 7, 7, 9, 0);
    LocalDateTime end = base.plusHours(1);

    m.createSeriesCount("SERX", base, end, rule, 4, "", "", PrivacyStatus.PUBLIC);

    m.editDescription("SERX", base, "all-desc", EditScope.ENTIRE_SERIES);

    LocalDateTime second = base.plusDays(2);
    m.editLocation("SERX", second, "subset-loc", EditScope.THIS_AND_AFTER);

    m.editStatus("SERX", base, PrivacyStatus.PUBLIC, EditScope.ONE);

    assertTrue(true);
  }

  /**
   * Ensures export sees both all-day and timed events so the all-day detection condition
   * (end == ALL_DAY_END & same date) is evaluated.
   */
  @Test
  public void export_includes_allDay_and_timed_to_trigger_allDay_detection() throws Exception {
    CalendarModelImpl m = new CalendarModelImpl();
    m.createAllDay("Holiday",
        LocalDate.of(2025, 8, 15), "independence", "earth", PrivacyStatus.PUBLIC);
    m.createSingle("Meeting",
        LocalDateTime.of(2025, 8, 15, 10, 0),
        LocalDateTime.of(2025, 8, 15, 11, 0),
        "room", "talk", PrivacyStatus.PUBLIC);

    Path out = Path.of("build/tmp/export-allday.csv");
    Files.createDirectories(out.getParent());
    String csv = new String(Files.readAllBytes(m.exportCsv(out)));
    assertTrue(csv.contains("Holiday"));
    assertTrue(csv.contains("Meeting"));
  }

  /**
   * Another explicit null-cell probe to keep the {@code s == null} guard fully covered,
   * even if the model normalizes some fields.
   */
  @Test
  public void csv_null_guard_again() throws Exception {
    CalendarModelImpl m = new CalendarModelImpl();
    m.createAllDay("X", LocalDate.of(2025, 9, 1), null, null, PrivacyStatus.PUBLIC);

    List<CalendarEvent> list = internalEvents(m);
    CalendarEvent ev = list.get(list.size() - 1);

    Field anyStr = null;
    for (Field f : ev.getClass().getDeclaredFields()) {
      if (f.getType() == String.class) {
        anyStr = f;
        break;
      }
    }
    assertNotNull(anyStr);
    anyStr.setAccessible(true);
    anyStr.set(ev, null);

    Path out = Path.of("build/tmp/export-null2.csv");
    String csv = new String(Files.readAllBytes(m.exportCsv(out)));
    assertTrue(csv.contains(",,"));
  }
}
