# VIVA VOCE & EXPERIMENT VIDEO RECORDING GUIDE
## Student Management & Academic Intelligence System (SMAIS)
### Register Number: URK25CO2019 | Course: 23DC2001 (OOP)

---

## PART 1: 5-TO-8 MINUTE EXPERIMENT VIDEO DEMONSTRATION SCRIPT

When recording your experiment video, follow this exact sequence to demonstrate all rubric criteria smoothly:

### 1. Introduction (30 seconds)
- **Face the camera / screen share:**
  *"Hello, I am Alena B, Register Number URK25CO2019, from the Division of Data Science and Cyber Security. Today, I am demonstrating my Object-Oriented Programming micro-project: the Student Management & Academic Intelligence System (SMAIS). This project is developed in Java 21, utilizing Java Swing, HSQLDB embedded persistence, custom exception hierarchies, and an event-driven architecture."*

### 2. Launching the Application (30 seconds)
- Open PowerShell and run: `.\run.ps1`
- **Explain:** *"Notice how running the script automatically compiles all Java source files and simultaneously launches our native Java Swing Desktop GUI while starting the embedded web server on port 8080."*

### 3. Demonstrating Class Design & OOP Concepts (1 minute)
- Point to the Student Table.
- **Explain:**
  - *"Our domain model uses an abstract base class `Person` extended by `Student` and `Staff`."*
  - *"Under `Student`, we have `UndergraduateStudent` and `PostgraduateStudent` demonstrating inheritance and polymorphism."*
  - *"The grading logic uses dynamic method dispatch: Undergraduates are graded on a 10.0 scale, while Postgraduates are evaluated on a 4.0 scale."*

### 4. Demonstrating GUI Layouts, Event Handling & Dialog Boxes (1.5 minutes)
- **Search bar:** Type `Vian` in the search box. Show that the table filters instantly. Explain: *"Here, a `DocumentListener` listens to every keystroke to update the table model in real time."*
- **Add Student:** Click `➕ Enroll New Student`.
  - Enter test data. Click `Enroll Student`.
  - Show the `JOptionPane.showMessageDialog` confirming success.
  - Show the new student appearing in the table.
- **Delete Guard:** Select a student and click `🗑️ Delete Student`.
  - Point to the confirmation dialog: *"Notice how `JOptionPane.showConfirmDialog` prevents accidental deletions."* Click `No` to show safety.

### 5. Demonstrating Backend Logic & Exception Handling (1 minute)
- Switch to the **Marks & GPA Engine** tab.
- Enter an invalid mark like `150` or `-10`. Click `Save Mark`.
  - Show the error dialog: *"Our `ValidationEngine` throws a custom `ValidationException`, caught gracefully to display this error dialog."*
- Now enter a valid mark (`92`). Click `Save Mark`.
  - Point to the result dialog showing GPA calculation, weighted credit points, and the assigned letter grade computed by `GPAEngine`.

### 6. Demonstrating Attendance Tracking & Early Warning Risk Engine (1 minute)
- Switch to the **Attendance Tracker** tab.
- Mark a student absent multiple times until attendance falls below 75%.
- Show the warning dialog: *"Institutional policy requires a minimum of 75% attendance. When attendance drops below threshold, `AttendanceAlertEngine` immediately fires an event."*
- Switch to the **Risk Analysis Engine** tab.
  - Show the color-coded risk table displaying high-risk students in bold red with automated academic intervention recommendations.

### 7. Demonstrating Innovation: Dual Web Gateway & Live Codespace (1 minute)
- Switch to the **Web Gateway & Logs** tab in Swing. Show the live scrolling telemetry console.
- Click `🌐 Open Web Dashboard in Browser` (or open `http://localhost:8080`).
- Show the web interface running simultaneously from the same Java process!
- Click **Live Codespace** in the web dashboard, click `src` $\rightarrow$ `sms` $\rightarrow$ `runtime` to show live source reflection from disk.

### 8. Conclusion (30 seconds)
- *"In summary, SMAIS fulfills all Course Outcomes (CO1 to CO5), demonstrating encapsulation, inheritance, polymorphism, custom exception hierarchies, multi-component Swing GUI design, and embedded JDBC persistence. Thank you!"*

---

## PART 2: TOP 25 VIVA VOCE QUESTIONS & HIGH-SCORING ANSWERS

### Category A: Object-Oriented Programming (OOP)
**Q1: How does your project demonstrate Abstraction?**  
**Answer:** Abstraction is achieved through abstract classes `Person` and `Student` and the `EventListener` interface. `Person` abstracts common attributes (`id`, `name`, `email`) while leaving role-specific behaviors abstract. `EventListener` defines a contract (`onEvent`) without revealing how specific subscribers process events.

