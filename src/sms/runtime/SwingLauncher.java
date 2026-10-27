package sms.runtime;

import sms.communication.gateway.AcademicSystemGUI;
import sms.communication.gateway.WebServer;
import sms.system_services.data.DatabaseManager;
import sms.system_services.events.*;
import sms.observability.AuditLogger;
import sms.domain.analytics.StudentRiskAnalyzer;
import sms.domain.attendance.AttendanceAlertEngine;

import javax.swing.SwingUtilities;

/**
 * Native Java Entrypoint for the Academic System Environment.
 * Satisfies the Swing GUI & Event Handling requirements.
 */
public class SwingLauncher {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                System.out.println("=== ACADEMIC SYSTEM ENVIRONMENT ===");
                System.out.println("Initializing Database Connection...");
                DatabaseManager.getInstance();

                System.out.println("Configuring Event Bus Subscriptions...");
                EventBus bus = EventBus.getInstance();
                bus.subscribe(MarkUpdatedEvent.class, new AuditLogger());
                bus.subscribe(MarkUpdatedEvent.class, new StudentRiskAnalyzer());
                bus.subscribe(AttendanceChangedEvent.class, new AttendanceAlertEngine());

                System.out.println("Starting Web Gateway on http://localhost:8080...");
                WebServer server = new WebServer();
                try {
                    server.start(8080);
                } catch (Exception ex) {
                    System.out.println("[Notice] Web server already running or port in use: " + ex.getMessage());
                }

                System.out.println("Launching Desktop GUI...");
                AcademicSystemGUI gui = new AcademicSystemGUI(server);
                gui.setVisible(true);

            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}
