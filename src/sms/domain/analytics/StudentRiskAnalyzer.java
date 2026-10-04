package sms.domain.analytics;
import sms.system_services.events.EventListener;
import sms.system_services.events.MarkUpdatedEvent;

public class StudentRiskAnalyzer implements EventListener {
    @Override
    public void onEvent(Object event) {
        if (event instanceof MarkUpdatedEvent) {
            MarkUpdatedEvent mue = (MarkUpdatedEvent) event;
            System.out.println("[RiskAnalyzer] Analyzing risk for student " + mue.studentId + " after mark update...");
            if (mue.newMark < 40) {
                System.out.println("[RiskAnalyzer] ALERT: Student " + mue.studentId + " is at HIGH academic risk.");
            } else {
                System.out.println("[RiskAnalyzer] Student " + mue.studentId + " risk is LOW.");
            }
        }
    }
}
