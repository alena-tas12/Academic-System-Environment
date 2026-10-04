package sms.system_services.events;
public class MarkUpdatedEvent {
    public String studentId;
    public String subjectCode;
    public double newMark;
    public MarkUpdatedEvent(String studentId, String subjectCode, double newMark) {
        this.studentId = studentId;
        this.subjectCode = subjectCode;
        this.newMark = newMark;
    }
}