**Q2: What is the difference between Method Overloading and Method Overriding in your project?**  
**Answer:** 
- *Overloading (Compile-time Polymorphism):* Occurs in `ValidationEngine` and `GPAEngine`, where methods share the same name but differ in parameter count or types (e.g. `validateMark(double)` vs `validateStudent(...)`).
- *Overriding (Run-time Polymorphism):* Occurs in `UndergraduateStudent` and `PostgraduateStudent`, which override `classifyGrade()` and `gradePointFor()` from abstract class `Student`.

**Q3: How is Encapsulation enforced?**  
**Answer:** All entity attributes in `Student`, `Person`, and `Subject` are declared `private` or `protected`. They can only be accessed or modified through getters and setters containing validation logic. Furthermore, the `StudentStatus` state machine restricts invalid state transitions.

**Q4: What is Dynamic Method Dispatch?**  
**Answer:** It is Java's mechanism where a call to an overridden method is resolved at runtime rather than compile time. For example, when calling `student.gradePointFor(mark)` on a reference of type `Student`, the JVM dynamically invokes either `UndergraduateStudent.gradePointFor` or `PostgraduateStudent.gradePointFor` depending on the actual underlying object.

---

### Category B: GUI & Event Handling (Swing)
**Q5: What layout managers did you use and why?**  
**Answer:**
- `BorderLayout`: For structured top-level frames (North = header, Center = main content, South = status bar).
- `GridLayout`: For equal-sized metric cards in the header.
- `GridBagLayout`: For complex form alignment in modal dialogs where labels and input fields have varying widths.
- `FlowLayout`: For toolbars and button clusters.

**Q6: What is the Event Dispatch Thread (EDT) in Swing?**  
**Answer:** The EDT is the dedicated background thread that handles all Swing events and component rendering. To prevent thread contention and GUI freezing, all UI updates are dispatched via `SwingUtilities.invokeLater()`.

**Q7: How did you implement live search in the student table?**  
**Answer:** We attached a `DocumentListener` to the `JTextField`. Whenever the text changes (`insertUpdate`, `removeUpdate`), it triggers `filterStudents()`, which queries or filters the `DefaultTableModel` rows in real time.

**Q8: What dialog boxes did you use from `JOptionPane`?**  
**Answer:** 
- `JOptionPane.showMessageDialog` for informational messages and error alerts.
- `JOptionPane.showConfirmDialog` with `YES_NO_OPTION` to guard critical operations like student deletion.
- Custom modal `JDialog` instances for complex multi-field forms like student enrollment.

---

### Category C: Exception Handling
**Q9: What is the difference between checked and unchecked exceptions in your design?**  
**Answer:**
- *Unchecked Exceptions (`RuntimeException`):* `ValidationException` and `EntityNotFoundException` extend `DomainException` (which extends `RuntimeException`). They represent programming or input rule violations that can occur during user interactions.
- *Checked Exceptions (`Exception`):* `InvalidAcademicRecordException` extends `Exception`. It forces calling classes to handle potential missing academic records during GPA computations using explicit `try-catch` blocks.

**Q10: Why use custom exceptions instead of generic `Exception`?**  
**Answer:** Custom exceptions provide domain-specific context, allowing distinct catch blocks to handle validation failures, database errors, and entity lookup issues differently, improving system maintainability and user feedback.

---

### Category D: Database & Connectivity (JDBC)
**Q11: Why did you use `PreparedStatement` instead of `Statement`?**  
**Answer:** `PreparedStatement` pre-compiles SQL queries and uses parameterized placeholders (`?`). This provides two critical benefits: it prevents SQL Injection attacks and improves execution performance through query plan caching.

**Q12: What database is used and how is it connected?**  
**Answer:** Embedded HSQLDB (HyperSQL) running in persistent file mode (`jdbc:hsqldb:file:data/academic`). It requires no separate server installation, stores tables in the local `data/` folder, and persists data across application restarts.

**Q13: How does the system handle database resource cleanup?**  
**Answer:** We utilize Java's `try-with-resources` statement for all `Connection`, `PreparedStatement`, and `ResultSet` objects, ensuring that database connections and cursors are automatically closed even if an exception occurs.

---

### Category E: Architecture & Innovation
**Q14: What is the Event-Driven Architecture (EventBus)?**  
**Answer:** The `EventBus` is an in-memory Publish-Subscribe bus. When a domain event occurs (such as `MarkUpdatedEvent` or `AttendanceChangedEvent`), it is published to the `EventBus`. Registered listeners (`AuditLogger`, `StudentRiskAnalyzer`, `AcademicSystemGUI`) react independently without tight coupling between components.

**Q15: How can a single Java process run both Swing and Web server?**  
**Answer:** The Java application spawns the embedded `HttpServer` (from `com.sun.net.httpserver`) on a background virtual thread on port 8080, while the main Swing GUI runs on the Event Dispatch Thread (EDT). Both interfaces share the same `DatabaseManager` singleton and `EventBus` instance in memory.
