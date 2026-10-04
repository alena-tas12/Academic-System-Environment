package sms.domain.academic;
import sms.system_services.events.EventBus;
import sms.system_services.events.MarkUpdatedEvent;

public class AcademicService {
    public void updateMark(String studentId, String subjectCode, double newMark) {
        System.out.println("[AcademicService] Updating mark for " + studentId + " to " + newMark);
        // Trigger event
        EventBus.getInstance().publish(new MarkUpdatedEvent(studentId, subjectCode, newMark));
    }
}
