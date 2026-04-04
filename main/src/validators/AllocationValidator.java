package src.validators;

import src.models.Allocation;

public class AllocationValidator {

    public static void validate(Allocation allocation) {

        Validator.requireNonNull(allocation, "Allocation cannot be null");

        Validator.requireNonEmpty(allocation.getRollNumber(), "Roll number required");
        Validator.requirePositive(allocation.getExamId(), "Exam ID required");
        Validator.requirePositive(allocation.getHallId(), "Hall ID required");
    }
}