package src.ui.loginwindow;
import java.awt.*;
import javax.swing.*;
import src.ui.view.MainFrame;

public class LoginPanel extends JPanel {

    private JTextField usernameField;
    private JPasswordField passwordField;
    private JButton loginButton;

    private final Color neonBlue = new Color(0, 200, 255);
    private final Color darkCard = new Color(30, 30, 30);

    public LoginPanel() {

        setPreferredSize(new Dimension(420, 400));
        setOpaque(false);
        setLayout(new BorderLayout(20, 20));

JPanel card = new JPanel() {

    protected void paintComponent(Graphics g) {

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

        int arc = 30;

        // ===== OUTER GLOW =====
        for (int i = 20; i >= 4; i -= 4) {
            g2.setColor(new Color(255, 255, 255, 15));
            g2.setStroke(new BasicStroke(i));
            g2.drawRoundRect(
                    i/2,
                    i/2,
                    getWidth() - i,
                    getHeight() - i,
                    arc,
                    arc
            );
        }

        
        g2.setColor(new Color(30, 30, 30));
        g2.fillRoundRect(
                10,
                10,
                getWidth() - 20,
                getHeight() - 20,
                arc,
                arc
        );

        
        g2.setColor(new Color(120, 120, 120));
        g2.setStroke(new BasicStroke(2));
        g2.drawRoundRect(
                10,
                10,
                getWidth() - 20,
                getHeight() - 20,
                arc,
                arc
        );

        g2.dispose();
    }
};

        card.setOpaque(false);
        card.setLayout(new BorderLayout(20, 20));
        card.setBorder(BorderFactory.createEmptyBorder(40, 40, 40, 40));

        JLabel title = new JLabel("SIGN IN", SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 34));
        title.setForeground(Color.WHITE);

        JPanel formPanel = new JPanel();
        formPanel.setOpaque(false);
        formPanel.setLayout(new GridLayout(3, 1, 10, 25));

        usernameField = createNeonTextField();
        passwordField = createNeonPasswordField();
        loginButton = createNeonButton();

        formPanel.add(usernameField);
        formPanel.add(passwordField);
        formPanel.add(loginButton);

        card.add(title, BorderLayout.NORTH);
        card.add(formPanel, BorderLayout.CENTER);

        add(card);
    }


    

    private JTextField createNeonTextField() {

        JTextField field = new JTextField() {

            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);

                g2.setColor(new Color(40, 40, 40));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);

                super.paintComponent(g);
            }

            protected void paintBorder(Graphics g) {

    Graphics2D g2 = (Graphics2D) g.create();
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
            RenderingHints.VALUE_ANTIALIAS_ON);

    int arc = 20;

    if (hasFocus()) {

        
        for (int i = 8; i >= 2; i -= 2) {
            g2.setColor(new Color(255, 255, 255, 30));
            g2.setStroke(new BasicStroke(i));
            g2.drawRoundRect(
                    i/2,
                    i/2,
                    getWidth() - i,
                    getHeight() - i,
                    arc,
                    arc
            );
        }

        
        g2.setColor(Color.WHITE);
        g2.setStroke(new BasicStroke(2));
        g2.drawRoundRect(1, 1,
                getWidth()-3,
                getHeight()-3,
                arc,
                arc);

    } else {

        
        g2.setColor(new Color(120,120,120));
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawRoundRect(1,1,
                getWidth()-3,
                getHeight()-3,
                arc,
                arc);
    }

    g2.dispose();
}

        };

        field.setOpaque(false);
        field.setForeground(Color.WHITE);
        field.setCaretColor(neonBlue);
        field.setBorder(BorderFactory.createEmptyBorder(12, 15, 12, 15));
        field.setFont(new Font("SansSerif", Font.PLAIN, 20));

        return field;
    }

    // Neon Password Field
    private JPasswordField createNeonPasswordField() {

        JPasswordField field = new JPasswordField() {

            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);

                g2.setColor(new Color(40, 40, 40));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);

                super.paintComponent(g);
            }

            protected void paintBorder(Graphics g) {

    Graphics2D g2 = (Graphics2D) g.create();
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
            RenderingHints.VALUE_ANTIALIAS_ON);

    int arc = 20;

    if (hasFocus()) {

        
        for (int i = 8; i >= 2; i -= 2) {
            g2.setColor(new Color(255, 255, 255, 30));
            g2.setStroke(new BasicStroke(i));
            g2.drawRoundRect(
                    i/2,
                    i/2,
                    getWidth() - i,
                    getHeight() - i,
                    arc,
                    arc
            );
        }

    
        g2.setColor(Color.WHITE);
        g2.setStroke(new BasicStroke(2));
        g2.drawRoundRect(1, 1,
                getWidth()-3,
                getHeight()-3,
                arc,
                arc);

    } else {

        
        g2.setColor(new Color(120,120,120));
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawRoundRect(1,1,
                getWidth()-3,
                getHeight()-3,
                arc,
                arc);
    }

    g2.dispose();
}

        };

        field.setOpaque(false);
        field.setForeground(Color.WHITE);
        field.setCaretColor(neonBlue);
        field.setBorder(BorderFactory.createEmptyBorder(12, 15, 12, 15));
        field.setFont(new Font("SansSerif", Font.PLAIN, 20));

        return field;
    }

    
    private JButton createNeonButton() {

        JButton button = new JButton("LOGIN") {

            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);

                g2.setColor(new Color(0, 120, 255));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 25, 25);

                super.paintComponent(g);
            }
        };

        button.setContentAreaFilled(false);
        button.setForeground(Color.WHITE);
        button.setFont(new Font("SansSerif", Font.BOLD, 18));
        button.setBorder(BorderFactory.createEmptyBorder(12, 20, 12, 20));
        button.setFocusPainted(false);

        button.addActionListener(e -> handleLogin());

        return button;
    }

    private void handleLogin() {

        String username = usernameField.getText();
        String password = new String(passwordField.getPassword());

         if (username.equals("admin") && password.equals("1234")) {

        
        new MainFrame();

        
        SwingUtilities.getWindowAncestor(this).dispose();

    } else {
        JOptionPane.showMessageDialog(this, "Invalid Credentials");
    }
    }
}
