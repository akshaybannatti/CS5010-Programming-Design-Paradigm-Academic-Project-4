package calendar;

/** Represents a parsed command that can be executed against the model. */
public interface Command {
  /** The command kind (for diagnostics or logging). */
  CommandType type();

  /** Execute the command, reporting results and errors to the view. */
  void execute(CalendarModel model, EventView out);
}
