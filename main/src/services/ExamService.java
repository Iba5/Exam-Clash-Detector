package src.services;

import src.DBConnection.DBConnection;
import src.dao.AllocationDAO;
import src.dao.ExamDAO;
import src.fsm.ExamFSM;
import src.fsm.ExamState;
import src.models.Exam;
import src.services.BatchAllocator;
import src.services.CsvImporter;
import src.services.ImportResult;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * ExamService — manages exam lifecycle transitions and related cleanup.
 *
 * Key rules enforced here:
 *   1. DRAFT → SCHEDULED : auto-allocates seats for all enrolled students (state = ALLOCATED).
 *   2. SCHEDULED → ONGOING : blocked unless attendance CSV has been uploaded via transitionToOngoing().
 *   3. DELETE / CANCEL / COMPLETE : cleans up seat allocations and invigilator assignments.
 */
public class ExamService {

    private final ExamDAO examDAO = new ExamDAO();
    private final AllocationDAO allocationDAO = new AllocationDAO();

    // ── CREATE ──────────────────────────────────────────────────────

    public Exam create(Exam exam) {
        try {
            exam.setState("DRAFT");
            return examDAO.insert(exam);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create exam: " + e.getMessage(), e);
        }
    }

    // ── READ ────────────────────────────────────────────────────────

    public Exam getById(int examId) {
        try { return examDAO.getById(examId); }
        catch (Exception e) { throw new RuntimeException(e); }
    }

    public List<Exam> getAll() {
        try { return examDAO.getAll(); }
        catch (Exception e) { throw new RuntimeException(e); }
    }

    // ── STATE TRANSITION ────────────────────────────────────────────

    /**
     * Transitions an exam to a new state, enforcing FSM and business rules.
     *
     * DRAFT → SCHEDULED  : auto-allocates seats for all enrolled students (state = ALLOCATED)
     *                       and auto-assigns invigilators. Allocation happens here, not before.
     * SCHEDULED → ONGOING: BLOCKED — must use transitionToOngoing(examId, csvFile).
     * → CANCELLED        : rolls back all seat allocations and invigilator assignments.
     * → COMPLETED        : rolls back all seat allocations and invigilator assignments.
     * → DELETED          : rolls back all seat allocations and invigilator assignments.
     */
    public Exam transition(int examId, ExamState target) {
        if (target == ExamState.ONGOING) {
            throw new IllegalStateException(
                "Cannot transition to ONGOING without an attendance CSV. " +
                "Use transitionToOngoing(examId, csvFile) instead.");
        }
        Connection con = null;
        try {
            Exam exam = requireExam(examId);
            ExamState current = ExamState.fromString(exam.getState());
            ExamFSM.requireTransition(current, target);

            con = DBConnection.getConnection();
            con.setAutoCommit(false);

            if (target == ExamState.SCHEDULED) {
                // Update state and run allocation in the same transaction — fully atomic.
                // BatchAllocator reads the updated state via the same connection so its
                // FSM guard sees SCHEDULED without needing a separate commit.
                updateStateInTx(con, examId, target.name());
                BatchAllocator.Result r = BatchAllocator.allocate(examId, true, con);
                con.commit();
                AuditService.log("TRANSITION", "EXAM",
                    "Exam " + examId + ": DRAFT → SCHEDULED. " +
                    r.allocated + " seats allocated, " + r.invigilatorsAssigned + " invigilators assigned.");
                exam.setState(target.name());
                return exam;
            } else if (target == ExamState.CANCELLED || target == ExamState.COMPLETED) {
                cleanupExamAllocations(con, examId);
            }

            updateStateInTx(con, examId, target.name());
            con.commit();

            AuditService.log("TRANSITION", "EXAM",
                "Exam " + examId + ": " + current.getLabel() + " → " + target.getLabel());

            exam.setState(target.name());
            return exam;

        } catch (IllegalStateException | IllegalArgumentException e) {
            rollback(con); throw e;
        } catch (Exception e) {
            rollback(con);
            throw new RuntimeException("Transition failed: " + e.getMessage(), e);
        } finally {
            close(con);
        }
    }

