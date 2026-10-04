package sms.observability;

import sms.system_services.events.EventListener;
import sms.system_services.events.MarkUpdatedEvent;
import sms.system_services.events.AttendanceChangedEvent;
import sms.system_services.data.DatabaseManager;

public class AuditLogger implements EventListener {
    @Override public void onEvent(Object event) {
        DatabaseManager db=DatabaseManager.getInstance();
        if(event instanceof MarkUpdatedEvent m) {
            db.executeUpdate("INSERT INTO audit_logs(timestamp,actor,action,details) VALUES(CURRENT_TIMESTAMP,?,?,?)",
                    "System","UPDATE_MARK",m.studentId+" — "+m.subjectCode+" = "+String.format("%.2f",m.newMark));
            System.out.println("[AuditLogger] Mark update recorded.");
        } else if(event instanceof AttendanceChangedEvent a) {
            db.executeUpdate("INSERT INTO audit_logs(timestamp,actor,action,details) VALUES(CURRENT_TIMESTAMP,?,?,?)",
                    "System","ATTENDANCE_CHANGED",a.studentId+" — "+String.format("%.2f%%",a.currentPercentage));
        }
    }
}
