package sms.domain.attendance;
import sms.system_services.events.EventBus;
import sms.system_services.events.AttendanceChangedEvent;

public class AttendanceService {
    public void recordAttendance(String studentId, boolean isPresent) {
        System.out.println("[AttendanceService] Recording attendance for " + studentId + ": " + (isPresent ? "Present" : "Absent"));
        // Mock logic: assume new percentage is calculated based on history
        double newPercentage = isPresent ? 82.5 : 74.0; 
        EventBus.getInstance().publish(new AttendanceChangedEvent(studentId, newPercentage));
    }
}
