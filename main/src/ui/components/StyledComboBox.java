package src.ui.components;

import java.awt.*;
import javax.swing.*;
import javax.swing.plaf.basic.BasicComboBoxUI;

/**
 * StyledComboBox — reusable dark-themed combo box with a "— Choose —" placeholder.
 *
 * Usage:
 *   JComboBox<String> box = StyledComboBox.create(new String[]{"A","B","C"});
 *   boolean ok  = StyledComboBox.hasSelection(box);   // false when placeholder shown
 *   String  val = StyledComboBox.getValue(box);        // real selected value (never the placeholder)
 *   StyledComboBox.reset(box);                         // back to placeholder
 *   StyledComboBox.repopulate(box, newItems);          // swap items, keep placeholder first
 */
public class StyledComboBox {

    private static final String PLACEHOLDER = "— Choose —";

    // ── factory ──────────────────────────────────────────────────────────

    /** Build a styled combo with a leading placeholder item. */
    public static JComboBox<String> create(String[] items) {

        // Build item array with placeholder at index 0
        String[] full = buildItems(items);

        JComboBox<String> combo = new JComboBox<>(full) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                int arc = 20;
                // glow border
                for (int i = 6; i >= 2; i -= 2) {
                    g2.setColor(new Color(0, 180, 255, 35));
                    g2.setStroke(new BasicStroke(i));
                    g2.drawRoundRect(i/2, i/2, getWidth()-i, getHeight()-i, arc, arc);
                }
                // dark fill
                g2.setColor(new Color(28, 28, 38));
                g2.fillRoundRect(4, 4, getWidth()-8, getHeight()-8, arc, arc);
                g2.dispose();
                super.paintComponent(g);
            }
        };

        combo.setOpaque(false);
        combo.setForeground(Color.WHITE);
        combo.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        combo.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 40));
        combo.setSelectedIndex(0); // placeholder

        // ── custom arrow button ──
        combo.setUI(new BasicComboBoxUI() {
            @Override
            protected JButton createArrowButton() {
                JButton arrow = new JButton() {
                    @Override
                    protected void paintComponent(Graphics g) {
                        Graphics2D g2 = (Graphics2D) g.create();
                        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                RenderingHints.VALUE_ANTIALIAS_ON);
                        g2.setColor(new Color(0, 200, 255));
                        int cx = getWidth() / 2, cy = getHeight() / 2, sz = 6;
                        Polygon tri = new Polygon();
                        tri.addPoint(cx - sz, cy - sz / 2);
                        tri.addPoint(cx + sz, cy - sz / 2);
                        tri.addPoint(cx,      cy + sz);
                        g2.fill(tri);
                        g2.dispose();
                    }
                };
                arrow.setOpaque(false);
                arrow.setContentAreaFilled(false);
                arrow.setBorder(null);
                return arrow;
            }
        });

        // ── renderer: placeholder in grey, real items in white ──
        combo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(
                    JList<?> list, Object value,
                    int index, boolean isSelected, boolean cellHasFocus) {

                JLabel label = (JLabel) super.getListCellRendererComponent(
                        list, value, index, isSelected, cellHasFocus);

                label.setFont(new Font("Segoe UI", Font.PLAIN, 16));
                label.setOpaque(true);

                if (PLACEHOLDER.equals(value)) {
                    label.setForeground(new Color(130, 130, 130)); // grey hint text
                    label.setBackground(isSelected
                            ? new Color(30, 30, 42)
                            : new Color(22, 22, 30));
                } else {
                    label.setForeground(Color.WHITE);
                    label.setBackground(isSelected
                            ? new Color(0, 140, 255)
                            : new Color(22, 22, 30));
                }

                return label;
            }
        });

        return combo;
    }

    // ── helpers ──────────────────────────────────────────────────────────

    /** True only when the user has picked a real item (not the placeholder). */
    public static boolean hasSelection(JComboBox<String> combo) {
        Object sel = combo.getSelectedItem();
        return sel != null && !PLACEHOLDER.equals(sel);
    }

    /**
     * Returns the currently selected real value.
     * Throws IllegalStateException if only the placeholder is selected
     * (always guard with hasSelection() first).
     */
    public static String getValue(JComboBox<String> combo) {
        if (!hasSelection(combo)) {
            throw new IllegalStateException("No item selected");
        }
        return (String) combo.getSelectedItem();
    }

    /** Reset to placeholder (index 0). */
    public static void reset(JComboBox<String> combo) {
        combo.setSelectedIndex(0);
    }

    /**
     * Replace the item list while keeping the placeholder at index 0.
     * After repopulate the combo shows the placeholder again.
     */
    public static void repopulate(JComboBox<String> combo, String[] newItems) {
        combo.removeAllItems();
        combo.addItem(PLACEHOLDER);
        for (String item : newItems) {
            combo.addItem(item);
        }
        combo.setSelectedIndex(0);
    }

    // ── private ──────────────────────────────────────────────────────────

    private static String[] buildItems(String[] items) {
        String[] full = new String[items.length + 1];
        full[0] = PLACEHOLDER;
        System.arraycopy(items, 0, full, 1, items.length);
        return full;
    }
}