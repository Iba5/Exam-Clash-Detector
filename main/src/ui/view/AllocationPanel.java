package src.ui.view;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.table.*;
import java.util.List;

import src.fsm.AllocationFSM;
import src.fsm.AllocationState;
import src.models.*;
import src.services.*;
import src.ui.components.StyledComboBox;
import src.ui.components.TableSearchBar;

public class AllocationPanel extends JPanel {

    private JComboBox<String> rollBox, examBox, hallBox, invigilatorBox;
    private JTable allocationTable;
    private DefaultTableModel tableModel;

    private final AllocationService  allocationService  = new AllocationService();
    private final ExamService        examService        = new ExamService();
    private final HallService        hallService        = new HallService();
    private final InvigilatorService invigilatorService = new InvigilatorService();
    private final StudentService     studentService     = new StudentService();

    private JSplitPane splitPane;
    private JPanel formContainerRef, tableContainerRef;

    // FSM buttons
    private JButton confirmBtn, presentBtn, absentBtn;

    public AllocationPanel() {
        setLayout(new BorderLayout(25, 25));
        setBorder(BorderFactory.createEmptyBorder(30, 40, 30, 40));
        setBackground(new Color(15, 15, 20));
        add(createTitle(), BorderLayout.NORTH);
        add(createMainPanel(), BorderLayout.CENTER);
        loadStudents(); loadExams(); loadHalls(); loadInvigilators();
        loadTable(); addTableSelectionListener();
        addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentShown(java.awt.event.ComponentEvent e) {
                loadStudents();
                loadExams();
                loadHalls();
                loadInvigilators();
                loadTable();
            }
        });
    }

    private JLabel createTitle() {
        JLabel t = new JLabel("ALLOCATION");
        t.setForeground(Color.WHITE); t.setFont(new Font("Segoe UI", Font.BOLD, 30));
        return t;
    }

    private JPanel createMainPanel() {
        JPanel main = new JPanel(new BorderLayout(25, 25)); main.setOpaque(false);
        formContainerRef = createFormPanel(); tableContainerRef = createTableContainer();
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

    private JPanel createFormPanel() {
        JPanel container = createGlowPanel(25);
        container.setLayout(new BorderLayout());
        container.setBorder(BorderFactory.createEmptyBorder(25, 25, 25, 25));

        JPanel form = new JPanel(new GridBagLayout()); form.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        rollBox = StyledComboBox.create(new String[]{});
        examBox = StyledComboBox.create(new String[]{});
        hallBox = StyledComboBox.create(new String[]{});
        invigilatorBox = StyledComboBox.create(new String[]{});

        JButton allocateBtn  = createStyledButton("Allocate");
        JButton updateBtn    = createStyledButton("Update");
        JButton deleteBtn    = createStyledButton("Delete");
        JButton batchBtn     = createStyledButton("⚡ Auto-Allocate Exam");

        // FSM state buttons
        confirmBtn = createStyledButton("✔ Confirm");
        presentBtn = createStyledButton("✔ Present");
        absentBtn  = createStyledButton("✖ Absent");
        confirmBtn.setEnabled(false); presentBtn.setEnabled(false); absentBtn.setEnabled(false);

        allocateBtn.addActionListener(e -> allocateSeat());
        updateBtn.addActionListener(e -> updateAllocation());
        deleteBtn.addActionListener(e -> deleteAllocation());
        batchBtn.addActionListener(e -> batchAllocate());
        confirmBtn.addActionListener(e -> transitionAllocation(AllocationState.CONFIRMED));
        presentBtn.addActionListener(e -> transitionAllocation(AllocationState.PRESENT));
        absentBtn.addActionListener(e -> transitionAllocation(AllocationState.ABSENT));

        gbc.gridx = 0; gbc.gridy = 0; form.add(createLabel("Student:"), gbc);
        gbc.gridx = 1; form.add(rollBox, gbc);
        gbc.gridx = 0; gbc.gridy = 1; form.add(createLabel("Exam:"), gbc);
        gbc.gridx = 1; form.add(examBox, gbc);
        gbc.gridx = 0; gbc.gridy = 2; form.add(createLabel("Hall:"), gbc);
        gbc.gridx = 1; form.add(hallBox, gbc);
        gbc.gridx = 0; gbc.gridy = 3; form.add(createLabel("Invigilator:"), gbc);
        gbc.gridx = 1; form.add(invigilatorBox, gbc);

        // CRUD buttons
        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 2;
        JPanel btnPanel = new JPanel(); btnPanel.setOpaque(false);
        btnPanel.add(allocateBtn); btnPanel.add(updateBtn); btnPanel.add(deleteBtn);
        btnPanel.add(batchBtn);
        form.add(btnPanel, gbc);

        // FSM buttons
        gbc.gridy = 5;
        JPanel fsmPanel = new JPanel(); fsmPanel.setOpaque(false);
        fsmPanel.add(confirmBtn); fsmPanel.add(presentBtn); fsmPanel.add(absentBtn);
        form.add(fsmPanel, gbc);

        container.add(form);
        return container;
    }

    private JPanel createTableContainer() {
        JPanel container = createGlowPanel(30);
        container.setLayout(new BorderLayout());
        container.setBorder(BorderFactory.createEmptyBorder(25, 25, 25, 25));
        JScrollPane scroll = createTablePanel();
        container.add(new TableSearchBar(allocationTable), BorderLayout.NORTH);
        container.add(scroll, BorderLayout.CENTER);
        return container;
    }

    private JScrollPane createTablePanel() {
        tableModel = new DefaultTableModel();
        tableModel.setColumnIdentifiers(new String[]{"ID", "Roll Number", "Exam ID", "Hall ID", "State"});
        allocationTable = new JTable(tableModel);
        allocationTable.setRowHeight(40);
        allocationTable.setFont(new Font("Segoe UI", Font.PLAIN, 18));
        allocationTable.setForeground(Color.WHITE);
        allocationTable.setBackground(new Color(22, 22, 30));
        allocationTable.setGridColor(new Color(40, 40, 55));
        allocationTable.setSelectionBackground(new Color(0, 140, 255));
        allocationTable.setSelectionForeground(Color.WHITE);

        // Color the State column
        allocationTable.getColumnModel().getColumn(4).setCellRenderer(new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable t, Object val,
                    boolean sel, boolean focus, int row, int col) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(t, val, sel, focus, row, col);
                if (!sel && val != null) {
                    AllocationState s = AllocationState.fromString(val.toString());
                    l.setForeground(s.getColor());
                }
                l.setBackground(sel ? new Color(0, 140, 255) : new Color(22, 22, 30));
                return l;
            }
        });

        JTableHeader header = allocationTable.getTableHeader();
        header.setFont(new Font("Segoe UI", Font.BOLD, 20));
        header.setForeground(new Color(0, 220, 255));
        header.setBackground(new Color(18, 18, 25));
        header.setPreferredSize(new Dimension(header.getWidth(), 50));

        JScrollPane scroll = new JScrollPane(allocationTable);
        scroll.setBorder(null); scroll.getViewport().setBackground(new Color(22, 22, 30));
        return scroll;
    }

    private void addTableSelectionListener() {
        allocationTable.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                int row = allocationTable.getSelectedRow();
                if (row < 0) return;
                int modelRow = allocationTable.convertRowIndexToModel(row);
                String roll = tableModel.getValueAt(modelRow, 1).toString();
                int examId  = Integer.parseInt(tableModel.getValueAt(modelRow, 2).toString());
                int hallId  = Integer.parseInt(tableModel.getValueAt(modelRow, 3).toString());
                String stateStr = tableModel.getValueAt(modelRow, 4).toString();

                selectByPrefix(rollBox, roll);
                selectByPrefix(examBox, String.valueOf(examId));
                selectByPrefix(hallBox, String.valueOf(hallId));

                // Enable/disable FSM buttons
                AllocationState state = AllocationState.fromString(stateStr);
                confirmBtn.setEnabled(AllocationFSM.canTransition(state, AllocationState.CONFIRMED));
                presentBtn.setEnabled(AllocationFSM.canTransition(state, AllocationState.PRESENT));
                absentBtn.setEnabled(AllocationFSM.canTransition(state, AllocationState.ABSENT));
            }
        });
    }

    private void selectByPrefix(JComboBox<String> box, String prefix) {
        for (int i = 0; i < box.getItemCount(); i++) {
            if (box.getItemAt(i).startsWith(prefix)) { box.setSelectedIndex(i); return; }
        }
    }

    // ── BATCH ALLOCATE (the killer feature) ──
    private void batchAllocate() {
        if (!StyledComboBox.hasSelection(examBox)) { showPopup("Select an Exam first"); return; }
        int examId = Integer.parseInt(StyledComboBox.getValue(examBox).split(" - ")[0]);
        if (!showConfirmPopup("Auto-allocate ALL enrolled students for this exam?\n" +
                "(Distributes across halls, assigns invigilators)")) return;

        SwingWorker<BatchAllocator.Result, Void> worker = new SwingWorker<>() {
            @Override protected BatchAllocator.Result doInBackground() {
                return BatchAllocator.allocate(examId, true);
            }
            @Override protected void done() {
                try {
                    BatchAllocator.Result r = get();
                    showPopup(r.toString());
                    loadTable();
                } catch (Exception ex) {
                    String msg = ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage();
                    showPopup("Error: " + msg);
                }
            }
        };
        worker.execute();
    }

    private void transitionAllocation(AllocationState target) {
        try {
            int row = allocationTable.getSelectedRow();
            if (row < 0) { showPopup("Select an allocation first"); return; }
            int modelRow = allocationTable.convertRowIndexToModel(row);
            String roll = tableModel.getValueAt(modelRow, 1).toString();
            int examId = Integer.parseInt(tableModel.getValueAt(modelRow, 2).toString());
            allocationService.transitionByStudentExam(roll, examId, target);
            showPopup("→ " + target.getLabel());
            loadTable();
        } catch (Exception ex) { showPopup(ex.getMessage()); }
    }

    // ── DB loaders ──
    private void loadStudents() {
        try {
            List<Students> list = studentService.getAllStudents();
            String[] items = new String[list.size()];
            for (int i = 0; i < list.size(); i++) {
                Students s = list.get(i); items[i] = s.getRollNumber() + " - " + s.getName();
            }
            StyledComboBox.repopulate(rollBox, items);
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void loadExams() {
        try {
            List<Exam> list = examService.getAll();
            String[] items = new String[list.size()];
            for (int i = 0; i < list.size(); i++) {
                Exam ex = list.get(i);
                items[i] = ex.getExamId() + " - " + ex.getCourseCode() +
                    " (" + ex.getExamDate() + " " + ex.getSession() + ") [" + ex.getState() + "]";
            }
            StyledComboBox.repopulate(examBox, items);
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void loadHalls() {
        try {
            List<Hall> list = hallService.getAll();
            String[] items = new String[list.size()];
            for (int i = 0; i < list.size(); i++) {
                Hall h = list.get(i); items[i] = h.getHallId() + " - " + h.getHallName();
            }
            StyledComboBox.repopulate(hallBox, items);
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void loadInvigilators() {
        try {
            List<Invigilator> list = invigilatorService.getAll();
            String[] items = new String[list.size()];
            for (int i = 0; i < list.size(); i++) {
                Invigilator inv = list.get(i); items[i] = inv.getInvigilatorId() + " - " + inv.getName();
            }
            StyledComboBox.repopulate(invigilatorBox, items);
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void loadTable() {
        tableModel.setRowCount(0);
        try {
            List<Allocation> list = allocationService.getAll();
            for (Allocation a : list) {
                tableModel.addRow(new Object[]{
                    a.getAllocationId(), a.getRollNumber(),
                    a.getExamId(), a.getHallId(),
                    a.getState() != null ? a.getState() : "ALLOCATED"
                });
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    // ── Button actions ──
    private void allocateSeat() {
        try {
            if (!StyledComboBox.hasSelection(rollBox)) { showPopup("Select a Student"); return; }
            if (!StyledComboBox.hasSelection(examBox)) { showPopup("Select an Exam"); return; }
            if (!StyledComboBox.hasSelection(hallBox)) { showPopup("Select a Hall"); return; }
            String roll = StyledComboBox.getValue(rollBox).split(" - ")[0];
            int examId = Integer.parseInt(StyledComboBox.getValue(examBox).split(" - ")[0]);
            int hallId = Integer.parseInt(StyledComboBox.getValue(hallBox).split(" - ")[0]);
            allocationService.allocate(new Allocation(0, roll, examId, hallId));
            showPopup("Allocation Successful"); loadTable(); clearSelections();
        } catch (Exception e) {
            showPopup(e.getCause() != null ? e.getCause().getMessage() : e.getMessage());
        }
    }

    private void updateAllocation() {
        try {
            if (!StyledComboBox.hasSelection(rollBox)) { showPopup("Select a Student"); return; }
            if (!StyledComboBox.hasSelection(examBox)) { showPopup("Select an Exam"); return; }
            if (!StyledComboBox.hasSelection(hallBox)) { showPopup("Select a Hall"); return; }
            if (!showConfirmPopup("Update this allocation?")) return;
            String roll = StyledComboBox.getValue(rollBox).split(" - ")[0];
            int examId = Integer.parseInt(StyledComboBox.getValue(examBox).split(" - ")[0]);
            int hallId = Integer.parseInt(StyledComboBox.getValue(hallBox).split(" - ")[0]);
            boolean ok = allocationService.updateByStudentExam(new Allocation(0, roll, examId, hallId));
            showPopup(ok ? "Allocation Updated" : "Not found"); loadTable();
        } catch (Exception e) {
            showPopup(e.getCause() != null ? e.getCause().getMessage() : e.getMessage());
        }
    }

    private void deleteAllocation() {
        try {
            if (!StyledComboBox.hasSelection(rollBox)) { showPopup("Select a Student"); return; }
            if (!StyledComboBox.hasSelection(examBox)) { showPopup("Select an Exam"); return; }
            if (!showConfirmPopup("Delete this allocation?")) return;
            String roll = StyledComboBox.getValue(rollBox).split(" - ")[0];
            int examId = Integer.parseInt(StyledComboBox.getValue(examBox).split(" - ")[0]);
            boolean ok = allocationService.deleteByStudentExam(roll, examId);
            showPopup(ok ? "Deleted" : "Not found"); loadTable(); clearSelections();
        } catch (Exception e) {
            showPopup(e.getCause() != null ? e.getCause().getMessage() : e.getMessage());
        }
    }

    private void clearSelections() {
        StyledComboBox.reset(rollBox); StyledComboBox.reset(examBox);
        StyledComboBox.reset(hallBox); StyledComboBox.reset(invigilatorBox);
        confirmBtn.setEnabled(false); presentBtn.setEnabled(false); absentBtn.setEnabled(false);
    }

    /* ─── SHARED UI ─── */
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
        JLabel l = new JLabel(text); l.setForeground(Color.WHITE);
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
        msg.setForeground(Color.WHITE); msg.setFont(new Font("Segoe UI", Font.BOLD, 15));
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
        JLabel msg = new JLabel("<html>" + message.replace("\n", "<br>") + "</html>", SwingConstants.CENTER);
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