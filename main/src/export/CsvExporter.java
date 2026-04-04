package src.export;

import src.DBConnection.DBConnection;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * CsvExporter — zero-dependency CSV export for seating plans and exam summaries.
 * Uses only java.io and JDBC; no external libraries needed.
 */
public class CsvExporter {

    // -------------------------
    // FULL SEATING PLAN (all exams)
    // -------------------------
    public static String exportFullSeatingPlan(String directory) throws IOException {

        String filename = directory + "/seating_plan_" + timestamp() + ".csv";

        String sql =
            "SELECT e.exam_id, e.course_code, e.exam_date, e.session, " +
            "       s.roll_number, s.name AS student_name, s.department, " +
            "       h.hall_name, h.seating_capacity, " +
            "       ssa.allocation_id " +
            "FROM STUDENT_SEAT_ALLOCATION ssa " +
            "JOIN EXAM e    ON ssa.exam_id    = e.exam_id " +
            "JOIN STUDENT s ON ssa.roll_number = s.roll_number " +
            "JOIN HALL h    ON ssa.hall_id    = h.hall_id " +
            "ORDER BY e.exam_date, e.session, h.hall_name, s.roll_number";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery();
             PrintWriter pw = new PrintWriter(new FileWriter(filename))) {

            pw.println("Allocation ID,Exam ID,Course Code,Exam Date,Session," +
                       "Roll Number,Student Name,Department,Hall,Hall Capacity");

            while (rs.next()) {
                pw.printf("%d,%d,%s,%s,%s,%s,%s,%s,%s,%d%n",
                    rs.getInt("allocation_id"),
                    rs.getInt("exam_id"),
                    escapeCsv(rs.getString("course_code")),
                    rs.getString("exam_date"),
                    rs.getString("session"),
                    escapeCsv(rs.getString("roll_number")),
                    escapeCsv(rs.getString("student_name")),
                    escapeCsv(rs.getString("department")),
                    escapeCsv(rs.getString("hall_name")),
                    rs.getInt("seating_capacity")
                );
            }
        } catch (Exception e) {
            throw new IOException("Failed to export seating plan: " + e.getMessage(), e);
        }

        return filename;
    }

    // -------------------------
    // SINGLE EXAM SEATING PLAN
    // -------------------------
    public static String exportExamSeatingPlan(int examId, String directory) throws IOException {

        String filename = directory + "/seating_exam_" + examId + "_" + timestamp() + ".csv";

        String sql =
            "SELECT e.exam_id, e.course_code, e.exam_date, e.session, " +
            "       s.roll_number, s.name AS student_name, s.department, " +
            "       h.hall_name, ssa.allocation_id " +
            "FROM STUDENT_SEAT_ALLOCATION ssa " +
            "JOIN EXAM e    ON ssa.exam_id    = e.exam_id " +
            "JOIN STUDENT s ON ssa.roll_number = s.roll_number " +
            "JOIN HALL h    ON ssa.hall_id    = h.hall_id " +
            "WHERE ssa.exam_id = ? " +
            "ORDER BY h.hall_name, s.roll_number";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, examId);

            try (ResultSet rs = ps.executeQuery();
                 PrintWriter pw = new PrintWriter(new FileWriter(filename))) {

                pw.println("Allocation ID,Course Code,Exam Date,Session," +
                           "Roll Number,Student Name,Department,Hall");

                while (rs.next()) {
                    pw.printf("%d,%s,%s,%s,%s,%s,%s,%s%n",
                        rs.getInt("allocation_id"),
                        escapeCsv(rs.getString("course_code")),
                        rs.getString("exam_date"),
                        rs.getString("session"),
                        escapeCsv(rs.getString("roll_number")),
                        escapeCsv(rs.getString("student_name")),
                        escapeCsv(rs.getString("department")),
                        escapeCsv(rs.getString("hall_name"))
                    );
                }
            }
        } catch (Exception e) {
            throw new IOException("Failed to export exam seating plan: " + e.getMessage(), e);
        }

        return filename;
    }

    // -------------------------
    // EXAM SUMMARY (per-exam totals)
    // -------------------------
    public static String exportExamSummary(String directory) throws IOException {

        String filename = directory + "/exam_summary_" + timestamp() + ".csv";

        String sql =
            "SELECT e.exam_id, e.course_code, e.exam_date, e.session, " +
            "       COUNT(DISTINCT ssa.roll_number) AS total_students, " +
            "       COUNT(DISTINCT ssa.hall_id)     AS halls_used, " +
            "       COUNT(DISTINCT ia.invigilator_id) AS invigilators_assigned " +
            "FROM EXAM e " +
            "LEFT JOIN STUDENT_SEAT_ALLOCATION ssa ON ssa.exam_id = e.exam_id " +
            "LEFT JOIN INVIGILATOR_ALLOCATION  ia  ON ia.exam_id  = e.exam_id " +
            "GROUP BY e.exam_id, e.course_code, e.exam_date, e.session " +
            "ORDER BY e.exam_date, e.session";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery();
             PrintWriter pw = new PrintWriter(new FileWriter(filename))) {

            pw.println("Exam ID,Course Code,Exam Date,Session," +
                       "Total Students,Halls Used,Invigilators Assigned");

            while (rs.next()) {
                pw.printf("%d,%s,%s,%s,%d,%d,%d%n",
                    rs.getInt("exam_id"),
                    escapeCsv(rs.getString("course_code")),
                    rs.getString("exam_date"),
                    rs.getString("session"),
                    rs.getInt("total_students"),
                    rs.getInt("halls_used"),
                    rs.getInt("invigilators_assigned")
                );
            }
        } catch (Exception e) {
            throw new IOException("Failed to export exam summary: " + e.getMessage(), e);
        }

        return filename;
    }

    // -------------------------
    // HELPERS
    // -------------------------

    /** Wraps a CSV field in quotes if it contains commas or quotes. */
    private static String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private static String timestamp() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
    }
}
