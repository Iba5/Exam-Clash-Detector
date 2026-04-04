package src.export;

import src.DBConnection.DBConnection;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * ReportExporter — generates a styled HTML report from live DB data.
 * Open the HTML file in any browser and use File → Print → Save as PDF for a clean PDF output.
 * Uses only java.io and JDBC; no external libraries needed.
 */
public class ReportExporter {

    // -------------------------
    // FULL HALL ALLOCATION REPORT
    // -------------------------
    public static String exportFullReport(String directory) throws IOException {

        String filename = directory + "/exam_report_" + timestamp() + ".html";

        try (Connection con = DBConnection.getConnection();
             PrintWriter pw = new PrintWriter(new FileWriter(filename))) {

            pw.println(htmlHead("Full Exam Hall Allocation Report"));
            pw.println("<body>");
            pw.println("<div class='container'>");
            pw.printf("<h1>Exam Hall Allocation Report</h1>%n");
            pw.printf("<p class='subtitle'>Generated: %s</p>%n",
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm")));

            // Stats summary bar
            pw.println(buildStatsBar(con));

            // Exam-by-exam tables
            String examSql =
                "SELECT exam_id, course_code, exam_date, session FROM EXAM ORDER BY exam_date, session";

            try (PreparedStatement ps = con.prepareStatement(examSql);
                 ResultSet exams = ps.executeQuery()) {

                while (exams.next()) {
                    int examId       = exams.getInt("exam_id");
                    String course    = exams.getString("course_code");
                    String date      = exams.getString("exam_date");
                    String session   = exams.getString("session");

                    pw.printf("<div class='exam-block'>%n");
                    pw.printf("<h2>%s &nbsp;<span class='badge'>%s %s</span></h2>%n",
                        h(course), h(date), h(session));

                    pw.println(buildAllocationsTable(con, examId));
                    pw.println(buildInvigilatorsTable(con, examId));
                    pw.println("</div>");
                }
            }

            pw.println("</div></body></html>");

        } catch (Exception e) {
            throw new IOException("Failed to generate report: " + e.getMessage(), e);
        }

        return filename;
    }

    // -------------------------
    // SINGLE EXAM REPORT
    // -------------------------
    public static String exportExamReport(int examId, String directory) throws IOException {

        String filename = directory + "/report_exam_" + examId + "_" + timestamp() + ".html";

        try (Connection con = DBConnection.getConnection();
             PrintWriter pw = new PrintWriter(new FileWriter(filename))) {

            // Get exam header info
            String examInfo = "";
            String examSql = "SELECT course_code, exam_date, session FROM EXAM WHERE exam_id = ?";
            try (PreparedStatement ps = con.prepareStatement(examSql)) {
                ps.setInt(1, examId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        examInfo = rs.getString("course_code") + " — " +
                                   rs.getString("exam_date") + " " + rs.getString("session");
                    }
                }
            }

            pw.println(htmlHead("Exam Report: " + examInfo));
            pw.println("<body>");
            pw.println("<div class='container'>");
            pw.printf("<h1>Exam Report</h1>%n");
            pw.printf("<h2>%s</h2>%n", h(examInfo));
            pw.printf("<p class='subtitle'>Generated: %s</p>%n",
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm")));

            pw.println(buildAllocationsTable(con, examId));
            pw.println(buildInvigilatorsTable(con, examId));

            pw.println("</div></body></html>");

        } catch (Exception e) {
            throw new IOException("Failed to generate exam report: " + e.getMessage(), e);
        }

        return filename;
    }

    // -------------------------
    // HELPERS
    // -------------------------

    private static String buildStatsBar(Connection con) throws Exception {

        int students     = scalar(con, "SELECT COUNT(*) FROM STUDENT");
        int courses      = scalar(con, "SELECT COUNT(*) FROM COURSE");
        int halls        = scalar(con, "SELECT COUNT(*) FROM HALL");
        int invigilators = scalar(con, "SELECT COUNT(*) FROM INVIGILATOR");
        int exams        = scalar(con, "SELECT COUNT(*) FROM EXAM");
        int allocations  = scalar(con, "SELECT COUNT(*) FROM STUDENT_SEAT_ALLOCATION");

        return "<div class='stats-bar'>" +
               stat("Students",    students) +
               stat("Courses",     courses) +
               stat("Halls",       halls) +
               stat("Invigilators",invigilators) +
               stat("Exams",       exams) +
               stat("Allocations", allocations) +
               "</div>";
    }

    private static String stat(String label, int value) {
        return "<div class='stat'><div class='stat-val'>" + value +
               "</div><div class='stat-lbl'>" + label + "</div></div>";
    }

    private static String buildAllocationsTable(Connection con, int examId) throws Exception {

        StringBuilder sb = new StringBuilder();
        sb.append("<h3>Seat Allocations</h3>");
        sb.append("<table><thead><tr>")
          .append("<th>#</th><th>Roll Number</th><th>Student Name</th>")
          .append("<th>Department</th><th>Hall</th>")
          .append("</tr></thead><tbody>");

        String sql =
            "SELECT ssa.allocation_id, s.roll_number, s.name, s.department, h.hall_name " +
            "FROM STUDENT_SEAT_ALLOCATION ssa " +
            "JOIN STUDENT s ON ssa.roll_number = s.roll_number " +
            "JOIN HALL h    ON ssa.hall_id     = h.hall_id " +
            "WHERE ssa.exam_id = ? " +
            "ORDER BY h.hall_name, s.roll_number";

        int count = 0;

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, examId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    count++;
                    String rowClass = (count % 2 == 0) ? " class='even'" : "";
                    sb.append("<tr").append(rowClass).append(">")
                      .append("<td>").append(count).append("</td>")
                      .append("<td>").append(h(rs.getString("roll_number"))).append("</td>")
                      .append("<td>").append(h(rs.getString("name"))).append("</td>")
                      .append("<td>").append(h(rs.getString("department"))).append("</td>")
                      .append("<td>").append(h(rs.getString("hall_name"))).append("</td>")
                      .append("</tr>");
                }
            }
        }

        if (count == 0) {
            sb.append("<tr><td colspan='5' style='text-align:center;color:#aaa;'>No allocations found</td></tr>");
        }

        sb.append("</tbody></table>");
        sb.append("<p style='color:#aaa;font-size:13px;'>Total: ").append(count).append(" students</p>");

        return sb.toString();
    }

    private static String buildInvigilatorsTable(Connection con, int examId) throws Exception {

        StringBuilder sb = new StringBuilder();
        sb.append("<h3>Invigilator Assignments</h3>");
        sb.append("<table><thead><tr>")
          .append("<th>#</th><th>Name</th><th>Department</th><th>Email</th><th>Hall</th>")
          .append("</tr></thead><tbody>");

        String sql =
            "SELECT i.name, i.department, i.email, h.hall_name " +
            "FROM INVIGILATOR_ALLOCATION ia " +
            "JOIN INVIGILATOR i ON ia.invigilator_id = i.invigilator_id " +
            "JOIN HALL h        ON ia.hall_id          = h.hall_id " +
            "WHERE ia.exam_id = ? " +
            "ORDER BY h.hall_name, i.name";

        int count = 0;

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, examId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    count++;
                    String rowClass = (count % 2 == 0) ? " class='even'" : "";
                    sb.append("<tr").append(rowClass).append(">")
                      .append("<td>").append(count).append("</td>")
                      .append("<td>").append(h(rs.getString("name"))).append("</td>")
                      .append("<td>").append(h(rs.getString("department"))).append("</td>")
                      .append("<td>").append(h(rs.getString("email"))).append("</td>")
                      .append("<td>").append(h(rs.getString("hall_name"))).append("</td>")
                      .append("</tr>");
                }
            }
        }

        if (count == 0) {
            sb.append("<tr><td colspan='5' style='text-align:center;color:#aaa;'>No invigilators assigned</td></tr>");
        }

        sb.append("</tbody></table>");

        return sb.toString();
    }

    private static int scalar(Connection con, String sql) throws Exception {
        try (PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    /** HTML-escape a string to prevent injection in the report. */
    private static String h(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static String timestamp() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
    }

    private static String htmlHead(String title) {
        return "<!DOCTYPE html>\n<html lang='en'>\n<head>\n" +
               "<meta charset='UTF-8'>\n" +
               "<title>" + h(title) + "</title>\n" +
               "<style>\n" +
               "  body { font-family: 'Segoe UI', Arial, sans-serif; background:#0f0f14; color:#e0e0e0; margin:0; padding:0; }\n" +
               "  .container { max-width: 1100px; margin: 0 auto; padding: 30px; }\n" +
               "  h1 { color:#00c8ff; font-size:2rem; margin-bottom:4px; }\n" +
               "  h2 { color:#00c8ff; font-size:1.3rem; margin-top:40px; border-bottom:1px solid #333; padding-bottom:8px; }\n" +
               "  h3 { color:#aaa; font-size:1rem; margin-top:20px; margin-bottom:6px; }\n" +
               "  .subtitle { color:#777; font-size:0.9rem; margin-top:0; }\n" +
               "  .badge { background:#1a4a6e; color:#00c8ff; border-radius:4px; padding:2px 8px; font-size:0.85rem; }\n" +
               "  .stats-bar { display:flex; gap:20px; margin:24px 0; flex-wrap:wrap; }\n" +
               "  .stat { background:#16161e; border:1px solid #00c8ff55; border-radius:10px; padding:16px 24px; min-width:100px; text-align:center; }\n" +
               "  .stat-val { font-size:1.8rem; font-weight:bold; color:#fff; }\n" +
               "  .stat-lbl { font-size:0.8rem; color:#00c8ff; margin-top:4px; }\n" +
               "  .exam-block { margin-bottom:50px; }\n" +
               "  table { width:100%; border-collapse:collapse; margin-top:8px; }\n" +
               "  th { background:#16161e; color:#00dcff; font-size:0.85rem; padding:10px 12px; text-align:left; border-bottom:2px solid #00c8ff44; }\n" +
               "  td { padding:9px 12px; font-size:0.9rem; border-bottom:1px solid #ffffff18; }\n" +
               "  tr.even td { background:#16161e55; }\n" +
               "  tr:hover td { background:#00c8ff18; }\n" +
               "  @media print {\n" +
               "    body { background:#fff; color:#000; }\n" +
               "    .container { max-width:100%; }\n" +
               "    h1,h2,h3 { color:#000; }\n" +
               "    .badge { background:#eee; color:#000; }\n" +
               "    .stat { border:1px solid #ccc; }\n" +
               "    .stat-val { color:#000; }\n" +
               "    .stat-lbl,.subtitle { color:#555; }\n" +
               "    th { background:#eee; color:#000; }\n" +
               "    tr.even td { background:#f9f9f9; }\n" +
               "    tr:hover td { background:transparent; }\n" +
               "  }\n" +
               "</style>\n</head>\n";
    }
}
