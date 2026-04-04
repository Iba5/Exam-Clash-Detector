package src.services;

import src.DBConnection.DBConnection;
import src.dao.AllocationDAO;
import src.dao.ExamDAO;
import src.fsm.AllocationFSM;
import src.fsm.AllocationState;
import src.fsm.ExamFSM;
import src.fsm.ExamState;
import src.models.Allocation;
import src.models.Exam;
import src.validators.AllocationValidator;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;

public class AllocationService {

    private final AllocationDAO dao = new AllocationDAO();
    private final ExamDAO examDAO   = new ExamDAO();

    // ── ALLOCATE (single, with all checks) ──
    public Allocation allocate(Allocation a) {
        AllocationValidator.validate(a);

        Connection con = null;
        try {
            con = DBConnection.getConnection();
            con.setAutoCommit(false);

            // FSM guard: exam must be SCHEDULED
            Exam exam = examDAO.getById(a.getExamId());
            if (exam == null) throw new IllegalArgumentException("Exam not found.");
            ExamState examState = ExamState.fromString(exam.getState());
            if (!ExamFSM.canAllocate(examState)) {
                throw new IllegalStateException(
                    "Cannot allocate seats — exam is " + examState.getLabel() + " (must be Scheduled).");
            }

            if (studentHasClash(con, a))
                throw new IllegalStateException("Student has another exam in same session");
            if (hallSessionOccupied(con, a))
                throw new IllegalStateException("Hall already used in this session by a different exam");
            if (hallFull(con, a))
                throw new IllegalStateException("Hall capacity exceeded");
            if (!studentEnrolledInExam(con, a))
                throw new IllegalStateException("Student is not enrolled in the course for this exam");

            a.setState("ALLOCATED");
            Allocation saved = dao.insert(a, con);
            con.commit();

            AuditService.log("ALLOCATE", "SEAT",
                a.getRollNumber() + " → Exam " + a.getExamId() + " Hall " + a.getHallId());
            return saved;

        } catch (IllegalStateException | IllegalArgumentException e) {
            rollback(con); throw e;
        } catch (Exception e) {
            rollback(con); throw new RuntimeException(e);
        } finally {
            closeConnection(con);
        }
    }

    // ── FSM TRANSITION (single allocation) ──
    public boolean transitionByStudentExam(String rollNumber, int examId, AllocationState target) {
        try {
            Allocation alloc = dao.getByStudentExam(rollNumber, examId);
            if (alloc == null) throw new IllegalArgumentException("Allocation not found.");
            AllocationState current = AllocationState.fromString(alloc.getState());
            AllocationFSM.requireTransition(current, target);

            // Attendance can only be marked if exam is ONGOING
            if (target == AllocationState.PRESENT || target == AllocationState.ABSENT) {
                Exam exam = examDAO.getById(examId);
                ExamState examState = ExamState.fromString(exam.getState());
                if (examState != ExamState.ONGOING) {
                    throw new IllegalStateException("Attendance can only be marked for ONGOING exams.");
                }
            }

            dao.updateStateByStudentExam(rollNumber, examId, target.name());
            AuditService.log("TRANSITION", "ALLOCATION",
                rollNumber + " exam " + examId + ": " + current.getLabel() + " → " + target.getLabel());
            return true;
        } catch (IllegalStateException | IllegalArgumentException e) { throw e; }
        catch (Exception e) { throw new RuntimeException(e); }
    }

    // ── READ ──
    public List<Allocation> getAll() {
        try { return dao.getAll(); }
        catch (Exception e) { throw new RuntimeException(e); }
    }

