package src.services;

import java.util.List;
import src.dao.CourseDAO;
import src.models.Courses;
import src.validators.CourseValidator;

public class CourseService {

    private final CourseDAO dao = new CourseDAO();

    // -------------------------
    // CREATE
    // -------------------------
    public Courses addCourse(Courses course) {

        CourseValidator.validate(course);

        try {
            return dao.insert(course);
        } catch (Exception e) {
            throw new RuntimeException("Error adding course", e);
        }
    }

    // -------------------------
    // READ ALL
    // -------------------------
    public List<Courses> getAllCourses() {

        try {
            return dao.getAll();
        } catch (Exception e) {
            throw new RuntimeException("Error fetching courses", e);
        }
    }

    // -------------------------
    // READ BY COURSE CODE
    // -------------------------
    public Courses getCourseByCode(String courseCode) {

        try {
            return dao.getByCode(courseCode);
        } catch (Exception e) {
            throw new RuntimeException("Error fetching course by code", e);
        }
    }

    // -------------------------
    // UPDATE
    // -------------------------
    public Courses updateCourse(Courses course) {

        CourseValidator.validate(course);

        try {
            return dao.update(course);
        } catch (Exception e) {
            throw new RuntimeException("Error updating course", e);
        }
    }

    // -------------------------
    // DELETE
    // -------------------------
    public Courses deleteCourse(String courseCode) {

        try {

            Courses course = dao.getByCode(courseCode);

            if (course == null) {
                throw new IllegalArgumentException("Course not found");
            }

            dao.delete(courseCode);

            return course;

        } catch (Exception e) {
            throw new RuntimeException("Error deleting course", e);
        }
    }
        // -------------------------
    // IMPORT BATCH (from CSV)
    // -------------------------
    public ImportResult importBatch(CsvImporter.ParsedData<Courses> parsed) {

        ImportResult result = new ImportResult();

        for (ImportResult.FailedRow f : parsed.failed) {
            result.addFailure(f.getRow(), f.getReason());
        }

        for (int i = 0; i < parsed.items.size(); i++) {
            Courses c = parsed.items.get(i);
            String[] raw = parsed.rawRows.get(i);

            try {
                addCourse(c);
                result.addSuccess(raw);
                AuditService.log("IMPORT", "COURSE", "Imported: " + c.getCourseCode());
            } catch (Exception e) {
                String reason = extractMessage(e);
                result.addFailure(raw, reason);
            }
        }

        AuditService.log("BATCH_IMPORT", "COURSE", result.summary());
        return result;
    }

    private String extractMessage(Exception e) {
        if (e.getCause() != null && e.getCause().getMessage() != null)
            return cleanMessage(e.getCause().getMessage());
        return cleanMessage(e.getMessage());
    }

    private String cleanMessage(String msg) {
        if (msg == null) return "Unknown error";
        msg = msg.replaceAll("^(java\\.lang\\.|java\\.sql\\.|org\\.postgresql\\.util\\.PSQLException:\\s*)", "");
        if (msg.contains("duplicate key")) return "This course code already exists";
        if (msg.contains("violates not-null")) return "A required field is missing";
        return msg;
    }
}