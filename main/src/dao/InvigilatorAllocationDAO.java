package src.dao;

import src.DBConnection.DBConnection;
import src.models.InvigilatorAllocation;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class InvigilatorAllocationDAO {

    // -------------------------
    // INSERT
    // -------------------------
    public InvigilatorAllocation insert(InvigilatorAllocation invAlloc) throws Exception {
        String sql = "INSERT INTO INVIGILATOR_ALLOCATION (invigilator_id, exam_id, hall_id) VALUES (?, ?, ?)";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, invAlloc.getInvigilatorId());
            ps.setInt(2, invAlloc.getExamId());
            ps.setInt(3, invAlloc.getHallId());
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) {
                invAlloc.setInvigilatorAllocationId(rs.getInt(1));
            }
        }
        return invAlloc;
    }

    // -------------------------
    // INSERT with external connection (for transactions)
    // -------------------------
    public InvigilatorAllocation insert(InvigilatorAllocation invAlloc, Connection con) throws Exception {
        String sql = "INSERT INTO INVIGILATOR_ALLOCATION (invigilator_id, exam_id, hall_id) VALUES (?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, invAlloc.getInvigilatorId());
            ps.setInt(2, invAlloc.getExamId());
            ps.setInt(3, invAlloc.getHallId());
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) {
                invAlloc.setInvigilatorAllocationId(rs.getInt(1));
            }
        }
        return invAlloc;
    }

    // -------------------------
    // DELETE
    // -------------------------
    public boolean delete(int invigilatorAllocationId) throws Exception {
        String sql = "DELETE FROM INVIGILATOR_ALLOCATION WHERE invigilator_allocation_id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, invigilatorAllocationId);
            return ps.executeUpdate() > 0;
        }
    }

    // -------------------------
    // UPDATE
    // -------------------------
    public InvigilatorAllocation update(InvigilatorAllocation invAlloc) throws Exception {
        String sql = "UPDATE INVIGILATOR_ALLOCATION SET invigilator_id = ?, exam_id = ?, hall_id = ? WHERE invigilator_allocation_id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, invAlloc.getInvigilatorId());
            ps.setInt(2, invAlloc.getExamId());
            ps.setInt(3, invAlloc.getHallId());
            ps.setInt(4, invAlloc.getInvigilatorAllocationId());
            ps.executeUpdate();
        }
        return invAlloc;
    }

    // -------------------------
    // SELECT BY ID
    // -------------------------
    public InvigilatorAllocation getById(int invigilatorAllocationId) throws Exception {
        String sql = "SELECT * FROM INVIGILATOR_ALLOCATION WHERE invigilator_allocation_id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, invigilatorAllocationId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return mapResultSet(rs);
            }
        }
        return null;
    }

    // -------------------------
    // SELECT ALL
    // -------------------------
    public List<InvigilatorAllocation> getAll() throws Exception {
        String sql = "SELECT * FROM INVIGILATOR_ALLOCATION";
        List<InvigilatorAllocation> list = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(mapResultSet(rs));
            }
        }
        return list;
    }

    // -------------------------
    // GET BY EXAM ID
    // -------------------------
    public List<InvigilatorAllocation> getByExamId(int examId) throws Exception {
        String sql = "SELECT * FROM INVIGILATOR_ALLOCATION WHERE exam_id = ?";
        List<InvigilatorAllocation> list = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, examId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(mapResultSet(rs));
            }
        }
        return list;
    }

    // -------------------------
    // GET BY INVIGILATOR ID
    // -------------------------
    public List<InvigilatorAllocation> getByInvigilatorId(int invigilatorId) throws Exception {
        String sql = "SELECT * FROM INVIGILATOR_ALLOCATION WHERE invigilator_id = ?";
        List<InvigilatorAllocation> list = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, invigilatorId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(mapResultSet(rs));
            }
        }
        return list;
    }

    // -------------------------
    // GET BY HALL ID
    // -------------------------
    public List<InvigilatorAllocation> getByHallId(int hallId) throws Exception {
        String sql = "SELECT * FROM INVIGILATOR_ALLOCATION WHERE hall_id = ?";
        List<InvigilatorAllocation> list = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, hallId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(mapResultSet(rs));
            }
        }
        return list;
    }

    // -------------------------
    // HELPER: Map ResultSet to Object
    // -------------------------
    private InvigilatorAllocation mapResultSet(ResultSet rs) throws Exception {
        return new InvigilatorAllocation(
                rs.getInt("invigilator_allocation_id"),
                rs.getInt("invigilator_id"),
                rs.getInt("exam_id"),
                rs.getInt("hall_id")
        );
    }
}
