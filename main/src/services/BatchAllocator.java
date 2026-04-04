package src.services;

import src.DBConnection.DBConnection;
import src.fsm.ExamFSM;
import src.fsm.ExamState;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class BatchAllocator {

    public static class Result {
        public int allocated      = 0;
        public int skippedClash   = 0;
        public int skippedFull    = 0;
        public int hallsUsed      = 0;
        public int invigilatorsAssigned = 0;
        public List<String> hallBreakdown = new ArrayList<>();

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(allocated).append(" students allocated across ").append(hallsUsed).append(" hall(s).\n");
            if (skippedClash > 0)  sb.append(skippedClash).append(" skipped (session clash).\n");
            if (skippedFull > 0)   sb.append(skippedFull).append(" skipped (all halls full).\n");
            if (invigilatorsAssigned > 0) sb.append(invigilatorsAssigned).append(" invigilator(s) auto-assigned.\n");
            if (!hallBreakdown.isEmpty()) {
                sb.append("\nHall breakdown:\n");
                for (String line : hallBreakdown) sb.append("  • ").append(line).append("\n");
            }
            return sb.toString();
        }
    }

    /**
     * Standalone entry point — opens and manages its own connection.
     * Use when calling BatchAllocator independently (e.g. from AllocationPanel).
     */
    public static Result allocate(int examId, boolean autoInvigilate) {
        Connection con = null;
        try {
            con = DBConnection.getConnection();
            con.setAutoCommit(false);
            Result result = allocate(examId, autoInvigilate, con);
            con.commit();
            return result;
        } catch (IllegalStateException | IllegalArgumentException e) {
            rollback(con); throw e;
        } catch (Exception e) {
            rollback(con);
            throw new RuntimeException("Batch allocation failed: " + e.getMessage(), e);
        } finally {
            close(con);
        }
    }

    /**
     * Transactional entry point — uses the provided connection without committing or closing it.
     * The caller is responsible for commit/rollback/close.
     * Used by ExamService.transition() so scheduling + allocation are one atomic operation.
     */
    public static Result allocate(int examId, boolean autoInvigilate, Connection con) throws Exception {

        Result result = new Result();

        try {

            // ── 1. FSM guard ──
            String examState = null, examDate = null, examSession = null, courseCode = null;
            try (PreparedStatement ps = con.prepareStatement(
                    "SELECT course_code, exam_date, session, state FROM EXAM WHERE exam_id = ?")) {
                ps.setInt(1, examId);
                ResultSet rs = ps.executeQuery();
                if (!rs.next()) throw new IllegalArgumentException("Exam not found: " + examId);
                courseCode  = rs.getString("course_code");
                examDate    = rs.getString("exam_date");
                examSession = rs.getString("session");
                examState   = rs.getString("state");
            }
            ExamState state = ExamState.fromString(examState);
            if (!ExamFSM.canAllocate(state)) {
                throw new IllegalStateException(
                    "Cannot allocate — exam is " + state.getLabel() + ". Must be Scheduled.");
            }

            // ── 2. Find unallocated enrolled students ──
            // Joins COURSE_OFFERING to ensure only students whose department and semester
            // actually match the course offering are included — guards against stale
            // STUDENT_COURSE rows that don't correspond to a valid offering.
            String studentsSql =
                "SELECT sc.roll_number, s.department " +
                "FROM STUDENT_COURSE sc " +
                "JOIN STUDENT s ON s.roll_number = sc.roll_number " +
                "JOIN COURSE_OFFERING co ON co.course_code = sc.course_code " +
                "    AND co.department = s.department " +
                "    AND co.semester   = s.semester " +
                "WHERE sc.course_code = ? " +
                "AND sc.roll_number NOT IN (" +
                "    SELECT ssa.roll_number FROM STUDENT_SEAT_ALLOCATION ssa WHERE ssa.exam_id = ?" +
                ") " +
                "ORDER BY s.department, sc.roll_number";

            List<String[]> students = new ArrayList<>();
            try (PreparedStatement ps = con.prepareStatement(studentsSql)) {
                ps.setString(1, courseCode);
                ps.setInt(2, examId);
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    students.add(new String[]{ rs.getString("roll_number"), rs.getString("department") });
                }
            }

            if (students.isEmpty()) { return result; }

            // ── 3. Get halls with remaining capacity ──
            String hallsSql =
                "SELECT h.hall_id, h.hall_name, h.seating_capacity, " +
                "       COALESCE(used.cnt, 0) AS used_seats " +
                "FROM HALL h " +
                "LEFT JOIN (" +
                "    SELECT hall_id, COUNT(*) AS cnt " +
                "    FROM STUDENT_SEAT_ALLOCATION " +
                "    WHERE exam_id = ? " +
                "    GROUP BY hall_id" +
                ") used ON h.hall_id = used.hall_id " +
                "WHERE h.hall_id NOT IN (" +
                "    SELECT DISTINCT ssa2.hall_id " +
                "    FROM STUDENT_SEAT_ALLOCATION ssa2 " +
                "    JOIN EXAM e2 ON ssa2.exam_id = e2.exam_id " +
                "    WHERE ssa2.exam_id != ? " +
                "    AND e2.exam_date = ? " +
                "    AND e2.session = ?" +
                ") " +
                "ORDER BY (h.seating_capacity - COALESCE(used.cnt, 0)) DESC";

            List<int[]> halls = new ArrayList<>();
            List<String> hallNames = new ArrayList<>();
            try (PreparedStatement ps = con.prepareStatement(hallsSql)) {
                ps.setInt(1, examId);
                ps.setInt(2, examId);
                ps.setString(3, examDate);
                ps.setString(4, examSession);
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    int remaining = rs.getInt("seating_capacity") - rs.getInt("used_seats");
                    if (remaining > 0) {
                        halls.add(new int[]{ rs.getInt("hall_id"), remaining });
                        hallNames.add(rs.getString("hall_name") + " (" + remaining + " seats free)");
                    }
                }
            }

            if (halls.isEmpty()) {
                result.skippedFull = students.size();
                return result;
            }

            // ── 4. Session clash check ──
            String clashSql =
                "SELECT 1 FROM STUDENT_SEAT_ALLOCATION al " +
                "JOIN EXAM e ON al.exam_id = e.exam_id " +
                "WHERE al.roll_number = ? " +
                "AND e.exam_date = ? " +
                "AND e.session = ?";

            // ── 5. Insert — state is ALLOCATED; attendance CSV later sets PRESENT/ABSENT ──
            String insertSql =
                "INSERT INTO STUDENT_SEAT_ALLOCATION (roll_number, exam_id, hall_id, state) " +
                "VALUES (?, ?, ?, 'ALLOCATED')";

            // ── 6. Distribute ──
            int hallIdx = 0;
            int seatsLeft = halls.get(0)[1];
            int[] perHallCount = new int[halls.size()];

            for (String[] student : students) {
                String roll = student[0];

                if (hallIdx >= halls.size()) { result.skippedFull++; continue; }

                try (PreparedStatement ps = con.prepareStatement(clashSql)) {
                    ps.setString(1, roll);
                    ps.setString(2, examDate);
                    ps.setString(3, examSession);
                    if (ps.executeQuery().next()) { result.skippedClash++; continue; }
                }

                try (PreparedStatement ps = con.prepareStatement(insertSql)) {
                    ps.setString(1, roll);
                    ps.setInt(2, examId);
                    ps.setInt(3, halls.get(hallIdx)[0]);
                    ps.executeUpdate();
                    result.allocated++;
                    perHallCount[hallIdx]++;
                }

                seatsLeft--;
                if (seatsLeft <= 0) {
                    hallIdx++;
                    if (hallIdx < halls.size()) seatsLeft = halls.get(hallIdx)[1];
                }
            }

            // ── 7. Hall breakdown ──
            for (int i = 0; i < halls.size(); i++) {
                if (perHallCount[i] > 0) {
                    result.hallsUsed++;
                    result.hallBreakdown.add(hallNames.get(i) + " → " + perHallCount[i] + " students");
                }
            }

            // ── 8. Auto-assign invigilators ──
            if (autoInvigilate) {
                result.invigilatorsAssigned = autoAssignInvigilators(con, examId, examDate, examSession, halls, perHallCount);
            }

            AuditService.log("BATCH_ALLOCATE", "EXAM",
                "Exam " + examId + ": " + result.allocated + " allocated, " +
                result.skippedClash + " clash, " + result.skippedFull + " full, " +
                result.hallsUsed + " halls");

        } catch (IllegalStateException | IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Batch allocation failed: " + e.getMessage(), e);
        }

        return result;
    }

    private static int autoAssignInvigilators(Connection con, int examId,
            String examDate, String examSession, List<int[]> halls, int[] perHallCount) throws Exception {

        String freeInvSql =
            "SELECT i.invigilator_id FROM INVIGILATOR i " +
            "WHERE i.invigilator_id NOT IN (" +
            "    SELECT ia.invigilator_id FROM INVIGILATOR_ALLOCATION ia " +
            "    JOIN EXAM e ON ia.exam_id = e.exam_id " +
            "    WHERE e.exam_date = ? AND e.session = ?" +
            ") " +
            "ORDER BY i.invigilator_id";

        List<Integer> freeInvigilators = new ArrayList<>();
        try (PreparedStatement ps = con.prepareStatement(freeInvSql)) {
            ps.setString(1, examDate);
            ps.setString(2, examSession);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) freeInvigilators.add(rs.getInt(1));
        }

        String insertInvSql =
            "INSERT IGNORE INTO INVIGILATOR_ALLOCATION (invigilator_id, exam_id, hall_id) " +
            "VALUES (?, ?, ?)";

        int assigned = 0;
        int invIdx = 0;

        for (int i = 0; i < halls.size() && invIdx < freeInvigilators.size(); i++) {
            if (perHallCount[i] > 0) {
                try (PreparedStatement ps = con.prepareStatement(insertInvSql)) {
                    ps.setInt(1, freeInvigilators.get(invIdx));
                    ps.setInt(2, examId);
                    ps.setInt(3, halls.get(i)[0]);
                    ps.executeUpdate();
                    assigned++;
                    invIdx++;
                }
            }
        }

        return assigned;
    }

    private static void rollback(Connection con) {
        try { if (con != null) con.rollback(); } catch (Exception ignored) {}
    }
    private static void close(Connection con) {
        try { if (con != null) con.close(); } catch (Exception ignored) {}
    }
}