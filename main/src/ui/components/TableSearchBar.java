package src.ui.components;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;

/**
 * Reusable search/filter bar for any JTable.
 * Add above any table: container.add(new TableSearchBar(table), BorderLayout.NORTH);
 */
public class TableSearchBar extends JPanel {

    public TableSearchBar(JTable table) {
        setLayout(new BorderLayout(10, 0));
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(0, 0, 12, 0));

        JLabel icon = new JLabel("🔍 Filter:");
        icon.setForeground(new Color(0, 200, 255));
        icon.setFont(new Font("Segoe UI", Font.BOLD, 14));

        JTextField searchField = new JTextField() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(28, 28, 38));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        searchField.setOpaque(false);
        searchField.setForeground(Color.WHITE);
        searchField.setCaretColor(new Color(0, 200, 255));
        searchField.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        searchField.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));

        TableRowSorter<DefaultTableModel> sorter =
                new TableRowSorter<>((DefaultTableModel) table.getModel());
        table.setRowSorter(sorter);

        searchField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e)  { filter(); }
            public void removeUpdate(DocumentEvent e)  { filter(); }
            public void changedUpdate(DocumentEvent e) { filter(); }
            private void filter() {
                String text = searchField.getText().trim();
                if (text.isEmpty()) sorter.setRowFilter(null);
                else sorter.setRowFilter(RowFilter.regexFilter("(?i)" + java.util.regex.Pattern.quote(text)));
            }
        });

        add(icon, BorderLayout.WEST);
        add(searchField, BorderLayout.CENTER);
    }
}