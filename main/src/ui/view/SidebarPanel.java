package src.ui.view;

import java.awt.*;
import javax.swing.*;

public class SidebarPanel extends JPanel {

    private JPanel container;

    public SidebarPanel(MainFrame frame) {

        setLayout(new BorderLayout());
        setOpaque(false);
        setPreferredSize(new Dimension(320, 0));

        container = new JPanel() {

            protected void paintComponent(Graphics g) {

                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);

                int arc = 30;

                for (int i = 6; i >= 2; i -= 2) {
                    g2.setColor(new Color(0, 180, 255, 40));
                    g2.setStroke(new BasicStroke(i));
                    g2.drawRoundRect(
                            i / 2,
                            i / 2,
                            getWidth() - i,
                            getHeight() - i,
                            arc,
                            arc
                    );
                }

                g2.setColor(new Color(22, 22, 28));
                g2.fillRoundRect(
                        5,
                        5,
                        getWidth() - 10,
                        getHeight() - 10,
                        arc,
                        arc
                );

                g2.dispose();
            }
        };

        container.setOpaque(false);
        container.setLayout(new BorderLayout());
        container.setBorder(BorderFactory.createEmptyBorder(30, 25, 30, 25));

        JLabel title = new JLabel("Exam Hall System");
        title.setForeground(Color.WHITE);
        title.setFont(new Font("SansSerif", Font.BOLD, 25));
        title.setBorder(BorderFactory.createEmptyBorder(0, 0, 35, 0));

        container.add(title, BorderLayout.NORTH);

        JPanel menuPanel = new JPanel();
        menuPanel.setOpaque(false);
        menuPanel.setLayout(new BoxLayout(menuPanel, BoxLayout.Y_AXIS));

        String[] items = {
            "Dashboard", 
            "Students", 
            "Subjects", 
            "Exams",
            "Halls", 
            "Allocation", 
            "Invigilator", 
            "Import", 
            "Export", 
            "Logout"
        };

        for (int i = 0; i < items.length; i++) {

            String item = items[i];

            JButton btn = createButton(item);

            btn.addActionListener(e -> {
                if (item.equals("Logout")) {
                    frame.dispose();
                    new src.ui.loginwindow.LoginFrame();
                } else {
                    frame.showPanel(item);
                }
            });

            menuPanel.add(btn);

            if (i != items.length - 1) {
                menuPanel.add(Box.createVerticalStrut(10));
                menuPanel.add(createDivider());
                menuPanel.add(Box.createVerticalStrut(10));
            }
        }

        container.add(menuPanel, BorderLayout.CENTER);
        add(container);
    }

    private JButton createButton(String text) {

        JButton button = new JButton(text);
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        button.setForeground(Color.WHITE);
        button.setFont(new Font("SansSerif", Font.PLAIN, 20));
        button.setBorder(BorderFactory.createEmptyBorder(12, 20, 12, 20));
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        return button;
    }

    private JSeparator createDivider() {
        JSeparator sep = new JSeparator();
        sep.setForeground(new Color(70, 70, 70));
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        return sep;
    }
}
