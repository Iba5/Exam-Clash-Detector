package src.ui.view;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.DefaultTableCellRenderer;

import src.fsm.ExamFSM;
import src.fsm.ExamState;
import src.models.Courses;
import src.models.Exam;
import src.services.CourseService;
import src.services.ExamService;
import src.ui.components.StyledComboBox;
import src.ui.components.TableSearchBar;

public class ExamPanel extends JPanel {

    private JTextField examIdField, dateField, sessionField;
    private JComboBox<String> courseBox;
    private JTable examTable;
    private DefaultTableModel tableModel;
    private CourseService courseService = new CourseService();
    private ExamService examService = new ExamService();
    private JSplitPane splitPane;
    private JPanel formContainerRef, tableContainerRef;

    // FSM buttons
    private JButton scheduleBtn, startBtn, completeBtn, cancelBtn;

    public ExamPanel() {
        setLayout(new BorderLayout(25, 25));
        setBorder(BorderFactory.createEmptyBorder(30, 40, 30, 40));
        setBackground(new Color(15, 15, 20));
        add(createTitle(), BorderLayout.NORTH);
        add(createMainContent(), BorderLayout.CENTER);
        loadCourses();
        loadTable();
        addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentShown(java.awt.event.ComponentEvent e) {
                loadCourses();
                loadTable();
            }
        });
    }

    private JLabel createTitle() {
        JLabel title = new JLabel("EXAMS");
        title.setForeground(Color.WHITE);
        title.setFont(new Font("Segoe UI", Font.BOLD, 30));
        return title;
    }

    private JPanel createMainContent() {
        JPanel main = new JPanel(new BorderLayout(25, 25));
        main.setOpaque(false);
        formContainerRef = createFormContainer();
        tableContainerRef = createTableContainer();
        splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, formContainerRef, tableContainerRef);
        splitPane.setDividerSize(0); splitPane.setBorder(null);
        splitPane.setResizeWeight(0.55); splitPane.setOpaque(false);
        splitPane.setBackground(new Color(15, 15, 20));
        main.add(splitPane, BorderLayout.CENTER);
        addMouseResizeLogic();
        return main;
    }

    private void addMouseResizeLogic() {
        formContainerRef.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                SwingUtilities.invokeLater(() -> splitPane.setDividerLocation(0.65));
            }
        });
        tableContainerRef.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                SwingUtilities.invokeLater(() -> splitPane.setDividerLocation(0.35));
            }
        });
    }

    private JPanel createFormContainer() {
        JPanel container = createGlowPanel(25);
        container.setLayout(new BorderLayout());
        container.setBorder(BorderFactory.createEmptyBorder(25, 25, 25, 25));
        container.add(createFormPanel(), BorderLayout.CENTER);
        return container;
    }

    private JPanel createFormPanel() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        examIdField = createStyledField(); dateField = createStyledField(); sessionField = createStyledField();
        courseBox = StyledComboBox.create(new String[]{});

        JButton addBtn = createStyledButton("Add");
        JButton updateBtn = createStyledButton("Update");
        JButton deleteBtn = createStyledButton("Delete");

        // FSM lifecycle buttons
        scheduleBtn = createStyledButton("→ Schedule");
        startBtn    = createStyledButton("▶ Start");
        completeBtn = createStyledButton("✔ Complete");
        cancelBtn   = createStyledButton("✖ Cancel");

        addBtn.addActionListener(e -> addExam());
        updateBtn.addActionListener(e -> updateExam());
        deleteBtn.addActionListener(e -> deleteExam());
        scheduleBtn.addActionListener(e -> transitionExam(ExamState.SCHEDULED));
        startBtn.addActionListener(e -> startExamWithAttendance());
        completeBtn.addActionListener(e -> transitionExam(ExamState.COMPLETED));
        cancelBtn.addActionListener(e -> transitionExam(ExamState.CANCELLED));

        // Disable lifecycle buttons until a row is selected
        setLifecycleButtonsEnabled(false);

        gbc.gridx = 0; gbc.gridy = 0; form.add(createLabel("Exam ID:"), gbc);
        gbc.gridx = 1; form.add(examIdField, gbc);
        gbc.gridx = 0; gbc.gridy = 1; form.add(createLabel("Course:"), gbc);
        gbc.gridx = 1; form.add(courseBox, gbc);
        gbc.gridx = 0; gbc.gridy = 2; form.add(createLabel("Date (YYYY-MM-DD):"), gbc);
        gbc.gridx = 1; form.add(dateField, gbc);
        gbc.gridx = 0; gbc.gridy = 3; form.add(createLabel("Session (FN / AN):"), gbc);
        gbc.gridx = 1; form.add(sessionField, gbc);

        // CRUD buttons
        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 2;
        JPanel btnPanel = new JPanel(); btnPanel.setOpaque(false);
        btnPanel.add(addBtn); btnPanel.add(updateBtn); btnPanel.add(deleteBtn);
        form.add(btnPanel, gbc);

        // FSM buttons
        gbc.gridy = 5;
        JPanel fsmPanel = new JPanel(); fsmPanel.setOpaque(false);
        fsmPanel.add(scheduleBtn); fsmPanel.add(startBtn);
        fsmPanel.add(completeBtn); fsmPanel.add(cancelBtn);
        form.add(fsmPanel, gbc);

        return form;
    }

    private JPanel createTableContainer() {
        JPanel container = createGlowPanel(30);
        container.setLayout(new BorderLayout());
        container.setBorder(BorderFactory.createEmptyBorder(25, 25, 25, 25));
        JScrollPane scroll = createTable();
        container.add(new TableSearchBar(examTable), BorderLayout.NORTH);
        container.add(scroll, BorderLayout.CENTER);
        return container;
    }

    private JScrollPane createTable() {
        tableModel = new DefaultTableModel();
        tableModel.setColumnIdentifiers(new String[]{"Exam ID", "Course", "Date", "Session", "State"});
        examTable = new JTable(tableModel);
        examTable.setRowHeight(40);
        examTable.setFont(new Font("Segoe UI", Font.PLAIN, 18));
        examTable.setForeground(Color.WHITE);
        examTable.setBackground(new Color(22, 22, 30));
        examTable.setGridColor(Color.WHITE);
        examTable.setSelectionBackground(new Color(0, 150, 255));
        examTable.setSelectionForeground(Color.WHITE);

        // Color the State column
        examTable.getColumnModel().getColumn(4).setCellRenderer(new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable t, Object val,
                    boolean sel, boolean focus, int row, int col) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(t, val, sel, focus, row, col);
                if (!sel && val != null) {
                    ExamState s = ExamState.fromString(val.toString());
                    l.setForeground(s.getColor());
                }
                l.setBackground(sel ? new Color(0, 150, 255) : new Color(22, 22, 30));
                return l;
            }
        });

        JTableHeader header = examTable.getTableHeader();
        header.setFont(new Font("Segoe UI", Font.BOLD, 20));
        header.setForeground(new Color(0, 220, 255));
        header.setBackground(new Color(18, 18, 25));
        header.setPreferredSize(new Dimension(header.getWidth(), 50));

        examTable.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                int row = examTable.getSelectedRow();
                if (row >= 0) {
                    // Convert view row to model row (important when filtered/sorted)
                    int modelRow = examTable.convertRowIndexToModel(row);
                    examIdField.setText(tableModel.getValueAt(modelRow, 0).toString());
                    examIdField.setEditable(false);
                    dateField.setText(tableModel.getValueAt(modelRow, 2).toString());
                    sessionField.setText(tableModel.getValueAt(modelRow, 3).toString());
                    String course = tableModel.getValueAt(modelRow, 1).toString();
                    for (int i = 0; i < courseBox.getItemCount(); i++) {
                        if (courseBox.getItemAt(i).startsWith(course)) { courseBox.setSelectedIndex(i); break; }
                    }
                    // Enable/disable FSM buttons based on current state
                    String stateStr = tableModel.getValueAt(modelRow, 4).toString();
                    updateLifecycleButtons(ExamState.fromString(stateStr));
                }
            }
        });

        JScrollPane scroll = new JScrollPane(examTable);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(new Color(22, 22, 30));
        return scroll;
    }

    private void updateLifecycleButtons(ExamState current) {
        scheduleBtn.setEnabled(ExamFSM.canTransition(current, ExamState.SCHEDULED));
        startBtn.setEnabled(ExamFSM.canTransition(current, ExamState.ONGOING));
        completeBtn.setEnabled(ExamFSM.canTransition(current, ExamState.COMPLETED));
        cancelBtn.setEnabled(ExamFSM.canTransition(current, ExamState.CANCELLED));
    }

    private void setLifecycleButtonsEnabled(boolean enabled) {
        scheduleBtn.setEnabled(enabled);
        startBtn.setEnabled(enabled);
        completeBtn.setEnabled(enabled);
        cancelBtn.setEnabled(enabled);
    }

    private void transitionExam(ExamState target) {
        try {
            if (examIdField.getText().trim().isEmpty()) { showPopup("Select an exam first"); return; }
            int examId = Integer.parseInt(examIdField.getText().trim());
            if (!showConfirmPopup("Move exam to " + target.getLabel() + "?")) return;
            examService.transition(examId, target);
            showPopup("Exam → " + target.getLabel());
            loadTable(); clearFields();
        } catch (Exception ex) {
            showPopup(ex.getMessage());
        }
    }

    private void startExamWithAttendance() {
        try {
            if (examIdField.getText().trim().isEmpty()) { showPopup("Select an exam first"); return; }
            int examId = Integer.parseInt(examIdField.getText().trim());

            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Select Attendance CSV (present students only)");
            chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("CSV Files (*.csv)", "csv"));
            chooser.setAcceptAllFileFilterUsed(false);

            int result = chooser.showOpenDialog(this);
            if (result != JFileChooser.APPROVE_OPTION) return;

            java.io.File csvFile = chooser.getSelectedFile();
            if (!showConfirmPopup("Start exam and load attendance from: " + csvFile.getName() + "?")) return;

            ExamService.AttendanceResult ar = examService.transitionToOngoing(examId, csvFile);
            showPopup("Exam → Ongoing\n" + ar.toString());
            loadTable(); clearFields();
        } catch (Exception ex) {
            showPopup(ex.getMessage());
        }
    }

    private void loadCourses() {
        while (courseBox.getItemCount() > 1) courseBox.removeItemAt(1);
        try {
            List<Courses> courses = courseService.getAllCourses();
            for (Courses c : courses) courseBox.addItem(c.getCourseCode() + " - " + c.getCourseName());
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void loadTable() {
        tableModel.setRowCount(0);
        try {
            List<Exam> exams = examService.getAll();
            for (Exam e : exams) {
                tableModel.addRow(new Object[]{
                    e.getExamId(), e.getCourseCode(),
                    e.getExamDate().toString(), e.getSession(),
                    e.getState() != null ? e.getState() : "DRAFT"
                });
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void addExam() {
        try {
            if (dateField.getText().trim().isEmpty()) { showPopup("Enter Exam Date"); return; }
            try { LocalDate.parse(dateField.getText()); }
            catch (Exception ex) { showPopup("Use date format YYYY-MM-DD"); return; }
            if (sessionField.getText().trim().isEmpty()) { showPopup("Enter Session (FN or AN)"); return; }
            if (!StyledComboBox.hasSelection(courseBox)) { showPopup("Select a Course"); return; }
            Exam e = new Exam();
            e.setExamId(0);
            e.setCourseCode(StyledComboBox.getValue(courseBox).split(" - ")[0]);
            e.setExamDate(LocalDate.parse(dateField.getText()));
            e.setSession(sessionField.getText().trim().toUpperCase());
            examService.addSingle(e);
            showPopup("Exam Added (DRAFT)"); loadTable(); clearFields();
        } catch (Exception ex) { ex.printStackTrace(); showPopup(ex.getMessage()); }
    }

    private void updateExam() {
        try {
            if (examIdField.getText().trim().isEmpty()) { showPopup("Select an exam first"); return; }
            try { Integer.parseInt(examIdField.getText()); }
            catch (Exception ex) { showPopup("Exam ID must be numeric"); return; }
            if (dateField.getText().trim().isEmpty()) { showPopup("Enter Exam Date"); return; }
            try { LocalDate.parse(dateField.getText()); }
            catch (Exception ex) { showPopup("Use date format YYYY-MM-DD"); return; }
            if (sessionField.getText().trim().isEmpty()) { showPopup("Enter Session"); return; }
            if (!StyledComboBox.hasSelection(courseBox)) { showPopup("Select a Course"); return; }
            if (!showConfirmPopup("Update Exam ID: " + examIdField.getText() + " ?")) return;
            Exam e = new Exam();
            e.setExamId(Integer.parseInt(examIdField.getText()));
            e.setCourseCode(StyledComboBox.getValue(courseBox).split(" - ")[0]);
            e.setExamDate(LocalDate.parse(dateField.getText()));
            e.setSession(sessionField.getText().trim().toUpperCase());
            examService.update(e);
            showPopup("Exam Updated"); loadTable(); clearFields();
        } catch (Exception ex) { ex.printStackTrace(); showPopup(ex.getMessage()); }
    }

    private void deleteExam() {
        try {
            if (examIdField.getText().trim().isEmpty()) { showPopup("Select an exam"); return; }
            if (!showConfirmPopup("Delete Exam ID: " + examIdField.getText() + " ?")) return;
            examService.deleteById(Integer.parseInt(examIdField.getText()));
            showPopup("Exam Deleted"); loadTable(); clearFields();
        } catch (Exception ex) { ex.printStackTrace(); showPopup(ex.getMessage()); }
    }

    private void clearFields() {
        examIdField.setText(""); dateField.setText(""); sessionField.setText("");
        examIdField.setEditable(true);
        StyledComboBox.reset(courseBox);
        setLifecycleButtonsEnabled(false);
    }

    /* ─── SHARED UI (same as your existing code) ─── */

    private JPanel createGlowPanel(int arc) {
        return new JPanel() {
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                for (int i = 8; i >= 2; i -= 2) {
                    g2.setColor(new Color(0, 180, 255, 35)); g2.setStroke(new BasicStroke(i));
                    g2.drawRoundRect(i/2, i/2, getWidth()-i, getHeight()-i, arc, arc);
                }
                g2.setColor(new Color(22, 22, 30));
                g2.fillRoundRect(6, 6, getWidth()-12, getHeight()-12, arc, arc);
                g2.dispose();
            }
        };
    }

    private JTextField createStyledField() {
        JTextField field = new JTextField(15) {
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int arc = 20;
                for (int i = 6; i >= 2; i -= 2) {
                    g2.setColor(new Color(0, 180, 255, 35)); g2.setStroke(new BasicStroke(i));
                    g2.drawRoundRect(i/2, i/2, getWidth()-i, getHeight()-i, arc, arc);
                }
                g2.setColor(new Color(28, 28, 38));
                g2.fillRoundRect(4, 4, getWidth()-8, getHeight()-8, arc, arc);
                g2.dispose(); super.paintComponent(g);
            }
        };
        field.setOpaque(false); field.setForeground(Color.WHITE);
        field.setCaretColor(new Color(0, 200, 255));
        field.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        field.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
        return field;
    }

    private JButton createStyledButton(String text) {
        JButton btn = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int arc = 25;
                for (int i = 8; i >= 2; i -= 2) {
                    g2.setColor(new Color(0, 180, 255, 40)); g2.setStroke(new BasicStroke(i));
                    g2.drawRoundRect(i/2, i/2, getWidth()-i, getHeight()-i, arc, arc);
                }
                g2.setColor(isEnabled() ? new Color(0, 140, 255) : new Color(60, 60, 70));
                g2.fillRoundRect(6, 6, getWidth()-12, getHeight()-12, arc, arc);
                g2.dispose(); super.paintComponent(g);
            }
        };
        btn.setOpaque(false); btn.setContentAreaFilled(false);
        btn.setBorder(BorderFactory.createEmptyBorder(12, 20, 12, 20));
        btn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btn.setForeground(Color.WHITE); btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private JLabel createLabel(String text) {
        JLabel l = new JLabel(text); l.setForeground(new Color(230, 230, 230));
        l.setFont(new Font("Segoe UI", Font.BOLD, 16)); return l;
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
        msg.setForeground(Color.WHITE); msg.setFont(new Font("Segoe UI", Font.BOLD, 16));
        JButton ok = new JButton("OK");
        ok.setFocusPainted(false); ok.setFont(new Font("Segoe UI", Font.BOLD, 14));
        ok.setForeground(Color.WHITE); ok.setBackground(new Color(0, 140, 255));
        ok.setPreferredSize(new Dimension(90, 35)); ok.addActionListener(e -> d.dispose());
        JPanel bp = new JPanel(); bp.setOpaque(false); bp.add(ok);
        p.add(msg, BorderLayout.CENTER); p.add(bp, BorderLayout.SOUTH);
        d.add(p); d.pack(); d.setLocationRelativeTo(this); d.setVisible(true);
    }

    private boolean showConfirmPopup(String message) {
        final boolean[] r = {false};
        JDialog d = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), true);
        d.setUndecorated(true);
        JPanel p = new JPanel(new BorderLayout(20, 20));
        p.setBackground(new Color(22, 22, 30));
        p.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(0, 200, 255), 2),
            BorderFactory.createEmptyBorder(25, 30, 25, 30)));
        JLabel msg = new JLabel(message, SwingConstants.CENTER);
        msg.setForeground(Color.WHITE); msg.setFont(new Font("Segoe UI", Font.BOLD, 16));
        JButton yes = new JButton("Yes"), no = new JButton("No");
        yes.setBackground(new Color(0, 160, 255)); no.setBackground(new Color(180, 50, 50));
        for (JButton b : new JButton[]{yes, no}) {
            b.setForeground(Color.WHITE); b.setFocusPainted(false);
            b.setFont(new Font("Segoe UI", Font.BOLD, 14));
            b.setPreferredSize(new Dimension(90, 35));
        }
        yes.addActionListener(e -> { r[0] = true; d.dispose(); });
        no.addActionListener(e -> d.dispose());
        JPanel bp = new JPanel(); bp.setOpaque(false); bp.add(yes); bp.add(no);
        p.add(msg, BorderLayout.CENTER); p.add(bp, BorderLayout.SOUTH);
        d.add(p); d.pack(); d.setLocationRelativeTo(this); d.setVisible(true);
        return r[0];
    }
}