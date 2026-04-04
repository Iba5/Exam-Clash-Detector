package src.models;

import java.util.Objects;

public class Hall {

    private int hallId;
    private String hallName;
    private int seatingCapacity;

    public Hall() {}

    public Hall(int hallId, String hallName, int seatingCapacity) {
        this.hallId = hallId;
        this.hallName = hallName;
        setSeatingCapacity(seatingCapacity); // use validation
    }

    public int getHallId() {
        return hallId;
    }

    public void setHallId(int hallId) {
        this.hallId = hallId;
    }

    public String getHallName() {
        return hallName;
    }

    public void setHallName(String hallName) {
        this.hallName = hallName;
    }

    public int getSeatingCapacity() {
        return seatingCapacity;
    }

    public void setSeatingCapacity(int seatingCapacity) {
        if (seatingCapacity <= 0) {
            throw new IllegalArgumentException("Seating capacity must be greater than 0");
        }
        this.seatingCapacity = seatingCapacity;
    }

    @Override
    public String toString() {
        return hallName + " (Capacity: " + seatingCapacity + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Hall)) return false;
        Hall hall = (Hall) o;
        return hallId == hall.hallId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(hallId);
    }
}