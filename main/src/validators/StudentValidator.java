package src.validators;

import src.models.Students;

public class StudentValidator {

    public static void validate(Students student) {

        Validator.requireNonNull(student, "Student cannot be null");

        Validator.requireNonEmpty(student.getRollNumber(), "Roll number is required");
        Validator.requireNonEmpty(student.getName(), "Name is required");
        Validator.requireNonEmpty(student.getDepartment(), "Department is required");

        Validator.requireRange(student.getSemester(), 1, 8,
                "Semester must be between 1 and 8");
    }
}
