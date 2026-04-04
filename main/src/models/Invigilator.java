package src.models;

public class Invigilator {

    private int invigilatorId;
    private String name;
    private String department;
    private String email;

    public Invigilator(int invigilatorId, String name, String department, String email) {
        this.invigilatorId = invigilatorId;
        this.name = name;
        this.department = department;
        this.email = email;
    }

    public int getInvigilatorId() {
        return invigilatorId;
    }

    public void setInvigilatorId(int invigilatorId) {
        this.invigilatorId = invigilatorId;
    }

    public String getName() {
        return name;
    }

    public String getDepartment() {
        return department;
    }

    public String getEmail() {
        return email;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}