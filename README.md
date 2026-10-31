# Introduction

This project is the first in a series of assignments that will involve designing and implementing a virtual calendar application. The idea is to mimic features found on widely used calendar apps such as Google Calendar or Apple's iCalendar app.

You will start with implementing the core features of such an application. This would involve implementing the features and exposing them for use by the end user. For example, a user could interact with the app through a command line utility now, and eventually support a graphical user interface (GUI). In each subsequent iteration you will add more features end-to-end. 

The following sections describe the feature set of the app is expected to offer in this assignment. **For this assignment, we will assume that the timezone is EST, and you are required to support, at the minimum, a single calendar.**

# Feature Set

## Events

An event is required to have a subject, start date and time. Optionally it may have a longer description, end date and time, location (physical or online) and a status (whether the event is public or private).

There cannot be two events with the same subject, start date/time and end date/time.

## Create A Single Calendar Event

The application should support the ability to create a single event in a calendar. If an event does not have an end date/time then the event is an "All Day Event", which is defined as 8am to 5pm. It is possible for a single event to span several days.

Keep in mind that two events cannot have the same subject, start date/time and end date/time.

## Create An Event Series

The application should support the ability to create a series of events (i.e. a recurring event). In addition to the required and optional details mentioned above, events in an event series repeat on specific days of a week (as indicated by the user) for a specific number of occurrences or until a specific end date and time. A single event in a series can only span one day (i.e. it must start and finish on the same day). 

All events in an event series are required to have the same start time.  Keep in mind that two events in the calendar cannot have the same subject, start date/time and end date/time.

## Edit Calendar Events

The application should support searching for a specific event on the calendar and modify its properties (subject, start date and time, end date and time, location, description or public/private status). 

Searching for an event requires specifying enough of its properties to uniquely identify it. If multiple events have the same properties that were specified, editing should not be possible. 

An event series is modified by first specifying the properties of an event within that series. Then event series can be modified in one of three ways:

- Modify that single event of the series
  
- Modify all events in the series, starting from the event (the event and all events after it in the series)
  
- Modify all events in the series (the event, all events before and after it in the series).

Keep in mind that modifying the start time of some events of a series would result in these events no longer being a part of that series. Here is an example:

```
1. Create an event series called "First" on May 5 2025 from 10am-11am that repeats six times on Mondays and Wednesdays. 

2. Edit the subject of the event starting on May 12 2025 to be "Second" and specify to change all events in the series starting from this. This will change the subject for 4 of the 6 instances created above.

3. Edit the subject of the event starting on May 5 2025 to be "Third" and specify for all events in this series to change. This will change the subject of all 6 instances.

4. Edit the start time of the event starting on May 12 2025 to be 10:30am and specify to change all events in the series starting from this. This will change the start times for 4 of the 6 instances created above.

5. Edit the subject of the event starting on May 5 2025 to be "Fourth" and specify for all events in this series to change. This will change the subject for only the instances on May 5 and May 7. 

6. Edit the subject of the event starting on May 12 2025 to be "Fifth" and specify for all events in this series to change. This will change the subject for events on May 12, 14, 19 and 21. 

```

(Try this in Google Calendar)



## Query Calendar

The application should support querying the calendar for events scheduled on 
a specific date. It should also be possible to ask for a schedule within a range of dates. 

The application should also support querying if the user is busy on
a specific date at a specific time.

## Export Calendar

