package src.ui.view;

import src.models.*;
import src.models.CourseOffering;
import src.services.*;
import src.ui.components.StyledComboBox;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.io.File;
import java.util.List;

/**
 * ImportPanel — dedicated panel for bulk CSV imports.
 *
 * Flow:
 *   1. User selects entity type from combo box
 *   2. Clicks "Choose File" → picks .csv
 *   3. Clicks "Import" → SwingWorker parses + inserts
 *   4. Two tables appear: ✅ Imported and ❌ Failed (with reasons)
 */
public class ImportPanel extends JPanel {

    private JComboBox<String> entityBox;
    private JLabel fileLabel;
    private File selectedFile;

    private JLabel summaryLabel;

    private DefaultTableModel successModel;
    private DefaultTableModel failedModel;
    private JTable successTable;
    private JTable failedTable;

    private JPanel resultsPanel;

    // Services
    private final StudentService          studentService          = new StudentService();
    private final CourseService           courseService           = new CourseService();
    private final HallService             hallService             = new HallService();
    private final InvigilatorService      invigilatorService      = new InvigilatorService();
    private final ExamService             examService             = new ExamService();
    private final CourseOfferingService   courseOfferingService   = new CourseOfferingService();

    // Column headers per entity type
    private static final String[][] COLUMNS = {
            {"Course Code", "Department", "Semester"},               // Course Offerings
            {"Roll Number", "Name", "Department", "Semester"},       // Students
            {"Course Code", "Course Name", "Credits"},                // Courses
            {"Hall Name", "Seating Capacity"},                        // Halls
            {"Name", "Department", "Email"},                          // Invigilators
            {"Course Code", "Exam Date", "Session"},                  // Exams
    };

    private static final String[] ENTITIES = {
            "Course Offerings", "Students", "Courses", "Halls", "Invigilators", "Exams"
    };

    public ImportPanel() {
        setLayout(new BorderLayout(25, 25));
        setBorder(BorderFactory.createEmptyBorder(30, 40, 30, 40));
        setBackground(new Color(15, 15, 20));

        add(createTitle(), BorderLayout.NORTH);
        add(createContent(), BorderLayout.CENTER);
    }

    private JLabel createTitle() {
        JLabel title = new JLabel("IMPORT");
        title.setForeground(Color.WHITE);
        title.setFont(new Font("Segoe UI", Font.BOLD, 30));
        return title;
    }

    private JPanel createContent() {
        JPanel content = new JPanel(new BorderLayout(20, 20));
        content.setOpaque(false);

        content.add(createControlsPanel(), BorderLayout.NORTH);
        content.add(createResultsPanel(), BorderLayout.CENTER);

        return content;
    }

    // ── CONTROLS (entity picker, file chooser, import button) ──

