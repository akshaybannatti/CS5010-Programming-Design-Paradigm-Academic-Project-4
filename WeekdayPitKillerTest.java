import static org.junit.Assert.assertNotNull;

import calendar.Weekday;
import java.time.DayOfWeek;
import org.junit.Test;

/**
 * PIT killers for Weekday mapping and null guard.
 */
public class WeekdayPitKillerTest {

  /** Ensures every Java DayOfWeek is mapped.
   *  If PIT mutates the map init (e.g., removes a put),
   *  this will fail and kill the mutant. */
  @Test
  public void fromJava_mapsAllSevenDays() {
    for (DayOfWeek d : DayOfWeek.values()) {
      assertNotNull("Mapping missing for: " + d, Weekday.fromJava(d));
    }
  }

  /** Exercises the null-guard path so mutants on the null check get killed. */
  @Test(expected = IllegalArgumentException.class)
  public void fromJava_null_throwsIae() {
    Weekday.fromJava(null);
  }
}
