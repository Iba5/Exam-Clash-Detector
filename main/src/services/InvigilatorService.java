package src.services;

import src.dao.InvigilatorDAO;
import src.models.Invigilator;
import src.validators.InvigilatorValidator;

import java.util.List;

public class InvigilatorService {

    private final InvigilatorDAO dao = new InvigilatorDAO();

    // -------------------------
    // CREATE
    // -------------------------
    public Invigilator add(Invigilator invigilator) {

        if (invigilator == null) {
            throw new IllegalArgumentException(
                    "Invigilator information cannot be empty."
            );
        }

        InvigilatorValidator.validate(invigilator);

        try {

            // check duplicate email
            Invigilator existing = dao.getByEmail(invigilator.getEmail());

            if (existing != null) {
                throw new IllegalArgumentException(
                        "An invigilator with this email already exists."
                );
            }

            return dao.insert(invigilator);

        } catch (IllegalArgumentException e) {

            throw e;

        } catch (Exception e) {

            throw new RuntimeException(
                    "Unable to create the invigilator."
            );
        }
    }

    // -------------------------
    // READ ALL
    // -------------------------
    public List<Invigilator> getAll() {

        try {

            return dao.getAll();

        } catch (Exception e) {

            throw new RuntimeException(
                    "Unable to retrieve invigilator records."
            );
        }
    }

    // -------------------------
    // READ BY ID
    // -------------------------
    public Invigilator getById(int invigilatorId) {

        try {

            Invigilator invigilator = dao.getById(invigilatorId);

            if (invigilator == null) {
                throw new IllegalArgumentException(
                        "The selected invigilator could not be found."
                );
            }

            return invigilator;

        } catch (IllegalArgumentException e) {

            throw e;

        } catch (Exception e) {

            throw new RuntimeException(
                    "Unable to retrieve the invigilator."
            );
        }
    }

    // -------------------------
    // READ BY DEPARTMENT
    // -------------------------
    public List<Invigilator> getByDepartment(String department) {

        try {

            return dao.getByDepartment(department);

        } catch (Exception e) {

            throw new RuntimeException(
                    "Unable to retrieve invigilators for this department."
            );
        }
    }

    // -------------------------
    // READ BY EMAIL
    // -------------------------
    public Invigilator getByEmail(String email) {

        try {

            Invigilator invigilator = dao.getByEmail(email);

            if (invigilator == null) {
                throw new IllegalArgumentException(
                        "No invigilator found with the provided email."
                );
            }

            return invigilator;

        } catch (IllegalArgumentException e) {

            throw e;

        } catch (Exception e) {

            throw new RuntimeException(
                    "Unable to retrieve the invigilator."
            );
        }
    }

    // -------------------------
    // DELETE
    // -------------------------
    public boolean deleteById(int invigilatorId) {

        try {

            Invigilator invigilator = dao.getById(invigilatorId);

            if (invigilator == null) {
                throw new IllegalArgumentException(
                        "The selected invigilator could not be found."
                );
            }

            return dao.delete(invigilatorId);

        } catch (IllegalArgumentException e) {

            throw e;

        } catch (Exception e) {

            throw new RuntimeException(
                    "Unable to delete the invigilator."
            );
        }
    }

    // -------------------------
    // UPDATE
    // -------------------------
    public Invigilator update(Invigilator invigilator) {

        if (invigilator == null) {
            throw new IllegalArgumentException(
                    "Invigilator information cannot be empty."
            );
        }

        InvigilatorValidator.validate(invigilator);

        try {

            Invigilator existing = dao.getById(invigilator.getInvigilatorId());

            if (existing == null) {
                throw new IllegalArgumentException(
                        "The selected invigilator does not exist."
                );
            }

            return dao.update(invigilator);

        } catch (IllegalArgumentException e) {

            throw e;

        } catch (Exception e) {

            throw new RuntimeException(
                    "Unable to update the invigilator."
            );
        }
    }
        // -------------------------
    // IMPORT BATCH (from CSV)
    // -------------------------
    public ImportResult importBatch(CsvImporter.ParsedData<Invigilator> parsed) {

        ImportResult result = new ImportResult();

        for (ImportResult.FailedRow f : parsed.failed) {
            result.addFailure(f.getRow(), f.getReason());
        }

        for (int i = 0; i < parsed.items.size(); i++) {
            Invigilator inv = parsed.items.get(i);
            String[] raw = parsed.rawRows.get(i);

            try {
                add(inv);
                result.addSuccess(raw);
                AuditService.log("IMPORT", "INVIGILATOR", "Imported: " + inv.getName());
            } catch (Exception e) {
                String reason = extractMessage(e);
                result.addFailure(raw, reason);
            }
        }

        AuditService.log("BATCH_IMPORT", "INVIGILATOR", result.summary());
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
        if (msg.contains("duplicate key") && msg.contains("email"))
            return "This email is already registered";
        if (msg.contains("duplicate key")) return "This invigilator already exists";
        if (msg.contains("violates not-null")) return "A required field is missing";
        return msg;
    }
}