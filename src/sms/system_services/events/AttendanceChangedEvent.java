package sms.system_services.events;
public class AttendanceChangedEvent {
    public String studentId;
    public double currentPercentage;
    public AttendanceChangedEvent(String studentId, double currentPercentage) {
        this.studentId = studentId;
        this.currentPercentage = currentPercentage;
    }
}
