package src.fsm;

import java.util.*;

/**
 * Finite State Machine transition table for exam lifecycle.
 * Single source of truth — services call canTransition() before any state change.
 */
public class ExamFSM {

    private static final Map<ExamState, Set<ExamState>> TRANSITIONS = new EnumMap<>(ExamState.class);

    static {
        TRANSITIONS.put(ExamState.DRAFT,     EnumSet.of(ExamState.SCHEDULED, ExamState.CANCELLED));
        TRANSITIONS.put(ExamState.SCHEDULED, EnumSet.of(ExamState.ONGOING, ExamState.CANCELLED));
        TRANSITIONS.put(ExamState.ONGOING,   EnumSet.of(ExamState.COMPLETED));
        TRANSITIONS.put(ExamState.COMPLETED, EnumSet.noneOf(ExamState.class));
        TRANSITIONS.put(ExamState.CANCELLED, EnumSet.noneOf(ExamState.class));
    }

    public static boolean canTransition(ExamState from, ExamState to) {
        Set<ExamState> allowed = TRANSITIONS.get(from);
        return allowed != null && allowed.contains(to);
    }

    public static Set<ExamState> allowedTransitions(ExamState from) {
        Set<ExamState> allowed = TRANSITIONS.get(from);
        return allowed != null ? Collections.unmodifiableSet(allowed) : Collections.emptySet();
    }

    public static void requireTransition(ExamState from, ExamState to) {
        if (!canTransition(from, to)) {
            throw new IllegalStateException(
                "Cannot move exam from " + from.getLabel() + " to " + to.getLabel());
        }
    }

    /** Can students be allocated to this exam? Only SCHEDULED exams. */
    public static boolean canAllocate(ExamState state) {
        return state == ExamState.SCHEDULED;
    }

    /** Can this exam be edited (course, date, session)? Only DRAFT. */
    public static boolean canEdit(ExamState state) {
        return state == ExamState.DRAFT;
    }

    /** Can this exam be deleted? Only DRAFT or CANCELLED. */
    public static boolean canDelete(ExamState state) {
        return state == ExamState.DRAFT || state == ExamState.CANCELLED;
    }
}