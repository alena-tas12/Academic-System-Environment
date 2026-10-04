package sms.communication.gateway;

import sms.system_services.events.EventBus;
import sms.system_services.events.EventListener;

import javax.swing.*;
import java.awt.*;

public class DashboardUI extends JFrame implements EventListener {
    private JTextArea eventLogArea;

    public DashboardUI() {
        setTitle("Student Management & Academic Intelligence System");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Top Panel: Metrics
        JPanel metricsPanel = new JPanel(new GridLayout(1, 3, 10, 10));
        metricsPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        metricsPanel.setBackground(Color.DARK_GRAY);

        metricsPanel.add(createMetricCard("ACTIVE STUDENTS", "247", Color.CYAN));
        metricsPanel.add(createMetricCard("ATTENDANCE RISK", "18", Color.ORANGE));
        metricsPanel.add(createMetricCard("ACADEMIC RISK", "23", Color.RED));

        add(metricsPanel, BorderLayout.NORTH);

        // Center Panel: System Event Monitor
        JPanel eventPanel = new JPanel(new BorderLayout());
        eventPanel.setBorder(BorderFactory.createTitledBorder("SYSTEM EVENT MONITOR / DIAGNOSTICS"));
        
        eventLogArea = new JTextArea();
        eventLogArea.setEditable(false);
        eventLogArea.setBackground(Color.BLACK);
        eventLogArea.setForeground(Color.GREEN);
        eventLogArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        
        JScrollPane scrollPane = new JScrollPane(eventLogArea);
        eventPanel.add(scrollPane, BorderLayout.CENTER);

        add(eventPanel, BorderLayout.CENTER);

        // Subscribe to all events for diagnostic monitoring
        EventBus.getInstance().subscribe(Object.class, this); // Catch-all for diagnostic UI
    }

    private JPanel createMetricCard(String title, String value, Color color) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        
        JLabel titleLabel = new JLabel(title, SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 12));
        
        JLabel valueLabel = new JLabel(value, SwingConstants.CENTER);
        valueLabel.setFont(new Font("Arial", Font.BOLD, 24));
        valueLabel.setForeground(color);
        
        card.add(titleLabel, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);
        return card;
    }

    public void logSystemEvent(String message) {
        SwingUtilities.invokeLater(() -> {
            eventLogArea.append(message + "\n");
        });
    }

    @Override
    public void onEvent(Object event) {
        // In a real system, we'd format based on event type.
        logSystemEvent(String.format("[SYSTEM EVENT] %s dispatched to subscribers.", event.getClass().getSimpleName()));
    }
}
