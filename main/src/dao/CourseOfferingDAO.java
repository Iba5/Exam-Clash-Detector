package src.dao;

import src.DBConnection.DBConnection;
import src.models.CourseOffering;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CourseOfferingDAO {

    // INSERT
    public CourseOffering insert(CourseOffering offering) throws Exception {

        String sql = "INSERT INTO COURSE_OFFERING (course_code, department, semester) VALUES (?, ?, ?)";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            statement.setString(1, offering.getCourseCode());
            statement.setString(2, offering.getDepartment());
            statement.setInt(3, offering.getSemester());
            statement.executeUpdate();

            ResultSet keys = statement.getGeneratedKeys();
            if (keys.next()) {
                offering.setOfferingId(keys.getInt(1));
            }
        }

        return offering;
    }

    // DELETE
    public boolean delete(int offeringId) throws Exception {

        String sql = "DELETE FROM COURSE_OFFERING WHERE offering_id = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, offeringId);
            return statement.executeUpdate() > 0;
        }
    }

    // UPDATE
    public CourseOffering update(CourseOffering offering) throws Exception {

        String sql = "UPDATE COURSE_OFFERING SET course_code = ?, department = ?, semester = ? WHERE offering_id = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, offering.getCourseCode());
            statement.setString(2, offering.getDepartment());
            statement.setInt(3, offering.getSemester());
            statement.setInt(4, offering.getOfferingId());
            statement.executeUpdate();
        }

        return offering;
    }

    // SELECT ALL
    public List<CourseOffering> getAll() throws Exception {

        String sql = "SELECT * FROM COURSE_OFFERING";
        List<CourseOffering> offerings = new ArrayList<>();

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                offerings.add(mapResultSet(rs));
            }
        }

        return offerings;
    }

    // GET BY COURSE CODE
    public List<CourseOffering> getByCourseCode(String courseCode) throws Exception {

        String sql = "SELECT * FROM COURSE_OFFERING WHERE course_code = ?";
        List<CourseOffering> offerings = new ArrayList<>();

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, courseCode);
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                offerings.add(mapResultSet(rs));
            }
        }

        return offerings;
    }

    // GET BY DEPARTMENT
    public List<CourseOffering> getByDepartment(String department) throws Exception {

        String sql = "SELECT * FROM COURSE_OFFERING WHERE department = ?";
        List<CourseOffering> offerings = new ArrayList<>();

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, department);
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                offerings.add(mapResultSet(rs));
            }
        }

        return offerings;
    }

    // RESULTSET MAPPER
    private CourseOffering mapResultSet(ResultSet rs) throws Exception {

        CourseOffering offering = new CourseOffering();
        offering.setOfferingId(rs.getInt("offering_id"));
        offering.setCourseCode(rs.getString("course_code"));
        offering.setDepartment(rs.getString("department"));
        offering.setSemester(rs.getInt("semester"));
        return offering;
    }
}