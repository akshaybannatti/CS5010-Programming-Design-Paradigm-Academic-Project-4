import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import calendar.CalendarEvent;
import calendar.PrivacyStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;
import org.junit.Test;

/** Coverage-focused tests for CalendarEvent invariants and behaviors. */
public class CalendarEventTest {

  private static LocalDateTime dt(int y, int m, int d, int h, int min) {
    return LocalDate.of(y, m, d).atTime(LocalTime.of(h, min));
  }

  /** Helper: expect an exception without depending on its exact message. */
  private static <T extends Throwable> void expectThrows(Class<T> cls, Runnable r) {
    try {
      r.run();
      org.junit.Assert.fail("Expected " + cls.getSimpleName());
    } catch (Throwable t) {
      if (!cls.isInstance(t)) {
        t.printStackTrace();
        org.junit.Assert.fail(
            "Expected " + cls.getSimpleName() + " but got " + t.getClass().getSimpleName());
      }
    }
  }

  /** Constructor guards: subject, start, end, end-before-start. */
  @Test
  public void ctor_validationGuards() {
    LocalDateTime s = dt(2025, 5, 5, 10, 0);
    LocalDateTime e = dt(2025, 5, 5, 11, 0);
    UUID sid = UUID.randomUUID();

    expectThrows(IllegalArgumentException.class,
        () -> new CalendarEvent(null, s, e, "", "", PrivacyStatus.PUBLIC, sid));
    expectThrows(IllegalArgumentException.class,
        () -> new CalendarEvent("  ", s, e, "", "", PrivacyStatus.PUBLIC, sid));
    expectThrows(IllegalArgumentException.class,
        () -> new CalendarEvent("A", null, e, "", "", PrivacyStatus.PUBLIC, sid));
    expectThrows(IllegalArgumentException.class,
        () -> new CalendarEvent("A", s, null, "", "", PrivacyStatus.PUBLIC, sid));
    expectThrows(IllegalArgumentException.class,
        () -> new CalendarEvent("A", e, s, "", "", PrivacyStatus.PUBLIC, sid));
  }

  /** Null normalization: description/location/status defaulting paths. */
  @Test
  public void ctor_normalizesNulls() {
    LocalDateTime s = dt(2025, 5, 5, 10, 0);
    LocalDateTime e = dt(2025, 5, 5, 11, 0);
    CalendarEvent ev = new CalendarEvent("A", s, e, null, null, null, null);

    assertEquals("", ev.description());
    assertEquals("", ev.location());
    assertEquals(PrivacyStatus.PUBLIC, ev.status());
  }

  /** changeStart: null/new-before-end guards; happy path updates. */
  @Test
  public void changeStart_guardsAndUpdate() {
    LocalDateTime s = dt(2025, 5, 5, 10, 0);
    LocalDateTime e = dt(2025, 5, 5, 11, 0);
    CalendarEvent ev = new CalendarEvent("A", s, e, "", "", PrivacyStatus.PUBLIC, null);

    expectThrows(IllegalArgumentException.class, () -> ev.changeStart(null));
    expectThrows(IllegalArgumentException.class, () -> ev.changeStart(dt(2025, 5, 5, 12, 0)));

    ev.changeStart(dt(2025, 5, 5, 10, 30));
    assertEquals(dt(2025, 5, 5, 10, 30), ev.start());
  }

  /** changeEnd: null/before-start guards; happy path updates. */
  @Test
  public void changeEnd_guardsAndUpdate() {
    LocalDateTime s = dt(2025, 5, 5, 10, 0);
    LocalDateTime e = dt(2025, 5, 5, 11, 0);
    CalendarEvent ev = new CalendarEvent("A", s, e, "", "", PrivacyStatus.PUBLIC, null);

    expectThrows(IllegalArgumentException.class, () -> ev.changeEnd(null));
    expectThrows(IllegalArgumentException.class, () -> ev.changeEnd(dt(2025, 5, 5, 9, 59)));

    ev.changeEnd(dt(2025, 5, 5, 11, 30));
    assertEquals(dt(2025, 5, 5, 11, 30), ev.end());
  }

  /** equals/hashCode: case-insensitive subject, exact start/end; non-CalendarEvent return false. */
  @Test
  public void equalsAndHashCode_contract() {
    LocalDateTime s = dt(2025, 5, 5, 10, 0);
    LocalDateTime e = dt(2025, 5, 5, 11, 0);
    CalendarEvent a = new CalendarEvent("Meet", s, e, "", "",
        PrivacyStatus.PUBLIC, null);
    CalendarEvent b = new CalendarEvent("meet", s, e, "x", "y",
        PrivacyStatus.PRIVATE, null);
    CalendarEvent c = new CalendarEvent("meet", s, dt(2025, 5, 5, 11, 1),
        "", "", null, null);

    assertTrue(a.equals(b));
    assertEquals(a.hashCode(), b.hashCode());

    assertFalse(a.equals(c));
    assertFalse(a.equals("not-an-event"));
  }

  /** toString includes ' @ location' only when location is non-blank. */
  @Test
  public void toString_includesLocationOnlyWhenPresent() {
    LocalDateTime s = dt(2025, 5, 5, 10, 0);
    LocalDateTime e = dt(2025, 5, 5, 11, 0);

    CalendarEvent noLoc = new CalendarEvent("A", s, e, "", "  ", PrivacyStatus.PUBLIC, null);
    String t1 = noLoc.toString().toLowerCase();
    assertFalse("Should not contain @ when location blank", t1.contains(" @ "));

    CalendarEvent withLoc = new CalendarEvent("B", s, e, "", "Room 101",
        PrivacyStatus.PUBLIC, null);
    String t2 = withLoc.toString();
    assertTrue("Should contain @ Room 101", t2.contains(" @ Room 101"));
  }
}
