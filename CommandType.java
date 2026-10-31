package calendar;

/** High-level kinds of commands recognized by the controller. */
public enum CommandType {
  CREATE_EVENT, EDIT_EVENT_ONE, EDIT_EVENT_THIS_AND_AFTER, EDIT_EVENT_SERIES,
  PRINT_EVENTS_ON, PRINT_EVENTS_RANGE, EXPORT, SHOW_STATUS, EXIT
}