    /**
     * Transitions SCHEDULED → ONGOING, loading attendance from a CSV file.
     *
     * CSV format — header row required, roll number is the first (or only) column:
     *   roll_number
     *   CS2021001
     *   CS2021002
     *
     * Students whose roll number appears in the CSV are marked PRESENT.
     * All other ALLOCATED students are marked ABSENT.
     * At least one valid roll number is required to proceed.
     *
     * @param csvFile  File pointing to the attendance CSV.
     * @return AttendanceResult with counts of marked/not-found rows.
     */
    public AttendanceResult transitionToOngoing(int examId, File csvFile) {
        Connection con = null;
        try {
            Exam exam = requireExam(examId);
            ExamState current = ExamState.fromString(exam.getState());
            ExamFSM.requireTransition(current, ExamState.ONGOING);

            Set<String> presentRolls = parseAttendanceCsv(csvFile);
            if (presentRolls.isEmpty()) {
                throw new IllegalArgumentException(
                    "Attendance CSV contains no valid roll numbers. " +
                    "At least one student must be present to begin the exam.");
            }

            con = DBConnection.getConnection();
            con.setAutoCommit(false);

            int marked  = markPresent(con, examId, presentRolls);
            int notFound = presentRolls.size() - marked;

            updateStateInTx(con, examId, ExamState.ONGOING.name());
            con.commit();

            AuditService.log("TRANSITION", "EXAM",
                "Exam " + examId + ": SCHEDULED → ONGOING via CSV. " +
                "Present: " + marked + ", unmatched: " + notFound);

            return new AttendanceResult(presentRolls.size(), marked, notFound);

        } catch (IllegalStateException | IllegalArgumentException e) {
            rollback(con); throw e;
        } catch (Exception e) {
            rollback(con);
            throw new RuntimeException("Failed to start exam: " + e.getMessage(), e);
        } finally {
            close(con);
        }
    }

    // ── DELETE ──────────────────────────────────────────────────────

    /**
     * Deletes an exam (only DRAFT or CANCELLED).
     * Cleans up seat allocations and invigilator assignments first.
     */
    public boolean delete(int examId) {
        Connection con = null;
        try {
            Exam exam = requireExam(examId);
            ExamState state = ExamState.fromString(exam.getState());
            if (!ExamFSM.canDelete(state)) {
                throw new IllegalStateException(
                    "Cannot delete exam in " + state.getLabel() +
                    " state. Only DRAFT or CANCELLED exams can be deleted.");
            }

            con = DBConnection.getConnection();
            con.setAutoCommit(false);
            cleanupExamAllocations(con, examId);
            deleteExamInTx(con, examId);
            con.commit();

            AuditService.log("DELETE", "EXAM", "Exam " + examId + " deleted.");
            return true;

        } catch (IllegalStateException | IllegalArgumentException e) {
            rollback(con); throw e;
        } catch (Exception e) {
            rollback(con);
            throw new RuntimeException("Delete failed: " + e.getMessage(), e);
        } finally {
            close(con);
        }
    }

    // ── ADD SINGLE (alias used by UI) ────────────────────────────────

    /** Creates a new exam in DRAFT state. Alias for create(), used by ExamPanel. */
    public Exam addSingle(Exam exam) {
        return create(exam);
    }

    // ── DELETE BY ID (alias used by UI) ──────────────────────────────

    /** Deletes an exam by ID. Delegates to delete(). Used by ExamPanel. */
    public boolean deleteById(int examId) {
        return delete(examId);
    }

    // ── IMPORT BATCH (from CSV via ImportPanel) ───────────────────────

    /**
     * Bulk-imports exams parsed from a CSV file.
     * Each exam is created in DRAFT state.
     * Parse-time failures are carried through; DB failures are caught per-row.
     */
    public ImportResult importBatch(CsvImporter.ParsedData<Exam> parsed) {
        ImportResult result = new ImportResult();

        for (ImportResult.FailedRow f : parsed.failed) {
            result.addFailure(f.getRow(), f.getReason());
        }

        for (int i = 0; i < parsed.items.size(); i++) {
            Exam e = parsed.items.get(i);
            String[] raw = parsed.rawRows.get(i);
            try {
                create(e);
                result.addSuccess(raw);
                AuditService.log("IMPORT", "EXAM",
                    "Imported: " + e.getCourseCode() + " " + e.getExamDate() + " " + e.getSession());
            } catch (Exception ex) {
                result.addFailure(raw, extractMessage(ex));
            }
        }

        AuditService.log("BATCH_IMPORT", "EXAM", result.summary());
        return result;
    }

    private String extractMessage(Exception e) {
        if (e.getCause() != null && e.getCause().getMessage() != null)
            return cleanMessage(e.getCause().getMessage());
        return cleanMessage(e.getMessage());
    }

    private String cleanMessage(String msg) {
        if (msg == null) return "Unknown error";
        msg = msg.replaceAll("^(java\\.lang\\.|java\\.sql\\.)", "");
        if (msg.contains("Duplicate entry") || msg.contains("duplicate key"))
            return "An exam for this course/date/session already exists";
        if (msg.contains("foreign key") || msg.contains("violates foreign key"))
            return "Course code does not exist — import courses first";
        return msg;
    }

    // ── EDIT ────────────────────────────────────────────────────────

    /** Updates exam details. Only allowed in DRAFT state. */
    public Exam update(Exam exam) {
        try {
            Exam existing = requireExam(exam.getExamId());
            ExamState state = ExamState.fromString(existing.getState());
            if (!ExamFSM.canEdit(state)) {
                throw new IllegalStateException(
                    "Cannot edit exam in " + state.getLabel() + " state. Only DRAFT exams can be edited.");
            }
            exam.setState(existing.getState());
            return examDAO.update(exam);
        } catch (IllegalStateException | IllegalArgumentException e) { throw e; }
        catch (Exception e) { throw new RuntimeException("Update failed: " + e.getMessage(), e); }
    }

