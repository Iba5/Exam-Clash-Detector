package src.dao;

import src.DBConnection.DBConnection;
import src.models.Exam;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ExamDAO {

    public Exam insert(Exam e) throws Exception {
        String sql = "INSERT INTO EXAM (course_code, exam_date, session, state) VALUES (?, ?, ?, ?)";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, e.getCourseCode());
            ps.setDate(2, Date.valueOf(e.getExamDate()));
            ps.setString(3, e.getSession());
            ps.setString(4, e.getState() != null ? e.getState() : "DRAFT");
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) e.setExamId(rs.getInt(1));
        }
        return e;
    }

    public boolean delete(int examId) throws Exception {
        String sql = "DELETE FROM EXAM WHERE exam_id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, examId);
            return ps.executeUpdate() > 0;
        }
    }

    public Exam update(Exam e) throws Exception {
        String sql = "UPDATE EXAM SET course_code = ?, exam_date = ?, session = ?, state = ? WHERE exam_id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, e.getCourseCode());
            ps.setDate(2, Date.valueOf(e.getExamDate()));
            ps.setString(3, e.getSession());
            ps.setString(4, e.getState());
            ps.setInt(5, e.getExamId());
            ps.executeUpdate();
        }
        return e;
    }

    public boolean updateState(int examId, String newState) throws Exception {
        String sql = "UPDATE EXAM SET state = ? WHERE exam_id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, newState);
            ps.setInt(2, examId);
            return ps.executeUpdate() > 0;
        }
    }

    public Exam getById(int examId) throws Exception {
        String sql = "SELECT * FROM EXAM WHERE exam_id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, examId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapResultSet(rs);
        }
        return null;
    }

    public List<Exam> getAll() throws Exception {
        String sql = "SELECT * FROM EXAM ORDER BY exam_date, session";
        List<Exam> exams = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) exams.add(mapResultSet(rs));
        }
        return exams;
    }

    public List<Exam> getByCourseCode(String courseCode) throws Exception {
        String sql = "SELECT * FROM EXAM WHERE course_code = ?";
        List<Exam> exams = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, courseCode);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) exams.add(mapResultSet(rs));
        }
        return exams;
    }

    public List<Exam> getByDateAndSession(LocalDate date, String session) throws Exception {
        String sql = "SELECT * FROM EXAM WHERE exam_date = ? AND session = ?";
        List<Exam> exams = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(date));
            ps.setString(2, session);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) exams.add(mapResultSet(rs));
        }
        return exams;
    }

    private Exam mapResultSet(ResultSet rs) throws Exception {
        String state = "DRAFT";
        try { state = rs.getString("state"); } catch (Exception ignored) {}
        return new Exam(
            rs.getInt("exam_id"),
            rs.getString("course_code"),
            rs.getDate("exam_date").toLocalDate(),
            rs.getString("session"),
            state
        );
    }
}