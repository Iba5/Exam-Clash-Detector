package src.services;

import src.dao.StudentDAO;
import src.models.Students;
import src.validators.StudentValidator;

import java.util.List;

public class StudentService {

    private final StudentDAO dao = new StudentDAO();

    // -------------------------
    // CREATE
    // -------------------------
    public Students add(Students student) {

        if(student == null){
            throw new IllegalArgumentException(
                    "Student information cannot be empty."
            );
        }

        StudentValidator.validate(student);

        try{

            Students existing = dao.getByRollNumber(student.getRollNumber());

            if(existing != null){
                throw new IllegalArgumentException(
                        "A student with this roll number already exists."
                );
            }

            return dao.insert(student);

        }catch(IllegalArgumentException e){

            throw e;

        }catch(Exception e){

            throw new RuntimeException(
                    "Unable to create the student record."
            );
        }
    }

    // -------------------------
    // READ ALL
    // -------------------------
    public List<Students> getAllStudents(){

        try{

            return dao.getAll();

        }catch(Exception e){

            throw new RuntimeException(
                    "Unable to retrieve the list of students."
            );
        }
    }

    // -------------------------
    // READ BY ROLL NUMBER
    // -------------------------
    public Students getByRollNumber(String rollNumber){

        try{

            Students student = dao.getByRollNumber(rollNumber);

            if(student == null){
                throw new IllegalArgumentException(
                        "The requested student could not be found."
                );
            }

            return student;

        }catch(IllegalArgumentException e){

            throw e;

        }catch(Exception e){

            throw new RuntimeException(
                    "Unable to retrieve the student record."
            );
        }
    }

    // -------------------------
    // READ BY DEPARTMENT
    // -------------------------
    public List<Students> getByDepartment(String department){

        try{

            return dao.getByDepartment(department);

        }catch(Exception e){

            throw new RuntimeException(
                    "Unable to retrieve students for the selected department."
            );
        }
    }

    // -------------------------
    // READ BY SEMESTER
    // -------------------------
    public List<Students> getBySemester(int semester){

        try{

            return dao.getBySemester(semester);

        }catch(Exception e){

            throw new RuntimeException(
                    "Unable to retrieve students for the selected semester."
            );
        }
    }

    // -------------------------
    // DELETE
    // -------------------------
    public Students delete(String rollNumber){

        try{

            Students student = dao.getByRollNumber(rollNumber);

            if(student == null){
                throw new IllegalArgumentException(
                        "The selected student does not exist."
                );
            }

            dao.deleteByRollNumber(rollNumber);

            return student;

        }catch(IllegalArgumentException e){

            throw e;

        }catch(Exception e){

            throw new RuntimeException(
                    "Unable to delete the student record."
            );
        }
    }

    // -------------------------
    // UPDATE
    // -------------------------
    public Students update(Students student){

        if(student == null){
            throw new IllegalArgumentException(
                    "Student information cannot be empty."
            );
        }

        StudentValidator.validate(student);

        try{

            Students existing = dao.getByRollNumber(student.getRollNumber());

            if(existing == null){
                throw new IllegalArgumentException(
                        "The selected student does not exist."
                );
            }

            return dao.update(student);

        }catch(IllegalArgumentException e){

            throw e;

        }catch(Exception e){

            throw new RuntimeException(
                    "Unable to update the student record."
            );
        }
    }
        // -------------------------
    // IMPORT BATCH (from CSV)
    // -------------------------
    public ImportResult importBatch(CsvImporter.ParsedData<Students> parsed) {

        ImportResult result = new ImportResult();

        // First, carry over any parse-time failures
        for (ImportResult.FailedRow f : parsed.failed) {
            result.addFailure(f.getRow(), f.getReason());
        }

        // Now try inserting each parsed student
        for (int i = 0; i < parsed.items.size(); i++) {
            Students s = parsed.items.get(i);
            String[] raw = parsed.rawRows.get(i);

            try {
                add(s); // calls existing single-add with validation + duplicate check
                result.addSuccess(raw);
                AuditService.log("IMPORT", "STUDENT", "Imported: " + s.getRollNumber());
            } catch (Exception e) {
                String reason = extractMessage(e);
                result.addFailure(raw, reason);
            }
        }

        AuditService.log("BATCH_IMPORT", "STUDENT", result.summary());
        return result;
    }

    private String extractMessage(Exception e) {
        if (e.getCause() != null && e.getCause().getMessage() != null) {
            return cleanMessage(e.getCause().getMessage());
        }
        return cleanMessage(e.getMessage());
    }

    private String cleanMessage(String msg) {
        if (msg == null) return "Unknown error";
        // Strip Java class prefixes that leak through
        msg = msg.replaceAll("^(java\\.lang\\.|java\\.sql\\.|org\\.postgresql\\.util\\.PSQLException:\\s*)", "");
        // Simplify common DB errors
        if (msg.contains("duplicate key")) {
            if (msg.contains("roll_number")) return "A student with this roll number already exists";
            if (msg.contains("course_code")) return "This course code already exists";
            if (msg.contains("hall_name"))   return "A hall with this name already exists";
            if (msg.contains("email"))       return "This email is already registered";
            return "Duplicate entry — this record already exists";
        }
        if (msg.contains("violates foreign key")) {
            if (msg.contains("course_code")) return "Course code does not exist — import courses first";
            return "Referenced record does not exist";
        }
        if (msg.contains("violates not-null")) return "A required field is missing";
        return msg;
    }
}