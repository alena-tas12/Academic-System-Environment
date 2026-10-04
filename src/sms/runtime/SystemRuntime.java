package sms.runtime;

import sms.communication.gateway.WebServer;
import sms.system_services.data.DatabaseManager;
import sms.system_services.events.*;
import sms.observability.AuditLogger;
import sms.domain.analytics.StudentRiskAnalyzer;
import sms.domain.attendance.AttendanceAlertEngine;

public class SystemRuntime {
    public static void main(String[] args) {
        System.out.println("=== ACADEMIC SYSTEM ENVIRONMENT ===");
        System.out.println("Starting unified Java runtime...");
        DatabaseManager.getInstance();

        EventBus bus = EventBus.getInstance();
        bus.subscribe(MarkUpdatedEvent.class, new AuditLogger());
        bus.subscribe(MarkUpdatedEvent.class, new StudentRiskAnalyzer());
        bus.subscribe(AttendanceChangedEvent.class, new AttendanceAlertEngine());

        try {
            new WebServer().start(args.length > 0 ? Integer.parseInt(args[0]) : 8080);
            System.out.println("Frontend + backend are running from the same Java process.");
            System.out.println("Open the URL printed above.");
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
