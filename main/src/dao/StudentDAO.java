package src.dao;

import src.DBConnection.DBConnection;
import src.models.Students;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class StudentDAO {

    // -------------------------
    // INSERT
    // -------------------------
    public Students insert(Students s) throws Exception {

        String sql = "INSERT INTO STUDENT (roll_number, name, department, semester) VALUES (?, ?, ?, ?)";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, s.getRollNumber());
            ps.setString(2, s.getName());
            ps.setString(3, s.getDepartment());
            ps.setInt(4, s.getSemester());

            ps.executeUpdate();
        }

        return s;
    }

    // -------------------------
    // DELETE
    // -------------------------
    public boolean deleteByRollNumber(String rollNumber) throws Exception {

        String sql = "DELETE FROM STUDENT WHERE roll_number = ?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, rollNumber);

            return ps.executeUpdate() > 0;
        }
    }

    // -------------------------
    // UPDATE
    // -------------------------
    public Students update(Students s) throws Exception {

        String sql = "UPDATE STUDENT SET name = ?, department = ?, semester = ? WHERE roll_number = ?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, s.getName());
            ps.setString(2, s.getDepartment());
            ps.setInt(3, s.getSemester());
            ps.setString(4, s.getRollNumber());

            ps.executeUpdate();
        }

        return s;
    }

    // -------------------------
    // SELECT BY ROLL NUMBER
    // -------------------------
    public Students getByRollNumber(String rollNumber) throws Exception {

        String sql = "SELECT * FROM STUDENT WHERE roll_number = ?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, rollNumber);

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
    public List<Students> getAll() throws Exception {

        String sql = "SELECT * FROM STUDENT";

        List<Students> students = new ArrayList<>();

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                students.add(mapResultSet(rs));
            }
        }

        return students;
    }

    // -------------------------
    // GET BY DEPARTMENT
    // -------------------------
    public List<Students> getByDepartment(String department) throws Exception {

        String sql = "SELECT * FROM STUDENT WHERE department = ?";

        List<Students> students = new ArrayList<>();

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, department);

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                students.add(mapResultSet(rs));
            }
        }

        return students;
    }

    // -------------------------
    // GET BY SEMESTER
    // -------------------------
    public List<Students> getBySemester(int semester) throws Exception {

        String sql = "SELECT * FROM STUDENT WHERE semester = ?";

        List<Students> students = new ArrayList<>();

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, semester);

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                students.add(mapResultSet(rs));
            }
        }

        return students;
    }

    // -------------------------
    // HELPER METHOD
    // -------------------------
    private Students mapResultSet(ResultSet rs) throws Exception {

        return new Students(
                rs.getString("roll_number"),
                rs.getString("name"),
                rs.getString("department"),
                rs.getInt("semester")
        );
    }
}
