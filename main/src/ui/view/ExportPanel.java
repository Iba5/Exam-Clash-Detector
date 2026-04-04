package src.ui.view;

import src.export.CsvExporter;
import src.export.ReportExporter;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.Desktop;
import java.io.File;
import javax.swing.*;

/**
 * ExportPanel — sidebar panel for triggering CSV and HTML report exports.
 * All exports write to a user-chosen directory and optionally open the file immediately.
 */
public class ExportPanel extends JPanel {

    public ExportPanel() {

        setLayout(new BorderLayout(25, 25));
        setBorder(BorderFactory.createEmptyBorder(30, 40, 30, 40));
        setBackground(new Color(15, 15, 20));

        add(createTitle(), BorderLayout.NORTH);
        add(createContent(), BorderLayout.CENTER);
    }

    private JLabel createTitle() {

        JLabel title = new JLabel("EXPORT");
        title.setForeground(Color.WHITE);
        title.setFont(new Font("Segoe UI", Font.BOLD, 30));

        return title;
    }

    private JPanel createContent() {

        JPanel content = new JPanel(new GridBagLayout());
        content.setOpaque(false);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(12, 0, 12, 0);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridwidth = 1;
        gbc.weightx = 1.0;

        // ── CSV exports ──
        gbc.gridx = 0; gbc.gridy = 0;
        content.add(sectionLabel("CSV Exports"), gbc);

        gbc.gridy++;
        content.add(exportCard(
            "Full Seating Plan (CSV)",
            "All students, all exams",
            () -> runCsv(() -> CsvExporter.exportFullSeatingPlan(chooseDir()))
        ), gbc);

        gbc.gridy++;
        content.add(exportCard(
            "Exam Summary (CSV)",
            "Per-exam totals: students, halls, invigilators",
            () -> runCsv(() -> CsvExporter.exportExamSummary(chooseDir()))
        ), gbc);

        gbc.gridy++;
        content.add(exportCard(
            "Single Exam Seating (CSV)",
            "Enter an exam ID when prompted",
            () -> runSingleExamCsv()
        ), gbc);

        // ── HTML / PDF reports ──
        gbc.gridy++;
        content.add(sectionLabel("HTML / PDF Reports"), gbc);

        gbc.gridy++;
        content.add(exportCard(
            "Full Allocation Report (HTML)",
            "All exams with seats and invigilators — open in browser → Print → PDF",
            () -> runHtml(() -> ReportExporter.exportFullReport(chooseDir()))
        ), gbc);

        gbc.gridy++;
        content.add(exportCard(
            "Single Exam Report (HTML)",
            "Enter an exam ID when prompted",
            () -> runSingleExamHtml()
        ), gbc);

        // Filler to push cards to top
        gbc.gridy++;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        content.add(new JPanel() {{ setOpaque(false); }}, gbc);

        return content;
    }

    // ── Single-exam helpers ──

    private void runSingleExamCsv() {

        String input = JOptionPane.showInputDialog(this, "Enter Exam ID:", "Single Exam CSV", JOptionPane.PLAIN_MESSAGE);
        if (input == null || input.trim().isEmpty()) return;

        try {
            int examId = Integer.parseInt(input.trim());
            runCsv(() -> CsvExporter.exportExamSeatingPlan(examId, chooseDir()));
        } catch (NumberFormatException ex) {
            showPopup("Exam ID must be a number.");
        }
    }

    private void runSingleExamHtml() {

        String input = JOptionPane.showInputDialog(this, "Enter Exam ID:", "Single Exam Report", JOptionPane.PLAIN_MESSAGE);
        if (input == null || input.trim().isEmpty()) return;

        try {
            int examId = Integer.parseInt(input.trim());
            runHtml(() -> ReportExporter.exportExamReport(examId, chooseDir()));
        } catch (NumberFormatException ex) {
            showPopup("Exam ID must be a number.");
        }
    }

    // ── Export runners ──

    @FunctionalInterface
    interface ExportTask { String run() throws Exception; }

    private void runCsv(ExportTask task) {
        runExport(task, "CSV");
    }

    private void runHtml(ExportTask task) {
        runExport(task, "HTML");
    }

