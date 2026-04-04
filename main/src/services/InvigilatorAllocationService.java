package src.services;

import src.DBConnection.DBConnection;
import src.dao.ExamDAO;
import src.dao.InvigilatorAllocationDAO;
import src.fsm.ExamFSM;
import src.fsm.ExamState;
import src.models.Exam;
import src.models.InvigilatorAllocation;
import src.validators.InvigilatorAllocationValidator;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;

public class InvigilatorAllocationService {

    private final InvigilatorAllocationDAO dao     = new InvigilatorAllocationDAO();
    private final ExamDAO                  examDAO = new ExamDAO();

    // -------------------------
    // ALLOCATE WITH BUSINESS RULE
    // -------------------------
    public InvigilatorAllocation allocate(InvigilatorAllocation invAlloc) {

        if (invAlloc == null) {
            throw new IllegalArgumentException(
                    "Invigilator allocation data cannot be empty."
            );
        }

        InvigilatorAllocationValidator.validate(invAlloc);

        // FSM guard: invigilators can only be assigned to SCHEDULED exams
        try {
            Exam exam = examDAO.getById(invAlloc.getExamId());
            if (exam == null) throw new IllegalArgumentException("Exam not found.");
            ExamState examState = ExamState.fromString(exam.getState());
            if (!ExamFSM.canAllocate(examState)) {
                throw new IllegalStateException(
                    "Cannot assign invigilator — exam is " + examState.getLabel() + " (must be Scheduled).");
            }
        } catch (IllegalStateException | IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Unable to verify exam state.", e);
        }

        Connection con = null;

        try {

            con = DBConnection.getConnection();
            con.setAutoCommit(false);

            if (invigilatorHasClash(con, invAlloc)) {
                throw new IllegalStateException(
                        "This invigilator is already assigned to another exam in the same session."
                );
            }

            InvigilatorAllocation saved = dao.insert(invAlloc, con);

            con.commit();

            return saved;

        } catch (IllegalStateException e) {

            rollback(con);
            throw e;

        } catch (Exception e) {

            rollback(con);

            throw new RuntimeException(
                    "Unable to assign the invigilator."
            );

        } finally {

            close(con);
        }
    }

    // -------------------------
    // CHECK SESSION CLASH
    // -------------------------
    private boolean invigilatorHasClash(Connection con,
                                        InvigilatorAllocation invAlloc) throws Exception {

        String sql =
                "SELECT 1 FROM INVIGILATOR_ALLOCATION ia " +
                "JOIN EXAM e ON ia.exam_id = e.exam_id " +
                "JOIN EXAM e2 ON e2.exam_id = ? " +
                "WHERE ia.invigilator_id = ? " +
                "AND e.exam_date = e2.exam_date " +
                "AND e.session = e2.session";

        try (PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, invAlloc.getExamId());
            ps.setInt(2, invAlloc.getInvigilatorId());

            ResultSet rs = ps.executeQuery();

            return rs.next();
        }
    }

    // -------------------------
    // SIMPLE CREATE
    // -------------------------
    public InvigilatorAllocation add(InvigilatorAllocation invAlloc) {

        if (invAlloc == null) {
            throw new IllegalArgumentException(
                    "Invigilator allocation data cannot be empty."
            );
        }

        InvigilatorAllocationValidator.validate(invAlloc);

        // FSM guard: invigilators can only be assigned to SCHEDULED exams
        try {
            Exam exam = examDAO.getById(invAlloc.getExamId());
            if (exam == null) throw new IllegalArgumentException("Exam not found.");
            ExamState examState = ExamState.fromString(exam.getState());
            if (!ExamFSM.canAllocate(examState)) {
                throw new IllegalStateException(
                    "Cannot assign invigilator — exam is " + examState.getLabel() + " (must be Scheduled).");
            }
        } catch (IllegalStateException | IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Unable to verify exam state.", e);
        }

        try {

            return dao.insert(invAlloc);

        } catch (Exception e) {

            throw new RuntimeException(
                    "Unable to create the invigilator allocation."
            );
        }
    }

    // -------------------------
    // READ BY ID
    // -------------------------
    public InvigilatorAllocation getById(int allocationId) {

        try {

            InvigilatorAllocation alloc = dao.getById(allocationId);

            if (alloc == null) {
                throw new IllegalArgumentException(
                        "The selected invigilator allocation could not be found."
                );
            }

            return alloc;

        } catch (IllegalArgumentException e) {

            throw e;

        } catch (Exception e) {

            throw new RuntimeException(
                    "Unable to retrieve the invigilator allocation."
            );
        }
    }

    // -------------------------
    // READ ALL
    // -------------------------
    public List<InvigilatorAllocation> getAll() {

        try {

            return dao.getAll();

        } catch (Exception e) {

            throw new RuntimeException(
                    "Unable to retrieve invigilator allocations."
            );
        }
    }

    // -------------------------
    // READ BY EXAM
    // -------------------------
    public List<InvigilatorAllocation> getByExamId(int examId) {

        try {

            return dao.getByExamId(examId);

        } catch (Exception e) {

            throw new RuntimeException(
                    "Unable to retrieve allocations for this exam."
            );
        }
    }

    // -------------------------
    // READ BY INVIGILATOR
    // -------------------------
    public List<InvigilatorAllocation> getByInvigilatorId(int invigilatorId) {

        try {

            return dao.getByInvigilatorId(invigilatorId);

        } catch (Exception e) {

            throw new RuntimeException(
                    "Unable to retrieve allocations for this invigilator."
            );
        }
    }

    // -------------------------
    // READ BY HALL
    // -------------------------
    public List<InvigilatorAllocation> getByHallId(int hallId) {

        try {

            return dao.getByHallId(hallId);

        } catch (Exception e) {

            throw new RuntimeException(
                    "Unable to retrieve allocations for this hall."
            );
        }
    }

    // -------------------------
    // UPDATE
    // -------------------------
    public InvigilatorAllocation update(InvigilatorAllocation invAlloc) {

        if (invAlloc == null) {
            throw new IllegalArgumentException(
                    "Invigilator allocation data cannot be empty."
            );
        }

        InvigilatorAllocationValidator.validate(invAlloc);

        try {

            return dao.update(invAlloc);

        } catch (Exception e) {

            throw new RuntimeException(
                    "Unable to update the invigilator allocation."
            );
        }
    }

    // -------------------------
    // DELETE
    // -------------------------
    public boolean delete(int allocationId) {

        try {

            return dao.delete(allocationId);

        } catch (Exception e) {

            throw new RuntimeException(
                    "Unable to delete the invigilator allocation."
            );
        }
    }

    // -------------------------
    // HELPER METHODS
    // -------------------------
    private void rollback(Connection con) {
        try {
            if (con != null) con.rollback();
        } catch (Exception ignored) {}
    }

    private void close(Connection con) {
        try {
            if (con != null) con.close();
        } catch (Exception ignored) {}
    }
}