    // ── PRIVATE HELPERS ─────────────────────────────────────────────



    /**
     * Marks students in the CSV as PRESENT, then marks everyone else ABSENT.
     * Only rows currently in ALLOCATED state are touched (safe re-run).
     * @return count of rows marked PRESENT.
     */
    private int markPresent(Connection con, int examId, Set<String> presentRolls) throws Exception {
        // Step 1: mark present students
        String presentSql =
            "UPDATE STUDENT_SEAT_ALLOCATION SET state = 'PRESENT' " +
            "WHERE exam_id = ? AND roll_number = ? AND state = 'ALLOCATED'";
        int count = 0;
        try (PreparedStatement ps = con.prepareStatement(presentSql)) {
            for (String roll : presentRolls) {
                ps.setInt(1, examId);
                ps.setString(2, roll);
                count += ps.executeUpdate();
            }
        }
        // Step 2: mark remaining ALLOCATED rows as ABSENT
        String absentSql =
            "UPDATE STUDENT_SEAT_ALLOCATION SET state = 'ABSENT' " +
            "WHERE exam_id = ? AND state = 'ALLOCATED'";
        try (PreparedStatement ps = con.prepareStatement(absentSql)) {
            ps.setInt(1, examId);
            ps.executeUpdate();
        }
        return count;
    }

    /**
     * Deletes all invigilator assignments and seat allocations for an exam.
     * Invigilator allocations must be deleted first due to FK constraints.
     */
    private void cleanupExamAllocations(Connection con, int examId) throws Exception {
        try (PreparedStatement ps = con.prepareStatement(
                "DELETE FROM INVIGILATOR_ALLOCATION WHERE exam_id = ?")) {
            ps.setInt(1, examId);
            ps.executeUpdate();
        }
        try (PreparedStatement ps = con.prepareStatement(
                "DELETE FROM STUDENT_SEAT_ALLOCATION WHERE exam_id = ?")) {
            ps.setInt(1, examId);
            ps.executeUpdate();
        }
    }

    private void updateStateInTx(Connection con, int examId, String state) throws Exception {
        try (PreparedStatement ps = con.prepareStatement(
                "UPDATE EXAM SET state = ? WHERE exam_id = ?")) {
            ps.setString(1, state);
            ps.setInt(2, examId);
            ps.executeUpdate();
        }
    }

    private void deleteExamInTx(Connection con, int examId) throws Exception {
        try (PreparedStatement ps = con.prepareStatement(
                "DELETE FROM EXAM WHERE exam_id = ?")) {
            ps.setInt(1, examId);
            ps.executeUpdate();
        }
    }

    private Exam requireExam(int examId) throws Exception {
        Exam exam = examDAO.getById(examId);
        if (exam == null) throw new IllegalArgumentException("Exam not found: " + examId);
        return exam;
    }

    /**
     * Parses an attendance CSV.
     * Expects a header row, then one row per present student.
     * Roll number must be the first (or only) column.
     * Example:
     *   roll_number
     *   CS2021001
     *   CS2021002
     */
    private Set<String> parseAttendanceCsv(File file) {
        if (file == null || !file.exists()) {
            throw new IllegalArgumentException("Attendance CSV file not found.");
        }
        if (!file.getName().toLowerCase().endsWith(".csv")) {
            throw new IllegalArgumentException("Attendance file must be a .csv file.");
        }

        Set<String> rolls = new HashSet<>();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), "UTF-8"))) {
            String line;
            boolean header = true;
            while ((line = br.readLine()) != null) {
                if (header) { header = false; continue; }
                line = line.trim();
                if (line.isEmpty()) continue;
                // First column, strip surrounding quotes if present
                String roll = line.split(",")[0].trim().replaceAll("^\"|\"$", "");
                if (!roll.isEmpty()) rolls.add(roll);
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Could not read attendance CSV: " + e.getMessage(), e);
        }
        return rolls;
    }

    private void rollback(Connection con) {
        try { if (con != null) con.rollback(); } catch (Exception ignored) {}
    }

    private void close(Connection con) {
        try { if (con != null) con.close(); } catch (Exception ignored) {}
    }

    // ── RESULT ──────────────────────────────────────────────────────

    /** Summary returned after uploading an attendance CSV and starting the exam. */
    public static class AttendanceResult {
        public final int csvRows;       // roll numbers read from CSV
        public final int markedPresent; // allocations updated to PRESENT
        public final int notFound;      // roll numbers in CSV not matched in allocation

        public AttendanceResult(int csvRows, int markedPresent, int notFound) {
            this.csvRows = csvRows;
            this.markedPresent = markedPresent;
            this.notFound = notFound;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(markedPresent).append(" student(s) marked PRESENT.\n");
            if (notFound > 0)
                sb.append(notFound)
                  .append(" roll number(s) in the CSV were not found in the seat allocation (ignored).\n");
            return sb.toString();
        }
    }
}
