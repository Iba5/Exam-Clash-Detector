package src.models;

import java.time.LocalDate;
import java.util.Objects;

public class Exam {

    private int examId;
    private String courseCode;
    private LocalDate examDate;
    private String session; // FN or AN
    private String state;   // FSM state: DRAFT, SCHEDULED, ONGOING, COMPLETED, CANCELLED

    public Exam() {
        this.state = "DRAFT";
    }

    public Exam(int examId, String courseCode, LocalDate examDate, String session) {
        this.examId = examId;
        this.courseCode = courseCode;
        this.examDate = examDate;
        this.session = session;
        this.state = "DRAFT";
    }

    public Exam(int examId, String courseCode, LocalDate examDate, String session, String state) {
        this.examId = examId;
        this.courseCode = courseCode;
        this.examDate = examDate;
        this.session = session;
        this.state = (state != null) ? state : "DRAFT";
    }

    public int getExamId() { return examId; }
    public void setExamId(int examId) { this.examId = examId; }

    public String getCourseCode() { return courseCode; }
    public void setCourseCode(String courseCode) { this.courseCode = courseCode; }

    public LocalDate getExamDate() { return examDate; }
    public void setExamDate(LocalDate examDate) { this.examDate = examDate; }

    public String getSession() { return session; }
    public void setSession(String session) { this.session = session; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Exam)) return false;
        Exam exam = (Exam) o;
        return examId == exam.examId;
    }

    @Override
    public int hashCode() { return Objects.hash(examId); }

    @Override
    public String toString() {
        return "Exam{" + examId + ", " + courseCode + ", " + examDate + " " + session + ", " + state + '}';
    }
}