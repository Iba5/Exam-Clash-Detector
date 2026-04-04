package src.fsm;

import java.util.*;

/**
 * Finite State Machine transition table for seat allocation lifecycle.
 */
public class AllocationFSM {

    private static final Map<AllocationState, Set<AllocationState>> TRANSITIONS =
            new EnumMap<>(AllocationState.class);

    static {
        // ALLOCATED → ABSENT      bulk-set when exam moves DRAFT → SCHEDULED
        // ALLOCATED → CONFIRMED   optional manual confirmation step
        TRANSITIONS.put(AllocationState.ALLOCATED,
                EnumSet.of(AllocationState.CONFIRMED, AllocationState.ABSENT));
        // CONFIRMED → PRESENT / ABSENT  manual attendance path
        TRANSITIONS.put(AllocationState.CONFIRMED,
                EnumSet.of(AllocationState.PRESENT, AllocationState.ABSENT));
        // ABSENT → PRESENT  CSV attendance upload when exam moves SCHEDULED → ONGOING
        TRANSITIONS.put(AllocationState.ABSENT,
                EnumSet.of(AllocationState.PRESENT));
        TRANSITIONS.put(AllocationState.PRESENT,
                EnumSet.noneOf(AllocationState.class));
    }

    public static boolean canTransition(AllocationState from, AllocationState to) {
        Set<AllocationState> allowed = TRANSITIONS.get(from);
        return allowed != null && allowed.contains(to);
    }

    public static Set<AllocationState> allowedTransitions(AllocationState from) {
        Set<AllocationState> allowed = TRANSITIONS.get(from);
        return allowed != null ? Collections.unmodifiableSet(allowed) : Collections.emptySet();
    }

    public static void requireTransition(AllocationState from, AllocationState to) {
        if (!canTransition(from, to)) {
            throw new IllegalStateException(
                "Cannot move allocation from " + from.getLabel() + " to " + to.getLabel());
        }
    }

    /** Can this allocation be removed? Only before attendance is finalised. */
    public static boolean canDelete(AllocationState state) {
        return state == AllocationState.ALLOCATED || state == AllocationState.ABSENT;
    }

    /** Can attendance be marked? CONFIRMED (manual) or ABSENT (CSV flow). */
    public static boolean canMarkAttendance(AllocationState state) {
        return state == AllocationState.CONFIRMED || state == AllocationState.ABSENT;
    }
}