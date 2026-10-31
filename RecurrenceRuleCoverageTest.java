import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import calendar.RecurrenceRule;
import calendar.Weekday;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.Set;
import org.junit.Test;

/**
 * Covers RecurrenceRule branches highlighted by JaCoCo:
 * - constructor validation (null/empty days)
 * - getDays() returns copy (and non-empty)
 * - isAllDay() returns value
 * - day+time predicate (true & false)
 *
 * <p>Uses reflection where method names may vary (e.g., occursOn / matches).
 */
public class RecurrenceRuleCoverageTest {

  /**
   * Builds a RecurrenceRule via its public constructor.
   */
  private RecurrenceRule newRule(EnumSet<Weekday> days, LocalTime time, boolean allDay) {
    return new RecurrenceRule(days, time, allDay);
  }

  /**
   * Find a public instance method on RecurrenceRule that:
   * returns boolean.
   * has exactly one parameter of type LocalDateTime.
   * and invoke it with the provided argument.
   */
  private boolean invokeDateTimePredicate(RecurrenceRule rule, LocalDateTime dt) throws Exception {
    Method candidate = null;
    for (Method m : RecurrenceRule.class.getMethods()) {
      if (!m.getDeclaringClass().equals(RecurrenceRule.class)) {
        continue;
      }
      if (m.getReturnType() != boolean.class) {
        continue;
      }
      Class<?>[] params = m.getParameterTypes();
      if (params.length == 1 && params[0].equals(LocalDateTime.class)) {
        candidate = m;
        break;
      }
    }
    assertNotNull("Could not find RecurrenceRule boolean(LocalDateTime) method", candidate);
    Object res = candidate.invoke(rule, dt);
    return (Boolean) res;
  }

  /**
   * Find a zero-arg getter returning a Set (the copy of days), invoke and return it.
   * If a direct getDays() exists we’ll hit that; else we accept any 0-arg Set-returning method.
   */
  @SuppressWarnings("unchecked")
  private Set<Weekday> getDaysCopy(RecurrenceRule rule) throws Exception {
    Method m = null;
    try {
      m = RecurrenceRule.class.getMethod("getDays");
    } catch (NoSuchMethodException ignored) {
      for (Method mm : RecurrenceRule.class.getMethods()) {
        if (!mm.getDeclaringClass().equals(RecurrenceRule.class)) {
          continue;
        }
        if (mm.getParameterCount() == 0 && Set.class.isAssignableFrom(mm.getReturnType())) {
          m = mm;
          break;
        }
      }
    }
    assertNotNull("Could not find a zero-arg Set<Weekday> getter on RecurrenceRule", m);
    Object out = m.invoke(rule);
    assertTrue("Expected a Set<Weekday> from getter", out instanceof Set);
    return (Set<Weekday>) out;
  }

  /**
   * Find a zero-arg boolean getter (isAllDay) and invoke it.
   */
  private boolean getIsAllDay(RecurrenceRule rule) throws Exception {
    Method m = null;
    try {
      m = RecurrenceRule.class.getMethod("isAllDay");
    } catch (NoSuchMethodException ignored) {
      for (Method mm : RecurrenceRule.class.getMethods()) {
        if (!mm.getDeclaringClass().equals(RecurrenceRule.class)) {
          continue;
        }
        if (mm.getParameterCount() == 0 && mm.getReturnType() == boolean.class) {
          m = mm;
          break;
        }
      }
    }
    assertNotNull("Could not find a zero-arg boolean getter (isAllDay) on RecurrenceRule", m);
    return (Boolean) m.invoke(rule);
  }

  @Test(expected = IllegalArgumentException.class)
  public void ctor_rejects_null_days() {
    newRule(null, LocalTime.of(9, 0), false);
  }

  @Test(expected = IllegalArgumentException.class)
  public void ctor_rejects_empty_days() {
    newRule(EnumSet.noneOf(Weekday.class), LocalTime.of(9, 0), false);
  }

  @Test
  public void getDays_returns_copy_and_nonEmpty() throws Exception {
    RecurrenceRule rule = newRule(EnumSet.of(Weekday.M, Weekday.W), LocalTime.of(9, 0), false);

    Set<Weekday> a = getDaysCopy(rule);
    assertFalse("Getter must return non-empty copy", a.isEmpty());
    assertTrue(a.contains(Weekday.M));
    assertTrue(a.contains(Weekday.W));

    a.clear();

    Set<Weekday> b = getDaysCopy(rule);
    assertTrue("Internal days must be intact (copy semantics)", b.contains(Weekday.M));
    assertTrue("Internal days must be intact (copy semantics)", b.contains(Weekday.W));
  }

  @Test
  public void isAllDay_reflects_constructor_value_true() throws Exception {
    RecurrenceRule rule = newRule(EnumSet.of(Weekday.M), LocalTime.of(0, 0), true);
    assertTrue(getIsAllDay(rule));
  }

  @Test
  public void isAllDay_reflects_constructor_value_false() throws Exception {
    RecurrenceRule rule = newRule(EnumSet.of(Weekday.M), LocalTime.of(0, 0), false);
    assertFalse(getIsAllDay(rule));
  }

  @Test
  public void predicate_true_when_day_and_time_match() throws Exception {
    LocalDateTime mondayNine = LocalDate.of(2025, 2, 3).atTime(9, 0);
    RecurrenceRule rule = newRule(EnumSet.of(Weekday.M, Weekday.W), LocalTime.of(9, 0), false);

    assertTrue(invokeDateTimePredicate(rule, mondayNine));
  }

  @Test
  public void predicate_false_when_time_differs() throws Exception {
    LocalDateTime mondayTen = LocalDate.of(2025, 2, 3).atTime(10, 0);
    RecurrenceRule rule = newRule(EnumSet.of(Weekday.M, Weekday.W), LocalTime.of(9, 0), false);

    assertFalse(invokeDateTimePredicate(rule, mondayTen));
  }

  @Test
  public void predicate_false_when_day_not_in_set() throws Exception {
    LocalDateTime tuesdayNine = LocalDate.of(2025, 2, 4).atTime(9, 0);
    RecurrenceRule rule = newRule(EnumSet.of(Weekday.M, Weekday.W), LocalTime.of(9, 0), false);

    assertFalse(invokeDateTimePredicate(rule, tuesdayNine));
  }
}
