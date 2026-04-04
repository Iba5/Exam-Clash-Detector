package src.models;

import java.util.Objects;

public class Students {

    private String rollNumber;
    private String name;
    private String department;
    private int semester;

    public Students() {}

    public Students(String rollNumber, String name, String department, int semester) {
        this.rollNumber = rollNumber;
        this.name = name;
        this.department = department;
        this.semester = semester;
    }

    public String getRollNumber() {
        return rollNumber;
    }

    public void setRollNumber(String rollNumber) {
        if (rollNumber == null || rollNumber.isBlank()) {
            throw new IllegalArgumentException("Roll number cannot be empty");
        }
        this.rollNumber = rollNumber;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public int getSemester() {
        return semester;
    }

    public void setSemester(int semester) {
        if (semester <= 0) {
            throw new IllegalArgumentException("Semester must be greater than 0");
        }
        this.semester = semester;
    }

    @Override
    public String toString() {
        return rollNumber + " - " + name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Students)) return false;
        Students student = (Students) o;
        return Objects.equals(rollNumber, student.rollNumber);
    }

    @Override
    public int hashCode() {
        return Objects.hash(rollNumber);
    }
}