    private JPanel createControlsPanel() {
        JPanel container = createGlowPanel(25);
        container.setLayout(new BorderLayout());
        container.setBorder(BorderFactory.createEmptyBorder(25, 25, 25, 25));

        JPanel controls = new JPanel(new GridBagLayout());
        controls.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Entity selector
        entityBox = StyledComboBox.create(ENTITIES);

        // File label
        fileLabel = new JLabel("No file selected");
        fileLabel.setForeground(new Color(160, 160, 160));
        fileLabel.setFont(new Font("Segoe UI", Font.ITALIC, 14));

        // Buttons
        JButton chooseBtn = createStyledButton("Choose CSV File");
        JButton importBtn = createStyledButton("⚡ Import to Database");

        chooseBtn.addActionListener(e -> chooseFile());
        importBtn.addActionListener(e -> runImport());

        // Summary
        summaryLabel = new JLabel(" ");
        summaryLabel.setForeground(new Color(0, 200, 255));
        summaryLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));

        // Layout
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 1;
        controls.add(createLabel("Import Type:"), gbc);
        gbc.gridx = 1; gbc.gridwidth = 2;
        controls.add(entityBox, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 1;
        controls.add(createLabel("File:"), gbc);
        gbc.gridx = 1;
        controls.add(fileLabel, gbc);
        gbc.gridx = 2;
        controls.add(chooseBtn, gbc);

        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 3;
        JPanel btnPanel = new JPanel(); btnPanel.setOpaque(false);
        btnPanel.add(importBtn);
        controls.add(btnPanel, gbc);

        gbc.gridy = 3;
        controls.add(summaryLabel, gbc);

        // CSV format hint — changes based on selected entity
        JLabel hintLabel = new JLabel();
        hintLabel.setForeground(new Color(120, 120, 120));
        hintLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        updateHint(hintLabel);

        entityBox.addActionListener(e -> updateHint(hintLabel));

        gbc.gridy = 4;
        controls.add(hintLabel, gbc);

        container.add(controls);
        return container;
    }

    private void updateHint(JLabel hintLabel) {
        if (!StyledComboBox.hasSelection(entityBox)) {
            hintLabel.setText("Select an import type to see the expected CSV format.");
            return;
        }
        String sel = StyledComboBox.getValue(entityBox);
        String hint = switch (sel) {
            case "Course Offerings" -> "CSV format: course_code, department, semester";
            case "Students"         -> "CSV format: roll_number, name, department, semester";
            case "Courses"          -> "CSV format: course_code, course_name, credits";
            case "Halls"            -> "CSV format: hall_name, seating_capacity";
            case "Invigilators"     -> "CSV format: name, department, email";
            case "Exams"            -> "CSV format: course_code, exam_date (YYYY-MM-DD), session (FN/AN)";
            default                  -> "";
        };
        hintLabel.setText("📋 " + hint + "  (first row = header, will be skipped)");
    }

    // ── RESULTS (two tables: success + failed) ──

    private JPanel createResultsPanel() {
        resultsPanel = new JPanel(new GridLayout(1, 2, 20, 0));
        resultsPanel.setOpaque(false);

        // Success table
        JPanel successPanel = createGlowPanel(20);
        successPanel.setLayout(new BorderLayout());
        successPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JLabel successTitle = new JLabel("✅ Imported Successfully");
        successTitle.setForeground(new Color(0, 200, 100));
        successTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));

        successModel = new DefaultTableModel();
        successTable = new JTable(successModel);
        styleResultTable(successTable, new Color(0, 200, 100));

        JScrollPane successScroll = new JScrollPane(successTable);
        successScroll.setBorder(null);
        successScroll.getViewport().setBackground(new Color(22, 22, 30));

        successPanel.add(successTitle, BorderLayout.NORTH);
        successPanel.add(successScroll, BorderLayout.CENTER);

        // Failed table
        JPanel failedPanel = createGlowPanel(20);
        failedPanel.setLayout(new BorderLayout());
        failedPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JLabel failedTitle = new JLabel("❌ Failed to Import");
        failedTitle.setForeground(new Color(220, 60, 60));
        failedTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));

        failedModel = new DefaultTableModel();
        failedTable = new JTable(failedModel);
        styleResultTable(failedTable, new Color(220, 60, 60));

        // Color the "Reason" column red
        failedTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(
                        table, value, isSelected, hasFocus, row, column);
                l.setBackground(isSelected ? new Color(0, 140, 255) : new Color(22, 22, 30));
                // Last column is always "Reason"
                if (column == table.getColumnCount() - 1) {
                    l.setForeground(isSelected ? Color.WHITE : new Color(255, 100, 100));
                } else {
                    l.setForeground(isSelected ? Color.WHITE : Color.WHITE);
                }
                return l;
            }
        });

        JScrollPane failedScroll = new JScrollPane(failedTable);
        failedScroll.setBorder(null);
        failedScroll.getViewport().setBackground(new Color(22, 22, 30));

        failedPanel.add(failedTitle, BorderLayout.NORTH);
        failedPanel.add(failedScroll, BorderLayout.CENTER);

        resultsPanel.add(successPanel);
        resultsPanel.add(failedPanel);

        return resultsPanel;
    }

    private void styleResultTable(JTable table, Color headerColor) {
        table.setRowHeight(35);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        table.setForeground(Color.WHITE);
        table.setBackground(new Color(22, 22, 30));
        table.setGridColor(new Color(40, 40, 55));
        table.setSelectionBackground(new Color(0, 140, 255));
        table.setSelectionForeground(Color.WHITE);

        JTableHeader header = table.getTableHeader();
        header.setFont(new Font("Segoe UI", Font.BOLD, 14));
        header.setForeground(headerColor);
        header.setBackground(new Color(18, 18, 25));
    }

    // ── FILE CHOOSER ──

    private void chooseFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select CSV File");
        chooser.setFileFilter(new FileNameExtensionFilter("CSV Files (*.csv)", "csv"));
        chooser.setAcceptAllFileFilterUsed(false);

        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            selectedFile = chooser.getSelectedFile();
            fileLabel.setText(selectedFile.getName());
            fileLabel.setForeground(new Color(0, 200, 255));
        }
    }

    // ── IMPORT LOGIC ──

    private void runImport() {
        // Validate selections
        if (!StyledComboBox.hasSelection(entityBox)) {
            showPopup("Please select what you want to import (Students, Courses, etc.)");
            return;
        }

        if (selectedFile == null) {
            showPopup("Please choose a CSV file first.");
            return;
        }

        // Validate file type BEFORE starting the worker
        try {
            CsvImporter.validateFile(selectedFile);
        } catch (IllegalArgumentException ex) {
            showPopup(ex.getMessage());
            return;
        }

        String entity = StyledComboBox.getValue(entityBox);

        // Block all imports except Course Offerings and Courses when the table is empty
        if (!entity.equals("Course Offerings") && !entity.equals("Courses") && !courseOfferingService.hasOfferings()) {
            showPopup("Course Offerings table is empty."+
                    "Please import Course Offerings first before importing  entity" + ".");
            return;
        }

        summaryLabel.setText("Importing...");

        SwingWorker<ImportResult, Void> worker = new SwingWorker<>() {
            @Override
            protected ImportResult doInBackground() {
                return switch (entity) {
                    case "Course Offerings" -> {
                        CsvImporter.ParsedData<CourseOffering> data = CsvImporter.parseCourseOfferings(selectedFile);
                        yield courseOfferingService.importBatch(data);
                    }
                    case "Students" -> {
                        CsvImporter.ParsedData<Students> data = CsvImporter.parseStudents(selectedFile);
                        yield studentService.importBatch(data);
                    }
                    case "Courses" -> {
                        CsvImporter.ParsedData<Courses> data = CsvImporter.parseCourses(selectedFile);
                        yield courseService.importBatch(data);
                    }
                    case "Halls" -> {
                        CsvImporter.ParsedData<Hall> data = CsvImporter.parseHalls(selectedFile);
                        yield hallService.importBatch(data);
                    }
                    case "Invigilators" -> {
                        CsvImporter.ParsedData<Invigilator> data = CsvImporter.parseInvigilators(selectedFile);
                        yield invigilatorService.importBatch(data);
                    }
                    case "Exams" -> {
                        CsvImporter.ParsedData<Exam> data = CsvImporter.parseExams(selectedFile);
                        yield examService.importBatch(data);
                    }
                    default -> throw new IllegalStateException("Unknown entity: " + entity);
                };
            }

            @Override
            protected void done() {
                try {
                    ImportResult result = get();
                    displayResults(entity, result);
                    summaryLabel.setText(result.summary());
                } catch (Exception e) {
                    String msg = e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
                    showPopup("Import failed: " + msg);
                    summaryLabel.setText("Import failed.");
                }
            }
        };

        worker.execute();
    }

    private void displayResults(String entity, ImportResult result) {
        int entityIdx = getEntityIndex(entity);
        String[] cols = COLUMNS[entityIdx];

        // ── Success table ──
        String[] successCols = cols;
        successModel.setColumnIdentifiers(successCols);
        successModel.setRowCount(0);
        for (String[] row : result.getSucceeded()) {
            // Pad row if shorter than columns
            Object[] paddedRow = new Object[successCols.length];
            for (int i = 0; i < paddedRow.length; i++) {
                paddedRow[i] = (i < row.length) ? row[i].trim() : "";
            }
            successModel.addRow(paddedRow);
        }

        // ── Failed table (same columns + "Reason" at the end) ──
        String[] failedCols = new String[cols.length + 1];
        System.arraycopy(cols, 0, failedCols, 0, cols.length);
        failedCols[cols.length] = "Reason";
        failedModel.setColumnIdentifiers(failedCols);
        failedModel.setRowCount(0);

        for (ImportResult.FailedRow fr : result.getFailed()) {
            String[] rawRow = fr.getRow();
            Object[] displayRow = new Object[failedCols.length];
            for (int i = 0; i < cols.length; i++) {
                displayRow[i] = (i < rawRow.length) ? rawRow[i].trim() : "";
            }
            displayRow[cols.length] = fr.getReason();
            failedModel.addRow(displayRow);
        }

        // Refresh UI
        successTable.revalidate();
        successTable.repaint();
        failedTable.revalidate();
        failedTable.repaint();
    }

    private int getEntityIndex(String entity) {
        for (int i = 0; i < ENTITIES.length; i++) {
            if (ENTITIES[i].equals(entity)) return i;
        }
        return 0;
    }

    // ── SHARED UI ──

    private JPanel createGlowPanel(int arc) {
        return new JPanel() {
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                for (int i = 8; i >= 2; i -= 2) {
                    g2.setColor(new Color(0, 180, 255, 35));
                    g2.setStroke(new BasicStroke(i));
                    g2.drawRoundRect(i / 2, i / 2, getWidth() - i, getHeight() - i, arc, arc);
                }
                g2.setColor(new Color(22, 22, 30));
                g2.fillRoundRect(6, 6, getWidth() - 12, getHeight() - 12, arc, arc);
                g2.dispose();
            }
        };
    }

    private JButton createStyledButton(String text) {
        JButton btn = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int arc = 25;
                for (int i = 8; i >= 2; i -= 2) {
                    g2.setColor(new Color(0, 180, 255, 40));
                    g2.setStroke(new BasicStroke(i));
                    g2.drawRoundRect(i / 2, i / 2, getWidth() - i, getHeight() - i, arc, arc);
                }
                g2.setColor(new Color(0, 140, 255));
                g2.fillRoundRect(6, 6, getWidth() - 12, getHeight() - 12, arc, arc);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setOpaque(false); btn.setContentAreaFilled(false);
        btn.setBorder(BorderFactory.createEmptyBorder(12, 30, 12, 30));
        btn.setFont(new Font("Segoe UI", Font.BOLD, 16));
        btn.setForeground(Color.WHITE); btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private JLabel createLabel(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(Color.WHITE);
        l.setFont(new Font("Segoe UI", Font.BOLD, 16));
        return l;
    }

    private void showPopup(String message) {
        JDialog d = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), true);
        d.setUndecorated(true);
        JPanel p = new JPanel(new BorderLayout(20, 20));
        p.setBackground(new Color(22, 22, 30));
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0, 200, 255), 2),
                BorderFactory.createEmptyBorder(25, 30, 25, 30)));
        JLabel msg = new JLabel("<html>" + message.replace("\n", "<br>") + "</html>", SwingConstants.CENTER);
        msg.setForeground(Color.WHITE); msg.setFont(new Font("Segoe UI", Font.BOLD, 15));
        JButton ok = new JButton("OK");
        ok.setFocusPainted(false); ok.setFont(new Font("Segoe UI", Font.BOLD, 14));
        ok.setForeground(Color.WHITE); ok.setBackground(new Color(0, 140, 255));
        ok.setPreferredSize(new Dimension(90, 35)); ok.addActionListener(e -> d.dispose());
        JPanel bp = new JPanel(); bp.setOpaque(false); bp.add(ok);
        p.add(msg, BorderLayout.CENTER); p.add(bp, BorderLayout.SOUTH);
        d.add(p); d.pack(); d.setLocationRelativeTo(this); d.setVisible(true);
    }
}