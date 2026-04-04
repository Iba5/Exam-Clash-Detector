package src.ui.view;

import java.awt.*;
import javax.swing.*;

public class MainFrame extends JFrame {

    private SidebarPanel sidebar;
    private JPanel contentPanel;
    private CardLayout cardLayout;
    private boolean sidebarVisible = false;

    public MainFrame() {

        setTitle("EXAM HALL ALLOCATION");
        setSize(1200, 800);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        getContentPane().setBackground(Color.BLACK);

        
        JPanel leftBar = new JPanel();
        leftBar.setBackground(new Color(20, 20, 25));
        leftBar.setPreferredSize(new Dimension(80, 0));
        leftBar.setLayout(new BoxLayout(leftBar, BoxLayout.Y_AXIS));
        leftBar.setBorder(BorderFactory.createEmptyBorder(20, 10, 10, 10));

        JButton menuButton = new JButton("☰");
        menuButton.setFont(new Font("Segoe UI", Font.BOLD, 26));
        menuButton.setForeground(Color.WHITE);
        menuButton.setContentAreaFilled(false);
        menuButton.setFocusPainted(false);
        menuButton.setBorder(null);
        menuButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        menuButton.setCursor(new Cursor(Cursor.HAND_CURSOR));

        JLabel menuLabel = new JLabel("MENU");
        menuLabel.setForeground(Color.GRAY);
        menuLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        menuLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        leftBar.add(menuButton);
        leftBar.add(Box.createVerticalStrut(5));
        leftBar.add(menuLabel);

        add(leftBar, BorderLayout.WEST);

        
        JPanel centerWrapper = new JPanel(new BorderLayout());
        centerWrapper.setOpaque(false);
        add(centerWrapper, BorderLayout.CENTER);

        sidebar = new SidebarPanel(this);
        sidebar.setPreferredSize(new Dimension(0, 0));
        centerWrapper.add(sidebar, BorderLayout.WEST);

        
        cardLayout = new CardLayout();
        contentPanel = new JPanel(cardLayout);
        contentPanel.setOpaque(false);

        contentPanel.add(new DashboardPanel(), "Dashboard");
        contentPanel.add(new StudentPanel(), "Students");
        contentPanel.add(new SubjectPanel(), "Subjects");
        contentPanel.add(new ExamPanel(), "Exams");
        contentPanel.add(new HallPanel(), "Halls");
        contentPanel.add(new AllocationPanel(), "Allocation");
        contentPanel.add(new InvigilatorPanel(), "Invigilator");
        contentPanel.add(new ImportPanel(), "Import");
        contentPanel.add(new ExportPanel(), "Export");

        centerWrapper.add(contentPanel, BorderLayout.CENTER);

        // Toggle sidebar
        menuButton.addActionListener(e -> toggleSidebar());

        setVisible(true);
    }

    
    private void toggleSidebar() {

        Timer timer = new Timer(5, null);

        timer.addActionListener(e -> {

            int currentWidth = sidebar.getPreferredSize().width;

            if (sidebarVisible) {
                currentWidth -= 15;
                if (currentWidth <= 0) {
                    currentWidth = 0;
                    sidebarVisible = false;
                    timer.stop();
                }
            } else {
                currentWidth += 15;
                if (currentWidth >= 320) {
                    currentWidth = 320;
                    sidebarVisible = true;
                    timer.stop();
                }
            }

            sidebar.setPreferredSize(new Dimension(currentWidth, 0));
            sidebar.revalidate();
        });

        timer.start();
    }


    public void showPanel(String name) {
        cardLayout.show(contentPanel, name);
    }
}
