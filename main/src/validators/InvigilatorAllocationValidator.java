package src.validators;

import src.models.InvigilatorAllocation;

public class InvigilatorAllocationValidator {
    public static void validate(InvigilatorAllocation invAlloc) {

        Validator.requireNonNull(invAlloc, "Invigilator allocation cannot be null");

        Validator.requirePositive(invAlloc.getInvigilatorId(), "Invigilator ID required");
        Validator.requirePositive(invAlloc.getExamId(), "Exam ID required");
        Validator.requirePositive(invAlloc.getHallId(), "Hall ID required");
    }
}
