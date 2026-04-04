package src.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import src.DBConnection.DBConnection;
import src.models.Courses;

public class CourseDAO {

    // -------------------------
    // INSERT
    // -------------------------
    public Courses insert(Courses course) throws Exception {

        String sql = "INSERT INTO COURSE (course_code, course_name, credits) VALUES (?, ?, ?)";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, course.getCourseCode());
            statement.setString(2, course.getCourseName());
            statement.setInt(3, course.getCredits());

            statement.executeUpdate();
        }

        return course;
    }

    // -------------------------
    // DELETE
    // -------------------------
    public boolean delete(String courseCode) throws Exception {

        String sql = "DELETE FROM COURSE WHERE course_code = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, courseCode);

            return statement.executeUpdate() > 0;
        }
    }

    // -------------------------
    // UPDATE
    // -------------------------
    public Courses update(Courses course) throws Exception {

        String sql = "UPDATE COURSE SET course_name = ?, credits = ? WHERE course_code = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, course.getCourseName());
            statement.setInt(2, course.getCredits());
            statement.setString(3, course.getCourseCode());

            statement.executeUpdate();
        }

        return course;
    }

    // -------------------------
    // SELECT ALL
    // -------------------------
    public List<Courses> getAll() throws Exception {

        String sql = "SELECT * FROM COURSE";

        List<Courses> courses = new ArrayList<>();

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                courses.add(mapResultSet(resultSet));
            }
        }

        return courses;
    }

    // -------------------------
    // GET BY COURSE CODE
    // -------------------------
    public Courses getByCode(String courseCode) throws Exception {

        String sql = "SELECT * FROM COURSE WHERE course_code = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, courseCode);

            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next()) {
                return mapResultSet(resultSet);
            }
        }

        return null;
    }

    // -------------------------
    // HELPER METHOD
    // -------------------------
    private Courses mapResultSet(ResultSet resultSet) throws Exception {

        return new Courses(
                resultSet.getString("course_code"),
                resultSet.getString("course_name"),
                resultSet.getInt("credits")
        );
    }
}