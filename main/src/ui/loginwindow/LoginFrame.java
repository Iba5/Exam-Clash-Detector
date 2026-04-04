package src.ui.loginwindow;

import java.awt.*;
import javax.swing.*;
public class LoginFrame  extends JFrame{
    
     public LoginFrame(){
        setTitle("Login Page");
        setSize(600,600);
        setLayout(null);
        setResizable(false);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setVisible(true);
        
        getContentPane().setBackground(Color.BLACK);
        setLayout(new GridBagLayout());

        add(new LoginPanel());
     }
}
