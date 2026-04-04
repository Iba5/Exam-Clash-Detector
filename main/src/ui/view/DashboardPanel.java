package src.ui.view;

import java.awt.*;
import java.util.List;
import javax.swing.*;
import javax.swing.table.*;

import src.services.DashboardService;

public class DashboardPanel extends JPanel {

    private JComboBox<String> filterBox;
    private JTable examTable;
    private DefaultTableModel examModel;

    // Live stats labels — updated from DB on load
    private JLabel studentsValue;
    private JLabel subjectsValue;
    private JLabel hallsValue;
    private JLabel invigilatorsValue;

    private final DashboardService dashboardService = new DashboardService();

    public DashboardPanel() {

        setLayout(new BorderLayout(30, 30));
        setBorder(BorderFactory.createEmptyBorder(40, 50, 40, 50));
        setBackground(new Color(15, 15, 20));

        add(createTitle(), BorderLayout.NORTH);
        add(createMainContent(), BorderLayout.CENTER);

        refreshStats();     // FIX: was hardcoded "320", "25", etc. — now live from DB
        showTodayExams();   // FIX: was hardcoded rows — now live from DB
                // Auto-refresh when panel becomes visible
        addComponentListener(new java.awt.event.ComponentAdapter() {
        @Override
        public void componentShown(java.awt.event.ComponentEvent e) {
            refreshStats();
                    applyFilter(); // reloads whichever filter is currently selected
            }
        });
    }

    private JLabel createTitle() {

        JLabel title = new JLabel("DASHBOARD");
        title.setForeground(Color.WHITE);
        title.setFont(new Font("Segoe UI", Font.BOLD, 32));

        return title;
    }

    private JPanel createMainContent() {

        JPanel main = new JPanel(new BorderLayout(30, 30));
        main.setOpaque(false);

        main.add(createTopSection(), BorderLayout.NORTH);
        main.add(createDashboardBody(), BorderLayout.CENTER);

        return main;
    }

    private JPanel createTopSection() {

        JPanel panel = createGlowPanel(25);
        panel.setLayout(new FlowLayout(FlowLayout.LEFT, 20, 20));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel label = new JLabel("Filter By:");
        label.setForeground(Color.WHITE);
        label.setFont(new Font("Segoe UI", Font.BOLD, 16));

        filterBox = createStyledComboBox(new String[]{
                "Today's Exams",
                "All Exams",
                "Upcoming Exams",
                "Completed Exams"
        });

        filterBox.addActionListener(e -> applyFilter());

        panel.add(label);
        panel.add(filterBox);

        return panel;
    }

    private JPanel createDashboardBody() {

        JPanel body = new JPanel(new GridLayout(1, 2, 30, 30));
        body.setOpaque(false);

        body.add(createExamSection());
        body.add(createStatsSection());

        return body;
    }

    private JPanel createExamSection() {

        JPanel panel = createGlowPanel(30);
        panel.setLayout(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(25, 25, 25, 25));

        JLabel title = new JLabel("EXAMINATION SCHEDULE");
        title.setForeground(new Color(0, 200, 255));
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));

        String[] columns = {"Course", "Halls", "Date / Session"};

        examModel = new DefaultTableModel(columns, 0);
        examTable = new JTable(examModel);

        styleTable(examTable);

        JScrollPane scroll = new JScrollPane(examTable);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(0, 200, 255), 1, true));
        scroll.getViewport().setBackground(new Color(22, 22, 30));

        panel.add(title, BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createStatsSection() {

        JPanel stats = new JPanel(new GridLayout(2, 2, 20, 20));
        stats.setOpaque(false);

        // Keep references so refreshStats() can update them
        studentsValue      = new JLabel("...");
        subjectsValue      = new JLabel("...");
        hallsValue         = new JLabel("...");
        invigilatorsValue  = new JLabel("...");

        stats.add(createCard("Students",    studentsValue));
        stats.add(createCard("Subjects",    subjectsValue));
        stats.add(createCard("Halls",       hallsValue));
        stats.add(createCard("Invigilators", invigilatorsValue));

        return stats;
    }

    // FIX: Cards now accept a live JLabel instead of a hardcoded String
    private JPanel createCard(String title, JLabel valueLabel) {

        JPanel card = createGlowPanel(25);
        card.setLayout(new BorderLayout());
        card.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setForeground(new Color(0, 200, 255));
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));

        valueLabel.setForeground(Color.WHITE);
        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 26));

        card.add(titleLabel, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);

        return card;
    }

    // FIX: Load live counts from DB
    private void refreshStats() {

        SwingWorker<int[], Void> worker = new SwingWorker<>() {

            @Override
            protected int[] doInBackground() {
                return new int[]{
                    dashboardService.countStudents(),
                    dashboardService.countCourses(),
                    dashboardService.countHalls(),
                    dashboardService.countInvigilators()
                };
            }

            @Override
            protected void done() {
                try {
                    int[] counts = get();
                    studentsValue.setText(String.valueOf(counts[0]));
                    subjectsValue.setText(String.valueOf(counts[1]));
                    hallsValue.setText(String.valueOf(counts[2]));
                    invigilatorsValue.setText(String.valueOf(counts[3]));
                } catch (Exception ignored) {}
            }
        };

        worker.execute();
    }

    private void applyFilter() {

        String selected = filterBox.getSelectedItem().toString();

        if (selected.equals("Today's Exams"))    showTodayExams();
        else if (selected.equals("All Exams"))   showAllExams();
        else if (selected.equals("Upcoming Exams")) showUpcomingExams();
        else if (selected.equals("Completed Exams")) showCompletedExams();
    }

    // FIX: All four methods now load real data from DB via DashboardService

    private void showTodayExams() {
        loadExamRows(dashboardService.getTodayExams());
    }

    private void showAllExams() {
        loadExamRows(dashboardService.getAllExams());
    }

    private void showUpcomingExams() {
        loadExamRows(dashboardService.getUpcomingExams());
    }

    private void showCompletedExams() {
        loadExamRows(dashboardService.getCompletedExams());
    }

    private void loadExamRows(List<Object[]> rows) {

        examModel.setRowCount(0);

        if (rows.isEmpty()) {
            examModel.addRow(new Object[]{"No records found", "", ""});
            return;
        }

        for (Object[] row : rows) {
            examModel.addRow(row);
        }
    }

    private void styleTable(JTable table) {

        table.setRowHeight(40);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        table.setForeground(Color.WHITE);
        table.setBackground(new Color(22, 22, 30));
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setSelectionBackground(new Color(0, 150, 255));
        table.setSelectionForeground(Color.WHITE);

        JTableHeader header = table.getTableHeader();
        header.setFont(new Font("Segoe UI", Font.BOLD, 18));
        header.setForeground(new Color(0, 220, 255));
        header.setBackground(new Color(18, 18, 25));
    }

    private JComboBox<String> createStyledComboBox(String[] items) {

        JComboBox<String> combo = new JComboBox<>(items);
        combo.setForeground(Color.WHITE);
        combo.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        combo.setBackground(new Color(22, 22, 30));

        return combo;
    }

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
}
