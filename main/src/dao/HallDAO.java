package src.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import src.DBConnection.DBConnection;
import src.models.Hall;

public class HallDAO {

    // -------------------------
    // INSERT
    // -------------------------
    public Hall insert(Hall hall) throws Exception {
        String sql = "INSERT INTO HALL (hall_name, seating_capacity) VALUES (?, ?)";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, hall.getHallName());
            ps.setInt(2, hall.getSeatingCapacity());
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) {
                hall.setHallId(rs.getInt(1));
            }
        }
        return hall;
    }

    // -------------------------
    // DELETE
    // -------------------------
    public boolean delete(int hallId) throws Exception {
        String sql = "DELETE FROM HALL WHERE hall_id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, hallId);
            return ps.executeUpdate() > 0;
        }
    }

    // -------------------------
    // UPDATE
    // -------------------------
    public Hall update(Hall hall) throws Exception {
        String sql = "UPDATE HALL SET hall_name = ?, seating_capacity = ? WHERE hall_id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, hall.getHallName());
            ps.setInt(2, hall.getSeatingCapacity());
            ps.setInt(3, hall.getHallId());
            ps.executeUpdate();
        }
        return hall;
    }

    // -------------------------
    // SELECT BY ID
    // -------------------------
    public Hall getById(int hallId) throws Exception {
        String sql = "SELECT * FROM HALL WHERE hall_id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, hallId);
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
    public List<Hall> getAll() throws Exception {
        String sql = "SELECT * FROM HALL";
        List<Hall> list = new ArrayList<>();
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
    // GET BY HALL NAME
    // -------------------------
    public Hall getByName(String hallName) throws Exception {
        String sql = "SELECT * FROM HALL WHERE hall_name = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, hallName);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return mapResultSet(rs);
            }
        }
        return null;
    }

    // -------------------------
    // HELPER: Map ResultSet to Object
    // -------------------------
    private Hall mapResultSet(ResultSet rs) throws Exception {
        return new Hall(
                rs.getInt("hall_id"),
                rs.getString("hall_name"),
                rs.getInt("seating_capacity")
        );
    }
}
