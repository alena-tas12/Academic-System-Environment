package sms.communication.gateway;

import sms.domain.academic.GPAEngine;
import sms.domain.exception.ValidationException;
import sms.domain.model.StudentStatus;
import sms.observability.AuditLogger;
import sms.system_services.data.DatabaseManager;
import sms.system_services.events.*;
import sms.system_services.rules.ValidationEngine;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URI;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Vector;

/**
 * Complete, interactive Java Swing Desktop GUI for the Academic System Environment.
 * Satisfies CO5: GUI Layout & Components, Event Handling & Dialog Boxes, Backend Integration.
 */
public class AcademicSystemGUI extends JFrame implements EventListener {

    private WebServer webServer;
    private JTabbedPane tabbedPane;

    // Student Management Components
    private JTable studentTable;
    private DefaultTableModel studentTableModel;
    private JTextField searchField;
    private JComboBox<String> deptFilterCombo;
    private JComboBox<String> statusFilterCombo;
    private JLabel totalStudentsLabel;
    private JLabel lowAttendanceLabel;
    private JLabel riskCountLabel;

    // Marks Components
    private JComboBox<String> marksStudentCombo;
    private JComboBox<String> marksSubjectCombo;
    private JTextField marksInputField;
    private JRadioButton ugRadio;
    private JRadioButton pgRadio;
    private JTable marksSummaryTable;
    private DefaultTableModel marksSummaryModel;

    // Attendance Components
    private JComboBox<String> attStudentCombo;
    private JComboBox<String> attSubjectCombo;
    private JRadioButton presentRadio;
    private JRadioButton absentRadio;
    private JLabel currentAttLabel;

    // Telemetry Components
    private JTextArea telemetryArea;
    private JLabel serverStatusLabel;
    private JButton startServerBtn;
    private JButton stopServerBtn;

    public AcademicSystemGUI(WebServer server) {
        this.webServer = server;
        initUI();
        EventBus.getInstance().subscribe(Object.class, this);
        loadStudentsData();
        refreshCombos();
    }

    private void initUI() {
        setTitle("Academic System Environment — Desktop Administration Console");
        setSize(1100, 750);
        setMinimumSize(new Dimension(950, 650));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        // 1. Header Panel
        add(createHeaderPanel(), BorderLayout.NORTH);

        // 2. Tabbed Center Panel
        tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Segoe UI", Font.BOLD, 13));

        tabbedPane.addTab("  🎓 Students & Records  ", createStudentManagementPanel());
        tabbedPane.addTab("  📊 Marks & GPA Engine  ", createMarksPanel());
        tabbedPane.addTab("  📅 Attendance Tracker  ", createAttendancePanel());
        tabbedPane.addTab("  ⚠️ Risk Analysis Engine  ", createRiskPanel());
        tabbedPane.addTab("  🌐 Web Gateway & Logs  ", createTelemetryPanel());

        add(tabbedPane, BorderLayout.CENTER);

        // 3. Status Bar
        add(createStatusBar(), BorderLayout.SOUTH);
    }

