import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Covers {@link CalendarRunner#main(String[])} branches:.
 * <ul>
 *   <li>No args &rarr; usage + {@code System.exit(2)}</li>
 *   <li>{@code --mode interactive} &rarr; runs interactive REPL</li>
 *   <li>{@code --mode headless} (missing path) &rarr; error + {@code System.exit(2)}</li>
 *   <li>{@code --mode headless} {@code {@literal <commands.txt>}} &rarr; executes commands</li>
 *   <li>Unknown mode &rarr; error + {@code System.exit(2)}</li>
 *   <li>Missing {@code --mode} keyword &rarr; error + {@code System.exit(2)}</li>
 * </ul>
 */
public class CalendarRunnerCoverageTest {

  private final PrintStream realOut = System.out;
  private final PrintStream realErr = System.err;
  private final InputStream realIn = System.in;
  private final SecurityManager realSec = System.getSecurityManager();

  private ByteArrayOutputStream outBuf;
  private ByteArrayOutputStream errBuf;

  /**
   * Security manager that turns {@code System.exit(status)} into thrown {@link SecurityException}.
   */
  private static final class ExitTrapper extends SecurityManager {
    private Integer statusSeen = null;

    @Override
    public void checkPermission(java.security.Permission perm) {}

    @Override
    public void checkExit(int status) {
      statusSeen = status;
      throw new SecurityException("System.exit(" + status + ")");
    }

    Integer status() {
      return statusSeen;
    }
  }

  private ExitTrapper trapper;

  /**
   * Set Up.
   */
  @Before
  public void setUp() {
    outBuf = new ByteArrayOutputStream();
    errBuf = new ByteArrayOutputStream();
    System.setOut(new PrintStream(outBuf, true));
    System.setErr(new PrintStream(errBuf, true));
    trapper = new ExitTrapper();
    System.setSecurityManager(trapper);
  }

  /**
   * Tear Down.
   */
  @After
  public void tearDown() {
    System.setOut(realOut);
    System.setErr(realErr);
    System.setIn(realIn);
    System.setSecurityManager(realSec);
  }

  /**
   * Verifies no-args prints usage and exits with status 2.
   */
  @Test
  public void noArgs_printsUsage_and_exits2() {
    try {
      CalendarRunner.main(new String[] {});
      fail("Expected SecurityException from System.exit");
    } catch (SecurityException se) {
      assertEquals(Integer.valueOf(2), trapper.status());
      String err = errBuf.toString().toLowerCase();
      assertTrue(err.contains(
          "usage: --mode interactive | --mode headless <commands.txt>".toLowerCase()));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Verifies interactive mode consumes "exit" and prints goodbye without throwing.
   */
  @Test
  public void interactive_mode_runs_and_prints_bye() throws Exception {
    System.setIn(new ByteArrayInputStream("exit\n".getBytes()));
    CalendarRunner.main(new String[] {"--mode", "interactive"});
    String out = outBuf.toString().toLowerCase();
    assertTrue(out.contains("calendar ready") || out.contains("bye."));
  }

  /**
   * Verifies headless without a file path exits with status 2 and prints an error.
   */
  @Test
  public void headless_missing_file_exits2_with_message() {
    try {
      CalendarRunner.main(new String[] {"--mode", "headless"});
      fail("Expected SecurityException from System.exit");
    } catch (SecurityException se) {
      assertEquals(Integer.valueOf(2), trapper.status());
      String err = errBuf.toString().toLowerCase();
      assertTrue(err.contains("headless requires path to commands file"));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Verifies headless with a valid commands {@code .txt} file executes and prints output.
   */
  @Test
  public void headless_with_file_runs_controller() throws Exception {
    Path tmp = Files.createTempFile("cmds", ".txt");
    Files.write(tmp, List.of(
        "create event Foo on 2025-06-01",
        "exit"
    ));
    CalendarRunner.main(new String[] {"--mode", "headless", tmp.toString()});

    String out = outBuf.toString();
    assertTrue(out.contains("OK") || out.toLowerCase().contains("bye"));
  }

  /**
   * Verifies unknown mode triggers error and {@code System.exit(2)}.
   */
  @Test
  public void unknown_mode_exits2_with_message() {
    try {
      CalendarRunner.main(new String[] {"--mode", "wat"});
      fail("Expected SecurityException from System.exit");
    } catch (SecurityException se) {
      assertEquals(Integer.valueOf(2), trapper.status());
      String err = errBuf.toString().toLowerCase();
      assertTrue(err.contains("unknown mode: wat"));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Verifies missing {@code --mode} keyword triggers error and {@code System.exit(2)}.
   */
  @Test
  public void missing_mode_keyword_exits2_with_message() {
    try {
      CalendarRunner.main(new String[] {"--mood", "interactive"});
      fail("Expected SecurityException from System.exit");
    } catch (SecurityException se) {
      assertEquals(Integer.valueOf(2), trapper.status());
      String err = errBuf.toString().toLowerCase();
      assertTrue(err.contains("expected --mode"));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
