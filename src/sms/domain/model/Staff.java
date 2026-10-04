package sms.domain.model;
public class Staff extends Person {
    private String department;
    
    public Staff(String id, String name, String email, String phone, String department) {
        super(id, name, email, phone);
        this.department = department;
    }
    
    @Override
    public String getRoleLabel() {
        return "STAFF";
    }
}
