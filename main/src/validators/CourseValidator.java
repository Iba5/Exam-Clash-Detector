package src.validators;

import src.models.Courses;

public class CourseValidator {

    public static void validate(Courses course) {

        Validator.requireNonNull(course, "Course cannot be null");

        Validator.requireNonEmpty(course.getCourseCode(), "Course code required");
        Validator.requireNonEmpty(course.getCourseName(), "Course name required");

        Validator.requirePositive(course.getCredits(), "Credits must be positive");
    }
}