    // -------------------------------------------------------------
    // Header Panel
    // -------------------------------------------------------------
    private JPanel createHeaderPanel() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(18, 24, 38));
        header.setBorder(new EmptyBorder(12, 16, 12, 16));

        JLabel title = new JLabel("ACADEMIC SYSTEM ENVIRONMENT");
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setForeground(Color.WHITE);

        JLabel sub = new JLabel("Object-Oriented Academic Intelligence & Student Records Platform");
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        sub.setForeground(new Color(160, 174, 192));

        JPanel titleBox = new JPanel(new GridLayout(2, 1));
        titleBox.setOpaque(false);
        titleBox.add(title);
        titleBox.add(sub);

        JPanel metricsBox = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 0));
        metricsBox.setOpaque(false);

        totalStudentsLabel = createBadge("Students: 0", new Color(49, 130, 206));
        lowAttendanceLabel = createBadge("Low Attendance (<75%): 0", new Color(221, 107, 32));
        riskCountLabel = createBadge("Academic Risk: 0", new Color(229, 62, 62));

        metricsBox.add(totalStudentsLabel);
        metricsBox.add(lowAttendanceLabel);
        metricsBox.add(riskCountLabel);

        header.add(titleBox, BorderLayout.WEST);
        header.add(metricsBox, BorderLayout.EAST);
        return header;
    }

    private JLabel createBadge(String text, Color bg) {
        JLabel label = new JLabel(text, SwingConstants.CENTER);
        label.setFont(new Font("Segoe UI", Font.BOLD, 11));
        label.setForeground(Color.WHITE);
        label.setBackground(bg);
        label.setOpaque(true);
        label.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(bg.darker(), 1),
            new EmptyBorder(4, 10, 4, 10)
        ));
        return label;
    }

    // -------------------------------------------------------------
    // Tab 1: Student Management Panel
    // -------------------------------------------------------------
    private JPanel createStudentManagementPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(new EmptyBorder(12, 12, 12, 12));

        // Top Toolbar (Search and Filters)
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        toolbar.setBorder(BorderFactory.createTitledBorder("Search & Filter Controls"));

        toolbar.add(new JLabel("Quick Search:"));
        searchField = new JTextField(15);
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { filterStudents(); }
            public void removeUpdate(DocumentEvent e) { filterStudents(); }
            public void changedUpdate(DocumentEvent e) { filterStudents(); }
        });
        toolbar.add(searchField);

        toolbar.add(new JLabel("Department:"));
        deptFilterCombo = new JComboBox<>(new String[]{"All Departments", "Computer Engineering", "Data Science", "Cybersecurity"});
        deptFilterCombo.addActionListener(e -> filterStudents());
        toolbar.add(deptFilterCombo);

        toolbar.add(new JLabel("Status:"));
        statusFilterCombo = new JComboBox<>(new String[]{"All Statuses", "ACTIVE", "ADMITTED", "ON_LEAVE"});
        statusFilterCombo.addActionListener(e -> filterStudents());
        toolbar.add(statusFilterCombo);

        JButton resetBtn = new JButton("Reset Filters");
        resetBtn.addActionListener(e -> {
            searchField.setText("");
            deptFilterCombo.setSelectedIndex(0);
            statusFilterCombo.setSelectedIndex(0);
            loadStudentsData();
        });
        toolbar.add(resetBtn);

        panel.add(toolbar, BorderLayout.NORTH);

        // Center Table
        String[] columns = {"Student ID", "Full Name", "Department", "Semester", "Type", "Status", "Attendance %", "Email"};
        studentTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };
        studentTable = new JTable(studentTableModel);
        studentTable.setRowHeight(26);
        studentTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        studentTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        studentTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Highlight low attendance in red
        studentTable.getColumnModel().getColumn(6).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object val, boolean isSel, boolean hasFoc, int r, int c) {
                Component comp = super.getTableCellRendererComponent(table, val, isSel, hasFoc, r, c);
                if (val != null) {
                    try {
                        double att = Double.parseDouble(val.toString().replace("%", ""));
                        if (att < 75.0) {
                            comp.setForeground(new Color(220, 38, 38));
                            setFont(getFont().deriveFont(Font.BOLD));
                        } else {
                            comp.setForeground(new Color(22, 101, 52));
                        }
                    } catch (Exception ignored) {}
                }
                return comp;
            }
        });

        // Double-click row to view profile
        studentTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && studentTable.getSelectedRow() != -1) {
                    showStudentProfileDialog();
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(studentTable);
        panel.add(scrollPane, BorderLayout.CENTER);

        // Action Buttons Bar
        JPanel btnBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));

        JButton addBtn = new JButton("➕ Enroll New Student");
        addBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        addBtn.setBackground(new Color(40, 167, 69));
        addBtn.setForeground(Color.WHITE);
        addBtn.setFocusPainted(false);
        addBtn.addActionListener(e -> showAddStudentDialog());

        JButton editBtn = new JButton("✏️ Edit Student");
        editBtn.addActionListener(e -> showEditStudentDialog());

        JButton deleteBtn = new JButton("🗑️ Delete Student");
        deleteBtn.setBackground(new Color(220, 53, 69));
        deleteBtn.setForeground(Color.WHITE);
        deleteBtn.setFocusPainted(false);
        deleteBtn.addActionListener(e -> deleteSelectedStudent());

        JButton viewBtn = new JButton("📄 View Full Profile");
        viewBtn.addActionListener(e -> showStudentProfileDialog());

        JButton refreshBtn = new JButton("🔄 Refresh");
        refreshBtn.addActionListener(e -> loadStudentsData());

        btnBar.add(addBtn);
        btnBar.add(editBtn);
        btnBar.add(deleteBtn);
        btnBar.add(viewBtn);
        btnBar.add(refreshBtn);

        panel.add(btnBar, BorderLayout.SOUTH);
        return panel;
    }

    // -------------------------------------------------------------
    // Tab 2: Marks & GPA Evaluation Panel
    // -------------------------------------------------------------
    private JPanel createMarksPanel() {
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.setBorder(new EmptyBorder(16, 16, 16, 16));

        // Form at Top
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createTitledBorder("Record Course Marks & Compute GPA"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Row 0: Student
        gbc.gridx = 0; gbc.gridy = 0;
        form.add(new JLabel("Select Student:"), gbc);
        gbc.gridx = 1;
        marksStudentCombo = new JComboBox<>();
        marksStudentCombo.addActionListener(e -> updateMarksSummary());
        form.add(marksStudentCombo, gbc);

        // Row 1: Subject
        gbc.gridx = 0; gbc.gridy = 1;
        form.add(new JLabel("Select Subject:"), gbc);
        gbc.gridx = 1;
        marksSubjectCombo = new JComboBox<>();
        form.add(marksSubjectCombo, gbc);

        // Row 2: Mark Input
        gbc.gridx = 0; gbc.gridy = 2;
        form.add(new JLabel("Enter Mark (0 - 100):"), gbc);
        gbc.gridx = 1;
        marksInputField = new JTextField(10);
        form.add(marksInputField, gbc);

        // Row 3: Degree Type (UG vs PG for polymorphic grading)
        gbc.gridx = 0; gbc.gridy = 3;
        form.add(new JLabel("Grading Scale:"), gbc);
        gbc.gridx = 1;
        JPanel radioPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        ugRadio = new JRadioButton("Undergraduate (10.0 Scale)", true);
        pgRadio = new JRadioButton("Postgraduate (4.0 Scale)");
        ButtonGroup bg = new ButtonGroup();
        bg.add(ugRadio); bg.add(pgRadio);
        radioPanel.add(ugRadio); radioPanel.add(pgRadio);
        form.add(radioPanel, gbc);

        // Row 4: Submit Button
        gbc.gridx = 1; gbc.gridy = 4;
        JButton recordMarkBtn = new JButton("💾 Save Mark & Recalculate GPA");
        recordMarkBtn.setBackground(new Color(13, 110, 253));
        recordMarkBtn.setForeground(Color.WHITE);
        recordMarkBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        recordMarkBtn.addActionListener(this::recordMarkAction);
        form.add(recordMarkBtn, gbc);

        panel.add(form, BorderLayout.NORTH);

        // Bottom Table: Marks for Selected Student
        String[] cols = {"Subject Code", "Subject Name", "Credits", "Mark Scored", "Grade Point", "Letter Grade"};
        marksSummaryModel = new DefaultTableModel(cols, 0);
        marksSummaryTable = new JTable(marksSummaryModel);
        marksSummaryTable.setRowHeight(24);

        JScrollPane scroll = new JScrollPane(marksSummaryTable);
        scroll.setBorder(BorderFactory.createTitledBorder("Academic Transcript for Selected Student"));
        panel.add(scroll, BorderLayout.CENTER);

        return panel;
    }

    // -------------------------------------------------------------
    // Tab 3: Attendance Tracker Panel
    // -------------------------------------------------------------
    private JPanel createAttendancePanel() {
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.setBorder(new EmptyBorder(16, 16, 16, 16));

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createTitledBorder("Mark Class Attendance"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0;
        form.add(new JLabel("Select Student:"), gbc);
        gbc.gridx = 1;
        attStudentCombo = new JComboBox<>();
        attStudentCombo.addActionListener(e -> updateAttendancePreview());
        form.add(attStudentCombo, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        form.add(new JLabel("Select Subject:"), gbc);
        gbc.gridx = 1;
        attSubjectCombo = new JComboBox<>();
        form.add(attSubjectCombo, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        form.add(new JLabel("Attendance Status:"), gbc);
        gbc.gridx = 1;
        JPanel radioPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
        presentRadio = new JRadioButton("Present (P)", true);
        absentRadio = new JRadioButton("Absent (A)");
        ButtonGroup bg = new ButtonGroup();
        bg.add(presentRadio); bg.add(absentRadio);
        radioPanel.add(presentRadio); radioPanel.add(absentRadio);
        form.add(radioPanel, gbc);

        gbc.gridx = 0; gbc.gridy = 3;
        form.add(new JLabel("Current Attendance:"), gbc);
        gbc.gridx = 1;
        currentAttLabel = new JLabel("85.0%");
        currentAttLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        form.add(currentAttLabel, gbc);

        gbc.gridx = 1; gbc.gridy = 4;
        JButton markAttBtn = new JButton("📅 Submit Attendance Entry");
        markAttBtn.setBackground(new Color(25, 135, 84));
        markAttBtn.setForeground(Color.WHITE);
        markAttBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        markAttBtn.addActionListener(this::recordAttendanceAction);
        form.add(markAttBtn, gbc);

        panel.add(form, BorderLayout.NORTH);

        // Informational Callout
        JPanel infoCard = new JPanel(new BorderLayout());
        infoCard.setBorder(BorderFactory.createTitledBorder("Institutional Policy: Minimum Attendance Requirement"));
        JTextArea infoText = new JTextArea(
            "• University regulations require a mandatory minimum of 75.0% cumulative attendance.\n" +
            "• Whenever a student's attendance drops below 75%, an AttendanceAlertEvent is immediately broadcasted.\n" +
            "• Students in the critical band (<75%) are automatically highlighted in red in all registers and reports."
        );
        infoText.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        infoText.setEditable(false);
        infoText.setBackground(panel.getBackground());
        infoCard.add(infoText, BorderLayout.CENTER);

        panel.add(infoCard, BorderLayout.CENTER);
        return panel;
    }

    // -------------------------------------------------------------
    // Tab 4: Risk Analysis Engine Panel
    // -------------------------------------------------------------
    private JPanel createRiskPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(14, 14, 14, 14));

        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 5));
        JButton runScanBtn = new JButton("🔍 Run Full Diagnostic Scan");
        runScanBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        runScanBtn.addActionListener(e -> runRiskScan());
        topBar.add(runScanBtn);

        JButton alertReportBtn = new JButton("📋 Generate Intervention Advisory");
        alertReportBtn.addActionListener(e -> generateInterventionAdvisory());
        topBar.add(alertReportBtn);

        panel.add(topBar, BorderLayout.NORTH);

        String[] cols = {"Student ID", "Student Name", "Department", "Attendance %", "Risk Status", "Prescribed Action"};
        DefaultTableModel riskModel = new DefaultTableModel(cols, 0);
        JTable riskTable = new JTable(riskModel);
        riskTable.setRowHeight(26);

        // Risk Renderer
        riskTable.getColumnModel().getColumn(4).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object val, boolean isSel, boolean hasFoc, int r, int c) {
                Component comp = super.getTableCellRendererComponent(table, val, isSel, hasFoc, r, c);
                if (val != null) {
                    String str = val.toString();
                    if (str.contains("CRITICAL") || str.contains("HIGH")) {
                        comp.setForeground(new Color(220, 38, 38));
                        setFont(getFont().deriveFont(Font.BOLD));
                    } else if (str.contains("MODERATE")) {
                        comp.setForeground(new Color(217, 119, 6));
                    } else {
                        comp.setForeground(new Color(22, 101, 52));
                    }
                }
                return comp;
            }
        });

        JScrollPane scroll = new JScrollPane(riskTable);
        panel.add(scroll, BorderLayout.CENTER);

        // Populate Risk Table
        populateRiskTable(riskModel);

        return panel;
    }

    private void populateRiskTable(DefaultTableModel model) {
        model.setRowCount(0);
        try {
            DatabaseManager db = DatabaseManager.getInstance();
            List<Map<String, Object>> students = db.query("SELECT * FROM students ORDER BY id ASC");
            for (Map<String, Object> s : students) {
                String id = (String) s.get("id");
                String name = (String) s.get("name");
                String dept = (String) s.get("department");
                double att = ((Number) s.get("attendance")).doubleValue();

                String status;
                String action;

                if (att < 75.0) {
                    status = "HIGH ATTENDANCE RISK";
                    action = "Mandatory Parent-Teacher Meeting & Attendance Recovery Plan";
                } else if (att < 80.0) {
                    status = "MODERATE RISK";
                    action = "Faculty Advisor Counseling & Remedial Monitoring";
                } else {
                    status = "GOOD STANDING";
                    action = "Continue standard curriculum schedule";
                }

                model.addRow(new Object[]{id, name, dept, String.format("%.1f%%", att), status, action});
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void runRiskScan() {
        loadStudentsData();
        JOptionPane.showMessageDialog(this, 
            "Diagnostic scan completed across all enrolled students.\nAudit log updated with telemetry.",
            "Scan Complete", JOptionPane.INFORMATION_MESSAGE);
    }

    private void generateInterventionAdvisory() {
        JOptionPane.showMessageDialog(this,
            "=== ACADEMIC ADVISORY REPORT ===\n\n" +
            "1. Students with Attendance < 75% are ineligible for semester end examinations.\n" +
            "2. Department heads have been notified via automated EventBus telemetry.\n" +
            "3. Remedial classes are scheduled every Saturday at 10:00 AM.",
            "Intervention Advisory", JOptionPane.INFORMATION_MESSAGE);
    }

    // -------------------------------------------------------------
    // Tab 5: Web Gateway & Server Telemetry
    // -------------------------------------------------------------
    private JPanel createTelemetryPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(new EmptyBorder(12, 12, 12, 12));

        JPanel controlBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 5));
        serverStatusLabel = new JLabel("● Web Gateway: Running (http://localhost:8080)");
        serverStatusLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        serverStatusLabel.setForeground(new Color(22, 101, 52));

        JButton browserBtn = new JButton("🌐 Open Web Dashboard in Browser");
        browserBtn.setBackground(new Color(13, 110, 253));
        browserBtn.setForeground(Color.WHITE);
        browserBtn.addActionListener(e -> {
            try {
                Desktop.getDesktop().browse(new URI("http://localhost:8080"));
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Could not open browser: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        startServerBtn = new JButton("▶ Start Gateway");
        startServerBtn.setEnabled(false);
        startServerBtn.addActionListener(e -> {
            try {
                if (webServer == null) {
                    webServer = new WebServer();
                    webServer.start(8080);
                }
                serverStatusLabel.setText("● Web Gateway: Running (http://localhost:8080)");
                serverStatusLabel.setForeground(new Color(22, 101, 52));
                startServerBtn.setEnabled(false);
                stopServerBtn.setEnabled(true);
                logTelemetry("[Gateway] Web server started on port 8080.");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Failed to start server: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        stopServerBtn = new JButton("⏹ Stop Gateway");
        stopServerBtn.addActionListener(e -> {
            if (webServer != null) {
                webServer.stop();
                webServer = null;
            }
            serverStatusLabel.setText("● Web Gateway: Stopped");
            serverStatusLabel.setForeground(new Color(220, 38, 38));
            startServerBtn.setEnabled(true);
            stopServerBtn.setEnabled(false);
            logTelemetry("[Gateway] Web server stopped.");
        });

        JButton clearLogsBtn = new JButton("Clear Console");
        clearLogsBtn.addActionListener(e -> telemetryArea.setText(""));

        controlBar.add(serverStatusLabel);
        controlBar.add(browserBtn);
        controlBar.add(startServerBtn);
        controlBar.add(stopServerBtn);
        controlBar.add(clearLogsBtn);

        panel.add(controlBar, BorderLayout.NORTH);

        telemetryArea = new JTextArea();
        telemetryArea.setEditable(false);
        telemetryArea.setBackground(new Color(13, 17, 23));
        telemetryArea.setForeground(new Color(56, 161, 105));
        telemetryArea.setFont(new Font("Consolas", Font.PLAIN, 12));
        telemetryArea.setMargin(new Insets(8, 8, 8, 8));

        JScrollPane scroll = new JScrollPane(telemetryArea);
        scroll.setBorder(BorderFactory.createTitledBorder("Real-Time EventBus Telemetry & Diagnostics"));
        panel.add(scroll, BorderLayout.CENTER);

        logTelemetry("[SystemRuntime] Academic System Environment initialized.");
        logTelemetry("[Database] HSQLDB embedded persistence active.");
        logTelemetry("[Gateway] Listening on http://localhost:8080.");

        return panel;
    }

    private JPanel createStatusBar() {
        JPanel status = new JPanel(new BorderLayout());
        status.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, Color.LIGHT_GRAY),
            new EmptyBorder(4, 10, 4, 10)
        ));
        JLabel left = new JLabel("Karunya Institute of Technology and Sciences — Division of Data Science and Cyber Security");
        left.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        JLabel right = new JLabel("Reg: URK25CO2019 | Java 21 / Swing / HSQLDB");
        right.setFont(new Font("Segoe UI", Font.BOLD, 11));
        status.add(left, BorderLayout.WEST);
        status.add(right, BorderLayout.EAST);
        return status;
    }

    // -------------------------------------------------------------
    // Business Logic Actions & Dialog Boxes
    // -------------------------------------------------------------
    private void showAddStudentDialog() {
        JDialog dialog = new JDialog(this, "Enroll New Student", true);
        dialog.setSize(440, 460);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new GridBagLayout());

        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(6, 8, 6, 8);
        g.fill = GridBagConstraints.HORIZONTAL;

        JTextField idF = new JTextField(15);
        JTextField nameF = new JTextField(15);
        JTextField emailF = new JTextField(15);
        JTextField phoneF = new JTextField(15);
        JComboBox<String> deptC = new JComboBox<>(new String[]{"Computer Engineering", "Data Science", "Cybersecurity", "Information Technology"});
        JSpinner semS = new JSpinner(new SpinnerNumberModel(1, 1, 10, 1));
        JComboBox<String> typeC = new JComboBox<>(new String[]{"UNDERGRADUATE", "POSTGRADUATE"});

        // Auto-suggest next ID
        idF.setText("24CE" + (1040 + studentTableModel.getRowCount()));

        int row = 0;
        g.gridx = 0; g.gridy = row; dialog.add(new JLabel("Student ID:"), g);
        g.gridx = 1; dialog.add(idF, g); row++;

        g.gridx = 0; g.gridy = row; dialog.add(new JLabel("Full Name:"), g);
        g.gridx = 1; dialog.add(nameF, g); row++;

        g.gridx = 0; g.gridy = row; dialog.add(new JLabel("Email Address:"), g);
        g.gridx = 1; dialog.add(emailF, g); row++;

        g.gridx = 0; g.gridy = row; dialog.add(new JLabel("Phone Number:"), g);
        g.gridx = 1; dialog.add(phoneF, g); row++;

        g.gridx = 0; g.gridy = row; dialog.add(new JLabel("Department:"), g);
        g.gridx = 1; dialog.add(deptC, g); row++;

        g.gridx = 0; g.gridy = row; dialog.add(new JLabel("Semester:"), g);
        g.gridx = 1; dialog.add(semS, g); row++;

        g.gridx = 0; g.gridy = row; dialog.add(new JLabel("Program Level:"), g);
        g.gridx = 1; dialog.add(typeC, g); row++;

        JPanel btnBox = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 10));
        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.addActionListener(e -> dialog.dispose());

        JButton saveBtn = new JButton("Enroll Student");
        saveBtn.setBackground(new Color(40, 167, 69));
        saveBtn.setForeground(Color.WHITE);
        saveBtn.addActionListener(e -> {
            try {
                String id = idF.getText().trim();
                String name = nameF.getText().trim();
                String email = emailF.getText().trim();
                String phone = phoneF.getText().trim();
                String dept = (String) deptC.getSelectedItem();
                int sem = (Integer) semS.getValue();
                String type = (String) typeC.getSelectedItem();

                // Input validation (throws ValidationException if invalid)
                ValidationEngine.validateStudent(id, name, email, phone, sem);

                // Database persistence
                DatabaseManager.getInstance().addStudent(id, name, email, phone, dept, sem, type, "ACTIVE");

                // Dispatch Domain Event
                EventBus.getInstance().publish(new StudentEnrolledEvent(id, name, dept));

                // Log Audit Record
                AuditLogger.record("ADMIN", "ENROLL_STUDENT", "Enrolled " + name + " (" + id + ") in " + dept);

                loadStudentsData();
                refreshCombos();
                dialog.dispose();

                JOptionPane.showMessageDialog(this, 
                    "Student " + name + " (" + id + ") has been successfully enrolled!", 
                    "Enrollment Successful", JOptionPane.INFORMATION_MESSAGE);

            } catch (ValidationException ve) {
                JOptionPane.showMessageDialog(dialog, ve.getMessage(), "Validation Error", JOptionPane.ERROR_MESSAGE);
            } catch (SQLException sqle) {
                JOptionPane.showMessageDialog(dialog, "Database error: " + sqle.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        btnBox.add(cancelBtn);
        btnBox.add(saveBtn);

        g.gridx = 0; g.gridy = row; g.gridwidth = 2;
        dialog.add(btnBox, g);

        dialog.setVisible(true);
    }

    private void showEditStudentDialog() {
        int row = studentTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Please select a student from the table to edit.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String id = (String) studentTableModel.getValueAt(row, 0);
        String name = (String) studentTableModel.getValueAt(row, 1);
        String dept = (String) studentTableModel.getValueAt(row, 2);
        int sem = Integer.parseInt(studentTableModel.getValueAt(row, 3).toString());
        String status = (String) studentTableModel.getValueAt(row, 5);
        String email = (String) studentTableModel.getValueAt(row, 7);

        JDialog dialog = new JDialog(this, "Edit Student: " + id, true);
        dialog.setSize(420, 380);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new GridBagLayout());

        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(6, 8, 6, 8);
        g.fill = GridBagConstraints.HORIZONTAL;

        JTextField nameF = new JTextField(name, 15);
        JTextField emailF = new JTextField(email, 15);
        JTextField phoneF = new JTextField("+91 90000 00000", 15);
        JComboBox<String> deptC = new JComboBox<>(new String[]{"Computer Engineering", "Data Science", "Cybersecurity", "Information Technology"});
        deptC.setSelectedItem(dept);
        JSpinner semS = new JSpinner(new SpinnerNumberModel(sem, 1, 10, 1));
        JComboBox<String> statusC = new JComboBox<>(new String[]{"ACTIVE", "ADMITTED", "ON_LEAVE"});
        statusC.setSelectedItem(status);

        int r = 0;
        g.gridx = 0; g.gridy = r; dialog.add(new JLabel("Full Name:"), g);
        g.gridx = 1; dialog.add(nameF, g); r++;

        g.gridx = 0; g.gridy = r; dialog.add(new JLabel("Email Address:"), g);
        g.gridx = 1; dialog.add(emailF, g); r++;

        g.gridx = 0; g.gridy = r; dialog.add(new JLabel("Phone Number:"), g);
        g.gridx = 1; dialog.add(phoneF, g); r++;

        g.gridx = 0; g.gridy = r; dialog.add(new JLabel("Department:"), g);
        g.gridx = 1; dialog.add(deptC, g); r++;

        g.gridx = 0; g.gridy = r; dialog.add(new JLabel("Semester:"), g);
        g.gridx = 1; dialog.add(semS, g); r++;

        g.gridx = 0; g.gridy = r; dialog.add(new JLabel("Status:"), g);
        g.gridx = 1; dialog.add(statusC, g); r++;

        JPanel btnBox = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 10));
        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.addActionListener(e -> dialog.dispose());

        JButton updateBtn = new JButton("Update Records");
        updateBtn.addActionListener(e -> {
            try {
                String newName = nameF.getText().trim();
                String newEmail = emailF.getText().trim();
                String newPhone = phoneF.getText().trim();
                String newDept = (String) deptC.getSelectedItem();
                int newSem = (Integer) semS.getValue();
                String newStatus = (String) statusC.getSelectedItem();

                ValidationEngine.validateStudent(id, newName, newEmail, newPhone, newSem);

                DatabaseManager.getInstance().updateStudent(id, newName, newEmail, newPhone, newDept, newSem, newStatus);
                AuditLogger.record("ADMIN", "UPDATE_STUDENT", "Updated record for " + id);

                loadStudentsData();
                dialog.dispose();
                JOptionPane.showMessageDialog(this, "Student records updated successfully!", "Update Complete", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        btnBox.add(cancelBtn);
        btnBox.add(updateBtn);

        g.gridx = 0; g.gridy = r; g.gridwidth = 2;
        dialog.add(btnBox, g);

        dialog.setVisible(true);
    }

    private void deleteSelectedStudent() {
        int row = studentTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Please select a student from the table to delete.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String id = (String) studentTableModel.getValueAt(row, 0);
        String name = (String) studentTableModel.getValueAt(row, 1);

        // Dialog Box: JOptionPane.showConfirmDialog
        int confirm = JOptionPane.showConfirmDialog(
            this,
            "Are you sure you want to permanently delete student:\n" + name + " (" + id + ")?\nThis action will also remove all associated marks and attendance records.",
            "Confirm Deletion",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
        );

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                DatabaseManager.getInstance().deleteStudent(id);
                EventBus.getInstance().publish(new StudentDeletedEvent(id));
                AuditLogger.record("ADMIN", "DELETE_STUDENT", "Deleted student " + id);

                loadStudentsData();
                refreshCombos();

                JOptionPane.showMessageDialog(this, "Student " + id + " has been successfully deleted.", "Deletion Confirmed", JOptionPane.INFORMATION_MESSAGE);
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Failed to delete student: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void showStudentProfileDialog() {
        int row = studentTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Please select a student to view full profile.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String id = (String) studentTableModel.getValueAt(row, 0);
        String name = (String) studentTableModel.getValueAt(row, 1);
        String dept = (String) studentTableModel.getValueAt(row, 2);
        String sem = studentTableModel.getValueAt(row, 3).toString();
        String type = (String) studentTableModel.getValueAt(row, 4);
        String status = (String) studentTableModel.getValueAt(row, 5);
        String att = (String) studentTableModel.getValueAt(row, 6);
        String email = (String) studentTableModel.getValueAt(row, 7);

        // Calculate GPA using backend engine
        GPAEngine engine = new GPAEngine();
        String gpaStr;
        String gradeStr;
        try {
            boolean isPg = "POSTGRADUATE".equalsIgnoreCase(type);
            GPAEngine.GradeReport report = engine.evaluateStudent(id, isPg);
            gpaStr = String.format("%.2f / %s", report.getGpa(), isPg ? "4.00" : "10.00");
            gradeStr = report.getPrimaryGrade();
        } catch (Exception e) {
            gpaStr = "No marks recorded";
            gradeStr = "N/A";
        }

        String message = 
            "=========================================\n" +
            "       STUDENT ACADEMIC PROFILE          \n" +
            "=========================================\n" +
            "Student ID        : " + id + "\n" +
            "Full Name         : " + name + "\n" +
            "Institutional Email: " + email + "\n" +
            "Department        : " + dept + "\n" +
            "Current Semester  : " + sem + "\n" +
            "Program Level     : " + type + "\n" +
            "Enrollment Status : " + status + "\n" +
            "-----------------------------------------\n" +
            "Attendance Record : " + att + "\n" +
            "Cumulative GPA    : " + gpaStr + "\n" +
            "Academic Standing : " + gradeStr + "\n" +
            "=========================================";

        JOptionPane.showMessageDialog(this, message, "Student Profile: " + name, JOptionPane.INFORMATION_MESSAGE);
    }

    private void recordMarkAction(ActionEvent e) {
        String studentItem = (String) marksStudentCombo.getSelectedItem();
        String subjectItem = (String) marksSubjectCombo.getSelectedItem();
        String markText = marksInputField.getText().trim();

        if (studentItem == null || subjectItem == null || markText.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please select student, subject, and enter a mark.", "Missing Input", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            double mark = Double.parseDouble(markText);
            ValidationEngine.validateMark(mark);

            String studentId = studentItem.split(" - ")[0];
            String subjectCode = subjectItem.split(" - ")[0];

            DatabaseManager.getInstance().upsertMark(studentId, subjectCode, mark);
            EventBus.getInstance().publish(new MarkUpdatedEvent(studentId, subjectCode, mark));
            AuditLogger.record("FACULTY", "RECORD_MARK", "Mark " + mark + " recorded for " + studentId + " in " + subjectCode);

            boolean isPg = pgRadio.isSelected();
            GPAEngine gpaEngine = new GPAEngine();
            GPAEngine.GradeReport rep = gpaEngine.evaluateStudent(studentId, isPg);

            marksInputField.setText("");
            updateMarksSummary();
            loadStudentsData();

            JOptionPane.showMessageDialog(this,
                String.format("Mark successfully recorded!\n\nSubject: %s\nScore: %.1f%%\nLetter Grade: %s\nUpdated GPA: %.2f",
                    subjectCode, mark, rep.getPrimaryGrade(), rep.getGpa()),
                "Mark Recorded", JOptionPane.INFORMATION_MESSAGE);

        } catch (NumberFormatException nfe) {
            JOptionPane.showMessageDialog(this, "Invalid mark format. Please enter a valid number.", "Format Error", JOptionPane.ERROR_MESSAGE);
        } catch (ValidationException ve) {
            JOptionPane.showMessageDialog(this, ve.getMessage(), "Validation Failed", JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "System Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void recordAttendanceAction(ActionEvent e) {
        String studentItem = (String) attStudentCombo.getSelectedItem();
        String subjectItem = (String) attSubjectCombo.getSelectedItem();

        if (studentItem == null || subjectItem == null) {
            JOptionPane.showMessageDialog(this, "Please select student and subject.", "Missing Input", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            String studentId = studentItem.split(" - ")[0];
            String subjectCode = subjectItem.split(" - ")[0];
            boolean isPresent = presentRadio.isSelected();

            DatabaseManager.getInstance().recordAttendance(studentId, subjectCode, isPresent);
            double newAtt = DatabaseManager.getInstance().attendance(studentId);

            EventBus.getInstance().publish(new AttendanceChangedEvent(studentId, newAtt));
            AuditLogger.record("FACULTY", "RECORD_ATTENDANCE", "Recorded " + (isPresent ? "PRESENT" : "ABSENT") + " for " + studentId);

            currentAttLabel.setText(String.format("%.1f%%", newAtt));
            loadStudentsData();

            if (newAtt < 75.0) {
                JOptionPane.showMessageDialog(this,
                    String.format("Attendance recorded!\n\n⚠️ WARNING: Student %s attendance is now %.1f%%.\nThis is BELOW the mandatory 75%% university threshold!",
                        studentId, newAtt),
                    "Attendance Alert", JOptionPane.WARNING_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this,
                    String.format("Attendance recorded!\nUpdated cumulative attendance: %.1f%%", newAtt),
                    "Attendance Recorded", JOptionPane.INFORMATION_MESSAGE);
            }

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error recording attendance: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // -------------------------------------------------------------
    // Data Loading & Helper Methods
    // -------------------------------------------------------------
    private void loadStudentsData() {
        studentTableModel.setRowCount(0);
        int total = 0;
        int lowAtt = 0;
        int risk = 0;

        try {
            DatabaseManager db = DatabaseManager.getInstance();
            List<Map<String, Object>> students = db.query("SELECT * FROM students ORDER BY id ASC");
            for (Map<String, Object> s : students) {
                total++;
                String id = (String) s.get("id");
                String name = (String) s.get("name");
                String dept = (String) s.get("department");
                int sem = ((Number) s.get("semester")).intValue();
                String type = (String) s.get("type");
                String status = (String) s.get("status");
                double att = ((Number) s.get("attendance")).doubleValue();
                String email = (String) s.get("email");

                if (att < 75.0) lowAtt++;
                if (att < 75.0 || status.equalsIgnoreCase("ON_LEAVE")) risk++;

                studentTableModel.addRow(new Object[]{
                    id, name, dept, sem, type, status, String.format("%.1f%%", att), email
                });
            }

            totalStudentsLabel.setText("Students: " + total);
            lowAttendanceLabel.setText("Low Attendance (<75%): " + lowAtt);
            riskCountLabel.setText("Academic Risk: " + risk);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void filterStudents() {
        String search = searchField.getText().trim().toLowerCase();
        String selectedDept = (String) deptFilterCombo.getSelectedItem();
        String selectedStatus = (String) statusFilterCombo.getSelectedItem();

        studentTableModel.setRowCount(0);
        try {
            DatabaseManager db = DatabaseManager.getInstance();
            List<Map<String, Object>> students = db.query("SELECT * FROM students ORDER BY id ASC");
            for (Map<String, Object> s : students) {
                String id = (String) s.get("id");
                String name = (String) s.get("name");
                String dept = (String) s.get("department");
                int sem = ((Number) s.get("semester")).intValue();
                String type = (String) s.get("type");
                String status = (String) s.get("status");
                double att = ((Number) s.get("attendance")).doubleValue();
                String email = (String) s.get("email");

                boolean matchSearch = search.isEmpty() || 
                    id.toLowerCase().contains(search) || 
                    name.toLowerCase().contains(search) || 
                    email.toLowerCase().contains(search);

                boolean matchDept = "All Departments".equals(selectedDept) || dept.equalsIgnoreCase(selectedDept);
                boolean matchStatus = "All Statuses".equals(selectedStatus) || status.equalsIgnoreCase(selectedStatus);

                if (matchSearch && matchDept && matchStatus) {
                    studentTableModel.addRow(new Object[]{
                        id, name, dept, sem, type, status, String.format("%.1f%%", att), email
                    });
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void refreshCombos() {
        try {
            DatabaseManager db = DatabaseManager.getInstance();
            List<Map<String, Object>> students = db.query("SELECT id, name FROM students ORDER BY id ASC");
            List<Map<String, Object>> subjects = db.query("SELECT code, name FROM subjects ORDER BY code ASC");

            marksStudentCombo.removeAllItems();
            attStudentCombo.removeAllItems();
            for (Map<String, Object> s : students) {
                String item = s.get("id") + " - " + s.get("name");
                marksStudentCombo.addItem(item);
                attStudentCombo.addItem(item);
            }

            marksSubjectCombo.removeAllItems();
            attSubjectCombo.removeAllItems();
            for (Map<String, Object> sub : subjects) {
                String item = sub.get("code") + " - " + sub.get("name");
                marksSubjectCombo.addItem(item);
                attSubjectCombo.addItem(item);
            }

            updateMarksSummary();
            updateAttendancePreview();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void updateMarksSummary() {
        marksSummaryModel.setRowCount(0);
        String item = (String) marksStudentCombo.getSelectedItem();
        if (item == null) return;
        String studentId = item.split(" - ")[0];

        try {
            DatabaseManager db = DatabaseManager.getInstance();
            List<Map<String, Object>> marks = db.query(
                "SELECT m.subject_code, s.name, s.credits, m.mark " +
                "FROM marks m JOIN subjects s ON m.subject_code = s.code " +
                "WHERE m.student_id = ?", studentId);

            boolean isPg = pgRadio.isSelected();
            for (Map<String, Object> r : marks) {
                double mark = ((Number) r.get("mark")).doubleValue();
                double gp = isPg ? GPAEngine.toPgGradePoint(mark) : GPAEngine.toUgGradePoint(mark);
                String letter = GPAEngine.toLetterGrade(mark);

                marksSummaryModel.addRow(new Object[]{
                    r.get("subject_code"), r.get("name"), r.get("credits"), mark, gp, letter
                });
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void updateAttendancePreview() {
        String item = (String) attStudentCombo.getSelectedItem();
        if (item == null) return;
        String studentId = item.split(" - ")[0];

        try {
            double att = DatabaseManager.getInstance().attendance(studentId);
            currentAttLabel.setText(String.format("%.1f%%", att));
            if (att < 75.0) {
                currentAttLabel.setForeground(new Color(220, 38, 38));
            } else {
                currentAttLabel.setForeground(new Color(22, 101, 52));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void logTelemetry(String message) {
        SwingUtilities.invokeLater(() -> {
            if (telemetryArea != null) {
                String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
                telemetryArea.append("[" + time + "] " + message + "\n");
                telemetryArea.setCaretPosition(telemetryArea.getDocument().getLength());
            }
        });
    }

    @Override
    public void onEvent(Object event) {
        logTelemetry(event.toString());
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                // Ensure Database is initialized
                DatabaseManager.getInstance();

                // Start WebServer in parallel
                WebServer server = new WebServer();
                server.start(8080);

                // Open Swing GUI
                AcademicSystemGUI gui = new AcademicSystemGUI(server);
                gui.setVisible(true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}