The application should be able to export the calendar to a CSV file. The expected format of the CSV file must conform
to the file format specs listed [here](https://support.google.com/calendar/answer/37118?hl=en&co=GENIE.Platform%3DDesktop#zippy=%2Ccreate-or-edit-a-csv-file).

You can verify the correctness of the CSV file by uploading it to Google Calendar. If the upload is successful, you should be able to see the events in Google calendar.

### Support for Commands

This application should have a text-based user interface that allows users to interactively enter commands to interact with it. The following description is a list of allowed commands and their expected behavior.

**Creating Events**

- `create event <eventSubject> from <dateStringTtimeString> to <dateStringTtimeString>` 

Creates a single event in the calendar. Note \<dateString\> is a string of the form "YYYY-MM-DD" \<timeString\> is a string of the form "hh:mm" and \<dateStringTtimeString\> is a string of the form "YYYY-MM-DDThh::mm".


- `create event <eventSubject> from <dateStringTtimeString> to <dateStringTtimeString> repeats <weekdays> for <N> times`

Creates an event series that repeats N times on specific weekdays. Note \<weekdays\> is a 
sequence of characters where character denotes a day of the week, e.g., MRU. 
'M' is Monday, 'T' is Tuesday, 'W' is Wednesday, 'R' is Thursday, 'F' is Friday, 'S' is Saturday, and 'U' is Sunday.

- `create event <eventSubject> from <dateStringTtimeString> to <dateStringTtimeString> repeats <weekdays> until <dateString>`

Creates an event series until a specific date (inclusive).

- `create event <eventSubject> on <dateString>`

Creates a single all day event.

- `create event <eventSubject> on <dateString> repeats <weekdays> for <N> times`

Creates a series of all day events that repeats N times on specific weekdays.

- `create event <eventSubject> on <dateString> repeats <weekdays> until <dateString>` 

Creates a series of all day events until a specific date (inclusive).

For all of the above, the subject may have multiple words. Only in this case, the subject must be enclosed in double quotes.

**Editing Events**

- `edit event <property> <eventSubject> from <dateStringTtimeString> to <dateStringTtimeString> with <NewPropertyValue>`

Identify the event that has the given subject and starts at the given date and time, and edit its property. This results in change in property for a single instance (irrespective of whether the identified event is single or part of a series).

- `edit events <property> <eventSubject> from <dateStringTtimeString> with <NewPropertyValue>`

Identify the event(s) that has the given subject and starts at the given date and time and edit its property. If this event is part of a series then the properties of all events in that series that start at or after the given date and time should be changed. If this event is not part of a series then this has the same effect as the command above. 

- `edit series <property> <eventSubject> from <dateStringTtimeString> with <NewPropertyValue>`

Identify the event that has the given subject and starts at the given date and time and edit its property. If this event is part of a series then the properties of all events in that series should be changed. If this event is not part of a series then this has the same effect as the first edit command.

For all these queries the `<property>` field may be one of the following: *subject*, *start*, *end*, *description*, *location*, *status*. The format of the new property values are `string`, `dateStringTtimeString`, `dateStringTtimeString`, `string`, `string` and `string` respectively. 

If an edition will result in any violations of the rule that two events cannot have the same subject, start and end date, then the edition should not occur. A useful error message may be printed in this case.

**Queries**

- `print events on <dateString>`

Prints a bulleted list of all events on that day along with their start and end time and location (if any).

- `print events from <dateStringTtimeString> to <dateStringTtimeString>`

Prints a bulleted list of all events that partly or completely lie in the given interval. Each event should be listed in a single line and must be in the following format: `<subject> starting on <startdate> at <starttime>, ending on <enddate> at <endtime>` including their start and end times and location (if any).

**Miscellaneous**

- `export cal fileName.csv`

Exports the calendar as a csv file that can be imported to Google Calendar app. The command should also print the absolute path of the generated csv file. Note all file system paths processed by your program must be platform independent, i.e., the src and tests should not depend on OS specific details of a file path.

- `show status on <dateStringTtimeString>`

Prints busy status if the user has events scheduled on a given day and time, otherwise, available.

Your program must halt with an error if a user enters an unexpected command. The error must clearly indicate which command was invalid and why.

- `exit`

Stops listening for further commands.

# Modes of Running

Your application must run in two modes -- **interactive** and **headless**. Hence, it must have a main program that takes command line arguments and runs the application in either mode based on the user's arguments. For example, if your main class is called `CalendarRunner.java`, a user must be able to run the main class with the argument `--mode interactive` for interactive mode or `--mode headless commands.txt` for headless mode. Note the given options are case-insensitive. **Keep in mind that command line arguments are different than starting the program and then asking the user to input the mode**.

In the interactive mode, the user can type one of the above commands and see the result of the command immediately. They can keep typing commands and the seeing the results until they type an exit command. On the other hand, in headless mode, the user can provide a text file with a list of commands, the last command must be an exit command. The commands in the file are executed sequentially. If the file ends without reading an exit command, the program should display an error message and then gracefully quit. In both modes, if a command fails to execute a suitable error message must be printed and the program should move on to the next command (i.e. ask for another command in the interactive mode, or read the next command in the file in headless mode).

A user should be able to run your application using a JAR file. 
To create a JAR file run the command `./gradlew jar`. This will create a JAR
file in the _build/libs_ directory. You can run the jar using the command
`java -jar build/libs/JARNAME.jar`. You can provide arguments after 
the jar file path. 

You should assume that the user will run your program from this project's root.
You must ensure that file paths that your program relies on are **platform independent**.

# Points to Consider

- How can you represent the core application logic as a model?

- What data representation are possible for calendars and calendar events?

- How can the model be decoupled from other aspects of the application such IO?

- How can you design the application so that it works with different kinds of IO devices (e.g., script vs, interactive)?

- What concrete and abstract data types should one use for date and time?

- How can one design the model such that it will work with minimal changes for new requirements?

- Apply the SOLID principles to evaluate the quality of your design.

- Remember not to fall into the trap of assuming that there is only one way to use your model. You do not know if there will be only one view, and what kind of user interaction will need to be supported.

- There may come a time in the semester when you will be asked to share your code with other students, or see others' code. So your design, code and documentation is not "for your eyes only".

- You are allowed to use all classes in the JDK, but not others. You should avoid using classes related to GUIs or views in the model.

# What to Submit

This is a group assignment. The submission should be made to the group repo created on Pawtograder for this assignment. 

You must submit the following to be considered for grading:

- A directory called **res/** with the following:
  - A screenshot of the Google Calendar with your events
  - A txt file, **commands.txt**, with the list of valid commands.
  - A txt file, **invalid.txt** with a list of commands where at least one command is invalid.
  - A class diagram representing the structure of the application's design. The class diagram should show names of classes and interfaces, signature of methods and relationships (inheritance and composition). Do not show field names they may clutter the diagram. We expect these to be not hand-scribbled, you stand to lose points for submitting a picture of a hand-drawn diagram.
- A **USEME.md file** with instructions to run the JAR file with arguments. Make sure to provide example commands. 
- The main method must be in the class 'src/main/java/CalendarRunner.java'.
- Complete the anonymous [peer evaluation survey](https://forms.gle/jAtVdZbJJEpTn32P6).
	
# Grading standards
 
For this assignment, you will be graded on 

- completeness of your submission, including all the expected examples

- adherence to an MVC design

- the design of your interface(s), in terms of clarity, flexibility, and how plausibly it will support needed functionality;

- the appropriateness of your representation choices for the data, and the adequacy of any documented class invariants (please comment on why you chose the representation you did in the code);

-  the forward thinking in your design, in terms of its flexibility, use of abstraction, etc.

-  the correctness and style of your implementation, and

-  the comprehensiveness and correctness of your test coverage.
