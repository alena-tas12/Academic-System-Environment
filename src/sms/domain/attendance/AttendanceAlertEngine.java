package sms.domain.attendance;
import sms.system_services.events.EventListener;
import sms.system_services.events.AttendanceChangedEvent;

public class AttendanceAlertEngine implements EventListener {
    @Override
    public void onEvent(Object event) {
        if (event instanceof AttendanceChangedEvent) {
            AttendanceChangedEvent ace = (AttendanceChangedEvent) event;
            if (ace.currentPercentage < 75.0) {
                System.out.println("[AlertEngine] WARNING: Student " + ace.studentId + " attendance dropped to " + ace.currentPercentage + "%. Minimum threshold is 75%.");
            }
        }
    }
}
