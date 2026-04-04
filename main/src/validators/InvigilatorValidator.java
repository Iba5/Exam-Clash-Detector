package src.validators;

import src.models.Invigilator;

public class InvigilatorValidator {

    public static void validate(Invigilator inv) {

        Validator.requireNonNull(inv, "Invigilator cannot be null");

        Validator.requireNonEmpty(inv.getName(), "Name required");
        Validator.requireNonEmpty(inv.getDepartment(), "Department required");

       
    }
}
