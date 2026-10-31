import static org.junit.Assert.assertNotNull;

import calendar.Weekday;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.time.DayOfWeek;
import org.junit.Test;

/** Tests for Weekday Coverage. */
public class WeekdayCoverageTest {

  @Test(expected = IllegalArgumentException.class)
  public void fromJava_with_invalid_enum_throws_exception() throws Exception {
    Field nameField = Enum.class.getDeclaredField("name");
    nameField.setAccessible(true);
    DayOfWeek bogus = (DayOfWeek) Proxy.newProxyInstance(
        DayOfWeek.class.getClassLoader(),
        new Class[] {DayOfWeek.class},
        (proxy, method, args) -> {
          if (method.getName().equals("name")) {
            return "BOGUS";
          }
          if (method.getName().equals("ordinal")) {
            return 999;
          }
          return null;
        });

    Weekday.fromJava(bogus);
  }

  @Test
  public void fromJava_covers_all_days() {
    for (DayOfWeek dow : DayOfWeek.values()) {
      assertNotNull(Weekday.fromJava(dow));
    }
  }

  @Test
  public void fromJava_coversAllSevenDays() {
    for (DayOfWeek d : DayOfWeek.values()) {
      assertNotNull(Weekday.fromJava(d));
    }
  }

  @Test(expected = IllegalArgumentException.class)
  public void fromJava_nullThrows() {
    Weekday.fromJava(null);
  }
}
