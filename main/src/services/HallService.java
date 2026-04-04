package src.services;

import src.dao.HallDAO;
import src.models.Hall;
import src.validators.HallValidator;

import java.util.ArrayList;
import java.util.List;

public class HallService {

    private final HallDAO dao = new HallDAO();

    // -------------------------
    // CREATE (batch)
    // DB assigns hall_id via SERIAL — do NOT check for pre-existing ID.
    // -------------------------
    public List<Hall> add(List<Hall> halls) {

        if (halls == null || halls.isEmpty()) {
            throw new IllegalArgumentException("No halls were provided.");
        }

        List<Hall> newHalls = new ArrayList<>();

        try {
            for (Hall hall : halls) {

                if (hall == null) {
                    throw new IllegalArgumentException("Hall information cannot be empty.");
                }

                HallValidator.validate(hall);
                Hall added = dao.insert(hall);
                newHalls.add(added);
            }
            return newHalls;

        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Unable to create halls. Please try again.", e);
        }
    }

    // -------------------------
    // CREATE (single)
    // DB assigns hall_id via SERIAL.
    // -------------------------
    public Hall addSingle(Hall hall) {

        if (hall == null) {
            throw new IllegalArgumentException("Hall information cannot be empty.");
        }

        validateForInsert(hall);

        try {
            return dao.insert(hall);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Unable to create the hall.", e);
        }
    }

    // -------------------------
    // READ ALL
    // -------------------------
    public List<Hall> getAll() {

        try {
            return dao.getAll();
        } catch (Exception e) {
            throw new RuntimeException("Unable to retrieve hall records.", e);
        }
    }

    // -------------------------
    // READ BY ID
    // -------------------------
    public Hall getById(int hallId) {

        try {
            Hall hall = dao.getById(hallId);
            if (hall == null) {
                throw new IllegalArgumentException("The selected hall could not be found.");
            }
            return hall;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Unable to retrieve the hall information.", e);
        }
    }

    // -------------------------
    // READ BY NAME
    // -------------------------
    public Hall getByName(String hallName) {

        try {
            Hall hall = dao.getByName(hallName);
            if (hall == null) {
                throw new IllegalArgumentException("The selected hall could not be found.");
            }
            return hall;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Unable to retrieve the hall.", e);
        }
    }

    // -------------------------
    // UPDATE (requires existing hall_id)
    // -------------------------
    public Hall update(Hall hall) {

        if (hall == null) {
            throw new IllegalArgumentException("Hall information cannot be empty.");
        }

        HallValidator.validate(hall);

        try {
            Hall existing = dao.getById(hall.getHallId());
            if (existing == null) {
                throw new IllegalArgumentException("The selected hall does not exist.");
            }
            return dao.update(hall);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Unable to update the hall.", e);
        }
    }

    // -------------------------
    // DELETE BY NAME
    // -------------------------
    public Hall delete(String name) {

        try {
            Hall hall = dao.getByName(name);
            if (hall == null) {
                throw new IllegalArgumentException("The selected hall could not be found.");
            }
            dao.delete(hall.getHallId());
            return hall;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Unable to delete the hall.", e);
        }
    }

    // -------------------------
    // DELETE BY ID
    // -------------------------
    public boolean deleteById(int hallId) {

        try {
            Hall hall = dao.getById(hallId);
            if (hall == null) {
                throw new IllegalArgumentException("The selected hall could not be found.");
            }
            return dao.delete(hallId);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Unable to delete the hall.", e);
        }
    }

    // -------------------------
    // PRIVATE: validate for INSERT (no id check)
    // -------------------------
    private void validateForInsert(Hall hall) {
        if (hall.getHallName() == null || hall.getHallName().isBlank()) {
            throw new IllegalArgumentException("Hall name cannot be empty.");
        }
        if (hall.getSeatingCapacity() <= 0) {
            throw new IllegalArgumentException("Seating capacity must be greater than 0.");
        }
    }
        // -------------------------
    // IMPORT BATCH (from CSV)
    // -------------------------
    public ImportResult importBatch(CsvImporter.ParsedData<Hall> parsed) {

        ImportResult result = new ImportResult();

        for (ImportResult.FailedRow f : parsed.failed) {
            result.addFailure(f.getRow(), f.getReason());
        }

        for (int i = 0; i < parsed.items.size(); i++) {
            Hall h = parsed.items.get(i);
            String[] raw = parsed.rawRows.get(i);

            try {
                addSingle(h);
                result.addSuccess(raw);
                AuditService.log("IMPORT", "HALL", "Imported: " + h.getHallName());
            } catch (Exception e) {
                String reason = extractMessage(e);
                result.addFailure(raw, reason);
            }
        }

        AuditService.log("BATCH_IMPORT", "HALL", result.summary());
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
        if (msg.contains("duplicate key")) return "A hall with this name already exists";
        if (msg.contains("violates not-null")) return "A required field is missing";
        return msg;
    }
}