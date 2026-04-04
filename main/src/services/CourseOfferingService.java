package src.services;

import src.dao.CourseOfferingDAO;
import src.models.CourseOffering;

import java.util.List;

public class CourseOfferingService {

    private final CourseOfferingDAO dao = new CourseOfferingDAO();

    // ── CREATE ──────────────────────────────────────────────────────

    public CourseOffering add(CourseOffering offering) {
        if (offering == null) {
            throw new IllegalArgumentException("Course offering cannot be empty.");
        }
        try {
            return dao.insert(offering);
        } catch (Exception e) {
            throw new RuntimeException("Unable to create course offering.", e);
        }
    }

    // ── READ ALL ────────────────────────────────────────────────────

    public List<CourseOffering> getAll() {
        try {
            return dao.getAll();
        } catch (Exception e) {
            throw new RuntimeException("Unable to retrieve course offerings.", e);
        }
    }

    // ── EMPTY CHECK ─────────────────────────────────────────────────

    /** Returns true if the COURSE_OFFERING table has at least one row. */
    public boolean hasOfferings() {
        try {
            return !dao.getAll().isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    // ── IMPORT BATCH (from CSV) ──────────────────────────────────────

    public ImportResult importBatch(CsvImporter.ParsedData<CourseOffering> parsed) {
        ImportResult result = new ImportResult();

        for (ImportResult.FailedRow f : parsed.failed) {
            result.addFailure(f.getRow(), f.getReason());
        }

        for (int i = 0; i < parsed.items.size(); i++) {
            CourseOffering co = parsed.items.get(i);
            String[] raw = parsed.rawRows.get(i);
            try {
                add(co);
                result.addSuccess(raw);
                AuditService.log("IMPORT", "COURSE_OFFERING",
                    "Imported: " + co.getCourseCode() + " / " + co.getDepartment() + " sem " + co.getSemester());
            } catch (Exception e) {
                result.addFailure(raw, extractMessage(e));
            }
        }

        AuditService.log("BATCH_IMPORT", "COURSE_OFFERING", result.summary());
        return result;
    }

    private String extractMessage(Exception e) {
        if (e.getCause() != null && e.getCause().getMessage() != null)
            return cleanMessage(e.getCause().getMessage());
        return cleanMessage(e.getMessage());
    }

    private String cleanMessage(String msg) {
        if (msg == null) return "Unknown error";
        msg = msg.replaceAll("^(java\\.lang\\.|java\\.sql\\.)", "");
        if (msg.contains("Duplicate entry") || msg.contains("duplicate key"))
            return "This course offering already exists";
        if (msg.contains("foreign key") || msg.contains("violates foreign key"))
            return "Course code does not exist — import courses first";
        return msg;
    }
}
