package src.ui.view;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import src.models.Students;
import src.services.StudentService;
import src.ui.components.StyledComboBox;

public class StudentPanel extends JPanel {

    private JTextField idField;
    private JTextField nameField;
    private JComboBox<String> semesterBox;
    private JComboBox<String> departmentBox;

    private JTable studentTable;
    private DefaultTableModel tableModel;

    private final StudentService service = new StudentService();

    public StudentPanel() {
        setLayout(new BorderLayout(25, 25));
        setBorder(BorderFactory.createEmptyBorder(30, 40, 30, 40));
        setBackground(new Color(15, 15, 20));
        add(createTitle(), BorderLayout.NORTH);
        add(createMainContent(), BorderLayout.CENTER);
        loadTable();
        addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentShown(java.awt.event.ComponentEvent e) {
                loadTable();
            }
        });
    }

    private JLabel createTitle() {
        JLabel t = new JLabel("STUDENTS");
        t.setForeground(Color.WHITE);
        t.setFont(new Font("Segoe UI", Font.BOLD, 30));
        return t;
    }

    private JPanel createMainContent() {
        JPanel main = new JPanel(new BorderLayout(25, 25));
        main.setOpaque(false);
        main.add(createFormContainer(), BorderLayout.NORTH);
        main.add(createTableContainer(), BorderLayout.CENTER);
        return main;
    }

    private JPanel createFormContainer() {
        JPanel c = createGlowPanel(25);
        c.setLayout(new BorderLayout());
        c.setBorder(BorderFactory.createEmptyBorder(25, 25, 25, 25));
        c.add(createFormPanel(), BorderLayout.CENTER);
        return c;
    }

    private JPanel createFormPanel() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill   = GridBagConstraints.HORIZONTAL;

        idField   = createStyledField();
        nameField = createStyledField();

        semesterBox = StyledComboBox.create(new String[]{
                "1", "2", "3", "4", "5", "6", "7", "8"
        });
        departmentBox = StyledComboBox.create(new String[]{
                "Computer Science Engineering",
                "Information Technology",
                "Electronics Engineering",
                "Mechanical Engineering",
                "Civil Engineering"
        });

        JButton addBtn    = createStyledButton("Add");
        JButton updateBtn = createStyledButton("Update");
        JButton deleteBtn = createStyledButton("Delete");

        addBtn.addActionListener(e -> addStudent());
        updateBtn.addActionListener(e -> updateStudent());
        deleteBtn.addActionListener(e -> deleteStudent());

        gbc.gridx = 0; gbc.gridy = 0; form.add(createLabel("Roll Number:"), gbc);
        gbc.gridx = 1;                form.add(idField, gbc);

        gbc.gridx = 0; gbc.gridy = 1; form.add(createLabel("Name:"), gbc);
        gbc.gridx = 1;                form.add(nameField, gbc);

        gbc.gridx = 0; gbc.gridy = 2; form.add(createLabel("Semester:"), gbc);
        gbc.gridx = 1;                form.add(semesterBox, gbc);

        gbc.gridx = 0; gbc.gridy = 3; form.add(createLabel("Department:"), gbc);
        gbc.gridx = 1;                form.add(departmentBox, gbc);

        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 2;
        JPanel bp = new JPanel(); bp.setOpaque(false);
        bp.add(addBtn); bp.add(updateBtn); bp.add(deleteBtn);
        form.add(bp, gbc);

        return form;
    }

    private JPanel createTableContainer() {
        JPanel c = createGlowPanel(30);
        c.setLayout(new BorderLayout());
        c.setBorder(BorderFactory.createEmptyBorder(25, 25, 25, 25));
        c.add(createTable(), BorderLayout.CENTER);
        return c;
    }

    private JScrollPane createTable() {
        tableModel = new DefaultTableModel();
        tableModel.setColumnIdentifiers(new String[]{"Roll Number", "Name", "Semester", "Department"});

        studentTable = new JTable(tableModel);
        studentTable.setRowHeight(40);
        studentTable.setFont(new Font("Segoe UI", Font.PLAIN, 18));
        studentTable.setForeground(Color.WHITE);
        studentTable.setBackground(new Color(22, 22, 30));
        studentTable.setGridColor(Color.WHITE);
        studentTable.setSelectionBackground(new Color(0, 150, 255));
        studentTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JTableHeader header = studentTable.getTableHeader();
        header.setFont(new Font("Segoe UI", Font.BOLD, 20));
        header.setForeground(new Color(0, 220, 255));
        header.setBackground(new Color(18, 18, 25));

        studentTable.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                int row = studentTable.getSelectedRow();
                if (row < 0) return;
                idField.setText(tableModel.getValueAt(row, 0).toString());
                nameField.setText(tableModel.getValueAt(row, 1).toString());
                // semester stored as int — match as string in combo
                String sem  = tableModel.getValueAt(row, 2).toString();
                String dept = tableModel.getValueAt(row, 3).toString();
                selectExact(semesterBox,  sem);
                selectExact(departmentBox, dept);
                idField.setEditable(false);
            }
        });

        JScrollPane scroll = new JScrollPane(studentTable);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(new Color(22, 22, 30));
        return scroll;
    }

    /** Selects the item that exactly matches {@code value} (skips placeholder). */
    private void selectExact(JComboBox<String> box, String value) {
        for (int i = 1; i < box.getItemCount(); i++) {
            if (box.getItemAt(i).equals(value)) { box.setSelectedIndex(i); return; }
        }
    }

    private boolean validateInput() {
        if (idField.getText().trim().isEmpty())   { showPopup("Enter Roll Number"); return false; }
        if (nameField.getText().trim().isEmpty()) { showPopup("Enter Student Name"); return false; }
        if (!StyledComboBox.hasSelection(semesterBox))   { showPopup("Select Semester");   return false; }
        if (!StyledComboBox.hasSelection(departmentBox)) { showPopup("Select Department"); return false; }
        return true;
    }

    private void addStudent() {
        try {
            if (!validateInput()) return;
            Students s = new Students();
            s.setRollNumber(idField.getText().trim());
            s.setName(nameField.getText().trim());
            s.setSemester(Integer.parseInt(StyledComboBox.getValue(semesterBox)));
            s.setDepartment(StyledComboBox.getValue(departmentBox));
            service.add(s);
            showPopup("Student Added Successfully");
            loadTable(); clearFields();
        } catch (Exception e) { e.printStackTrace(); showPopup(e.getMessage()); }
    }

    private void updateStudent() {
        try {
            if (!validateInput()) return;
            if (!showConfirmPopup("Update Student: " + idField.getText() + " ?")) return;
            Students s = new Students();
            s.setRollNumber(idField.getText().trim());
            s.setName(nameField.getText().trim());
            s.setSemester(Integer.parseInt(StyledComboBox.getValue(semesterBox)));
            s.setDepartment(StyledComboBox.getValue(departmentBox));
            service.update(s);
            showPopup("Student Updated Successfully");
            loadTable(); clearFields();
        } catch (Exception e) { e.printStackTrace(); showPopup(e.getMessage()); }
    }

    private void deleteStudent() {
        try {
            if (idField.getText().trim().isEmpty()) { showPopup("Select a student first"); return; }
            if (!showConfirmPopup("Delete Student: " + idField.getText() + " ?")) return;
            service.delete(idField.getText().trim());
            showPopup("Student Deleted Successfully");
            loadTable(); clearFields();
        } catch (Exception e) { e.printStackTrace(); showPopup(e.getMessage()); }
    }

    private void loadTable() {
        tableModel.setRowCount(0);
        try {
            for (Students s : service.getAllStudents()) {
                tableModel.addRow(new Object[]{
                        s.getRollNumber(), s.getName(),
                        s.getSemester(), s.getDepartment()
                });
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void clearFields() {
        idField.setText(""); nameField.setText("");
        idField.setEditable(true);
        StyledComboBox.reset(semesterBox);
        StyledComboBox.reset(departmentBox);
    }

    /* ─── UI helpers ─── */
    private JPanel createGlowPanel(int arc) {
        return new JPanel() {
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                for (int i = 8; i >= 2; i -= 2) {
                    g2.setColor(new Color(0, 180, 255, 35));
                    g2.setStroke(new BasicStroke(i));
                    g2.drawRoundRect(i/2, i/2, getWidth()-i, getHeight()-i, arc, arc);
                }
                g2.setColor(new Color(22, 22, 30));
                g2.fillRoundRect(6, 6, getWidth()-12, getHeight()-12, arc, arc);
                g2.dispose();
            }
        };
    }

    private JTextField createStyledField() {
        JTextField f = new JTextField(15) {
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int arc = 20;
                for (int i = 6; i >= 2; i -= 2) {
                    g2.setColor(new Color(0, 180, 255, 35));
                    g2.setStroke(new BasicStroke(i));
                    g2.drawRoundRect(i/2, i/2, getWidth()-i, getHeight()-i, arc, arc);
                }
                g2.setColor(new Color(28, 28, 38));
                g2.fillRoundRect(4, 4, getWidth()-8, getHeight()-8, arc, arc);
                g2.dispose(); super.paintComponent(g);
            }
        };
        f.setOpaque(false); f.setForeground(Color.WHITE);
        f.setCaretColor(new Color(0, 200, 255));
        f.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        f.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
        return f;
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
                    g2.drawRoundRect(i/2, i/2, getWidth()-i, getHeight()-i, arc, arc);
                }
                g2.setColor(new Color(0, 140, 255));
                g2.fillRoundRect(6, 6, getWidth()-12, getHeight()-12, arc, arc);
                g2.dispose(); super.paintComponent(g);
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
        JLabel msg = new JLabel(message, SwingConstants.CENTER);
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