package sms.observability;

import sms.system_services.events.EventListener;
import sms.system_services.events.MarkUpdatedEvent;
import sms.system_services.events.AttendanceChangedEvent;
import sms.system_services.events.StudentEnrolledEvent;
import sms.system_services.events.StudentDeletedEvent;
import sms.system_services.data.DatabaseManager;

/**
 * Persists administrative audit trails and listens to system events.
 */
public class AuditLogger implements EventListener {

    public static void record(String actor, String action, String details) {
        try {
            DatabaseManager db = DatabaseManager.getInstance();
            db.executeUpdate("INSERT INTO audit_logs(timestamp,actor,action,details) VALUES(CURRENT_TIMESTAMP,?,?,?)",
                    actor, action, details);
            System.out.println("[AuditLogger] " + action + " logged by " + actor);
        } catch (Exception e) {
            System.err.println("[AuditLogger] Failed to write audit log: " + e.getMessage());
        }
    }

    @Override
    public void onEvent(Object event) {
        if (event instanceof MarkUpdatedEvent m) {
            record("System", "UPDATE_MARK", m.studentId + " — " + m.subjectCode + " = " + String.format("%.2f", m.newMark));
        } else if (event instanceof AttendanceChangedEvent a) {
            record("System", "ATTENDANCE_CHANGED", a.studentId + " — " + String.format("%.2f%%", a.currentPercentage));
        } else if (event instanceof StudentEnrolledEvent e) {
            record("System", "STUDENT_ENROLLED", e.studentName + " (" + e.studentId + ") in " + e.department);
        } else if (event instanceof StudentDeletedEvent d) {
            record("System", "STUDENT_DELETED", "Student ID: " + d.studentId);
        }
    }
}
