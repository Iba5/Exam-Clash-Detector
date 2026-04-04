package src.models;

import java.util.Objects;

public class InvigilatorAllocation {

    private int invigilatorAllocationId;
    private int invigilatorId;
    private int examId;
    private int hallId;

    public InvigilatorAllocation() {}

    public InvigilatorAllocation(int invigilatorAllocationId, int invigilatorId, int examId, int hallId) {
        this.invigilatorAllocationId = invigilatorAllocationId;
        this.invigilatorId = invigilatorId;
        this.examId = examId;
        this.hallId = hallId;
    }

    public int getInvigilatorAllocationId() {
        return invigilatorAllocationId;
    }

    public void setInvigilatorAllocationId(int invigilatorAllocationId) {
        this.invigilatorAllocationId = invigilatorAllocationId;
    }

    public int getInvigilatorId() {
        return invigilatorId;
    }

    public void setInvigilatorId(int invigilatorId) {
        this.invigilatorId = invigilatorId;
    }

    public int getExamId() {
        return examId;
    }

    public void setExamId(int examId) {
        this.examId = examId;
    }

    public int getHallId() {
        return hallId;
    }

    public void setHallId(int hallId) {
        this.hallId = hallId;
    }

    @Override
    public String toString() {
        return "InvigilatorAllocation{" +
                "allocationId=" + invigilatorAllocationId +
                ", invigilatorId=" + invigilatorId +
                ", examId=" + examId +
                ", hallId=" + hallId +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof InvigilatorAllocation)) return false;
        InvigilatorAllocation that = (InvigilatorAllocation) o;
        return invigilatorAllocationId == that.invigilatorAllocationId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(invigilatorAllocationId);
    }
}