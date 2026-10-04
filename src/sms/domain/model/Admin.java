package sms.domain.model;
public class Admin extends Staff {
    public Admin(String id, String name, String email, String phone, String department) {
        super(id, name, email, phone, department);
    }
    
    @Override
    public String getRoleLabel() {
        return "ADMIN";
    }
}
