package sms.domain.model;

import java.io.Serializable;

/**
 * Abstract base class of every person in the system.
 * Demonstrates ABSTRACTION + INHERITANCE: Student and Staff both extend Person.
 */
public abstract class Person implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String id;          // studentId / staffId - immutable identity
    private String name;
    private String email;
    private String phone;

    protected Person(String id, String name, String email, String phone) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.phone = phone;
    }

    /** Polymorphic hook: every concrete person kind describes its own role. */
    public abstract String getRoleLabel();

    public String getId()        { return id; }
    public String getName()      { return name; }
    public String getEmail()     { return email; }
    public String getPhone()     { return phone; }

    public void setName(String name)   { this.name = name; }
    public void setEmail(String email) { this.email = email; }
    public void setPhone(String phone) { this.phone = phone; }

    @Override
    public String toString() {
        return String.format("%s [%s] - %s", name, id, getRoleLabel());
    }
}
