package sms.runtime;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.OutputStream;
import java.io.PrintStream;
import sms.communication.gateway.WebServer;
import sms.system_services.data.DatabaseManager;
import sms.system_services.events.*;
import sms.observability.AuditLogger;
import sms.domain.analytics.StudentRiskAnalyzer;
import sms.domain.attendance.AttendanceAlertEngine;

public class SwingLauncher {
    private JFrame frame;
    private JTextArea logArea;
    private JButton startBtn;
    private JButton stopBtn;
    private WebServer server;
    private Thread serverThread;

    public SwingLauncher() {
        frame = new JFrame("Academic System Environment - Server Console");
        frame.setSize(700, 500);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        // Top Panel with Controls
        JPanel topPanel = new JPanel();
        topPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        topPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 10, 0));

        startBtn = new JButton("Start Web Gateway");
        startBtn.setBackground(new Color(40, 167, 69));
        startBtn.setForeground(Color.WHITE);
        startBtn.setFocusPainted(false);
        
        stopBtn = new JButton("Stop Server");
        stopBtn.setBackground(new Color(220, 53, 69));
        stopBtn.setForeground(Color.WHITE);
        stopBtn.setFocusPainted(false);
        stopBtn.setEnabled(false);

        JButton browserBtn = new JButton("Open Dashboard");
        browserBtn.addActionListener(e -> {
            try {
                Desktop.getDesktop().browse(new java.net.URI("http://localhost:8080"));
            } catch(Exception ex) {}
        });

        topPanel.add(new JLabel("Control Panel: "));
        topPanel.add(startBtn);
        topPanel.add(stopBtn);
        topPanel.add(browserBtn);

        frame.add(topPanel, BorderLayout.NORTH);

        // Center Panel with Log Output
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font("Consolas", Font.PLAIN, 12));
        logArea.setBackground(new Color(13, 17, 23));
        logArea.setForeground(new Color(201, 209, 217));
        JScrollPane scrollPane = new JScrollPane(logArea);
        scrollPane.setBorder(BorderFactory.createTitledBorder("System Telemetry"));
        frame.add(scrollPane, BorderLayout.CENTER);

        // Event Handling
        startBtn.addActionListener(this::startServer);
        stopBtn.addActionListener(this::stopServer);

        // Redirect System.out to the GUI
        System.setOut(new PrintStream(new OutputStream() {
            @Override
            public void write(int b) {
                SwingUtilities.invokeLater(() -> {
                    logArea.append(String.valueOf((char)b));
                    logArea.setCaretPosition(logArea.getDocument().getLength());
                });
            }
        }));

        frame.setLocationRelativeTo(null);
    }

    private void startServer(ActionEvent e) {
        startBtn.setEnabled(false);
        
        serverThread = new Thread(() -> {
            try {
                System.out.println("=== ACADEMIC SYSTEM ENVIRONMENT ===");
                System.out.println("Initializing Database...");
                DatabaseManager.getInstance();

                System.out.println("Configuring Event Bus Subscriptions...");
                EventBus bus = EventBus.getInstance();
                bus.subscribe(MarkUpdatedEvent.class, new AuditLogger());
                bus.subscribe(MarkUpdatedEvent.class, new StudentRiskAnalyzer());
                bus.subscribe(AttendanceChangedEvent.class, new AttendanceAlertEngine());

                System.out.println("Starting Web Server on port 8080...");
                server = new WebServer();
                server.start(8080);
                
                SwingUtilities.invokeLater(() -> stopBtn.setEnabled(true));
            } catch(Exception ex) {
                ex.printStackTrace();
                SwingUtilities.invokeLater(() -> startBtn.setEnabled(true));
            }
        });
        serverThread.start();
    }

    private void stopServer(ActionEvent e) {
        if(server != null) {
            System.out.println("Shutting down Web Gateway...");
            server.stop();
            server = null;
        }
        stopBtn.setEnabled(false);
        startBtn.setEnabled(true);
        System.out.println("Server stopped.");
    }

    public void show() {
        frame.setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new SwingLauncher().show());
    }
}
