package sms.system_services.events;

import java.time.LocalDateTime;

public class StudentEnrolledEvent {
    public final String studentId;
    public final String studentName;
    public final String department;
    public final LocalDateTime timestamp;

    public StudentEnrolledEvent(String studentId, String studentName, String department) {
        this.studentId = studentId;
        this.studentName = studentName;
        this.department = department;
        this.timestamp = LocalDateTime.now();
    }

    @Override
    public String toString() {
        return String.format("[StudentEnrolledEvent] Student %s (%s) enrolled in %s at %s", 
            studentName, studentId, department, timestamp);
    }
}
