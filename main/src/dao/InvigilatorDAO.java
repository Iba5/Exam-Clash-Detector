package src.dao;

import src.DBConnection.DBConnection;
import src.models.Invigilator;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class InvigilatorDAO {

    // -------------------------
    // INSERT
    // -------------------------
    public Invigilator insert(Invigilator inv) throws Exception {

        String sql = "INSERT INTO INVIGILATOR (name, department, email) VALUES (?, ?, ?)";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, inv.getName());
            ps.setString(2, inv.getDepartment());
            ps.setString(3, inv.getEmail());

            ps.executeUpdate();

            ResultSet rs = ps.getGeneratedKeys();

            if (rs.next()) {
                inv.setInvigilatorId(rs.getInt(1));
            }
        }

        return inv;
    }

    // -------------------------
    // DELETE
    // -------------------------
    public boolean delete(int invigilatorId) throws Exception {

        String sql = "DELETE FROM INVIGILATOR WHERE invigilator_id = ?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, invigilatorId);

            return ps.executeUpdate() > 0;
        }
    }

    // -------------------------
    // UPDATE
    // -------------------------
    public Invigilator update(Invigilator inv) throws Exception {

        String sql = "UPDATE INVIGILATOR SET name = ?, department = ?, email = ? WHERE invigilator_id = ?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, inv.getName());
            ps.setString(2, inv.getDepartment());
            ps.setString(3, inv.getEmail());
            ps.setInt(4, inv.getInvigilatorId());

            ps.executeUpdate();
        }

        return inv;
    }

    // -------------------------
    // SELECT BY ID
    // -------------------------
    public Invigilator getById(int invigilatorId) throws Exception {

        String sql = "SELECT * FROM INVIGILATOR WHERE invigilator_id = ?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, invigilatorId);

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
    public List<Invigilator> getAll() throws Exception {

        String sql = "SELECT * FROM INVIGILATOR";

        List<Invigilator> list = new ArrayList<>();

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
    // GET BY DEPARTMENT
    // -------------------------
    public List<Invigilator> getByDepartment(String department) throws Exception {

        String sql = "SELECT * FROM INVIGILATOR WHERE department = ?";

        List<Invigilator> list = new ArrayList<>();

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, department);

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                list.add(mapResultSet(rs));
            }
        }

        return list;
    }

    // -------------------------
    // GET BY EMAIL
    // -------------------------
    public Invigilator getByEmail(String email) throws Exception {

        String sql = "SELECT * FROM INVIGILATOR WHERE email = ?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, email);

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return mapResultSet(rs);
            }
        }

        return null;
    }

    // -------------------------
    // HELPER METHOD
    // -------------------------
    private Invigilator mapResultSet(ResultSet rs) throws Exception {

        return new Invigilator(
                rs.getInt("invigilator_id"),
                rs.getString("name"),
                rs.getString("department"),
                rs.getString("email")
        );
    }
}
