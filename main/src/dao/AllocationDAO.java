package src.dao;

import src.DBConnection.DBConnection;
import src.models.Allocation;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class AllocationDAO {

    public Allocation insert(Allocation allocation) throws Exception {
        String sql = "INSERT INTO STUDENT_SEAT_ALLOCATION (roll_number, exam_id, hall_id, state) VALUES (?, ?, ?, ?)";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, allocation.getRollNumber());
            ps.setInt(2, allocation.getExamId());
            ps.setInt(3, allocation.getHallId());
            ps.setString(4, allocation.getState() != null ? allocation.getState() : "ALLOCATED");
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) allocation.setAllocationId(rs.getInt(1));
        }
        return allocation;
    }

    public Allocation insert(Allocation allocation, Connection con) throws Exception {
        String sql = "INSERT INTO STUDENT_SEAT_ALLOCATION (roll_number, exam_id, hall_id, state) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, allocation.getRollNumber());
            ps.setInt(2, allocation.getExamId());
            ps.setInt(3, allocation.getHallId());
            ps.setString(4, allocation.getState() != null ? allocation.getState() : "ALLOCATED");
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) allocation.setAllocationId(rs.getInt(1));
        }
        return allocation;
    }

    public boolean updateState(int allocationId, String newState) throws Exception {
        String sql = "UPDATE STUDENT_SEAT_ALLOCATION SET state = ? WHERE allocation_id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, newState);
            ps.setInt(2, allocationId);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean updateStateByStudentExam(String rollNumber, int examId, String newState) throws Exception {
        String sql = "UPDATE STUDENT_SEAT_ALLOCATION SET state = ? WHERE roll_number = ? AND exam_id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, newState);
            ps.setString(2, rollNumber);
            ps.setInt(3, examId);
            return ps.executeUpdate() > 0;
        }
    }

    /** Batch update all allocations for an exam to a new state */
    public int updateStateByExam(int examId, String fromState, String toState, Connection con) throws Exception {
        String sql = "UPDATE STUDENT_SEAT_ALLOCATION SET state = ? WHERE exam_id = ? AND state = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, toState);
            ps.setInt(2, examId);
            ps.setString(3, fromState);
            return ps.executeUpdate();
        }
    }

    public boolean deleteByStudentExam(String rollNumber, int examId) throws Exception {
        String sql = "DELETE FROM STUDENT_SEAT_ALLOCATION WHERE roll_number=? AND exam_id=?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, rollNumber);
            ps.setInt(2, examId);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean updateByStudentExam(Allocation allocation) throws Exception {
        String sql = "UPDATE STUDENT_SEAT_ALLOCATION SET hall_id=? WHERE roll_number=? AND exam_id=?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, allocation.getHallId());
            ps.setString(2, allocation.getRollNumber());
            ps.setInt(3, allocation.getExamId());
            return ps.executeUpdate() > 0;
        }
    }

    public Allocation getById(int allocationId) throws Exception {
        String sql = "SELECT * FROM STUDENT_SEAT_ALLOCATION WHERE allocation_id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, allocationId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapResultSet(rs);
        }
        return null;
    }

    public Allocation getByStudentExam(String rollNumber, int examId) throws Exception {
        String sql = "SELECT * FROM STUDENT_SEAT_ALLOCATION WHERE roll_number = ? AND exam_id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, rollNumber);
            ps.setInt(2, examId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapResultSet(rs);
        }
        return null;
    }

    public List<Allocation> getAll() throws Exception {
        String sql = "SELECT * FROM STUDENT_SEAT_ALLOCATION";
        List<Allocation> list = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapResultSet(rs));
        }
        return list;
    }

    public List<Allocation> getByExamId(int examId) throws Exception {
        String sql = "SELECT * FROM STUDENT_SEAT_ALLOCATION WHERE exam_id = ?";
        List<Allocation> list = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, examId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapResultSet(rs));
        }
        return list;
    }

    public List<Allocation> getByRollNumber(String rollNumber) throws Exception {
        String sql = "SELECT * FROM STUDENT_SEAT_ALLOCATION WHERE roll_number = ?";
        List<Allocation> list = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, rollNumber);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapResultSet(rs));
        }
        return list;
    }

    public List<Allocation> getByHallId(int hallId) throws Exception {
        String sql = "SELECT * FROM STUDENT_SEAT_ALLOCATION WHERE hall_id = ?";
        List<Allocation> list = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, hallId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapResultSet(rs));
        }
        return list;
    }

    private Allocation mapResultSet(ResultSet rs) throws Exception {
        String state = "ALLOCATED";
        try { state = rs.getString("state"); } catch (Exception ignored) {}
        return new Allocation(
            rs.getInt("allocation_id"),
            rs.getString("roll_number"),
            rs.getInt("exam_id"),
            rs.getInt("hall_id"),
            state
        );
    }
}