    // ── UPDATE (hall change — only ALLOCATED state) ──
    public boolean updateByStudentExam(Allocation a) {
        AllocationValidator.validate(a);
        try {
            Allocation existing = dao.getByStudentExam(a.getRollNumber(), a.getExamId());
            if (existing != null) {
                AllocationState state = AllocationState.fromString(existing.getState());
                if (!AllocationFSM.canDelete(state)) {
                    throw new IllegalStateException(
                        "Cannot modify allocation in " + state.getLabel() + " state.");
                }
            }
            return dao.updateByStudentExam(a);
        } catch (IllegalStateException e) { throw e; }
        catch (Exception e) { throw new RuntimeException(e); }
    }

    // ── DELETE (only ALLOCATED state) ──
    public boolean deleteByStudentExam(String rollNumber, int examId) {
        try {
            Allocation existing = dao.getByStudentExam(rollNumber, examId);
            if (existing != null) {
                AllocationState state = AllocationState.fromString(existing.getState());
                if (!AllocationFSM.canDelete(state)) {
                    throw new IllegalStateException(
                        "Cannot delete allocation in " + state.getLabel() + " state.");
                }
            }
            boolean ok = dao.deleteByStudentExam(rollNumber, examId);
            if (ok) AuditService.log("DELETE", "ALLOCATION", rollNumber + " exam " + examId);
            return ok;
        } catch (IllegalStateException e) { throw e; }
        catch (Exception e) { throw new RuntimeException(e); }
    }

    // ── ENROLLMENT CHECK ──
    private boolean studentEnrolledInExam(Connection con, Allocation a) throws Exception {
        String sql =
            "SELECT 1 FROM STUDENT_COURSE sc " +
            "JOIN EXAM e ON e.course_code = sc.course_code " +
            "WHERE sc.roll_number = ? AND e.exam_id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, a.getRollNumber());
            ps.setInt(2, a.getExamId());
            return ps.executeQuery().next();
        }
    }

    // ── CLASH CHECKS (unchanged logic) ──
    private boolean studentHasClash(Connection con, Allocation a) throws Exception {
        String sql =
            "SELECT 1 FROM STUDENT_SEAT_ALLOCATION al " +
            "JOIN EXAM e ON al.exam_id = e.exam_id " +
            "JOIN EXAM e2 ON e2.exam_id = ? " +
            "WHERE al.roll_number = ? AND e.exam_date = e2.exam_date AND e.session = e2.session";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, a.getExamId());
            ps.setString(2, a.getRollNumber());
            return ps.executeQuery().next();
        }
    }

    private boolean hallSessionOccupied(Connection con, Allocation a) throws Exception {
        String sql =
            "SELECT 1 FROM STUDENT_SEAT_ALLOCATION al " +
            "JOIN EXAM e ON al.exam_id = e.exam_id " +
            "JOIN EXAM e2 ON e2.exam_id = ? " +
            "WHERE al.hall_id = ? AND al.exam_id != ? " +
            "AND e.exam_date = e2.exam_date AND e.session = e2.session";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, a.getExamId());
            ps.setInt(2, a.getHallId());
            ps.setInt(3, a.getExamId());
            return ps.executeQuery().next();
        }
    }

    private boolean hallFull(Connection con, Allocation a) throws Exception {
        String countSql = "SELECT COUNT(*) FROM STUDENT_SEAT_ALLOCATION WHERE hall_id=? AND exam_id=?";
        String capSql   = "SELECT seating_capacity FROM HALL WHERE hall_id=?";
        int count = 0, capacity = 0;
        try (PreparedStatement ps = con.prepareStatement(countSql)) {
            ps.setInt(1, a.getHallId()); ps.setInt(2, a.getExamId());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) count = rs.getInt(1);
        }
        try (PreparedStatement ps = con.prepareStatement(capSql)) {
            ps.setInt(1, a.getHallId());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) capacity = rs.getInt(1);
        }
        return count >= capacity;
    }

    private void rollback(Connection con) {
        try { if (con != null) con.rollback(); } catch (Exception ignored) {}
    }
    private void closeConnection(Connection con) {
        try { if (con != null) con.close(); } catch (Exception ignored) {}
    }
}