    private void runExport(ExportTask task, String type) {

        SwingWorker<String, Void> worker = new SwingWorker<>() {

            @Override
            protected String doInBackground() throws Exception {
                return task.run();
            }

            @Override
            protected void done() {
                try {
                    String path = get();
                    if (path == null) return;   // user cancelled dir chooser

                    int open = JOptionPane.showConfirmDialog(
                        ExportPanel.this,
                        type + " exported to:\n" + path + "\n\nOpen file now?",
                        "Export Successful",
                        JOptionPane.YES_NO_OPTION
                    );

                    if (open == JOptionPane.YES_OPTION) {
                        tryOpen(new File(path));
                    }

                } catch (Exception e) {
                    showPopup("Export failed: " + e.getMessage());
                }
            }
        };

        worker.execute();
    }

    private void tryOpen(File file) {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                Desktop.getDesktop().open(file);
            } else {
                showPopup("Cannot open file automatically on this system.\nFile is at: " + file.getAbsolutePath());
            }
        } catch (Exception e) {
            showPopup("Could not open file:\n" + e.getMessage());
        }
    }

    /** Shows a directory chooser and returns the chosen path, or null if cancelled. */
    private String chooseDir() {

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Choose Export Directory");
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setAcceptAllFileFilterUsed(false);

        int result = chooser.showSaveDialog(this);

        if (result == JFileChooser.APPROVE_OPTION) {
            return chooser.getSelectedFile().getAbsolutePath();
        }

        return null;
    }

    // ── UI helpers ──

    private JLabel sectionLabel(String text) {

        JLabel label = new JLabel(text);
        label.setForeground(new Color(0, 200, 255));
        label.setFont(new Font("Segoe UI", Font.BOLD, 18));
        label.setBorder(BorderFactory.createEmptyBorder(12, 0, 4, 0));

        return label;
    }

    private JPanel exportCard(String title, String subtitle, Runnable action) {

        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                for (int i = 6; i >= 2; i -= 2) {
                    g2.setColor(new Color(0, 180, 255, 30));
                    g2.setStroke(new BasicStroke(i));
                    g2.drawRoundRect(i / 2, i / 2, getWidth() - i, getHeight() - i, 18, 18);
                }
                g2.setColor(new Color(22, 22, 32));
                g2.fillRoundRect(5, 5, getWidth() - 10, getHeight() - 10, 18, 18);
                g2.dispose();
            }
        };

        card.setLayout(new BorderLayout(8, 4));
        card.setOpaque(false);
        card.setBorder(BorderFactory.createEmptyBorder(14, 18, 14, 18));
        card.setCursor(new Cursor(Cursor.HAND_CURSOR));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 15));

        JLabel subLabel = new JLabel(subtitle);
        subLabel.setForeground(new Color(160, 160, 160));
        subLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        card.add(titleLabel, BorderLayout.CENTER);
        card.add(subLabel, BorderLayout.SOUTH);

        card.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent e) {
                action.run();
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                card.repaint();
            }
        });

        return card;
    }

    private void showPopup(String message) {

        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), true);
        dialog.setUndecorated(true);

        JPanel panel = new JPanel(new BorderLayout(20, 20));
        panel.setBackground(new Color(22, 22, 30));
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(0, 200, 255), 2),
            BorderFactory.createEmptyBorder(25, 30, 25, 30)));

        JLabel msg = new JLabel("<html>" + message.replace("\n", "<br>") + "</html>", SwingConstants.CENTER);
        msg.setForeground(Color.WHITE);
        msg.setFont(new Font("Segoe UI", Font.BOLD, 14));

        JButton ok = new JButton("OK");
        ok.setFocusPainted(false);
        ok.setFont(new Font("Segoe UI", Font.BOLD, 14));
        ok.setForeground(Color.WHITE);
        ok.setBackground(new Color(0, 140, 255));
        ok.setPreferredSize(new Dimension(90, 35));
        ok.addActionListener(e -> dialog.dispose());

        JPanel btnPanel = new JPanel();
        btnPanel.setOpaque(false);
        btnPanel.add(ok);

        panel.add(msg, BorderLayout.CENTER);
        panel.add(btnPanel, BorderLayout.SOUTH);

        dialog.add(panel);
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }
}
