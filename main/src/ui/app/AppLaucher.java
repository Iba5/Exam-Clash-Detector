package src.ui.app;

import javax.swing.SwingUtilities;
import src.ui.loginwindow.LoginFrame;

public class AppLaucher {
    
    public static void main(String[] args) {

        SwingUtilities.invokeLater(() -> {
            new LoginFrame();
        });
    }
}
