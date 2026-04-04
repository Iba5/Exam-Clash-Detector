package src.fsm;

import java.awt.Color;

/**
 * Seat allocation lifecycle states.
 *
 *   ALLOCATED → CONFIRMED → PRESENT
 *                         → ABSENT
 */
public enum AllocationState {

    ALLOCATED  ("Allocated",  new Color(0, 180, 255)),
    CONFIRMED  ("Confirmed",  new Color(180, 140, 255)),
    PRESENT    ("Present",    new Color(0, 200, 100)),
    ABSENT     ("Absent",     new Color(200, 60, 60));

    private final String label;
    private final Color  color;

    AllocationState(String label, Color color) {
        this.label = label;
        this.color = color;
    }

    public String getLabel() { return label; }
    public Color  getColor() { return color; }

    public static AllocationState fromString(String s) {
        if (s == null || s.isBlank()) return ALLOCATED;
        try {
            return valueOf(s.toUpperCase().trim());
        } catch (IllegalArgumentException e) {
            return ALLOCATED;
        }
    }
}