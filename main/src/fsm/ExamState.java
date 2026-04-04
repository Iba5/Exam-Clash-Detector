package src.fsm;

import java.awt.Color;

/**
 * Exam lifecycle states.
 *
 *   DRAFT → SCHEDULED → ONGOING → COMPLETED
 *                    ↘ CANCELLED
 */
public enum ExamState {

    DRAFT      ("Draft",      new Color(180, 180, 180)),
    SCHEDULED  ("Scheduled",  new Color(0, 180, 255)),
    ONGOING    ("Ongoing",    new Color(255, 180, 0)),
    COMPLETED  ("Completed",  new Color(0, 200, 100)),
    CANCELLED  ("Cancelled",  new Color(200, 60, 60));

    private final String label;
    private final Color  color;

    ExamState(String label, Color color) {
        this.label = label;
        this.color = color;
    }

    public String getLabel() { return label; }
    public Color  getColor() { return color; }

    /** Safe parse — returns DRAFT if the value is null or unknown. */
    public static ExamState fromString(String s) {
        if (s == null || s.isBlank()) return DRAFT;
        try {
            return valueOf(s.toUpperCase().trim());
        } catch (IllegalArgumentException e) {
            return DRAFT;
        }
    }
}