package src.services;

import src.DBConnection.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class DashboardService {

    // ── LIVE COUNTS ──

    public int countStudents()      { return countFrom("SELECT COUNT(*) FROM STUDENT"); }
    public int countCourses()       { return countFrom("SELECT COUNT(*) FROM COURSE"); }
    public int countHalls()         { return countFrom("SELECT COUNT(*) FROM HALL"); }
    public int countInvigilators()  { return countFrom("SELECT COUNT(*) FROM INVIGILATOR"); }
    public int countExams()         { return countFrom("SELECT COUNT(*) FROM EXAM"); }
    public int countAllocations()   { return countFrom("SELECT COUNT(*) FROM STUDENT_SEAT_ALLOCATION"); }

    // ── EXAM SCHEDULE QUERIES ──

    public List<Object[]> getTodayExams() {
        String sql =
            "SELECT e.exam_id, e.course_code, e.exam_date, e.session, " +
            "       GROUP_CONCAT(DISTINCT h.hall_name ORDER BY h.hall_name SEPARATOR ', ') AS halls " +
            "FROM EXAM e " +
            "LEFT JOIN STUDENT_SEAT_ALLOCATION ssa ON ssa.exam_id = e.exam_id " +
            "LEFT JOIN HALL h ON h.hall_id = ssa.hall_id " +
            "WHERE e.exam_date = ? " +
            "GROUP BY e.exam_id, e.course_code, e.exam_date, e.session " +
            "ORDER BY e.session";
        return queryExams(sql, LocalDate.now().toString());
    }

    public List<Object[]> getAllExams() {
        String sql =
            "SELECT e.exam_id, e.course_code, e.exam_date, e.session, " +
            "       GROUP_CONCAT(DISTINCT h.hall_name ORDER BY h.hall_name SEPARATOR ', ') AS halls " +
            "FROM EXAM e " +
            "LEFT JOIN STUDENT_SEAT_ALLOCATION ssa ON ssa.exam_id = e.exam_id " +
            "LEFT JOIN HALL h ON h.hall_id = ssa.hall_id " +
            "GROUP BY e.exam_id, e.course_code, e.exam_date, e.session " +
            "ORDER BY e.exam_date DESC, e.session";
        return queryExams(sql, null);
    }

    public List<Object[]> getUpcomingExams() {
        String sql =
            "SELECT e.exam_id, e.course_code, e.exam_date, e.session, " +
            "       GROUP_CONCAT(DISTINCT h.hall_name ORDER BY h.hall_name SEPARATOR ', ') AS halls " +
            "FROM EXAM e " +
            "LEFT JOIN STUDENT_SEAT_ALLOCATION ssa ON ssa.exam_id = e.exam_id " +
            "LEFT JOIN HALL h ON h.hall_id = ssa.hall_id " +
            "WHERE e.exam_date > ? " +
            "GROUP BY e.exam_id, e.course_code, e.exam_date, e.session " +
            "ORDER BY e.exam_date ASC, e.session";
        return queryExams(sql, LocalDate.now().toString());
    }

    public List<Object[]> getCompletedExams() {
        String sql =
            "SELECT e.exam_id, e.course_code, e.exam_date, e.session, " +
            "       GROUP_CONCAT(DISTINCT h.hall_name ORDER BY h.hall_name SEPARATOR ', ') AS halls " +
            "FROM EXAM e " +
            "LEFT JOIN STUDENT_SEAT_ALLOCATION ssa ON ssa.exam_id = e.exam_id " +
            "LEFT JOIN HALL h ON h.hall_id = ssa.hall_id " +
            "WHERE e.exam_date < ? " +
            "GROUP BY e.exam_id, e.course_code, e.exam_date, e.session " +
            "ORDER BY e.exam_date DESC, e.session";
        return queryExams(sql, LocalDate.now().toString());
    }

    // ── HELPERS ──

    private int countFrom(String sql) {
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        } catch (Exception e) { e.printStackTrace(); }
        return 0;
    }

    private List<Object[]> queryExams(String sql, String date) {
        List<Object[]> rows = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            if (date != null) ps.setString(1, date);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String halls    = rs.getString("halls");
                    String session  = rs.getString("session");
                    String examDate = rs.getString("exam_date");
                    rows.add(new Object[]{
                        rs.getString("course_code"),
                        halls != null ? halls : "-",
                        examDate + " " + session
                    });
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
        return rows;
    }
}