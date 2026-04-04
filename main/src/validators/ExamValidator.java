package src.validators;

import src.models.Exam;

public class ExamValidator {

    public static void validate(Exam exam) {

        Validator.requireNonNull(exam, "Exam cannot be null");

        Validator.requireNonEmpty(exam.getCourseCode(), "Course code is required");

        Validator.requireNonNull(exam.getExamDate(), "Exam date is required");

        Validator.requireNonEmpty(exam.getSession(), "Session is required");

        String session = exam.getSession().toUpperCase();

        if (!session.equals("FN") && !session.equals("AN")) {
            throw new IllegalArgumentException("Session must be FN or AN");
        }
    }
}
