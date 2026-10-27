package sms.system_services.events;

import java.time.LocalDateTime;

public class StudentDeletedEvent {
    public final String studentId;
    public final LocalDateTime timestamp;

    public StudentDeletedEvent(String studentId) {
        this.studentId = studentId;
        this.timestamp = LocalDateTime.now();
    }

    @Override
    public String toString() {
        return String.format("[StudentDeletedEvent] Student %s removed from system at %s", 
            studentId, timestamp);
    }
}
