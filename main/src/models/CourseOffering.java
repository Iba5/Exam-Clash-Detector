package src.models;

import java.util.Objects;

public class CourseOffering {

    private int offeringId;
    private String courseCode;
    private String department;
    private int semester;

    public CourseOffering() {}

    public CourseOffering(int offeringId, String courseCode, String department, int semester) {
        this.offeringId = offeringId;
        setCourseCode(courseCode);
        setDepartment(department);
        setSemester(semester);
    }

    public int getOfferingId() {
        return offeringId;
    }

    public void setOfferingId(int offeringId) {
        this.offeringId = offeringId;
    }

    public String getCourseCode() {
        return courseCode;
    }

    public void setCourseCode(String courseCode) {
        if (courseCode == null || courseCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Course code cannot be empty");
        }
        this.courseCode = courseCode;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        if (department == null || department.trim().isEmpty()) {
            throw new IllegalArgumentException("Department cannot be empty");
        }
        this.department = department;
    }

    public int getSemester() {
        return semester;
    }

    public void setSemester(int semester) {
        if (semester <= 0) {
            throw new IllegalArgumentException("Semester must be positive");
        }
        this.semester = semester;
    }

    @Override
    public String toString() {
        return courseCode + " - " + department + " (Sem " + semester + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CourseOffering)) return false;
        CourseOffering that = (CourseOffering) o;
        return offeringId == that.offeringId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(offeringId);
    }
}